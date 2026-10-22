package com.byr0n.glyphstopwatch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager

/**
 * Foreground service for app mode. While armed it watches for the phone being placed face
 * down and drives the shared [TimerSession], drawing through the app's Glyph Matrix access.
 */
class TimerService : Service() {
    private val session by lazy { TimerSession.get(this) }
    private lateinit var renderer: GlyphRenderer
    private lateinit var detector: FaceDownDetector
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate() {
        super.onCreate()
        renderer = GlyphRenderer(this, asToy = false).also { it.start() }
        session.attachApp(renderer)
        detector = FaceDownDetector(this, ::onFlip)
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GlyphStopwatch:armed")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISARM) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        // The sensors need the CPU while the screen is off.
        if (!wakeLock.isHeld) wakeLock.acquire()
        detector.start()
        armed = true
        return START_STICKY
    }

    override fun onDestroy() {
        armed = false
        detector.stop()
        session.stop(showFinal = false)
        session.detachApp(renderer)
        renderer.release()
        if (wakeLock.isHeld) wakeLock.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun onFlip(flip: Flip) {
        when (flip) {
            Flip.FACE_DOWN -> session.start()
            // Picking the phone up resets the timer; show the final time briefly first.
            Flip.FACE_UP -> session.stop(showFinal = true)
        }
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

        @Volatile
        var armed = false
            private set

        fun arm(context: Context) {
            context.startForegroundService(Intent(context, TimerService::class.java))
        }

        fun disarm(context: Context) {
            context.stopService(Intent(context, TimerService::class.java))
        }
    }
}
