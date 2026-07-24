package dev.akil.rapidoglyph

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphPreviewTest {
    @Test
    fun acceptsRecentPreviewRequest() {
        assertTrue(isPreviewRequestFresh(requestedAtMillis = 1_000L, nowMillis = 60_000L))
    }

    @Test
    fun rejectsExpiredOrFuturePreviewRequest() {
        assertFalse(isPreviewRequestFresh(requestedAtMillis = 1_000L, nowMillis = 121_001L))
        assertFalse(isPreviewRequestFresh(requestedAtMillis = 2_000L, nowMillis = 1_000L))
    }
}
