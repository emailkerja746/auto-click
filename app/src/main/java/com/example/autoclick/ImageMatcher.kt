package com.example.autoclick

import android.graphics.Bitmap

/**
 * Template matching sederhana tanpa dependency eksternal (tanpa OpenCV).
 * Cocok untuk target gambar kecil (misal ikon/tombol) dicari di dalam
 * screenshot layar penuh.
 *
 * Untuk performa lebih baik & akurasi lebih tinggi pada project nyata,
 * disarankan pakai OpenCV (Imgproc.matchTemplate). Kelas ini adalah
 * fallback ringan yang jalan tanpa setup tambahan.
 */
object ImageMatcher {

    /**
     * Cari apakah [template] muncul di dalam [screen].
     * threshold 0.0 - 1.0 (semakin tinggi = semakin ketat kecocokannya).
     * Mengembalikan koordinat (x, y) titik tengah kecocokan jika ditemukan, atau null.
     */
    fun findTemplate(
        screen: Bitmap,
        template: Bitmap,
        threshold: Double = 0.9,
        step: Int = 4 // loncat beberapa pixel biar lebih cepat, trade-off akurasi
    ): Pair<Int, Int>? {
        val sw = screen.width
        val sh = screen.height
        val tw = template.width
        val th = template.height

        if (tw > sw || th > sh) return null

        // Downsample sample points di dalam template supaya perbandingan cepat
        val samplePoints = buildSamplePoints(tw, th, maxSamples = 64)

        var bestScore = 0.0
        var bestX = -1
        var bestY = -1

        var y = 0
        while (y <= sh - th) {
            var x = 0
            while (x <= sw - tw) {
                val score = compareAt(screen, template, x, y, samplePoints)
                if (score > bestScore) {
                    bestScore = score
                    bestX = x
                    bestY = y
                }
                x += step
            }
            y += step
        }

        return if (bestScore >= threshold) {
            Pair(bestX + tw / 2, bestY + th / 2)
        } else null
    }

    private fun buildSamplePoints(tw: Int, th: Int, maxSamples: Int): List<Pair<Int, Int>> {
        val points = mutableListOf<Pair<Int, Int>>()
        val gridSize = kotlin.math.sqrt(maxSamples.toDouble()).toInt().coerceAtLeast(1)
        val stepX = (tw / gridSize).coerceAtLeast(1)
        val stepY = (th / gridSize).coerceAtLeast(1)

        var y = 0
        while (y < th) {
            var x = 0
            while (x < tw) {
                points.add(Pair(x, y))
                x += stepX
            }
            y += stepY
        }
        return points
    }

    private fun compareAt(
        screen: Bitmap,
        template: Bitmap,
        offsetX: Int,
        offsetY: Int,
        samplePoints: List<Pair<Int, Int>>
    ): Double {
        var matches = 0
        for ((tx, ty) in samplePoints) {
            val sPixel = screen.getPixel(offsetX + tx, offsetY + ty)
            val tPixel = template.getPixel(tx, ty)
            if (colorsClose(sPixel, tPixel)) matches++
        }
        return matches.toDouble() / samplePoints.size
    }

    private fun colorsClose(a: Int, b: Int, tolerance: Int = 24): Boolean {
        val rDiff = kotlin.math.abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF))
        val gDiff = kotlin.math.abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF))
        val bDiff = kotlin.math.abs((a and 0xFF) - (b and 0xFF))
        return rDiff <= tolerance && gDiff <= tolerance && bDiff <= tolerance
    }
}
