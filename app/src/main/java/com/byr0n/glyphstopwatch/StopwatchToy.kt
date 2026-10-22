package com.byr0n.glyphstopwatch

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.PowerManager
import com.nothing.ketchum.GlyphToy

/**
 * Glyph Toy mode. The system can show the toy at times other than a flip (when it is
 * chosen, or when the screen turns off), so being shown does not mean face down. While
 * shown, the toy runs the same [FaceDownDetector] as app mode and only starts the
 * stopwatch once the phone really is face down. Shakes pause and restart it through
 * [TimerSession].
 */
class StopwatchToy : Service() {
    private val session by lazy { TimerSession.get(this) }
    private var renderer: GlyphRenderer? = null
    private var detector: FaceDownDetector? = null
    private val wakeLock by lazy {
        getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GlyphStopwatch:toy")
    }

    private val messenger = Messenger(object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what != GlyphToy.MSG_GLYPH_TOY) return super.handleMessage(msg)
            when (msg.data.getString(GlyphToy.MSG_GLYPH_TOY_DATA)) {
                // Long press of the Glyph Button, on phones that have one.
                GlyphToy.EVENT_CHANGE -> session.togglePause()
                // Sent every minute while shown as the always-on toy.
                GlyphToy.EVENT_AOD -> session.redraw()
            }
        }
    })

    override fun onBind(intent: Intent?): IBinder {
        val toyRenderer = GlyphRenderer(this, asToy = true).also { it.start() }
        renderer = toyRenderer
        session.attachToy(toyRenderer)
        // The sensors need the CPU while the screen is off.
        if (!wakeLock.isHeld) wakeLock.acquire()
        detector = FaceDownDetector(this) { flip ->
            when (flip) {
                Flip.FACE_DOWN -> session.start()
                Flip.FACE_UP -> session.stop(showFinal = true)
            }
        }.also { it.start() }
        return messenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        detector?.stop()
        detector = null
        if (wakeLock.isHeld) wakeLock.release()
        session.stop(showFinal = true)
        renderer?.let {
            session.detachToy(it)
            it.release()
        }
        renderer = null
        return false
    }
}
