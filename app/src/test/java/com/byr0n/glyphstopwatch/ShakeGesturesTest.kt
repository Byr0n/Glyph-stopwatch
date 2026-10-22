package com.byr0n.glyphstopwatch

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Test

/** Realistic shakes at 25 Hz: a 4 Hz back and forth, in the hand or jiggled on the table. */
class ShakeGesturesTest {
    private val gestures = ShakeGestures()
    private var t = 0L
    private val results = mutableListOf<Int>()

    private fun sample(x: Float, gz: Float, threshold: Float, faceDown: Boolean) {
        gestures.onSample(x, 0f, gz, t, threshold, faceDown)?.let { results += it }
        t += 40
    }

    private fun still(ms: Long, inHand: Boolean) {
        val end = t + ms
        while (t < end) sample(0.5f, if (inHand) 9.81f else -9.81f, if (inHand) 10f else 4f, !inHand)
    }

    private fun shake(ms: Long, inHand: Boolean) {
        val start = t
        val amp = if (inHand) 20f else 8f
        while (t < start + ms) {
            val x = amp * sin(2 * PI * 4 * (t - start) / 1000).toFloat()
            sample(x, if (inHand) 9.81f else -9.81f, if (inHand) 10f else 4f, !inHand)
        }
    }

    private fun begin(inHand: Boolean) {
        still(2_000, inHand)
        gestures.reset(t, 0)
        results.clear()
    }

    @Test
    fun doubleShakesOfAnyLengthAreTwo() {
        for (inHand in listOf(true, false)) for (shakeMs in listOf(300L, 700L, 1_200L)) for (pauseMs in listOf(250L, 500L, 700L)) {
            begin(inHand)
            shake(shakeMs, inHand)
            still(pauseMs, inHand)
            shake(shakeMs, inHand)
            still(2_000, inHand)
            assertEquals("hand=$inHand shake=$shakeMs pause=$pauseMs", listOf(2), results)
        }
    }

    @Test
    fun singleShakesOfAnyLengthAreOne() {
        for (inHand in listOf(true, false)) for (shakeMs in listOf(300L, 1_000L, 2_500L)) {
            begin(inHand)
            shake(shakeMs, inHand)
            still(2_000, inHand)
            assertEquals("hand=$inHand shake=$shakeMs", listOf(1), results)
        }
    }

    @Test
    fun singleShakeIsReportedSoonAfterSettling() {
        begin(inHand = true)
        shake(500, inHand = true)
        val stoppedAt = t
        still(2_000, inHand = true)
        assertEquals(listOf(1), results)
        // Reported 0.8 s after the last movement, not 1.5 s after the shake began.
        assertEquals(true, t - stoppedAt > 800)
    }

    @Test
    fun twoSeparateShakesFarApartAreTwoSingles() {
        begin(inHand = false)
        shake(400, inHand = false)
        still(1_500, inHand = false)
        shake(400, inHand = false)
        still(1_500, inHand = false)
        assertEquals(listOf(1, 1), results)
    }
}
