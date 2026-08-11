package com.example.autoclick

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button

class PointPickerOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var params: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showPicker()
    }

    private fun showPicker() {
        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.point_picker_overlay, null)

        params = WindowManager.LayoutParams(
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
        params.x = 300
        params.y = 600

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

        overlayView.findViewById<Button>(R.id.btnConfirmPoint).setOnClickListener {
            confirmPoint()
        }

        overlayView.findViewById<Button>(R.id.btnCancelPoint).setOnClickListener {
            stopSelf()
        }

        windowManager.addView(overlayView, params)
    }

    private fun confirmPoint() {
        val crosshair = overlayView.findViewById<View>(R.id.crosshair)
        val loc = IntArray(2)
        crosshair.getLocationOnScreen(loc)
        val clickX = loc[0] + crosshair.width / 2f
        val clickY = loc[1] + crosshair.height / 2f

        val prefs = getSharedPreferences("autoclick_prefs", MODE_PRIVATE)
        prefs.edit()
            .putFloat("picked_x", clickX)
            .putFloat("picked_y", clickY)
            .putBoolean("picked_ready", true)
            .apply()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        startActivity(intent)

        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}
