package dev.akil.rapidoglyph

object EssentialKeyDetector {
    private const val KEYCODE_UNKNOWN = 0
    private const val ACTION_DOWN = 0

    fun shouldRefresh(keyCode: Int, action: Int, repeatCount: Int): Boolean =
        keyCode == KEYCODE_UNKNOWN && action == ACTION_DOWN && repeatCount == 0
}
