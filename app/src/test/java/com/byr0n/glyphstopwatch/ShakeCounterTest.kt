package com.byr0n.glyphstopwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShakeCounterTest {
    private val counter = ShakeCounter(windowMs = 1_500)

    @Test
    fun loneShakeIsOneAfterTheWindow() {
        assertNull(counter.onShake(0))
        assertNull(counter.onWindowElapsed(1_000))
        assertEquals(1, counter.onWindowElapsed(1_500))
        assertNull(counter.onWindowElapsed(2_000))
    }

    @Test
    fun twoShakesInsideTheWindowAreTwo() {
        assertNull(counter.onShake(0))
        assertEquals(2, counter.onShake(1_100))
        assertNull(counter.onWindowElapsed(1_500))
    }

    @Test
    fun secondShakeAfterTheWindowStartsAgain() {
        counter.onShake(0)
        assertNull(counter.onShake(2_000))
        assertEquals(1, counter.onWindowElapsed(3_500))
    }
}
