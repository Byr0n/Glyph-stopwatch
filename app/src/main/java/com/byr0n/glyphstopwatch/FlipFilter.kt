package com.byr0n.glyphstopwatch

enum class Flip { FACE_DOWN, FACE_UP }

/**
 * Debounced face down / face up detection from the accelerometer z axis.
 *
 * Face down needs z below [faceDownZ] (and the proximity sensor reporting near, when
 * there is one) for [holdMs]. Face up needs z above [faceUpZ] for [holdMs]. The gap
 * between the two thresholds stops a phone wobbling on a table from flapping.
 */
class FlipFilter(
    private val faceDownZ: Float = -9.0f,
    private val faceUpZ: Float = -6.0f,
    private val holdMs: Long = 500L,
) {
    var isFaceDown = false
        private set

    private var candidateSinceMs: Long? = null

    /** Feeds one sample. Returns a [Flip] when the debounced orientation changes. */
    fun onSample(z: Float, proximityNear: Boolean, nowMs: Long): Flip? {
        val leaning = if (isFaceDown) z > faceUpZ else z < faceDownZ && proximityNear
        if (!leaning) {
            candidateSinceMs = null
            return null
        }
        val since = candidateSinceMs ?: nowMs.also { candidateSinceMs = it }
        if (nowMs - since < holdMs) return null
        candidateSinceMs = null
        isFaceDown = !isFaceDown
        return if (isFaceDown) Flip.FACE_DOWN else Flip.FACE_UP
    }

    fun reset() {
        isFaceDown = false
        candidateSinceMs = null
    }
}
