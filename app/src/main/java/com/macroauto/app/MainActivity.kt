package com.macroauto.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)

        findViewById<Button>(R.id.btnOverlayPerm).setOnClickListener { requestOverlay() }
        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.btnStart).setOnClickListener { startOverlay() }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun canOverlay(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)

    private fun accessibilityOn(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.id.contains(packageName) }
    }

    private fun refreshStatus() {
        status.text = "Overlay: ${if (canOverlay()) "OK" else "CHƯA"}\n" +
                "Accessibility: ${if (accessibilityOn()) "OK" else "CHƯA"}\n\n" +
                "1) Cấp quyền overlay  2) Bật Accessibility  3) Bật nút nổi"
    }

    private fun requestOverlay() {
        if (canOverlay()) { Toast.makeText(this, "Đã có quyền overlay", Toast.LENGTH_SHORT).show(); return }
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }

    private fun startOverlay() {
        if (!canOverlay()) { requestOverlay(); return }
        startForegroundService(Intent(this, OverlayService::class.java))
        Toast.makeText(this, "Đã bật nút nổi điều khiển", Toast.LENGTH_SHORT).show()
    }
}
