package com.byr0n.glyphstopwatch

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.VibratorManager

/**
 * The one stopwatch shared by the armed [TimerService] and the [StopwatchToy]. It owns the
 * motion sensors, applies [StopwatchController]'s rules, and draws on the toy's matrix when
 * the toy is showing, otherwise on the app's. Call everything on the main thread.
 *
 * The sensors run while the toy is showing or app mode is armed, and also while the
 * stopwatch is active: after a lift the toy is hidden, but the session still has to catch
 * the shake that holds the time and the phone being placed back down.
 */
class TimerSession private constructor(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val controller = StopwatchController(GRACE_MS)
    private val flips = FlipFilter()
    private val shakes = ShakeFilter()
    private val sensors = MotionSensors(context, ::onMotion)
    private val wakeLock = context.getSystemService(PowerManager::class.java)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GlyphStopwatch:session")
    private var watchers = 0
    private var blinkOn = true
    private var finalTimeMs: Long? = null

    private var appRenderer: GlyphRenderer? = null
    private var toyRenderer: GlyphRenderer? = null

    // The Glyph Toy has priority on the matrix, so draw there whenever it is showing.
    private val renderer: GlyphRenderer?
        get() = toyRenderer ?: appRenderer

    private val isActive: Boolean
        get() = controller.state != TimerState.IDLE

    private val tick = object : Runnable {
        override fun run() {
            val elapsed = controller.elapsed(now())
            when (controller.state) {
                TimerState.RUNNING -> {
                    renderer?.showElapsed(elapsed)
                    // Wake just after the next whole second so the display never lags.
                    handler.postDelayed(this, 1000 - elapsed % 1000 + 5)
                }
                TimerState.PAUSED -> {
                    if (blinkOn) renderer?.showElapsed(elapsed, PAUSED_BRIGHTNESS) else renderer?.blank()
                    blinkOn = !blinkOn
                    handler.postDelayed(this, BLINK_MS)
                }
                TimerState.IDLE -> Unit
            }
        }
    }

    private val graceElapsed = Runnable {
        controller.onGraceElapsed(now())?.let(::showFinalTime)
        updateSensors()
        redraw()
    }

    private val holdLimit = Runnable {
        if (controller.isHeld) stop()
    }

    private val hideFinalTime = Runnable {
        finalTimeMs = null
        renderer?.clear()
    }

    /** Starts watching the phone's motion, for as long as the caller needs it. */
    fun watch() {
        watchers++
        updateSensors()
    }

    fun unwatch() {
        watchers = (watchers - 1).coerceAtLeast(0)
        updateSensors()
    }

    fun attachApp(renderer: GlyphRenderer) {
        appRenderer = renderer
        redraw()
    }

    fun detachApp(renderer: GlyphRenderer) {
        if (appRenderer === renderer) appRenderer = null
    }

    fun attachToy(renderer: GlyphRenderer) {
        appRenderer?.clear()
        toyRenderer = renderer
        redraw()
    }

    fun detachToy(renderer: GlyphRenderer) {
        if (toyRenderer !== renderer) return
        toyRenderer = null
        redraw()
    }

    /** Pauses or resumes, from a Glyph Button long press. */
    fun togglePause() {
        feedback(controller.togglePause(now()))
        redraw()
    }

    /** Stops and resets with nothing left on the matrix. */
    fun stop() {
        controller.stop(now())
        handler.removeCallbacks(graceElapsed)
        handler.removeCallbacks(holdLimit)
        handler.removeCallbacks(hideFinalTime)
        finalTimeMs = null
        updateSensors()
        redraw()
    }

    /** Draws the current state on whichever matrix output is active. */
    fun redraw() {
        handler.removeCallbacks(tick)
        blinkOn = true
        val shown = finalTimeMs
        when {
            isActive -> tick.run()
            shown != null -> renderer?.showElapsed(shown)
            else -> renderer?.clear()
        }
    }

    private fun onMotion(x: Float, y: Float, z: Float, proximityNear: Boolean) {
        val now = now()
        when (flips.onSample(z, proximityNear, now)) {
            Flip.FACE_DOWN -> onFaceDown(now)
            Flip.FACE_UP -> onFaceUp(now)
            null -> Unit
        }
        if (!isActive || controller.isHeld) return
        val faceDown = flips.isFaceDown
        val threshold = if (faceDown) TABLE_SHAKE else HAND_SHAKE
        if (shakes.onSample(x, y, z, now, threshold, requireFaceDown = faceDown)) {
            feedback(controller.onShake(now, faceDown))
            handler.removeCallbacks(graceElapsed)
            if (controller.isHeld) handler.postDelayed(holdLimit, HOLD_LIMIT_MS)
            redraw()
        }
    }

    private fun onFaceDown(now: Long) {
        handler.removeCallbacks(graceElapsed)
        handler.removeCallbacks(holdLimit)
        handler.removeCallbacks(hideFinalTime)
        finalTimeMs = null
        // Setting the phone down is not a shake.
        shakes.reset(now)
        feedback(controller.onFaceDown(now))
        updateSensors()
        redraw()
    }

    private fun onFaceUp(now: Long) {
        if (!isActive) return
        controller.onFaceUp(now)
        // Let the pick up settle, then give the user time to shake.
        shakes.reset(now, LIFT_QUIET_MS)
        handler.postDelayed(graceElapsed, GRACE_MS)
    }

    private fun showFinalTime(finalMs: Long) {
        finalTimeMs = finalMs
        handler.postDelayed(hideFinalTime, FINAL_TIME_MS)
    }

    private fun updateSensors() {
        if (watchers > 0 || isActive) {
            if (!sensors.isRunning) {
                flips.reset()
                sensors.start()
            }
            // The sensors need the CPU while the screen is off.
            if (!wakeLock.isHeld) wakeLock.acquire()
        } else {
            sensors.stop()
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    private fun feedback(feedback: Feedback) {
        val effect = when (feedback) {
            Feedback.NONE -> return
            Feedback.ONE_BUZZ -> VibrationEffect.createOneShot(BUZZ_MS, VibrationEffect.DEFAULT_AMPLITUDE)
            Feedback.TWO_BUZZES -> VibrationEffect.createWaveform(longArrayOf(0, BUZZ_MS, 80, BUZZ_MS), -1)
        }
        context.getSystemService(VibratorManager::class.java).defaultVibrator.vibrate(effect)
    }

    private fun now() = SystemClock.elapsedRealtime()

    companion object {
        private const val PAUSED_BRIGHTNESS = 90
        private const val BLINK_MS = 500L
        private const val FINAL_TIME_MS = 5_000L
        private const val BUZZ_MS = 40L
        private const val GRACE_MS = 3_000L
        private const val LIFT_QUIET_MS = 300L
        private const val HOLD_LIMIT_MS = 10 * 60_000L

        // Movement in m/s² that counts toward a shake: a jiggle on the table, or a firm
        // shake in the hand, well above the movement of simply holding the phone.
        private const val TABLE_SHAKE = 4.0f
        private const val HAND_SHAKE = 10.0f

        @Volatile
        private var instance: TimerSession? = null

        fun get(context: Context): TimerSession =
            instance ?: synchronized(this) {
                instance ?: TimerSession(context.applicationContext).also { instance = it }
            }
    }
}
