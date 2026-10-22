package com.byr0n.glyphstopwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FlipFilterTest {
    private val filter = FlipFilter(faceDownZ = -9.0f, faceUpZ = -6.0f, holdMs = 500)

    private fun feed(z: Float, from: Long, to: Long, near: Boolean = true): Flip? {
        var result: Flip? = null
        for (t in from..to step 50) filter.onSample(z, near, t)?.let { result = it }
        return result
    }

    @Test
    fun faceDownNeedsHoldTime() {
        assertNull(feed(-9.7f, 0, 450))
        assertEquals(Flip.FACE_DOWN, feed(-9.7f, 500, 500))
    }

    @Test
    fun wobbleResetsTheHold() {
        feed(-9.7f, 0, 400)
        filter.onSample(-8.0f, true, 450)
        assertNull(feed(-9.7f, 500, 900))
        assertEquals(Flip.FACE_DOWN, feed(-9.7f, 950, 1_000))
    }

    @Test
    fun proximityFarBlocksFaceDown() {
        assertNull(feed(-9.7f, 0, 2_000, near = false))
    }

    @Test
    fun smallTiltWhileFaceDownDoesNotFlipUp() {
        feed(-9.7f, 0, 500)
        assertNull(feed(-7.0f, 600, 3_000))
    }

    @Test
    fun pickingUpFlipsFaceUp() {
        feed(-9.7f, 0, 500)
        assertNull(feed(2.0f, 600, 1_050))
        assertEquals(Flip.FACE_UP, feed(2.0f, 1_100, 1_100))
    }
}
