package dev.akil.rapidoglyph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GlyphDesignImporterTest {
    @Test
    fun parsesGlyphMuseumPixelsAndNormalizesTheMaximumToFullBrightness() {
        val source = MutableList(137) { 0 }.apply {
            this[27] = 35
            this[40] = 129
            this[80] = 49
            this[94] = 93
        }
        val json = """{"v":4,"frames":[{"d":50,"p":[${source.joinToString()}]}]}"""

        val frame = GlyphDesignImporter.parse(json)

        assertEquals(169, frame.size)
        assertEquals(69, frame[3 * MatrixRenderer.SIZE + 3])
        assertEquals(255, frame[4 * MatrixRenderer.SIZE + 4])
        assertEquals(97, frame[7 * MatrixRenderer.SIZE + 5])
        assertEquals(184, frame[8 * MatrixRenderer.SIZE + 6])
        assertEquals(0, frame[0])
    }

    @Test
    fun rejectsFramesThatAreNotThirteenByThirteen() {
        assertThrows(IllegalArgumentException::class.java) {
            GlyphDesignImporter.parse("""{"frames":[{"p":[0,1]}]}""")
        }
    }

    @Test
    fun rejectsCompletelyDarkFrames() {
        val pixels = List(169) { 0 }.joinToString()
        assertThrows(IllegalArgumentException::class.java) {
            GlyphDesignImporter.parse("""{"frames":[{"p":[$pixels]}]}""")
        }
    }
}
