package com.byr0n.glyphstopwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class MatrixDigitsTest {
    @Test
    fun splitsMinutesAndSeconds() {
        assertEquals(12 to 34, MatrixDigits.split((12 * 60 + 34) * 1000L + 999))
    }

    @Test
    fun switchesToHoursAndMinutesAt100Minutes() {
        assertEquals(1 to 40, MatrixDigits.split(100 * 60 * 1000L))
    }

    @Test
    fun drawsCentredOnThirteenByThirteen() {
        val frame = MatrixDigits.renderPair(12, 34, 13)
        val expected = """
            .............
            ....#..###...
            ...##....#...
            ....#..###...
            ....#..#.....
            ...###.###...
            .............
            ...###.#.#...
            .....#.#.#...
            ...###.###...
            .....#...#...
            ...###...#...
            .............
        """.trimIndent()
        assertEquals(expected, frame.toList().chunked(13).joinToString("\n") { row ->
            row.joinToString("") { if (it > 0) "#" else "." }
        })
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
        for (top in 0..99) for (bottom in 0..99) {
            MatrixDigits.renderPair(top, bottom, 13).forEachIndexed { i, b ->
                if (b > 0) assertEquals("pixel $i for $top/$bottom", '#', leds[i])
            }
        }
    }

    @Test
    fun usesRequestedBrightness() {
        val frame = MatrixDigits.render(0, 13, brightness = 90)
        assertEquals(setOf(0, 90), frame.toSet())
    }
}
