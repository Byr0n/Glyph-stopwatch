package com.byr0n.glyphstopwatch

enum class Feedback { NONE, SHORT, LONG }

/**
 * The stopwatch rules, kept free of Android so they can be unit tested. Times are
 * monotonic milliseconds.
 *
 * - Face down starts the stopwatch from zero.
 * - Lift stops and resets it; placing it back down starts again from zero.
 * - A shake while face down turns it off until the phone is next lifted and put down.
 */
class StopwatchController {
    private val stopwatch = Stopwatch()

    val state: TimerState
        get() = stopwatch.state

    fun elapsed(nowMs: Long): Long = stopwatch.elapsed(nowMs)

    fun onFaceDown(nowMs: Long) {
        stopwatch.start(nowMs)
    }

    fun onFaceUp(nowMs: Long) {
        stopwatch.stop(nowMs)
    }

    fun onShake(nowMs: Long): Feedback {
        if (stopwatch.state == TimerState.IDLE) return Feedback.NONE
        stopwatch.stop(nowMs)
        return Feedback.LONG
    }

    /** Pauses or resumes, e.g. from a Glyph Button long press. */
    fun togglePause(nowMs: Long): Feedback {
        if (stopwatch.state == TimerState.IDLE) return Feedback.NONE
        stopwatch.togglePause(nowMs)
        return Feedback.SHORT
    }

    fun stop(nowMs: Long) {
        stopwatch.stop(nowMs)
    }
}
