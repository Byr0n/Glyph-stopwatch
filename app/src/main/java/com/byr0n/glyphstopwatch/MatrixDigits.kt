package com.byr0n.glyphstopwatch

import kotlin.math.floor

/**
 * Draws elapsed time as brightness values (0 to 255) for a square Glyph Matrix, row major:
 * the minutes as two centred digits, and to their right a bar that fills from the bottom
 * over each minute. When the bar reaches the top the minutes tick over and it starts again.
 *
 * On the 13x13 Phone (4a) Pro matrix the digits take rows 3 to 9 and columns 3 to 9, and
 * the bar column 11, rows 2 to 10: the full height the circle of LEDs allows there.
 */
object MatrixDigits {
    private const val W = 3
    private const val H = 7
    const val BAR_LENGTH = 9

    // 3x7 font, one string per row, '#' = lit.
    private val FONT = arrayOf(
        arrayOf("###", "#.#", "#.#", "#.#", "#.#", "#.#", "###"),
        arrayOf(".#.", "##.", ".#.", ".#.", ".#.", ".#.", "###"),
        arrayOf("###", "..#", "..#", "###", "#..", "#..", "###"),
        arrayOf("###", "..#", "..#", "###", "..#", "..#", "###"),
        arrayOf("#.#", "#.#", "#.#", "###", "..#", "..#", "..#"),
        arrayOf("###", "#..", "#..", "###", "..#", "..#", "###"),
        arrayOf("###", "#..", "#..", "###", "#.#", "#.#", "###"),
        arrayOf("###", "..#", "..#", "..#", "..#", "..#", "..#"),
        arrayOf("###", "#.#", "#.#", "###", "#.#", "#.#", "###"),
        arrayOf("###", "#.#", "#.#", "###", "..#", "..#", "###"),
    )

    /** Whole minutes shown, up to 99. */
    fun minutes(elapsedMs: Long): Int = (elapsedMs / 60_000).coerceAtMost(99).toInt()

    /** How full the bar is, from 0 (empty) to [BAR_LENGTH] (full), through the current minute. */
    fun barFill(elapsedMs: Long): Float = (elapsedMs % 60_000) / 60_000f * BAR_LENGTH

    fun render(elapsedMs: Long, size: Int, brightness: Int = 255): IntArray {
        val pixels = IntArray(size * size)
        val centre = size / 2
        drawNumber(pixels, size, minutes(elapsedMs), centre - W, centre - H / 2, brightness)
        drawBar(pixels, size, barFill(elapsedMs), centre + W + 2, centre + BAR_LENGTH / 2, brightness)
        return pixels
    }

    private fun drawNumber(pixels: IntArray, size: Int, value: Int, x: Int, y: Int, brightness: Int) {
        drawDigit(pixels, size, value / 10 % 10, x, y, brightness)
        drawDigit(pixels, size, value % 10, x + W + 1, y, brightness)
    }

    private fun drawDigit(pixels: IntArray, size: Int, digit: Int, x: Int, y: Int, brightness: Int) {
        val glyph = FONT[digit]
        for (row in 0 until H) {
            for (col in 0 until W) {
                if (glyph[row][col] == '#') set(pixels, size, x + col, y + row, brightness)
            }
        }
    }

    // Lit pixels from the bottom up; the next one fades in, so the bar moves every second.
    private fun drawBar(pixels: IntArray, size: Int, fill: Float, x: Int, bottomY: Int, brightness: Int) {
        val full = floor(fill).toInt()
        for (i in 0 until full) set(pixels, size, x, bottomY - i, brightness)
        val partial = ((fill - full) * brightness).toInt()
        if (partial > 0 && full < BAR_LENGTH) set(pixels, size, x, bottomY - full, partial)
    }

    private fun set(pixels: IntArray, size: Int, x: Int, y: Int, brightness: Int) {
        if (x in 0 until size && y in 0 until size) pixels[y * size + x] = brightness
    }
}
