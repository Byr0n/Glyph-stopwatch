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
 * Glyph Toy mode. On the Phone (4a) Pro toys are always-on toys that the system shows
 * through Flip to Glyph, so the system binding this service means the phone was just
 * placed face down: the stopwatch starts. Unbinding (picked up, or another toy chosen)
 * stops it. Shakes pause and restart it through [TimerSession].
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
        session.start()
        return messenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        session.stop(showFinal = true)
        renderer?.let {
            session.detachToy(it)
            it.release()
        }
        renderer = null
        return false
    }
}
