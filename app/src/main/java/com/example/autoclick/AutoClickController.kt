package com.example.autoclick

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper

/**
 * Menyatukan: klik berulang di titik (x, y) dengan interval tertentu.
 * Setiap beberapa frame, mengecek apakah ada popup/peringatan (kata kunci) muncul di layar.
 *
 * - Kalau kata kunci TERDETEKSI -> klik otomatis PAUSE (berhenti sementara).
 * - Kalau kata kunci SUDAH TIDAK ADA lagi (popup ditutup) -> klik otomatis LANJUT lagi.
 *
 * Ini beda dengan "stop total" -- tujuannya supaya popup peringatan apapun
 * (isinya bisa beda-beda) tidak ke-klik asal-asalan, tapi auto-click tetap
 * jalan lagi begitu popup sudah hilang.
 */
class AutoClickController(
    private val clickX: Float,
    private val clickY: Float,
    private val clickIntervalMs: Long,
    private val detectionMode: DetectionMode,
    private val targetKeywords: List<String> = emptyList(), // dipakai kalau mode TEXT, cek OR
    private val targetTemplate: Bitmap? = null,      // dipakai kalau mode IMAGE (opsional, tidak wajib)
    private val onPaused: (reason: String) -> Unit = {},
    private val onResumed: () -> Unit = {},
    private val onStopped: (reason: String) -> Unit = {}
) {
    enum class DetectionMode { TEXT, IMAGE, NONE }

    private val handler = Handler(Looper.getMainLooper())

    private var isRunning = false   // service auto-click aktif secara keseluruhan
    private var isPaused = false    // sedang jeda karena popup terdeteksi
    private var checkingInProgress = false

    private val clickRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            if (!isPaused) {
                ClickAccessibilityService.instance?.performClick(clickX, clickY)
            }
            handler.postDelayed(this, clickIntervalMs)
        }
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        isPaused = false
        handler.post(clickRunnable)
    }

    /** Berhenti total (bukan jeda). Dipanggil misalnya saat user tekan STOP manual. */
    fun stop(reason: String = "Dihentikan manual") {
        if (!isRunning) return
        isRunning = false
        isPaused = false
        handler.removeCallbacks(clickRunnable)
        onStopped(reason)
    }

    private fun pause(reason: String) {
        if (isPaused) return
        isPaused = true
        onPaused(reason)
    }

    private fun resume() {
        if (!isPaused) return
        isPaused = false
        onResumed()
    }

    /**
     * Panggil fungsi ini setiap kali dapat frame baru dari ScreenCaptureManager.
     * Fungsi ini yang memutuskan kapan klik di-pause / dilanjutkan lagi.
     */
    fun onNewFrame(bitmap: Bitmap) {
        if (!isRunning || checkingInProgress) return

        when (detectionMode) {
            DetectionMode.TEXT -> {
                if (targetKeywords.isEmpty()) return
                checkingInProgress = true
                TextDetector.detectAnyKeyword(bitmap, targetKeywords) { found, matched ->
                    checkingInProgress = false
                    if (found) {
                        pause("Popup terdeteksi (kata kunci: \"$matched\")")
                    } else {
                        resume()
                    }
                }
            }
            DetectionMode.IMAGE -> {
                val template = targetTemplate ?: return
                checkingInProgress = true
                // Jalankan di background thread supaya tidak nge-block UI
                Thread {
                    val match = ImageMatcher.findTemplate(bitmap, template)
                    checkingInProgress = false
                    handler.post {
                        if (match != null) {
                            pause("Gambar target ditemukan di posisi $match")
                        } else {
                            resume()
                        }
                    }
                }.start()
            }
            DetectionMode.NONE -> {
                // Auto-click biasa tanpa deteksi, tidak pernah pause otomatis
            }
        }
    }
}
