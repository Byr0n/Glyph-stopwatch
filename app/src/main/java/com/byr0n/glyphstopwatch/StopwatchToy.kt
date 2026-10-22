package com.byr0n.glyphstopwatch

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.nothing.ketchum.GlyphToy

/**
 * Glyph Toy mode. While the system shows the toy, the shared [TimerSession] watches the
 * phone's motion and draws on the toy's matrix. Being shown does not mean face down (the
 * system also shows the toy when it is chosen or the screen turns off), so the session
 * starts the stopwatch only once the phone really is face down.
 */
class StopwatchToy : Service() {
    private val session by lazy { TimerSession.get(this) }
    private var renderer: GlyphRenderer? = null

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
        session.watch()
        return messenger.binder
    }

    // A lift hides the toy, but the session keeps the stopwatch going until it decides
    // whether the lift was a hold or a stop.
    override fun onUnbind(intent: Intent?): Boolean {
        session.unwatch()
        renderer?.let {
            session.detachToy(it)
            it.release()
        }
        renderer = null
        return false
    }
}
