package com.byr0n.glyphstopwatch

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

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
        findViewById<Button>(R.id.battery).setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
            )
        }
        findViewById<Button>(R.id.toys).setOnClickListener {
            try {
                startActivity(Intent().setComponent(TOYS_MANAGER))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, R.string.toys_unavailable, Toast.LENGTH_LONG).show()
            }
        }
        findViewById<Button>(R.id.test_pattern).setOnClickListener {
            if (testRenderer == null) {
                testRenderer = GlyphRenderer(this, asToy = false).also {
                    it.start()
                    // 12 minutes 30 seconds: half a bar.
                    it.showElapsed((12 * 60 + 30) * 1000L)
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
        val batteryExempt = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

        findViewById<Button>(R.id.arm).setText(if (TimerService.armed) R.string.disarm else R.string.arm)
        findViewById<Button>(R.id.test_pattern)
            .setText(if (testRenderer == null) R.string.test_pattern else R.string.test_pattern_off)
        findViewById<TextView>(R.id.status).text = getString(
            R.string.status,
            getString(if (TimerService.armed) R.string.yes else R.string.no),
            getString(if (batteryExempt) R.string.yes else R.string.no),
            packageManager.getPackageInfo(packageName, 0).let { "${it.versionName} (build ${it.longVersionCode})" },
        )
    }

    private companion object {
        // Nothing's Manage Glyph Toys screen, where the user adds the toy to their list.
        val TOYS_MANAGER = ComponentName(
            "com.nothing.thirdparty", "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity"
        )
    }
}
