package dev.akil.rapidoglyph

import kotlin.math.roundToInt

object MatrixRenderer {
    const val SIZE = 13
    private const val MAX_BRIGHTNESS = 255

    private val glyphs = mapOf(
        '0' to listOf("111", "101", "101", "101", "111"),
        '1' to listOf("010", "110", "010", "010", "111"),
        '2' to listOf("111", "001", "111", "100", "111"),
        '3' to listOf("111", "001", "111", "001", "111"),
        '4' to listOf("101", "101", "111", "001", "001"),
        '5' to listOf("111", "100", "111", "001", "111"),
        '6' to listOf("111", "100", "111", "101", "111"),
        '7' to listOf("111", "001", "010", "010", "010"),
        '8' to listOf("111", "101", "111", "101", "111"),
        '9' to listOf("111", "101", "111", "001", "111"),
        'm' to listOf("000", "000", "111", "111", "101"),
    )

    fun eta(minutes: Int?): IntArray {
        if (minutes == null) return idle()
        val text = minutes.coerceIn(0, 99).toString().plus("m")
        val width = text.length * 3 + (text.length - 1)
        val startX = (SIZE - width) / 2
        val startY = (SIZE - 5) / 2
        val frame = IntArray(SIZE * SIZE)

        text.forEachIndexed { index, character ->
            val glyph = glyphs.getValue(character)
            val offsetX = startX + index * 4
            glyph.forEachIndexed { y, row ->
                row.forEachIndexed { x, pixel ->
                    if (pixel == '1') {
                        frame[(startY + y) * SIZE + offsetX + x] = MAX_BRIGHTNESS
                    }
                }
            }
        }
        return frame
    }

    fun idle(): IntArray =
        IntArray(SIZE * SIZE).also { frame ->
            val eyes = listOf(4 to 4, 8 to 4)
            val smile = listOf(
                4 to 8,
                5 to 9,
                6 to 9,
                7 to 9,
                8 to 8,
            )
            (eyes + smile).forEach { (x, y) ->
                frame[indexOf(x, y)] = MAX_BRIGHTNESS
            }
        }

    fun withBrightness(frame: IntArray, percent: Int): IntArray {
        val safePercent = percent.coerceIn(MIN_BRIGHTNESS_PERCENT, MAX_BRIGHTNESS_PERCENT)
        if (safePercent == MAX_BRIGHTNESS_PERCENT) return frame.copyOf()
        val factor = safePercent / MAX_BRIGHTNESS_PERCENT.toFloat()
        return IntArray(frame.size) { index ->
            (frame[index] * factor).roundToInt().coerceIn(0, MAX_BRIGHTNESS)
        }
    }

    fun essentialKeyAnimation(
        hourOfDay: Int,
        minute: Int,
        use24HourFormat: Boolean,
    ): List<IntArray> {
        require(hourOfDay in 0..23) { "hourOfDay must be between 0 and 23" }
        require(minute in 0..59) { "minute must be between 0 and 59" }

        val topPath = listOf(
            12 to 6,
            12 to 5,
            12 to 4,
            11 to 3,
            11 to 2,
            10 to 1,
            9 to 1,
            8 to 0,
            7 to 0,
            6 to 0,
            5 to 0,
            4 to 0,
            3 to 1,
            2 to 1,
            1 to 2,
            1 to 3,
            0 to 4,
            0 to 5,
            0 to 6,
        )
        val bottomPath = topPath.map { (x, y) -> x to SIZE - 1 - y }
        val trailBrightness = floatArrayOf(1f, 0.69f, 0.41f, 0.19f)
            .map(::brightness)
        val clock = clock(hourOfDay, minute, use24HourFormat)
        val movement = topPath.indices.map { step ->
            clock.copyOf().also { frame ->
                trailBrightness.forEachIndexed { offset, trailLevel ->
                    val pathIndex = step - offset
                    if (pathIndex >= 0) {
                        listOf(topPath[pathIndex], bottomPath[pathIndex]).forEach { (x, y) ->
                            val index = y * SIZE + x
                            frame[index] = maxOf(frame[index], trailLevel)
                        }
                    }
                }
            }
        }
        val finalFrame = movement.last()
        val fade = listOf(0.65f, 0.35f, 0.15f, 0f).map { factor ->
            IntArray(finalFrame.size) { index ->
                val fadedAnimation = brightness(
                    finalFrame[index] / MAX_BRIGHTNESS.toFloat() * factor,
                )
                maxOf(clock[index], fadedAnimation)
            }
        }
        return movement + fade
    }

    private fun clock(hourOfDay: Int, minute: Int, use24HourFormat: Boolean): IntArray {
        val hourText = if (use24HourFormat) {
            hourOfDay.toString().padStart(2, '0')
        } else {
            when (val hour12 = hourOfDay % 12) {
                0 -> "12"
                else -> hour12.toString()
            }
        }
        val frame = IntArray(SIZE * SIZE)
        drawCenteredText(frame, hourText, CLOCK_HOUR_Y)
        drawCenteredText(
            frame,
            minute.toString().padStart(2, '0'),
            CLOCK_MINUTE_Y,
        )
        return frame
    }

    private fun drawCenteredText(frame: IntArray, text: String, startY: Int) {
        val width = text.length * GLYPH_WIDTH + (text.length - 1) * GLYPH_SPACING
        val startX = (SIZE - width) / 2
        text.forEachIndexed { index, character ->
            val offsetX = startX + index * (GLYPH_WIDTH + GLYPH_SPACING)
            glyphs.getValue(character).forEachIndexed { y, row ->
                row.forEachIndexed { x, pixel ->
                    if (pixel == '1') {
                        frame[indexOf(offsetX + x, startY + y)] = MAX_BRIGHTNESS
                    }
                }
            }
        }
    }

    private const val GLYPH_WIDTH = 3
    private const val GLYPH_SPACING = 1
    private const val CLOCK_HOUR_Y = 1
    private const val CLOCK_MINUTE_Y = 7
    private const val MIN_BRIGHTNESS_PERCENT = 1
    private const val MAX_BRIGHTNESS_PERCENT = 100

    private fun indexOf(x: Int, y: Int) = y * SIZE + x

    private fun brightness(fraction: Float): Int =
        (MAX_BRIGHTNESS * fraction.coerceIn(0f, 1f)).roundToInt()
}
