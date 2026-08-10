package com.example.autoclick

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.core.app.NotificationCompat

/**
 * Menampilkan tombol Start/Stop mengambang di atas app lain (mis. game),
 * dan mengatur ScreenCaptureManager + AutoClickController.
 */
class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private var screenCaptureManager: ScreenCaptureManager? = null
    private var controller: AutoClickController? = null
    private var statusLabel: android.widget.TextView? = null

    companion object {
        const val CHANNEL_ID = "autoclick_channel"
        const val NOTIF_ID = 1

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA = "extra_data"
        const val EXTRA_CLICK_X = "extra_click_x"
        const val EXTRA_CLICK_Y = "extra_click_y"
        const val EXTRA_INTERVAL_MS = "extra_interval_ms"
        const val EXTRA_MODE = "extra_mode" // "TEXT" | "IMAGE" | "NONE"
        const val EXTRA_KEYWORDS = "extra_keywords" // dipisah koma, misal "PERINGATAN,Warning"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: -1
        val data = intent?.getParcelableExtra<Intent>(EXTRA_DATA)
        val clickX = intent?.getFloatExtra(EXTRA_CLICK_X, 0f) ?: 0f
        val clickY = intent?.getFloatExtra(EXTRA_CLICK_Y, 0f) ?: 0f
        val intervalMs = intent?.getLongExtra(EXTRA_INTERVAL_MS, 1000L) ?: 1000L
        val modeStr = intent?.getStringExtra(EXTRA_MODE) ?: "NONE"
        val keywordsRaw = intent?.getStringExtra(EXTRA_KEYWORDS) ?: ""
        val keywords = keywordsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        if (data != null && resultCode != -1) {
            setupCaptureAndController(
                resultCode, data, clickX, clickY, intervalMs, modeStr, keywords
            )
        }

        showOverlayButtons()
        return START_STICKY
    }

    private fun setupCaptureAndController(
        resultCode: Int,
        data: Intent,
        clickX: Float,
        clickY: Float,
        intervalMs: Long,
        modeStr: String,
        keywords: List<String>
    ) {
        val mode = when (modeStr) {
            "TEXT" -> AutoClickController.DetectionMode.TEXT
            "IMAGE" -> AutoClickController.DetectionMode.IMAGE
            else -> AutoClickController.DetectionMode.NONE
        }

        // TODO: kalau mode IMAGE, load targetTemplate bitmap dari file
        // yang sebelumnya disimpan user (misal hasil crop di MainActivity).
        val targetTemplate: Bitmap? = null

        controller = AutoClickController(
            clickX = clickX,
            clickY = clickY,
            clickIntervalMs = intervalMs,
            detectionMode = mode,
            targetKeywords = keywords,
            targetTemplate = targetTemplate,
            onPaused = { reason ->
                updateStatus("⏸ JEDA — $reason")
                updateNotification("Jeda: popup terdeteksi")
            },
            onResumed = {
                updateStatus("▶ Auto-click berjalan")
                updateNotification("Auto-click sedang berjalan di background")
            },
            onStopped = { reason ->
                updateStatus("⏹ Berhenti")
                Toast.makeText(this, "Auto-click berhenti: $reason", Toast.LENGTH_LONG).show()
            }
        )

        screenCaptureManager = ScreenCaptureManager(this, resultCode, data)
        screenCaptureManager?.start { bitmap ->
            controller?.onNewFrame(bitmap)
        }

        controller?.start()
    }

    private fun showOverlayButtons() {
        if (::overlayView.isInitialized) return // sudah ditampilkan

        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_controls, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 200

        // Supaya tombol bisa di-drag
        overlayView.setOnTouchListener(object : View.OnTouchListener {
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(overlayView, params)
                    }
                }
                return false
            }
        })

        overlayView.findViewById<Button>(R.id.btnStop).setOnClickListener {
            controller?.stop("Dihentikan manual dari overlay")
            stopSelf()
        }

        statusLabel = overlayView.findViewById(R.id.tvStatus)
        statusLabel?.text = "▶ Auto-click berjalan"

        windowManager.addView(overlayView, params)
    }

    private fun updateStatus(text: String) {
        statusLabel?.post { statusLabel?.text = text }
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AutoClick")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
        manager.notify(NOTIF_ID, notification)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AutoClick aktif")
            .setContentText("Auto-click sedang berjalan di background")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "AutoClick Service", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        controller?.stop("Service dihentikan")
        screenCaptureManager?.stop()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}
