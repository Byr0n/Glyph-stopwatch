package com.byr0n.glyphstopwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class MatrixDigitsTest {
    private fun ascii(frame: IntArray) = frame.toList().chunked(13).joinToString("\n") { row ->
        row.joinToString("") { b -> if (b == 255) "#" else if (b > 0) "+" else "." }
    }

    @Test
    fun showsWholeMinutesUpTo99() {
        assertEquals(0, MatrixDigits.minutes(59_999))
        assertEquals(1, MatrixDigits.minutes(60_000))
        assertEquals(42, MatrixDigits.minutes(42 * 60_000L + 30_000))
        assertEquals(99, MatrixDigits.minutes(150 * 60_000L))
    }

    @Test
    fun barFillsOverTheMinuteThenEmpties() {
        assertEquals(0f, MatrixDigits.barFill(0), 0.001f)
        assertEquals(4.5f, MatrixDigits.barFill(30_000), 0.001f)
        assertEquals(0f, MatrixDigits.barFill(60_000), 0.001f)
    }

    @Test
    fun drawsCentredMinutesWithTheBarOnTheRight() {
        // 12 minutes 30 seconds: four and a half of nine bar pixels.
        val expected = """
            .............
            .............
            .............
            ...#..###....
            ..##....#....
            ...#....#....
            ...#..###..+.
            ...#..#....#.
            ...#..#....#.
            ..###.###..#.
            ...........#.
            .............
            .............
        """.trimIndent()
        assertEquals(expected, ascii(MatrixDigits.render((12 * 60 + 30) * 1000L, 13)))
    }

    @Test
    fun barIsAlmostFullJustBeforeTheMinute() {
        val frame = MatrixDigits.render(59_999, 13)
        val bar = (2..10).map { row -> frame[row * 13 + 11] }
        assertEquals(255, bar.last())
        assertEquals(8, bar.count { it == 255 })
        assertEquals(true, bar.first() in 200..254)
    }

    @Test
    fun staysInsideThePhone4aProLeds() {
        // The 13x13 grid has 137 LEDs in a circle; the corners have none.
        // From the LED allocation image in Nothing's GlyphMatrix Developer Kit.
        val leds = """
            ....#####....
            ..#########..
            .###########.
            .###########.
            #############
            #############
            #############
            #############
            #############
            .###########.
            .###########.
            ..#########..
            ....#####....
        """.trimIndent().replace("\n", "")
        assertEquals(137, leds.count { it == '#' })
        for (minute in 0..99) for (second in 0..59) {
            val ms = (minute * 60 + second) * 1000L + 999
            MatrixDigits.render(ms, 13).forEachIndexed { i, b ->
                if (b > 0) assertEquals("pixel $i at $minute:$second", '#', leds[i])
            }
        }
    }

    @Test
    fun usesRequestedBrightness() {
        val frame = MatrixDigits.render(0, 13, brightness = 90)
        assertEquals(setOf(0, 90), frame.toSet())
    }
}
