package com.byr0n.glyphstopwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StopwatchControllerTest {
    private val c = StopwatchController()

    @Test
    fun faceDownStarts() {
        assertEquals(Feedback.NONE, c.onFaceDown(0))
        assertEquals(TimerState.RUNNING, c.state)
        assertEquals(5_000, c.elapsed(5_000))
    }

    @Test
    fun liftPausesAndPlacingDownCarriesOn() {
        c.onFaceDown(0)
        assertEquals(Feedback.SHORT, c.onFaceUp(60_000))
        assertTrue(c.isHeld)
        assertEquals(TimerState.PAUSED, c.state)
        assertEquals(60_000, c.elapsed(300_000))

        assertEquals(Feedback.SHORT, c.onFaceDown(300_000))
        assertFalse(c.isHeld)
        assertEquals(65_000, c.elapsed(305_000))
    }

    @Test
    fun oneShakeFaceDownResetsAndKeepsCounting() {
        c.onFaceDown(0)
        assertEquals(Feedback.DOUBLE, c.onShakes(1, 30_000))
        assertEquals(TimerState.RUNNING, c.state)
        assertEquals(2_000, c.elapsed(32_000))
    }

    @Test
    fun oneShakeWhileLiftedResetsAndStaysAtZeroUntilPlacedDown() {
        c.onFaceDown(0)
        c.onFaceUp(30_000)
        assertEquals(Feedback.DOUBLE, c.onShakes(1, 31_000))
        assertEquals(TimerState.PAUSED, c.state)
        assertEquals(0, c.elapsed(40_000))
        c.onFaceDown(40_000)
        assertEquals(3_000, c.elapsed(43_000))
    }

    @Test
    fun twoShakesTurnOff() {
        c.onFaceDown(0)
        c.onFaceUp(30_000)
        assertEquals(Feedback.LONG, c.onShakes(2, 31_000))
        assertEquals(TimerState.IDLE, c.state)
        assertFalse(c.isHeld)
    }

    @Test
    fun shakesWhileOffDoNothing() {
        assertEquals(Feedback.NONE, c.onShakes(1, 0))
        assertEquals(Feedback.NONE, c.onShakes(2, 0))
        assertEquals(TimerState.IDLE, c.state)
    }

    @Test
    fun liftWhileButtonPausedStaysPausedThenResumes() {
        c.onFaceDown(0)
        c.togglePause(10_000)
        assertEquals(Feedback.NONE, c.onFaceUp(20_000))
        c.onFaceDown(30_000)
        assertEquals(11_000, c.elapsed(31_000))
    }
}
