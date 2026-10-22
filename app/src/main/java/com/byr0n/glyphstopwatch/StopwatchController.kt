package com.byr0n.glyphstopwatch

enum class Feedback { NONE, SHORT, DOUBLE, LONG }

/**
 * The stopwatch rules, kept free of Android so they can be unit tested. Times are
 * monotonic milliseconds.
 *
 * - Face down starts the stopwatch.
 * - Lift pauses it; placing it back down carries on from there.
 * - One shake resets it to zero: counting on if face down, held at zero if lifted.
 * - Two shakes turn it off.
 */
class StopwatchController {
    private val stopwatch = Stopwatch()

    /** Paused by a lift; resumes when placed back down. */
    var isHeld = false
        private set

    val state: TimerState
        get() = stopwatch.state

    fun elapsed(nowMs: Long): Long = stopwatch.elapsed(nowMs)

    fun onFaceDown(nowMs: Long): Feedback {
        if (isHeld) {
            isHeld = false
            if (stopwatch.state == TimerState.PAUSED) stopwatch.togglePause(nowMs)
            return Feedback.SHORT
        }
        stopwatch.start(nowMs)
        return Feedback.NONE
    }

    fun onFaceUp(nowMs: Long): Feedback {
        if (stopwatch.state == TimerState.IDLE) return Feedback.NONE
        isHeld = true
        if (stopwatch.state != TimerState.RUNNING) return Feedback.NONE
        stopwatch.togglePause(nowMs)
        return Feedback.SHORT
    }

    fun onShakes(count: Int, nowMs: Long): Feedback {
        if (stopwatch.state == TimerState.IDLE) return Feedback.NONE
        if (count >= 2) {
            stop(nowMs)
            return Feedback.LONG
        }
        stopwatch.restart(nowMs)
        if (isHeld) stopwatch.togglePause(nowMs)
        return Feedback.DOUBLE
    }

    /** Pauses or resumes, e.g. from a Glyph Button long press. */
    fun togglePause(nowMs: Long): Feedback {
        if (stopwatch.state == TimerState.IDLE) return Feedback.NONE
        stopwatch.togglePause(nowMs)
        return Feedback.SHORT
    }

    /** Stops and resets everything. */
    fun stop(nowMs: Long) {
        isHeld = false
        stopwatch.stop(nowMs)
    }
}
