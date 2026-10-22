package com.byr0n.glyphstopwatch

import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphMatrixManager

/**
 * Draws onto the Glyph Matrix through the Nothing SDK. Frames sent before the Glyph
 * service connects are held and shown once it does. A toy draws with the toy calls; the
 * app uses the app calls, which the system shows below any active toy.
 */
class GlyphRenderer(context: Context, private val asToy: Boolean) {
    private val manager = GlyphMatrixManager.getInstance(context.applicationContext)
    private var connected = false
    private var pendingFrame: IntArray? = null

    val size: Int = Common.getDeviceMatrixLength().takeIf { it > 0 } ?: Glyph.DEVICE_25111p_MATRIX_LENGTH

    private val callback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(name: ComponentName) {
            manager.register(if (Common.is23112()) Glyph.DEVICE_23112 else Glyph.DEVICE_25111p)
            connected = true
            if (!asToy) safely { manager.setGlyphMatrixTimeout(false) }
            pendingFrame?.let { send(it) }
            pendingFrame = null
        }

        override fun onServiceDisconnected(name: ComponentName) {
            connected = false
        }
    }

    fun start() {
        manager.init(callback)
    }

    fun showElapsed(elapsedMs: Long, brightness: Int = 255) {
        send(MatrixDigits.render(elapsedMs, size, brightness))
    }

    fun blank() {
        send(IntArray(size * size))
    }

    /** Turns the matrix off and hands it back to the system. */
    fun clear() {
        pendingFrame = null
        if (!connected) return
        if (asToy) manager.turnOff() else safely { manager.closeAppMatrix() }
    }

    fun release() {
        clear()
        connected = false
        manager.unInit()
    }

    private fun send(frame: IntArray) {
        if (!connected) {
            pendingFrame = frame
            return
        }
        safely { if (asToy) manager.setMatrixFrame(frame) else manager.setAppMatrixFrame(frame) }
    }

    private inline fun safely(block: () -> Unit) {
        try {
            block()
        } catch (e: GlyphException) {
            Log.w(TAG, "Glyph call failed", e)
        }
    }

    private companion object {
        const val TAG = "GlyphRenderer"
    }
}
