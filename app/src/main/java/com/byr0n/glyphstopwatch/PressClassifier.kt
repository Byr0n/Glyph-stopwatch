package com.byr0n.glyphstopwatch

enum class Press { SINGLE, DOUBLE }

/**
 * Tells single presses from double presses. The caller reports each key down with
 * [onPress]; when that returns null it schedules [onWindowElapsed] after [windowMs].
 */
class PressClassifier(val windowMs: Long = 400L) {
    private var firstPressMs: Long? = null

    /** Returns [Press.DOUBLE] when this press completes a double press, otherwise null. */
    fun onPress(nowMs: Long): Press? {
        val first = firstPressMs
        if (first != null && nowMs - first <= windowMs) {
            firstPressMs = null
            return Press.DOUBLE
        }
        firstPressMs = nowMs
        return null
    }

    /** Returns [Press.SINGLE] when a pending press was not followed by a second one in time. */
    fun onWindowElapsed(nowMs: Long): Press? {
        val first = firstPressMs ?: return null
        if (nowMs - first < windowMs) return null
        firstPressMs = null
        return Press.SINGLE
    }

    fun reset() {
        firstPressMs = null
    }
}
