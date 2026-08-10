package com.example.autoclick

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Deteksi apakah teks tertentu (keyword) muncul di dalam screenshot layar,
 * menggunakan ML Kit Text Recognition (on-device, gratis, tanpa internet).
 */
object TextDetector {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * [onResult] mengembalikan true jika [keyword] ditemukan di layar.
     */
    fun detectKeyword(bitmap: Bitmap, keyword: String, onResult: (Boolean) -> Unit) {
        detectAnyKeyword(bitmap, listOf(keyword)) { found, _ -> onResult(found) }
    }

    /**
     * Cek apakah SALAH SATU dari [keywords] muncul di layar (kondisi OR).
     * Berguna untuk kasus seperti popup "PERINGATAN" yang isinya bisa beda-beda
     * (Nightfall Paket, dsb) tapi judulnya selalu sama, misalnya "PERINGATAN".
     *
     * [onResult] mengembalikan (found, matchedKeyword) — matchedKeyword null kalau tidak ada yang cocok.
     */
    fun detectAnyKeyword(
        bitmap: Bitmap,
        keywords: List<String>,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (keywords.isEmpty()) {
            onResult(false, null)
            return
        }
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val screenText = visionText.text
                val matched = keywords.firstOrNull { kw ->
                    kw.isNotBlank() && screenText.contains(kw.trim(), ignoreCase = true)
                }
                onResult(matched != null, matched)
            }
            .addOnFailureListener {
                onResult(false, null)
            }
    }
}
