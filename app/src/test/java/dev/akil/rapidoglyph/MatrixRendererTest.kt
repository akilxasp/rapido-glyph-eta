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
    fun everyEtaValueProducesAUniqueValidFrame() {
        val frames = (1..99).map(MatrixRenderer::eta)

        frames.forEach { frame ->
            assertEquals(169, frame.size)
            assertTrue(frame.any { it == 255 })
            assertTrue(frame.all { it == 0 || it == 255 })
        }
        assertEquals(99, frames.map(IntArray::toList).distinct().size)
    }

    @Test
    fun idleFrameIsACenteredSmilingFace() {
        val frame = MatrixRenderer.eta(null)

        assertEquals(169, frame.size)
        assertEquals(255, frame[4 * MatrixRenderer.SIZE + 3])
        assertEquals(255, frame[8 * MatrixRenderer.SIZE + 3])
        assertEquals(255, frame[3 * MatrixRenderer.SIZE + 7])
        assertEquals(255, frame[6 * MatrixRenderer.SIZE + 9])
        assertEquals(255, frame[9 * MatrixRenderer.SIZE + 7])
        assertEquals(13, frame.count { it == 255 })
        assertTrue(frame.all { it == 0 || it == 255 })
    }

    @Test
    fun essentialKeyAnimationTravelsAroundACircularRingWithStackedTime() {
        val frames = MatrixRenderer.essentialKeyAnimation(
            hourOfDay = 18,
            minute = 7,
            use24HourFormat = false,
        )
        val rightMiddle = 6 * MatrixRenderer.SIZE + 12
        val leftMiddle = 6 * MatrixRenderer.SIZE
        val separator = 6 * MatrixRenderer.SIZE + 6

        assertEquals(23, frames.size)
        assertEquals(255, frames.first()[rightMiddle])
        assertEquals(255, frames[18][leftMiddle])
        assertEquals(255, frames.first()[separator])
        assertEquals(255, frames.last()[separator])
        frames.forEach { frame ->
            assertEquals(169, frame.size)
            assertEquals(255, frame[separator])
            assertTrue(frame.all { it in 0..255 })
        }
        listOf(0, 12, 156, 168).forEach { corner ->
            assertTrue(frames.all { it[corner] == 0 })
        }

        val clock = frames.last()
        assertTrue((1..5).any { y -> clock.row(y).any { it == 255 } })
        assertTrue((7..11).any { y -> clock.row(y).any { it == 255 } })
        assertEquals(1, clock.row(6).count { it == 255 })
    }

    @Test
    fun essentialKeyClockUsesTwelveHourTimeAndPadsMinutes() {
        val midnight = MatrixRenderer.essentialKeyAnimation(
            hourOfDay = 0,
            minute = 5,
            use24HourFormat = false,
        ).last()
        val noon = MatrixRenderer.essentialKeyAnimation(
            hourOfDay = 12,
            minute = 5,
            use24HourFormat = false,
        ).last()

        assertEquals(midnight.toList(), noon.toList())
        assertEquals(255, midnight[6 * MatrixRenderer.SIZE + 6])
        assertTrue(midnight.row(1).any { it == 255 })
        assertTrue(midnight.row(11).any { it == 255 })
        assertTrue(midnight.any { it == 255 })
    }

    @Test
    fun essentialKeyClockHonorsTwentyFourHourPreference() {
        val twelveHour = MatrixRenderer.essentialKeyAnimation(
            hourOfDay = 18,
            minute = 7,
            use24HourFormat = false,
        ).last()
        val twentyFourHour = MatrixRenderer.essentialKeyAnimation(
            hourOfDay = 18,
            minute = 7,
            use24HourFormat = true,
        ).last()

        assertTrue(twelveHour.toList() != twentyFourHour.toList())
        assertEquals(255, twentyFourHour[6 * MatrixRenderer.SIZE + 6])
        assertTrue(twentyFourHour.row(1).any { it == 255 })
        assertTrue(twentyFourHour.row(11).any { it == 255 })
    }

    private fun IntArray.row(y: Int): List<Int> =
        slice(y * MatrixRenderer.SIZE until (y + 1) * MatrixRenderer.SIZE)
}
