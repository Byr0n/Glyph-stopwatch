package com.byr0n.glyphstopwatch

import android.content.Context
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.VibratorManager

/**
 * The one stopwatch shared by the armed [TimerService] and the [StopwatchToy]. Both can
 * start and stop it; it draws on the toy's matrix when the toy is showing, otherwise on
 * the app's. Call everything on the main thread.
 *
 * Volume down reaches it two ways. With the screen on, [VolumeKeyService] catches the key.
 * With the screen off Android does not pass volume keys to accessibility services, so while
 * the stopwatch is active a media session with remote volume takes them instead. Both feed
 * the same [PressClassifier].
 */
class TimerSession private constructor(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val stopwatch = Stopwatch()
    private val wakeLock = context.getSystemService(PowerManager::class.java)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GlyphStopwatch:session")
    private val classifier = PressClassifier()
    private val windowElapsed = Runnable {
        classifier.onWindowElapsed(SystemClock.uptimeMillis())?.let(::onVolumeDown)
    }
    private var mediaSession: MediaSession? = null
    private var blinkOn = true
    private var finalTimeMs: Long? = null

    private var appRenderer: GlyphRenderer? = null
    private var toyRenderer: GlyphRenderer? = null

    // The Glyph Toy has priority on the matrix, so draw there whenever it is showing.
    private val renderer: GlyphRenderer?
        get() = toyRenderer ?: appRenderer

    /** True while the stopwatch is running or paused, which is when volume down is taken over. */
    val isActive: Boolean
        get() = stopwatch.state != TimerState.IDLE

    private val tick = object : Runnable {
        override fun run() {
            val elapsed = stopwatch.elapsed(SystemClock.elapsedRealtime())
            when (stopwatch.state) {
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

    private val hideFinalTime = Runnable {
        finalTimeMs = null
        renderer?.clear()
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

    fun start() {
        handler.removeCallbacks(hideFinalTime)
        finalTimeMs = null
        stopwatch.start(SystemClock.elapsedRealtime())
        if (!wakeLock.isHeld) wakeLock.acquire()
        captureVolumeKeys()
        redraw()
    }

    /** Stops and resets the stopwatch. With [showFinal] the final time stays up for 5 seconds. */
    fun stop(showFinal: Boolean) {
        if (!isActive) return
        handler.removeCallbacks(tick)
        val finalMs = stopwatch.stop(SystemClock.elapsedRealtime())
        if (wakeLock.isHeld) wakeLock.release()
        releaseVolumeKeys()
        if (showFinal) {
            finalTimeMs = finalMs
            handler.postDelayed(hideFinalTime, FINAL_TIME_MS)
        } else {
            handler.removeCallbacks(hideFinalTime)
            finalTimeMs = null
        }
        redraw()
    }

    fun togglePause() {
        if (!isActive) return
        stopwatch.togglePause(SystemClock.elapsedRealtime())
        vibrate(VibrationEffect.createOneShot(BUZZ_MS, VibrationEffect.DEFAULT_AMPLITUDE))
        redraw()
    }

    /** Reports one volume down key press, from whichever path caught it. */
    fun onVolumeDownPressed(uptimeMs: Long) {
        if (!isActive) return
        handler.removeCallbacks(windowElapsed)
        val press = classifier.onPress(uptimeMs)
        if (press != null) onVolumeDown(press) else handler.postDelayed(windowElapsed, classifier.windowMs)
    }

    private fun onVolumeDown(press: Press) {
        // A press can resolve after the phone was already picked up.
        if (!isActive) return
        when (press) {
            Press.SINGLE -> togglePause()
            Press.DOUBLE -> {
                stopwatch.restart(SystemClock.elapsedRealtime())
                vibrate(VibrationEffect.createWaveform(longArrayOf(0, BUZZ_MS, 80, BUZZ_MS), -1))
                redraw()
            }
        }
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

    private fun captureVolumeKeys() {
        if (mediaSession != null) return
        val volume = object : VolumeProvider(VolumeProvider.VOLUME_CONTROL_RELATIVE, VOLUME_MAX, VOLUME_MAX / 2) {
            override fun onAdjustVolume(direction: Int) {
                if (direction < 0) onVolumeDownPressed(SystemClock.uptimeMillis())
            }
        }
        mediaSession = MediaSession(context, "GlyphStopwatch").apply {
            setPlaybackToRemote(volume)
            // Android only sends volume keys to a session that reports it is playing.
            setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 0, 1f).build())
            isActive = true
        }
    }

    private fun releaseVolumeKeys() {
        handler.removeCallbacks(windowElapsed)
        classifier.reset()
        mediaSession?.release()
        mediaSession = null
    }

    private fun vibrate(effect: VibrationEffect) {
        context.getSystemService(VibratorManager::class.java).defaultVibrator.vibrate(effect)
    }

    companion object {
        private const val PAUSED_BRIGHTNESS = 90
        private const val BLINK_MS = 500L
        private const val FINAL_TIME_MS = 5_000L
        private const val BUZZ_MS = 40L
        private const val VOLUME_MAX = 100

        @Volatile
        private var instance: TimerSession? = null

        fun get(context: Context): TimerSession =
            instance ?: synchronized(this) {
                instance ?: TimerSession(context.applicationContext).also { instance = it }
            }

        /** The session if one exists, without creating it. */
        val current: TimerSession?
            get() = instance
    }
}
