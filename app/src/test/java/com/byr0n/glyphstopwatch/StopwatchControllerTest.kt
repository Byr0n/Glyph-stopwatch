package com.byr0n.glyphstopwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StopwatchControllerTest {
    private val c = StopwatchController(graceMs = 3_000)

    @Test
    fun faceDownStarts() {
        c.onFaceDown(0)
        assertEquals(TimerState.RUNNING, c.state)
        assertEquals(5_000, c.elapsed(5_000))
    }

    @Test
    fun shakeFaceDownPausesThenRestarts() {
        c.onFaceDown(0)
        assertEquals(Feedback.ONE_BUZZ, c.onShake(10_000, faceDown = true))
        assertEquals(TimerState.PAUSED, c.state)
        assertEquals(Feedback.TWO_BUZZES, c.onShake(20_000, faceDown = true))
        assertEquals(TimerState.RUNNING, c.state)
        assertEquals(1_000, c.elapsed(21_000))
    }

    @Test
    fun liftWithoutShakeStopsAtTheLiftTime() {
        c.onFaceDown(0)
        c.onFaceUp(60_000)
        assertNull(c.onGraceElapsed(62_000))
        assertEquals(60_000L, c.onGraceElapsed(63_000))
        assertEquals(TimerState.IDLE, c.state)
    }

    @Test
    fun liftAndShakeHoldsThenResumesWhenPlacedDown() {
        c.onFaceDown(0)
        c.onFaceUp(60_000)
        assertEquals(Feedback.ONE_BUZZ, c.onShake(61_500, faceDown = false))
        assertTrue(c.isHeld)
        assertEquals(TimerState.PAUSED, c.state)
        // Held at the moment it was lifted, however long it is away.
        assertEquals(60_000, c.elapsed(300_000))
        assertNull(c.onGraceElapsed(300_000))

        assertEquals(Feedback.ONE_BUZZ, c.onFaceDown(300_000))
        assertFalse(c.isHeld)
        assertEquals(TimerState.RUNNING, c.state)
        assertEquals(65_000, c.elapsed(305_000))
    }

    @Test
    fun liftWhilePausedAndShakeKeepsThePausedTime() {
        c.onFaceDown(0)
        c.onShake(30_000, faceDown = true)
        c.onFaceUp(40_000)
        c.onShake(41_000, faceDown = false)
        assertEquals(30_000, c.elapsed(50_000))
        c.onFaceDown(50_000)
        assertEquals(31_000, c.elapsed(51_000))
    }

    @Test
    fun putBackDownInsideTheGraceJustCarriesOn() {
        c.onFaceDown(0)
        c.onFaceUp(10_000)
        assertEquals(Feedback.NONE, c.onFaceDown(11_000))
        assertNull(c.onGraceElapsed(20_000))
        assertEquals(TimerState.RUNNING, c.state)
        assertEquals(20_000, c.elapsed(20_000))
    }

    @Test
    fun shakeInTheHandAfterTheGraceDoesNothing() {
        c.onFaceDown(0)
        c.onFaceUp(10_000)
        c.onGraceElapsed(13_000)
        assertEquals(Feedback.NONE, c.onShake(14_000, faceDown = false))
        assertEquals(TimerState.IDLE, c.state)
    }

    @Test
    fun stopClearsAHold() {
        c.onFaceDown(0)
        c.onFaceUp(10_000)
        c.onShake(11_000, faceDown = false)
        c.stop(20_000)
        assertFalse(c.isHeld)
        assertEquals(TimerState.IDLE, c.state)
    }
}
