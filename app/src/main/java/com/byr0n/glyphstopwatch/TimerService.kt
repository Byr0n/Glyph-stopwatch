package com.byr0n.glyphstopwatch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.VibratorManager

/**
 * Foreground service that keeps the stopwatch alive with the screen off. While armed it
 * watches for the phone being placed face down, runs the stopwatch, and draws it on the
 * Glyph Matrix.
 */
class TimerService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val stopwatch = Stopwatch()
    private lateinit var renderer: GlyphRenderer
    private lateinit var detector: FaceDownDetector
    private lateinit var wakeLock: PowerManager.WakeLock
    private var blinkOn = true

    private val tick = object : Runnable {
        override fun run() {
            val now = SystemClock.elapsedRealtime()
            val elapsed = stopwatch.elapsed(now)
            when (stopwatch.state) {
                TimerState.RUNNING -> {
                    renderer.showElapsed(elapsed)
                    // Wake just after the next whole second so the display never lags.
                    handler.postDelayed(this, 1000 - elapsed % 1000 + 5)
                }
                TimerState.PAUSED -> {
                    if (blinkOn) renderer.showElapsed(elapsed, PAUSED_BRIGHTNESS) else renderer.blank()
                    blinkOn = !blinkOn
                    handler.postDelayed(this, BLINK_MS)
                }
                TimerState.IDLE -> Unit
            }
        }
    }

    private val hideFinalTime = Runnable { renderer.clear() }

    override fun onCreate() {
        super.onCreate()
        renderer = GlyphRenderer(this).also { it.start() }
        detector = FaceDownDetector(this, ::onFlip)
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GlyphStopwatch:timer")
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISARM) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        // The sensors and the ticking display need the CPU while the screen is off.
        if (!wakeLock.isHeld) wakeLock.acquire()
        detector.start()
        armed = true
        return START_STICKY
    }

    override fun onDestroy() {
        armed = false
        instance = null
        detector.stop()
        handler.removeCallbacksAndMessages(null)
        stopwatch.stop(SystemClock.elapsedRealtime())
        renderer.release()
        if (wakeLock.isHeld) wakeLock.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun onFlip(flip: Flip) {
        val now = SystemClock.elapsedRealtime()
        when (flip) {
            Flip.FACE_DOWN -> {
                handler.removeCallbacks(hideFinalTime)
                stopwatch.start(now)
                restartTicking()
            }
            Flip.FACE_UP -> {
                if (stopwatch.state == TimerState.IDLE) return
                handler.removeCallbacks(tick)
                // Picking the phone up resets the timer; show the final time briefly first.
                renderer.showElapsed(stopwatch.stop(now))
                handler.postDelayed(hideFinalTime, FINAL_TIME_MS)
            }
        }
    }

    fun onVolumeDown(press: Press) {
        // A press can resolve after the phone was already picked up.
        if (stopwatch.state == TimerState.IDLE) return
        val now = SystemClock.elapsedRealtime()
        when (press) {
            Press.SINGLE -> {
                stopwatch.togglePause(now)
                vibrate(VibrationEffect.createOneShot(BUZZ_MS, VibrationEffect.DEFAULT_AMPLITUDE))
            }
            Press.DOUBLE -> {
                stopwatch.restart(now)
                vibrate(VibrationEffect.createWaveform(longArrayOf(0, BUZZ_MS, 80, BUZZ_MS), -1))
            }
        }
        restartTicking()
    }

    private fun restartTicking() {
        handler.removeCallbacks(tick)
        blinkOn = true
        tick.run()
    }

    private fun vibrate(effect: VibrationEffect) {
        getSystemService(VibratorManager::class.java).defaultVibrator.vibrate(effect)
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val disarm = PendingIntent.getService(
            this, 1, Intent(this, TimerService::class.java).setAction(ACTION_DISARM), PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stopwatch)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, getString(R.string.disarm), disarm).build())
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "timer"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_DISARM = "com.byr0n.glyphstopwatch.DISARM"
        private const val PAUSED_BRIGHTNESS = 90
        private const val BLINK_MS = 500L
        private const val FINAL_TIME_MS = 5_000L
        private const val BUZZ_MS = 40L

        @Volatile
        var instance: TimerService? = null
            private set

        @Volatile
        var armed = false
            private set

        /** True while the stopwatch is running or paused, which is when volume down is taken over. */
        val isTimerActive: Boolean
            get() = instance?.stopwatch?.state.let { it != null && it != TimerState.IDLE }

        fun arm(context: Context) {
            context.startForegroundService(Intent(context, TimerService::class.java))
        }

        fun disarm(context: Context) {
            context.stopService(Intent(context, TimerService::class.java))
        }
    }
}
