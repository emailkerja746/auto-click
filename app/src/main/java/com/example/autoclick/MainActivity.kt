package com.example.autoclick

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {

    private lateinit var etClickX: EditText
    private lateinit var etClickY: EditText
    private lateinit var etInterval: EditText
    private lateinit var etKeyword: EditText

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            launchOverlayService(result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, "Izin screen capture ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etClickX = findViewById(R.id.etClickX)
        etClickY = findViewById(R.id.etClickY)
        etInterval = findViewById(R.id.etInterval)
        etKeyword = findViewById(R.id.etKeyword)

        findViewById<android.widget.Button>(R.id.btnEnableAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<android.widget.Button>(R.id.btnEnableOverlay).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Izin overlay sudah aktif", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<android.widget.Button>(R.id.btnStart).setOnClickListener {
            if (ClickAccessibilityService.instance == null) {
                Toast.makeText(
                    this,
                    "Aktifkan Accessibility Service dulu (langkah 1)",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(
                    this,
                    "Izinkan overlay dulu (langkah 2)",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val projectionManager =
                getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            screenCaptureLauncher.launch(projectionManager.createScreenCaptureIntent())
        }
    }

    private fun launchOverlayService(resultCode: Int, data: Intent) {
        val x = etClickX.text.toString().toFloatOrNull() ?: 0f
        val y = etClickY.text.toString().toFloatOrNull() ?: 0f
        val interval = etInterval.text.toString().toLongOrNull() ?: 1000L
        val keywordsRaw = etKeyword.text.toString().trim()
        val mode = if (keywordsRaw.isNotEmpty()) "TEXT" else "NONE"

        val serviceIntent = Intent(this, OverlayService::class.java).apply {
            putExtra(OverlayService.EXTRA_RESULT_CODE, resultCode)
            putExtra(OverlayService.EXTRA_DATA, data)
            putExtra(OverlayService.EXTRA_CLICK_X, x)
            putExtra(OverlayService.EXTRA_CLICK_Y, y)
            putExtra(OverlayService.EXTRA_INTERVAL_MS, interval)
            putExtra(OverlayService.EXTRA_MODE, mode)
            putExtra(OverlayService.EXTRA_KEYWORDS, keywordsRaw)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        Toast.makeText(this, "AutoClick dimulai", Toast.LENGTH_SHORT).show()
        moveTaskToBack(true)
    }
}
