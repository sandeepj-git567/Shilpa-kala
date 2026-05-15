package com.shilpakala

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class GuideOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E07B29")
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val dimPaint = Paint().apply {
        color = Color.parseColor("#33000000")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        val left   = w * 0.12f
        val right  = w * 0.88f
        val top    = h * 0.15f
        val bottom = h * 0.85f
        val len    = 55f

        // Dim the area outside the guide box (optional visual aid)
        // Top band
        canvas.drawRect(0f, 0f, w, top, dimPaint)
        // Bottom band
        canvas.drawRect(0f, bottom, w, h, dimPaint)
        // Left band
        canvas.drawRect(0f, top, left, bottom, dimPaint)
        // Right band
        canvas.drawRect(right, top, w, bottom, dimPaint)

        // Top-left corner
        canvas.drawLine(left, top, left + len, top, cornerPaint)
        canvas.drawLine(left, top, left, top + len, cornerPaint)

        // Top-right corner
        canvas.drawLine(right, top, right - len, top, cornerPaint)
        canvas.drawLine(right, top, right, top + len, cornerPaint)

        // Bottom-left corner
        canvas.drawLine(left, bottom, left + len, bottom, cornerPaint)
        canvas.drawLine(left, bottom, left, bottom - len, cornerPaint)

        // Bottom-right corner
        canvas.drawLine(right, bottom, right - len, bottom, cornerPaint)
        canvas.drawLine(right, bottom, right, bottom - len, cornerPaint)
    }
}
