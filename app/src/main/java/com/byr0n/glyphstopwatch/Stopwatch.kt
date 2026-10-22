package com.byr0n.glyphstopwatch

enum class TimerState { IDLE, RUNNING, PAUSED }

/**
 * Pure stopwatch state machine. Every call takes the current monotonic time in
 * milliseconds so it can be driven by SystemClock in the app and by fake clocks in tests.
 *
 *   IDLE -> start -> RUNNING
 *   RUNNING <-> togglePause <-> PAUSED
 *   RUNNING or PAUSED -> restart -> RUNNING from 0:00
 *   RUNNING or PAUSED -> stop -> IDLE (elapsed time resets to zero)
 */
class Stopwatch {
    var state = TimerState.IDLE
        private set

    private var accumulatedMs = 0L
    private var runningSinceMs = 0L

    fun elapsed(nowMs: Long): Long = when (state) {
        TimerState.RUNNING -> accumulatedMs + (nowMs - runningSinceMs)
        else -> accumulatedMs
    }

    fun start(nowMs: Long) {
        if (state != TimerState.IDLE) return
        restart(nowMs)
    }

    /** Pauses a running timer or resumes a paused one. Returns the new state. */
    fun togglePause(nowMs: Long): TimerState {
        when (state) {
            TimerState.RUNNING -> {
                accumulatedMs = elapsed(nowMs)
                state = TimerState.PAUSED
            }
            TimerState.PAUSED -> {
                runningSinceMs = nowMs
                state = TimerState.RUNNING
            }
            TimerState.IDLE -> Unit
        }
        return state
    }

    fun restart(nowMs: Long) {
        accumulatedMs = 0L
        runningSinceMs = nowMs
        state = TimerState.RUNNING
    }

    /** Stops the timer and returns the final elapsed time. */
    fun stop(nowMs: Long): Long {
        val finalMs = elapsed(nowMs)
        accumulatedMs = 0L
        state = TimerState.IDLE
        return finalMs
    }
}
