package com.byr0n.glyphstopwatch

enum class Feedback { NONE, SHORT, LONG }

/**
 * The stopwatch rules, kept free of Android so they can be unit tested. Times are
 * monotonic milliseconds.
 *
 * - Face down starts the stopwatch from zero.
 * - Lift stops and resets it; placing it back down starts again from zero.
 * - A shake turns it off: while face down, or in the hand within [listenAfterLiftMs] of a
 *   lift. Placing it face down again always starts a new run.
 */
class StopwatchController(private val listenAfterLiftMs: Long = 5_000L) {
    private val stopwatch = Stopwatch()
    private var liftedAtMs: Long? = null

    val state: TimerState
        get() = stopwatch.state

    fun elapsed(nowMs: Long): Long = stopwatch.elapsed(nowMs)

    /** True for a short while after a lift, while a shake in the hand still counts. */
    fun isListeningAfterLift(nowMs: Long): Boolean {
        val lifted = liftedAtMs ?: return false
        return nowMs - lifted <= listenAfterLiftMs
    }

    fun onFaceDown(nowMs: Long) {
        liftedAtMs = null
        stopwatch.start(nowMs)
    }

    fun onFaceUp(nowMs: Long) {
        if (stopwatch.state == TimerState.IDLE) return
        stopwatch.stop(nowMs)
        liftedAtMs = nowMs
    }

    fun onShake(nowMs: Long): Feedback {
        if (stopwatch.state != TimerState.IDLE) {
            stopwatch.stop(nowMs)
            return Feedback.LONG
        }
        if (!isListeningAfterLift(nowMs)) return Feedback.NONE
        liftedAtMs = null
        return Feedback.LONG
    }

    /** Pauses or resumes, e.g. from a Glyph Button long press. */
    fun togglePause(nowMs: Long): Feedback {
        if (stopwatch.state == TimerState.IDLE) return Feedback.NONE
        stopwatch.togglePause(nowMs)
        return Feedback.SHORT
    }

    fun stop(nowMs: Long) {
        liftedAtMs = null
        stopwatch.stop(nowMs)
    }
}
