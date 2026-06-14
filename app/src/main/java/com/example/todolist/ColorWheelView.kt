package com.example.todolist

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class ColorWheelView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val wheelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val innerSelectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f
    private var selectedX = 0f
    private var selectedY = 0f
    private var selectedColor = Color.RED

    var onColorChangeListener: ((Int) -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        centerX = w / 2f
        centerY = h / 2f
        radius = min(centerX, centerY) - 20f
        
        val sweepGradient = SweepGradient(centerX, centerY, 
            intArrayOf(Color.RED, Color.MAGENTA, Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW, Color.RED),
            null)
        wheelPaint.shader = sweepGradient
        
        updateSelectorPosition()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawCircle(centerX, centerY, radius, wheelPaint)
        
        // Draw Radial Gradient (White in center)
        val radialShader = RadialGradient(centerX, centerY, radius, Color.WHITE, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        val radialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = radialShader }
        canvas.drawCircle(centerX, centerY, radius, radialPaint)

        // Draw Selector
        canvas.drawCircle(selectedX, selectedY, 15f, innerSelectorPaint.apply { color = selectedColor })
        canvas.drawCircle(selectedX, selectedY, 15f, selectorPaint)
    }

    private fun updateSelectorPosition() {
        val hsv = FloatArray(3)
        Color.colorToHSV(selectedColor, hsv)
        val angle = Math.toRadians(hsv[0].toDouble())
        val dist = hsv[1] * radius
        selectedX = (centerX + cos(angle) * dist).toFloat()
        selectedY = (centerY - sin(angle) * dist).toFloat()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x - centerX
        val y = event.y - centerY
        var dist = sqrt(x * x + y * y)

        if (dist > radius) dist = radius

        val angle = atan2(-y, x)
        var hue = Math.toDegrees(angle.toDouble()).toFloat()
        if (hue < 0) hue += 360f

        val saturation = dist / radius
        selectedColor = Color.HSVToColor(floatArrayOf(hue, saturation, 1f))
        
        selectedX = centerX + x * (dist / sqrt(x * x + y * y))
        selectedY = centerY + y * (dist / sqrt(x * x + y * y))

        onColorChangeListener?.invoke(selectedColor)
        invalidate()
        return true
    }

    fun setColor(color: Int) {
        selectedColor = color
        if (width > 0) {
            updateSelectorPosition()
            invalidate()
        }
    }
}
