package dev.akil.rapidoglyph

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EssentialKeyDetectorTest {
    @Test
    fun acceptsOnlyTheInitialUnknownKeyDownEvent() {
        assertTrue(EssentialKeyDetector.shouldRefresh(keyCode = 0, action = 0, repeatCount = 0))
        assertFalse(EssentialKeyDetector.shouldRefresh(keyCode = 0, action = 1, repeatCount = 0))
        assertFalse(EssentialKeyDetector.shouldRefresh(keyCode = 0, action = 0, repeatCount = 1))
        assertFalse(EssentialKeyDetector.shouldRefresh(keyCode = 24, action = 0, repeatCount = 0))
    }
}
