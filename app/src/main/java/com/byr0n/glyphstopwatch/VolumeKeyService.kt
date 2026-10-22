package com.byr0n.glyphstopwatch

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Catches volume down while the stopwatch is running or paused and the screen is on, so the
 * presses control the timer instead of the volume. Any other time the key passes through.
 * With the screen off the media session in [TimerSession] takes the presses instead.
 */
class VolumeKeyService : AccessibilityService() {
    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return false
        val session = TimerSession.current?.takeIf { it.isActive } ?: return false
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            session.onVolumeDownPressed(event.eventTime)
        }
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit
}
