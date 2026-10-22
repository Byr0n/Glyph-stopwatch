package com.byr0n.glyphstopwatch

/**
 * Counts shakes that come close together. The caller reports each shake with [onShake];
 * when that returns null it calls [onWindowElapsed] after [windowMs].
 */
class ShakeCounter(val windowMs: Long = 1_500L) {
    private var firstShakeMs: Long? = null

    /** Returns 2 when this shake is the second within [windowMs], otherwise null. */
    fun onShake(nowMs: Long): Int? {
        val first = firstShakeMs
        if (first != null && nowMs - first <= windowMs) {
            firstShakeMs = null
            return 2
        }
        firstShakeMs = nowMs
        return null
    }

    /** Returns 1 when a shake was not followed by a second one in time, otherwise null. */
    fun onWindowElapsed(nowMs: Long): Int? {
        val first = firstShakeMs ?: return null
        if (nowMs - first < windowMs) return null
        firstShakeMs = null
        return 1
    }

    fun reset() {
        firstShakeMs = null
    }
}
