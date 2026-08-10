package com.example.autoclick

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

/**
 * Service ini bertugas HANYA untuk mengeksekusi klik di koordinat tertentu.
 * Logika kapan harus klik, kapan harus berhenti (karena target ketemu)
 * diatur dari OverlayService / AutoClickController.
 */
class ClickAccessibilityService : AccessibilityService() {

    companion object {
        // Supaya service lain (OverlayService) bisa memanggil fungsi klik
        var instance: ClickAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Tidak dipakai untuk auto-click berbasis koordinat.
        // Kalau nanti mau deteksi via UI node (misal cari teks tombol
        // lewat AccessibilityNodeInfo bukan lewat screenshot), logikanya
        // ditaruh di sini.
    }

    override fun onInterrupt() {}

    /**
     * Simulasikan satu kali tap di koordinat (x, y).
     */
    fun performClick(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()

        dispatchGesture(gesture, null, null)
    }
}
