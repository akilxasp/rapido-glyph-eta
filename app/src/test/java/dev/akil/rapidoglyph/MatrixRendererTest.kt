package dev.akil.rapidoglyph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatrixRendererTest {
    @Test
    fun producesA13By13Frame() {
        val frame = MatrixRenderer.eta(7)
        assertEquals(169, frame.size)
        assertTrue(frame.any { it == 255 })
        assertTrue(frame.all { it == 0 || it == 255 })
    }
}

