package dev.akil.rapidoglyph

object MatrixRenderer {
    const val SIZE = 13
    private const val ON = 255

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
                    if (pixel == '1') frame[(startY + y) * SIZE + offsetX + x] = ON
                }
            }
        }
        return frame
    }

    fun idle(): IntArray =
        IntArray(SIZE * SIZE).also { frame ->
            frame[indexOf(6, 6)] = 255
            listOf(4 to 4, 8 to 4, 8 to 8, 4 to 8).forEach { (x, y) ->
                frame[indexOf(x, y)] = 144
            }
            listOf(6 to 3, 9 to 6, 6 to 9, 3 to 6).forEach { (x, y) ->
                frame[indexOf(x, y)] = 64
            }
        }

    fun essentialKeyAnimation(): List<IntArray> {
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
        val trailBrightness = intArrayOf(255, 176, 104, 48)
        val movement = topPath.indices.map { step ->
            IntArray(SIZE * SIZE).also { frame ->
                trailBrightness.forEachIndexed { offset, brightness ->
                    val pathIndex = step - offset
                    if (pathIndex >= 0) {
                        listOf(topPath[pathIndex], bottomPath[pathIndex]).forEach { (x, y) ->
                            val index = y * SIZE + x
                            frame[index] = maxOf(frame[index], brightness)
                        }
                    }
                }
            }
        }
        val finalFrame = movement.last()
        val fade = listOf(0.65f, 0.35f, 0.15f, 0f).map { factor ->
            IntArray(finalFrame.size) { index -> (finalFrame[index] * factor).toInt() }
        }
        return movement + fade
    }

    private fun indexOf(x: Int, y: Int) = y * SIZE + x
}
