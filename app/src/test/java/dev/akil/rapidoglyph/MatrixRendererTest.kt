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

    @Test
    fun everySweepValueProducesAUniqueValidFrame() {
        val frames = (1..99).map(MatrixRenderer::eta)

        frames.forEach { frame ->
            assertEquals(169, frame.size)
            assertTrue(frame.any { it == 255 })
            assertTrue(frame.all { it == 0 || it == 255 })
        }
        assertEquals(99, frames.map(IntArray::toList).distinct().size)
    }

    @Test
    fun essentialKeyAnimationTravelsFromRightToLeftAlongEdgesAndFades() {
        val frames = MatrixRenderer.essentialKeyAnimation()
        val rightMiddle = 6 * MatrixRenderer.SIZE + 12
        val leftMiddle = 6 * MatrixRenderer.SIZE

        assertEquals(29, frames.size)
        assertEquals(255, frames.first()[rightMiddle])
        assertEquals(255, frames[24][leftMiddle])
        assertTrue(frames.last().all { it == 0 })
        frames.forEach { frame ->
            assertEquals(169, frame.size)
            assertTrue(frame.all { it in 0..255 })
            frame.forEachIndexed { index, brightness ->
                val x = index % MatrixRenderer.SIZE
                val y = index / MatrixRenderer.SIZE
                if (brightness > 0) {
                    assertTrue(x == 0 || x == 12 || y == 0 || y == 12)
                }
            }
        }
    }
}
