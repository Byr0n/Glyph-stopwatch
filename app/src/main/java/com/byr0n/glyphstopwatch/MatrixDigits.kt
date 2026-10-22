package com.byr0n.glyphstopwatch

/**
 * Draws elapsed time as brightness values (0 to 255) for a square Glyph Matrix,
 * row major. A 13x13 matrix cannot fit "mm:ss" on one line, so minutes sit above
 * seconds. From 100 minutes on it shows hours above minutes instead.
 */
object MatrixDigits {
    private const val W = 3
    private const val H = 5

    // 3x5 font, one string per row, '#' = lit.
    private val FONT = arrayOf(
        arrayOf("###", "#.#", "#.#", "#.#", "###"),
        arrayOf(".#.", "##.", ".#.", ".#.", "###"),
        arrayOf("###", "..#", "###", "#..", "###"),
        arrayOf("###", "..#", "###", "..#", "###"),
        arrayOf("#.#", "#.#", "###", "..#", "..#"),
        arrayOf("###", "#..", "###", "..#", "###"),
        arrayOf("###", "#..", "###", "#.#", "###"),
        arrayOf("###", "..#", ".#.", ".#.", ".#."),
        arrayOf("###", "#.#", "###", "#.#", "###"),
        arrayOf("###", "#.#", "###", "..#", "###"),
    )

    /** Splits elapsed time into the two numbers shown, top and bottom. */
    fun split(elapsedMs: Long): Pair<Int, Int> {
        val totalSeconds = elapsedMs / 1000
        val minutes = totalSeconds / 60
        return if (minutes < 100) {
            minutes.toInt() to (totalSeconds % 60).toInt()
        } else {
            (minutes / 60).coerceAtMost(99).toInt() to (minutes % 60).toInt()
        }
    }

    fun render(elapsedMs: Long, size: Int, brightness: Int = 255): IntArray {
        val (top, bottom) = split(elapsedMs)
        return renderPair(top, bottom, size, brightness)
    }

    fun renderPair(top: Int, bottom: Int, size: Int, brightness: Int = 255): IntArray {
        val pixels = IntArray(size * size)
        // Two digits plus a one pixel gap, two rows plus a one pixel gap, centred.
        val blockW = W * 2 + 1
        val blockH = H * 2 + 1
        val x0 = (size - blockW) / 2
        val y0 = (size - blockH) / 2
        drawNumber(pixels, size, top, x0, y0, brightness)
        drawNumber(pixels, size, bottom, x0, y0 + H + 1, brightness)
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
                if (glyph[row][col] != '#') continue
                val px = x + col
                val py = y + row
                if (px in 0 until size && py in 0 until size) pixels[py * size + px] = brightness
            }
        }
    }
}
