package com.example.todolist

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class YearDotsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dotPaintPassed = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaintRemaining = Paint(Paint.ANTI_ALIAS_FLAG)

    var passedDays: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    init {
        // Vibrant white for passed days, subtle grey for remaining
        dotPaintPassed.color = ContextCompat.getColor(context, R.color.text_primary)
        dotPaintRemaining.color = ContextCompat.getColor(context, R.color.text_secondary).let {
            (it and 0x00FFFFFF) or 0x22000000 // Very low alpha
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val dotsTotal = 365
        val columns = 30 
        val rows = (dotsTotal + columns - 1) / columns
        
        val dotRadius = (width.toFloat() / (columns * 3.2f)).coerceAtMost(height.toFloat() / (rows * 3.2f))
        val spacingX = width.toFloat() / columns
        val spacingY = height.toFloat() / rows

        for (i in 0 until dotsTotal) {
            val row = i / columns
            val col = i % columns
            
            val cx = col * spacingX + spacingX / 2
            val cy = row * spacingY + spacingY / 2
            
            val paint = if (i < passedDays) dotPaintPassed else dotPaintRemaining
            canvas.drawCircle(cx, cy, dotRadius, paint)
        }
    }
}
