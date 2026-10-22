package com.byr0n.glyphstopwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ShakeFilterTest {
    private val filter = ShakeFilter()

    // 50 Hz samples, like SENSOR_DELAY_GAME. Settles gravity face down first.
    private var t = 0L

    private fun lieFaceDown(ms: Long) {
        repeat((ms / 20).toInt()) { filter.onSample(0f, 0f, -9.81f, t, 4f, requireFaceDown = true); t += 20 }
    }

    private fun jiggle(ms: Long, amplitude: Float, baseZ: Float = -9.81f, inHand: Boolean = false): Int {
        var shakes = 0
        var sign = 1
        repeat((ms / 20).toInt()) {
            if (filter.onSample(amplitude * sign, 0f, baseZ, t, if (inHand) 10f else 4f, requireFaceDown = !inHand)) shakes++
            sign = -sign
            t += 20
        }
        return shakes
    }

    @Test
    fun jiggleWhileFaceDownIsOneShake() {
        lieFaceDown(2_000)
        assertEquals(1, jiggle(400, 8f))
    }

    @Test
    fun lyingStillIsNotAShake() {
        lieFaceDown(5_000)
        assertFalse(filter.onSample(0f, 0f, -9.81f, t, 4f, requireFaceDown = true))
    }

    @Test
    fun singleKnockIsNotAShake() {
        lieFaceDown(2_000)
        assertFalse(filter.onSample(0f, 0f, -20f, t, 4f, requireFaceDown = true))
        t += 20
        assertEquals(0, jiggle(1_000, 0f))
    }

    @Test
    fun shakingFaceUpDoesNotCount() {
        repeat(100) { filter.onSample(0f, 0f, 9.81f, t, 4f, requireFaceDown = true); t += 20 }
        assertEquals(0, jiggle(1_000, 8f, baseZ = 9.81f))
    }

    @Test
    fun firmShakeInTheHandCounts() {
        repeat(100) { filter.onSample(0f, 0f, 9.81f, t, 10f, requireFaceDown = false); t += 20 }
        assertEquals(0, jiggle(1_000, 4f, baseZ = 9.81f, inHand = true))
        assertEquals(1, jiggle(400, 15f, baseZ = 9.81f, inHand = true))
    }

    @Test
    fun cooldownSeparatesShakes() {
        lieFaceDown(2_000)
        assertEquals(1, jiggle(800, 8f))
        lieFaceDown(1_000)
        assertEquals(1, jiggle(400, 8f))
    }

    @Test
    fun resetIgnoresSettingThePhoneDown() {
        filter.reset(t)
        assertEquals(0, jiggle(900, 8f))
        assertEquals(1, jiggle(400, 8f))
    }
}
