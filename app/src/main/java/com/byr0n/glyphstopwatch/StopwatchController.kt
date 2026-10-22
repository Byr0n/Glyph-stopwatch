package com.byr0n.glyphstopwatch

enum class Feedback { NONE, ONE_BUZZ, TWO_BUZZES }

/**
 * The stopwatch rules, kept free of Android so they can be unit tested. Times are
 * monotonic milliseconds.
 *
 * - Face down starts the stopwatch.
 * - Shake while face down: running pauses, paused restarts from zero.
 * - Lift: the stopwatch waits [graceMs] for a shake. A shake holds it at the time it was
 *   lifted, and placing it back down carries on from there. No shake stops and resets it.
 */
class StopwatchController(private val graceMs: Long = 3_000L) {
    private val stopwatch = Stopwatch()

    /** When the phone was lifted, while waiting for a shake. */
    var liftedAtMs: Long? = null
        private set

    /** Paused by a lift and shake; resumes when placed back down. */
    var isHeld = false
        private set

    val state: TimerState
        get() = stopwatch.state

    fun elapsed(nowMs: Long): Long = stopwatch.elapsed(nowMs)

    fun onFaceDown(nowMs: Long): Feedback {
        liftedAtMs = null
        if (isHeld) {
            isHeld = false
            stopwatch.togglePause(nowMs)
            return Feedback.ONE_BUZZ
        }
        stopwatch.start(nowMs)
        return Feedback.NONE
    }

    fun onFaceUp(nowMs: Long) {
        if (stopwatch.state != TimerState.IDLE && !isHeld) liftedAtMs = nowMs
    }

    /** Returns the final time when the lift went unanswered for [graceMs], else null. */
    fun onGraceElapsed(nowMs: Long): Long? {
        val lifted = liftedAtMs ?: return null
        if (nowMs - lifted < graceMs) return null
        // The time stopped when the phone was picked up, not when the wait ran out.
        return stop(lifted)
    }

    fun onShake(nowMs: Long, faceDown: Boolean): Feedback {
        val lifted = liftedAtMs
        return when {
            lifted != null -> {
                liftedAtMs = null
                isHeld = true
                if (stopwatch.state == TimerState.RUNNING) stopwatch.togglePause(lifted)
                Feedback.ONE_BUZZ
            }
            !faceDown -> Feedback.NONE
            stopwatch.state == TimerState.RUNNING -> {
                stopwatch.togglePause(nowMs)
                Feedback.ONE_BUZZ
            }
            stopwatch.state == TimerState.PAUSED -> {
                stopwatch.restart(nowMs)
                Feedback.TWO_BUZZES
            }
            else -> Feedback.NONE
        }
    }

    /** Pauses or resumes, e.g. from a Glyph Button long press. */
    fun togglePause(nowMs: Long): Feedback {
        if (stopwatch.state == TimerState.IDLE) return Feedback.NONE
        stopwatch.togglePause(nowMs)
        return Feedback.ONE_BUZZ
    }

    /** Stops and resets everything. Returns the final time. */
    fun stop(nowMs: Long): Long {
        liftedAtMs = null
        isHeld = false
        return stopwatch.stop(nowMs)
    }
}
