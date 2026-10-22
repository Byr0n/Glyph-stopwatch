package com.byr0n.glyphstopwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PressClassifierTest {
    private val classifier = PressClassifier(windowMs = 400)

    @Test
    fun lonePressBecomesSingleAfterWindow() {
        assertNull(classifier.onPress(1_000))
        assertNull(classifier.onWindowElapsed(1_200))
        assertEquals(Press.SINGLE, classifier.onWindowElapsed(1_400))
        assertNull(classifier.onWindowElapsed(1_500))
    }

    @Test
    fun twoPressesInsideWindowAreDouble() {
        assertNull(classifier.onPress(1_000))
        assertEquals(Press.DOUBLE, classifier.onPress(1_300))
        assertNull(classifier.onWindowElapsed(1_400))
    }

    @Test
    fun secondPressAfterWindowStartsNewPress() {
        classifier.onPress(1_000)
        assertNull(classifier.onPress(1_500))
        assertEquals(Press.SINGLE, classifier.onWindowElapsed(1_900))
    }

    @Test
    fun threePressesAreDoubleThenPending() {
        classifier.onPress(0)
        assertEquals(Press.DOUBLE, classifier.onPress(200))
        assertNull(classifier.onPress(300))
        assertEquals(Press.SINGLE, classifier.onWindowElapsed(700))
    }
}
