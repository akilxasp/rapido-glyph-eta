package dev.akil.rapidoglyph

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import kotlin.math.min

/**
 * A quiet, decorative preview of the exact 13×13 frame sent to the Glyph Matrix.
 *
 * It has no timer of its own: the activity invalidates it only when the displayed
 * ETA changes, so the flourish adds no background work during a ride.
 */
class GlyphMatrixPreviewView(context: Context) : View(context) {
    private val offPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = OFF
    }
    private val onPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ON
    }
    private var frame = MatrixRenderer.eta(null)

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun showMinutes(
        minutes: Int?,
        brightnessPercent: Int,
        restingFrame: IntArray? = null,
    ) {
        val next = MatrixRenderer.withBrightness(
            MatrixRenderer.eta(minutes, restingFrame),
            brightnessPercent,
        )
        if (!frame.contentEquals(next)) {
            frame = next
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        val cell = size / MatrixRenderer.SIZE
        val radius = cell * 0.24f
        val offsetX = (width - size) / 2f
        val offsetY = (height - size) / 2f

        frame.forEachIndexed { index, brightness ->
            val x = index % MatrixRenderer.SIZE
            val y = index / MatrixRenderer.SIZE
            if (brightness > 0) {
                onPaint.color = Color.rgb(brightness, brightness, brightness)
            }
            canvas.drawCircle(
                offsetX + (x + 0.5f) * cell,
                offsetY + (y + 0.5f) * cell,
                radius,
                if (brightness > 0) onPaint else offPaint,
            )
        }
    }

    private companion object {
        const val OFF = 0xFF303030.toInt()
        val ON = Color.WHITE
    }
}
