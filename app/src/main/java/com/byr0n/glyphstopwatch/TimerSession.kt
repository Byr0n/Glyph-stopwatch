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
 * To save battery the sensors run in stages. With the stopwatch off, only the proximity
 * sensor listens and the processor can sleep. When something covers the phone, the
 * accelerometer checks for [CONFIRM_MS] whether it really is face down. While the stopwatch
 * runs, and for 5 seconds after a lift, the accelerometer runs at 25 Hz under a wake lock,
 * watching for a lift or a shake.
 */
class TimerSession private constructor(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val controller = StopwatchController(LISTEN_AFTER_LIFT_MS)
    private val flips = FlipFilter()
    private val shakes = ShakeFilter()
    private val sensors = MotionSensors(context, ::onAcceleration, ::onProximity)
    private val wakeLock = context.getSystemService(PowerManager::class.java)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GlyphStopwatch:session")
    private var watchers = 0
    private var proximityNear = !sensors.hasProximity
    private var confirming = false
    private var blinkOn = true

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
                    if (blinkOn) renderer?.showElapsed(elapsed) else renderer?.blank()
                    blinkOn = !blinkOn
                    handler.postDelayed(this, BLINK_MS)
                }
                TimerState.IDLE -> Unit
            }
        }
    }

    private val confirmTimeout = Runnable {
        confirming = false
        updateSensors()
    }

    private val stopListening = Runnable { updateSensors() }

    /** Starts watching for the phone being placed face down, for as long as the caller needs. */
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
        afterStop()
    }

    /** Draws the current state on whichever matrix output is active. */
    fun redraw() {
        handler.removeCallbacks(tick)
        blinkOn = true
        if (isActive) tick.run() else renderer?.clear()
    }

    private fun onProximity(near: Boolean) {
        val wasNear = proximityNear
        proximityNear = near
        if (near && !wasNear && !isActive) {
            // Something covered the phone: check whether it was placed face down.
            confirming = true
            handler.removeCallbacks(confirmTimeout)
            handler.postDelayed(confirmTimeout, CONFIRM_MS)
            updateSensors()
        }
    }

    private fun onAcceleration(x: Float, y: Float, z: Float) {
        val now = now()
        // Waiting on the accelerometer alone, the proximity reading is not kept up to date.
        val near = proximityNear || sensors.mode != MotionSensors.Mode.WATCH
        when (flips.onSample(z, near, now)) {
            Flip.FACE_DOWN -> onFaceDown(now)
            Flip.FACE_UP -> onFaceUp(now)
            null -> Unit
        }
        val shaken = when {
            isActive && flips.isFaceDown -> shakes.onSample(x, y, z, now, TABLE_SHAKE, requireFaceDown = true)
            controller.isListeningAfterLift(now) -> shakes.onSample(x, y, z, now, HAND_SHAKE, requireFaceDown = false)
            else -> false
        }
        if (shaken) onShake()
    }

    private fun onShake() {
        feedback(controller.onShake(now()))
        afterStop()
    }

    private fun onFaceDown(now: Long) {
        handler.removeCallbacks(confirmTimeout)
        confirming = false
        // Setting the phone down is not a shake.
        shakes.reset(now)
        controller.onFaceDown(now)
        updateSensors()
        redraw()
    }

    private fun onFaceUp(now: Long) {
        if (!isActive) return
        // Lifting stops and resets; putting it back down starts again. For a few seconds a
        // shake in the hand still turns it off.
        controller.onFaceUp(now)
        afterStop(quietMs = LIFT_QUIET_MS)
        handler.removeCallbacks(stopListening)
        handler.postDelayed(stopListening, LISTEN_AFTER_LIFT_MS + 50)
    }

    // Ignores movement briefly, so the move that caused the stop is not taken as a shake.
    private fun afterStop(quietMs: Long = 1_000L) {
        shakes.reset(now(), quietMs)
        updateSensors()
        redraw()
    }

    private fun updateSensors() {
        val mode = when {
            isActive || confirming || controller.isListeningAfterLift(now()) -> MotionSensors.Mode.WATCH
            watchers > 0 -> MotionSensors.Mode.WAIT_FOR_COVER
            else -> MotionSensors.Mode.OFF
        }
        if (mode == MotionSensors.Mode.WATCH && sensors.mode != mode && !isActive) flips.reset()
        sensors.setMode(mode)
        val keepAwake = mode == MotionSensors.Mode.WATCH ||
            (mode == MotionSensors.Mode.WAIT_FOR_COVER && sensors.needsWakeLockToWait)
        if (keepAwake && !wakeLock.isHeld) wakeLock.acquire()
        if (!keepAwake && wakeLock.isHeld) wakeLock.release()
    }

    private fun feedback(feedback: Feedback) {
        val effect = when (feedback) {
            Feedback.NONE -> return
            Feedback.SHORT -> VibrationEffect.createOneShot(BUZZ_MS, VibrationEffect.DEFAULT_AMPLITUDE)
            Feedback.LONG -> VibrationEffect.createOneShot(LONG_BUZZ_MS, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        context.getSystemService(VibratorManager::class.java).defaultVibrator.vibrate(effect)
    }

    private fun now() = SystemClock.elapsedRealtime()

    companion object {
        private const val BLINK_MS = 500L
        private const val BUZZ_MS = 40L
        private const val LONG_BUZZ_MS = 300L
        private const val CONFIRM_MS = 2_000L
        private const val LIFT_QUIET_MS = 300L
        private const val LISTEN_AFTER_LIFT_MS = 5_000L

        // Movement in m/s² that counts toward a shake: a jiggle face down, or a firm shake in
        // the hand, well above the movement of turning the phone over to look at it.
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
