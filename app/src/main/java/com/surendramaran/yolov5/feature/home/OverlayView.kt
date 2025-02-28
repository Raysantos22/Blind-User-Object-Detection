package com.surendramaran.yolov5.feature.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private var results = listOf<BoundingBox>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()
    private var distancePaint = Paint()
    private var bounds = Rect()

    init {
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        distancePaint.color = Color.YELLOW
        distancePaint.style = Paint.Style.FILL
        distancePaint.textSize = 40f

        boxPaint.color = Color.WHITE
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    // Add the missing setResults method
    fun setResults(boundingBoxes: List<BoundingBox>) {
        results = boundingBoxes
        invalidate() // Request a redraw of the view
    }

    fun clear() {
        results = emptyList()
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        distancePaint.reset()
        invalidate()
        initPaints()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        results.forEach { box ->
            val left = box.x1 * width
            val top = box.y1 * height
            val right = box.x2 * width
            val bottom = box.y2 * height
            val distance = box.calculateDistance()

            // Color-code the box based on distance
            boxPaint.color = when {
                distance <= 1.0f -> Color.RED
                distance <= 2.0f -> Color.YELLOW
                distance <= 3.0f -> Color.GREEN
                else -> Color.WHITE
            }

            // Draw bounding box
            canvas.drawRect(left, top, right, bottom, boxPaint)

            // Draw object name
            val drawableText = box.clsName
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()

            canvas.drawRect(
                left,
                top,
                left + textWidth + BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint)

            // Draw distance below the box
            val distanceText = String.format("%.1fm", distance)
            canvas.drawText(distanceText, left, bottom + 40f, distancePaint)
        }
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}