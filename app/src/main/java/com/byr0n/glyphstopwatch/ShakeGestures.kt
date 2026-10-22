package com.byr0n.glyphstopwatch

/**
 * Turns accelerometer samples into "one shake" or "two shakes". Shakes are counted until the
 * phone has been still for [settleMs], so it does not matter how long each shake lasts. A
 * second shake is reported straight away; a single one once the phone has settled.
 */
class ShakeGestures(
    private val filter: ShakeFilter = ShakeFilter(),
    private val settleMs: Long = 800L,
) {
    private var count = 0

    /** Returns 1 or 2 when a gesture is complete, otherwise null. */
    fun onSample(x: Float, y: Float, z: Float, nowMs: Long, threshold: Float, requireFaceDown: Boolean): Int? {
        if (filter.onSample(x, y, z, nowMs, threshold, requireFaceDown)) count++
        if (count >= 2) {
            count = 0
            return 2
        }
        val lastMovement = filter.lastMovementMs ?: return null
        if (count == 1 && nowMs - lastMovement >= settleMs) {
            count = 0
            return 1
        }
        return null
    }

    /** Forgets any shake in progress and ignores movement for [quietMs]. */
    fun reset(nowMs: Long, quietMs: Long = 1_000L) {
        count = 0
        filter.reset(nowMs, quietMs)
    }
}
