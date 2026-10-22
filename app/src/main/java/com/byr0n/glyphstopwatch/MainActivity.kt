package com.byr0n.glyphstopwatch

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.TextView

/** Setup screen: arms the timer and walks through the permissions it needs. */
class MainActivity : Activity() {
    private var testRenderer: GlyphRenderer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.arm).setOnClickListener {
            if (TimerService.armed) {
                TimerService.disarm(this)
            } else {
                stopTestPattern()
                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
                }
                TimerService.arm(this)
            }
            it.postDelayed(::refresh, 300)
        }
        findViewById<Button>(R.id.accessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.battery).setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
            )
        }
        findViewById<Button>(R.id.test_pattern).setOnClickListener {
            if (testRenderer == null) {
                testRenderer = GlyphRenderer(this).also {
                    it.start()
                    it.showPair(12, 34)
                }
            } else {
                stopTestPattern()
            }
            refresh()
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroy() {
        stopTestPattern()
        super.onDestroy()
    }

    private fun stopTestPattern() {
        testRenderer?.release()
        testRenderer = null
    }

    private fun refresh() {
        val accessibilityOn = isAccessibilityServiceEnabled()
        val batteryExempt = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

        findViewById<Button>(R.id.arm).setText(if (TimerService.armed) R.string.disarm else R.string.arm)
        findViewById<Button>(R.id.test_pattern)
            .setText(if (testRenderer == null) R.string.test_pattern else R.string.test_pattern_off)
        findViewById<TextView>(R.id.status).text = getString(
            R.string.status,
            getString(if (TimerService.armed) R.string.yes else R.string.no),
            getString(if (accessibilityOn) R.string.yes else R.string.no),
            getString(if (batteryExempt) R.string.yes else R.string.no),
        )
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        val ours = ComponentName(this, VolumeKeyService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == ours }
    }
}
