package com.byr0n.glyphstopwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StopwatchControllerTest {
    private val c = StopwatchController()

    @Test
    fun faceDownStartsFromZero() {
        c.onFaceDown(1_000)
        assertEquals(TimerState.RUNNING, c.state)
        assertEquals(5_000, c.elapsed(6_000))
    }

    @Test
    fun liftResetsAndPlacingDownStartsAgainFromZero() {
        c.onFaceDown(0)
        c.onFaceUp(60_000)
        assertEquals(TimerState.IDLE, c.state)
        assertEquals(0, c.elapsed(90_000))
        c.onFaceDown(90_000)
        assertEquals(2_000, c.elapsed(92_000))
    }

    @Test
    fun shakeTurnsOff() {
        c.onFaceDown(0)
        assertEquals(Feedback.LONG, c.onShake(30_000))
        assertEquals(TimerState.IDLE, c.state)
    }

    @Test
    fun shakeWhileOffDoesNothing() {
        assertEquals(Feedback.NONE, c.onShake(0))
        assertEquals(TimerState.IDLE, c.state)
    }

    @Test
    fun shakeInTheHandSoonAfterALiftTurnsOff() {
        c.onFaceDown(0)
        c.onFaceUp(60_000)
        assertTrue(c.isListeningAfterLift(64_000))
        assertEquals(Feedback.LONG, c.onShake(64_000))
        assertFalse(c.isListeningAfterLift(64_500))
        assertEquals(Feedback.NONE, c.onShake(64_500))
    }

    @Test
    fun shakeLongAfterALiftDoesNothing() {
        c.onFaceDown(0)
        c.onFaceUp(60_000)
        assertFalse(c.isListeningAfterLift(65_001))
        assertEquals(Feedback.NONE, c.onShake(65_001))
    }

    @Test
    fun placingDownAfterAShakeOffStartsAgain() {
        c.onFaceDown(0)
        c.onFaceUp(60_000)
        c.onShake(61_000)
        c.onFaceDown(70_000)
        assertEquals(TimerState.RUNNING, c.state)
        assertEquals(1_000, c.elapsed(71_000))
    }

    @Test
    fun buttonPausesAndResumes() {
        c.onFaceDown(0)
        assertEquals(Feedback.SHORT, c.togglePause(10_000))
        assertEquals(10_000, c.elapsed(20_000))
        c.togglePause(20_000)
        assertEquals(11_000, c.elapsed(21_000))
    }
}
