package com.byr0n.glyphstopwatch

import kotlin.math.sqrt

/**
 * Spots a shake from raw accelerometer samples.
 *
 * A low pass filter tracks gravity; what is left over is the phone's own movement. A shake
 * is at least [minSamples] samples with movement above a threshold within [windowMs].
 * Single knocks on the table are too short to count. With `requireFaceDown`, samples only
 * count while gravity still says face down, so a phone being picked up never reads as a
 * shake. After a shake, or after [reset], nothing counts for a quiet period.
 */
class ShakeFilter(
    private val minSamples: Int = 3,
    private val windowMs: Long = 500L,
    private val cooldownMs: Long = 1_000L,
    private val faceDownGravityZ: Float = -7.0f,
) {
    private val gravity = floatArrayOf(0f, 0f, -GRAVITY)
    private val hits = ArrayDeque<Long>()
    private var quietUntilMs = 0L

    /** Returns true when this sample completes a shake. */
    fun onSample(x: Float, y: Float, z: Float, nowMs: Long, threshold: Float, requireFaceDown: Boolean): Boolean {
        val sample = floatArrayOf(x, y, z)
        var movement = 0f
        for (i in 0..2) {
            gravity[i] = ALPHA * gravity[i] + (1 - ALPHA) * sample[i]
            val linear = sample[i] - gravity[i]
            movement += linear * linear
        }
        if (nowMs < quietUntilMs || (requireFaceDown && gravity[2] > faceDownGravityZ)) {
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

    /** Ignores movement for [quietMs], e.g. so setting the phone down is not taken as a shake. */
    fun reset(nowMs: Long, quietMs: Long = cooldownMs) {
        hits.clear()
        quietUntilMs = nowMs + quietMs
    }

    private companion object {
        const val ALPHA = 0.8f
        const val GRAVITY = 9.81f
    }
}
