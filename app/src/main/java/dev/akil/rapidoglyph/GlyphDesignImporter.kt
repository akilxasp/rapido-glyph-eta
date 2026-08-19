package dev.akil.rapidoglyph

import kotlin.math.roundToInt

object GlyphDesignImporter {
    private val pixelsPattern = Regex(
        "\\\"p\\\"\\s*:\\s*\\[([^]]*)]",
        RegexOption.DOT_MATCHES_ALL,
    )

    fun parse(json: String): IntArray {
        val pixelText = pixelsPattern.find(json)?.groupValues?.get(1)
            ?: throw IllegalArgumentException("No frame pixel array found")
        val values = pixelText.split(',').map { token ->
            token.trim().toIntOrNull()
                ?: throw IllegalArgumentException("Pixel values must be integers")
        }
        require(values.all { it in 0..255 }) {
            "Pixel values must be between 0 and 255"
        }
        val expanded = when (values.size) {
            PHYSICAL_PIXEL_COUNT -> expandPhysicalPixels(values)
            MatrixRenderer.SIZE * MatrixRenderer.SIZE -> values.toIntArray()
            else -> throw IllegalArgumentException(
                "Design must contain 137 physical or 169 matrix pixels",
            )
        }
        val sourceMaximum = expanded.max()
        require(sourceMaximum > 0) { "Design must contain at least one lit pixel" }
        return IntArray(expanded.size) { index ->
            (expanded[index] * 255f / sourceMaximum).roundToInt().coerceIn(0, 255)
        }
    }

    private fun expandPhysicalPixels(values: List<Int>): IntArray {
        val frame = IntArray(MatrixRenderer.SIZE * MatrixRenderer.SIZE)
        var sourceIndex = 0
        for (y in 0 until MatrixRenderer.SIZE) {
            for (x in 0 until MatrixRenderer.SIZE) {
                val offsetX = x - MatrixRenderer.SIZE / 2
                val offsetY = y - MatrixRenderer.SIZE / 2
                if (offsetX * offsetX + offsetY * offsetY <= CIRCLE_RADIUS_SQUARED) {
                    frame[y * MatrixRenderer.SIZE + x] = values[sourceIndex++]
                }
            }
        }
        check(sourceIndex == PHYSICAL_PIXEL_COUNT)
        return frame
    }

    private const val PHYSICAL_PIXEL_COUNT = 137
    private const val CIRCLE_RADIUS_SQUARED = 42
}
