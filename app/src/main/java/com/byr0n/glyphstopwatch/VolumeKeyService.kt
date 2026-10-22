package com.byr0n.glyphstopwatch

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Catches volume down while the timer is running or paused, so the presses control the
 * timer instead of the volume. Any other time the key passes through untouched.
 */
class VolumeKeyService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val classifier = PressClassifier()
    private val windowElapsed = Runnable {
        classifier.onWindowElapsed(SystemClock.uptimeMillis())?.let(::dispatch)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return false
        if (TimerSession.current?.isActive != true) {
            handler.removeCallbacks(windowElapsed)
            classifier.reset()
            return false
        }
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            handler.removeCallbacks(windowElapsed)
            val press = classifier.onPress(event.eventTime)
            if (press != null) dispatch(press) else handler.postDelayed(windowElapsed, classifier.windowMs)
        }
        return true
    }

    private fun dispatch(press: Press) {
        TimerSession.current?.onVolumeDown(press)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacks(windowElapsed)
        super.onDestroy()
    }
}
