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
        '-' to listOf("000", "000", "111", "000", "000"),
    )

    fun eta(minutes: Int?): IntArray {
        val text = minutes?.coerceIn(0, 99)?.toString()?.plus("m") ?: "--"
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
}

