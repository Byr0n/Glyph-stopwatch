package com.byr0n.glyphstopwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class StopwatchTest {
    private val stopwatch = Stopwatch()

    @Test
    fun countsUpFromStart() {
        stopwatch.start(1_000)
        assertEquals(TimerState.RUNNING, stopwatch.state)
        assertEquals(2_500, stopwatch.elapsed(3_500))
    }

    @Test
    fun pauseFreezesAndResumeContinues() {
        stopwatch.start(0)
        assertEquals(TimerState.PAUSED, stopwatch.togglePause(4_000))
        assertEquals(4_000, stopwatch.elapsed(10_000))
        assertEquals(TimerState.RUNNING, stopwatch.togglePause(10_000))
        assertEquals(5_000, stopwatch.elapsed(11_000))
    }

    @Test
    fun restartFromPausedRunsFromZero() {
        stopwatch.start(0)
        stopwatch.togglePause(4_000)
        stopwatch.restart(6_000)
        assertEquals(TimerState.RUNNING, stopwatch.state)
        assertEquals(1_000, stopwatch.elapsed(7_000))
    }

    @Test
    fun stopReturnsFinalTimeAndResets() {
        stopwatch.start(0)
        assertEquals(9_000, stopwatch.stop(9_000))
        assertEquals(TimerState.IDLE, stopwatch.state)
        assertEquals(0, stopwatch.elapsed(20_000))
    }

    @Test
    fun startWhileRunningIsIgnored() {
        stopwatch.start(0)
        stopwatch.start(5_000)
        assertEquals(6_000, stopwatch.elapsed(6_000))
    }

    @Test
    fun togglePauseWhileIdleDoesNothing() {
        assertEquals(TimerState.IDLE, stopwatch.togglePause(1_000))
    }
}
