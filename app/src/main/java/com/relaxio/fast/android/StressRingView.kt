package com.relaxio.fast.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class StressRingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var progress = 0 // Default progress to 0
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 20f
        color = ContextCompat.getColor(context, android.R.color.holo_orange_light)
        strokeCap = Paint.Cap.ROUND
    }

    private val ringBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 20f
        color = ContextCompat.getColor(context, android.R.color.darker_gray)
    }

    private val ovalRect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Define the ring bounds
        val padding = 30f
        ovalRect.set(padding, padding, width - padding, height - padding)

        // Draw background ring
        canvas.drawArc(ovalRect, 0f, 360f, false, ringBackgroundPaint)

        // Draw progress ring
        val sweepAngle = (progress / 100f) * 360f
        canvas.drawArc(ovalRect, -90f, sweepAngle, false, ringPaint)
    }

    fun setProgress(progress: Int) {
        this.progress = progress
        invalidate() // Redraw the view
    }
}
