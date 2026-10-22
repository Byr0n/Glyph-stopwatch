package com.byr0n.glyphstopwatch

import kotlin.math.sqrt

/**
 * Spots a shake from raw accelerometer samples while the phone lies face down.
 *
 * A low pass filter tracks gravity; what is left over is the phone's own movement. A shake
 * is at least [minSamples] samples with movement above [threshold] within [windowMs], while
 * gravity still says face down. Single knocks on the table are too short to count, and a
 * phone being picked up turns over, so neither reads as a shake. After a shake, or after
 * [reset], nothing counts for [cooldownMs].
 */
class ShakeFilter(
    private val threshold: Float = 4.0f,
    private val minSamples: Int = 3,
    private val windowMs: Long = 500L,
    private val cooldownMs: Long = 1_000L,
    private val faceDownGravityZ: Float = -7.0f,
) {
    private val gravity = floatArrayOf(0f, 0f, -GRAVITY)
    private val hits = ArrayDeque<Long>()
    private var quietUntilMs = 0L

    /** Returns true when this sample completes a shake. */
    fun onSample(x: Float, y: Float, z: Float, nowMs: Long): Boolean {
        val sample = floatArrayOf(x, y, z)
        var movement = 0f
        for (i in 0..2) {
            gravity[i] = ALPHA * gravity[i] + (1 - ALPHA) * sample[i]
            val linear = sample[i] - gravity[i]
            movement += linear * linear
        }
        if (nowMs < quietUntilMs || gravity[2] > faceDownGravityZ) {
            hits.clear()
            return false
        }
        if (sqrt(movement) > threshold) hits.addLast(nowMs)
        while (hits.isNotEmpty() && nowMs - hits.first() > windowMs) hits.removeFirst()
        if (hits.size < minSamples) return false
        hits.clear()
        quietUntilMs = nowMs + cooldownMs
        return true
    }

    /** Starts a cooldown, e.g. so setting the phone down is not taken as a shake. */
    fun reset(nowMs: Long) {
        hits.clear()
        gravity[0] = 0f
        gravity[1] = 0f
        gravity[2] = -GRAVITY
        quietUntilMs = nowMs + cooldownMs
    }

    private companion object {
        const val ALPHA = 0.8f
        const val GRAVITY = 9.81f
    }
}
