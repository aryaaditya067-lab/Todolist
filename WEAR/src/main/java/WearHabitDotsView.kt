package com.example.todolist.wear

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.util.Calendar

/**
 * Phone app ke HabitTrackerView ka Wear OS version.
 * Round screen ke hisab se dots circular arc mein arrange hote hain.
 * Last 30 days ke completion ratio se dot color fill hota hai.
 */
class WearHabitDotsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var habitData: Map<Long, Float> = emptyMap()
    private var accentColor: Int = 0xFFFF6B00.toInt() // Phone app Orange

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x33FFFFFF
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val todayRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = 0xFFFFFFFF.toInt()
    }

    fun setHabitData(data: Map<Long, Float>) {
        habitData = data
        invalidate()
    }

    fun setAccentColor(color: Int) {
        accentColor = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f

        // 28 dots in a grid — 7 columns x 4 rows (matching phone widget style)
        val totalDays = 28
        val cols = 7
        val rows = 4
        val dotRadius = (w / (cols * 3.8f)).coerceAtMost(7f)
        val spacingX = (w - (cols * dotRadius * 2)) / (cols + 1)
        val spacingY = (h - (rows * dotRadius * 2)) / (rows + 1)

        val today = getTodayTimestamp()

        for (i in 0 until totalDays) {
            val dayOffset = totalDays - 1 - i  // 0 = today, 29 = 30 days ago
            val dayTimestamp = today - (dayOffset * 86400000L)

            val col = i % cols
            val row = i / cols

            val dotX = spacingX + col * (dotRadius * 2 + spacingX) + dotRadius
            val dotY = spacingY + row * (dotRadius * 2 + spacingY) + dotRadius

            // Background circle
            canvas.drawCircle(dotX, dotY, dotRadius, bgPaint)

            // Fill based on completion ratio
            val ratio = habitData[dayTimestamp] ?: 0f
            if (ratio > 0f) {
                fillPaint.color = blendColor(accentColor, ratio)

                if (ratio >= 1f) {
                    // Full completion — full circle
                    canvas.drawCircle(dotX, dotY, dotRadius, fillPaint)
                } else {
                    // Partial — arc from bottom (like filling)
                    val sweepAngle = 360f * ratio
                    val oval = RectF(dotX - dotRadius, dotY - dotRadius, dotX + dotRadius, dotY + dotRadius)
                    canvas.drawArc(oval, -90f, sweepAngle, true, fillPaint)
                }
            }

            // Today ka ring highlight
            if (dayOffset == 0) {
                canvas.drawCircle(dotX, dotY, dotRadius + 1.5f, todayRingPaint)
            }
        }
    }

    private fun blendColor(baseColor: Int, ratio: Float): Int {
        val r = (baseColor shr 16) and 0xFF
        val g = (baseColor shr 8) and 0xFF
        val b = baseColor and 0xFF
        val alpha = (0x66 + (0x99 * ratio)).toInt().coerceIn(0, 255)
        return (alpha shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun getTodayTimestamp(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}