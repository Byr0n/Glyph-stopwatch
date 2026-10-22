package com.byr0n.glyphstopwatch

import kotlin.math.sqrt

/**
 * Spots a shake from raw accelerometer samples.
 *
 * A low pass filter tracks gravity; what is left over is the phone's own movement. A shake
 * is at least [minSamples] samples with movement above a threshold within [windowMs].
 * Single knocks on the table are too short to count. With `requireFaceDown`, samples only
 * count while gravity still says face down, so a phone being picked up never reads as a
 * shake. After a shake, the movement has to stay below the threshold for [calmMs] before
 * the next one can count, so one long shake is one shake and shake, stop, shake is two.
 * After [reset], nothing counts for a quiet period.
 */
class ShakeFilter(
    private val minSamples: Int = 3,
    private val windowMs: Long = 500L,
    private val calmMs: Long = 200L,
    private val faceDownGravityZ: Float = -7.0f,
) {
    private val gravity = FloatArray(3)
    private var hasGravity = false
    private val hits = ArrayDeque<Long>()
    private var quietUntilMs = 0L
    private var awaitingCalm = false

    /** When movement was last above the threshold, or null if it has not been yet. */
    var lastMovementMs: Long? = null
        private set

    /** Returns true when this sample completes a shake. */
    fun onSample(x: Float, y: Float, z: Float, nowMs: Long, threshold: Float, requireFaceDown: Boolean): Boolean {
        val sample = floatArrayOf(x, y, z)
        if (!hasGravity) {
            // Start from the first reading rather than guessing which way up the phone is.
            sample.copyInto(gravity)
            hasGravity = true
        }
        var movement = 0f
        for (i in 0..2) {
            gravity[i] = ALPHA * gravity[i] + (1 - ALPHA) * sample[i]
            val linear = sample[i] - gravity[i]
            movement += linear * linear
        }
        val moving = sqrt(movement) > threshold
        if (moving) lastMovementMs = nowMs
        if (awaitingCalm) {
            // Still settling after the last shake.
            if (nowMs - (lastMovementMs ?: nowMs) >= calmMs) awaitingCalm = false
            return false
        }
        if (nowMs < quietUntilMs || (requireFaceDown && gravity[2] > faceDownGravityZ)) {
            hits.clear()
            return false
        }
        if (moving) hits.addLast(nowMs)
        while (hits.isNotEmpty() && nowMs - hits.first() > windowMs) hits.removeFirst()
        if (hits.size < minSamples) return false
        hits.clear()
        awaitingCalm = true
        return true
    }

    /** Ignores movement for [quietMs], e.g. so setting the phone down is not taken as a shake. */
    fun reset(nowMs: Long, quietMs: Long = 1_000L) {
        hits.clear()
        awaitingCalm = false
        lastMovementMs = null
        quietUntilMs = nowMs + quietMs
    }

    private companion object {
        const val ALPHA = 0.8f
    }
}
