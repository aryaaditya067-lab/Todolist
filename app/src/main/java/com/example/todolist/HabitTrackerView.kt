package com.example.todolist

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class HabitTrackerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val completedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val pastPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * resources.displayMetrics.density
    }
    private val todayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val todayStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * resources.displayMetrics.density
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12f * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private var accentColor: Int = Color.parseColor("#FF6B00") // Vibrant Orange

    var completionData: Map<Int, Int> = emptyMap()
        set(value) {
            field = value
            invalidate()
        }

    var displayMonth: Int = Calendar.getInstance().get(Calendar.MONTH)
        set(value) {
            field = value
            invalidate()
        }

    var isYearlyMode: Boolean = false
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    init {
        updatePaints()
    }

    fun setAccentColor(color: Int) {
        if (color != 0) {
            accentColor = color
            updatePaints()
            invalidate()
        }
    }

    private fun updatePaints() {
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        
        completedPaint.color = accentColor
        
        val alpha = (0.4 * 255).toInt()
        val fadedColor = Color.argb(alpha, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        pastPaint.color = fadedColor
        
        emptyPaint.color = accentColor 
        
        todayPaint.color = accentColor
        todayStrokePaint.color = if (isDarkMode) Color.WHITE else Color.BLACK 
        textPaint.color = ContextCompat.getColor(context, R.color.text_primary)
    }

    fun setTasks(tasks: List<Task>) {
        val calendar = Calendar.getInstance()
        val data = mutableMapOf<Int, Int>()
        tasks.groupBy {
            calendar.timeInMillis = it.date
            calendar.get(Calendar.DAY_OF_YEAR)
        }.forEach { (dayOfYear, dayTasks) ->
            if (dayTasks.isNotEmpty() && dayTasks.all { it.done }) {
                data[dayOfYear] = 2
            } else if (dayTasks.any { it.done }) {
                data[dayOfYear] = 1
            }
        }
        completionData = data
    }

    private fun getYearlyHeight(width: Int): Int {
        val w = if (width <= 0) resources.displayMetrics.widthPixels else width
        val calendar = Calendar.getInstance()
        val usableWidth = (w - paddingLeft - paddingRight).toFloat().coerceAtLeast(100f)
        val spacingX = usableWidth / 7f
        
        var totalHeight = paddingTop.toFloat()
        for (month in 0..11) {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.MONTH, month)
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val firstDayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7
            val weeks = Math.ceil((daysInMonth + firstDayOfWeek).toDouble() / 7).toInt()
            totalHeight += (spacingX * 2.8f) + (weeks * spacingX) + (spacingX * 0.5f)
        }
        return (totalHeight + paddingBottom).toInt().coerceAtLeast(300)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec).coerceAtLeast(100)
        if (isYearlyMode) {
            setMeasuredDimension(width, getYearlyHeight(width))
        } else {
            val h = MeasureSpec.getSize(heightMeasureSpec)
            val heightMode = MeasureSpec.getMode(heightMeasureSpec)
            val measuredHeight = if (heightMode == MeasureSpec.UNSPECIFIED || h == 0) (width / 7 * 7) else h
            setMeasuredDimension(width, measuredHeight)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        updatePaints()

        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH)
        val todayDate = now.get(Calendar.DAY_OF_MONTH)

        if (isYearlyMode) {
            drawYearlyVerticalGrid(canvas, now)
        } else {
            drawMonthlyGrid(canvas, currentYear, currentMonth, todayDate)
        }
    }

    private fun drawMonthlyGrid(canvas: Canvas, currentYear: Int, currentMonth: Int, todayDate: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.MONTH, displayMonth)
        
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7
        
        val columns = 7
        val usableWidth = (width - paddingLeft - paddingRight).toFloat().coerceAtLeast(1f)
        val spacingX = usableWidth / columns
        
        val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
        for (i in daysOfWeek.indices) {
            val x = paddingLeft + (i + 0.5f) * spacingX
            canvas.drawText(daysOfWeek[i], x, paddingTop + spacingX * 0.5f, textPaint)
        }

        val gridStartY = spacingX * 1.0f
        val usableHeight = (height - paddingTop - paddingBottom - gridStartY).toFloat().coerceAtLeast(1f)
        val totalCells = daysInMonth + firstDayOfWeek
        val rows = Math.ceil(totalCells.toDouble() / columns).toInt().coerceAtLeast(1)
        val spacingY = usableHeight / rows
        val dotRadius = (spacingX * 0.28f).coerceAtMost(spacingY * 0.28f)

        for (day in 1..daysInMonth) {
            val gridIndex = day - 1 + firstDayOfWeek
            val row = gridIndex / columns
            val col = gridIndex % columns

            val cx = paddingLeft + col * spacingX + spacingX / 2
            val cy = paddingTop + gridStartY + row * spacingY + spacingY / 2

            val isToday = displayMonth == currentMonth && day == todayDate
            calendar.set(Calendar.DAY_OF_MONTH, day)
            calendar.set(Calendar.MONTH, displayMonth)
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            val isActualToday = isToday && (Calendar.getInstance().get(Calendar.YEAR) == currentYear)
            
            val status = completionData[dayOfYear] ?: 0
            val isPast = calendar.before(Calendar.getInstance()) && !isActualToday

            drawDot(canvas, cx, cy, dotRadius, isActualToday, isPast, status)
        }
    }

    private fun drawYearlyVerticalGrid(canvas: Canvas, now: Calendar) {
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH)
        val currentDay = now.get(Calendar.DAY_OF_MONTH)
        
        val usableWidth = (width - paddingLeft - paddingRight).toFloat().coerceAtLeast(1f)
        val spacingX = usableWidth / 7f
        var currentY = paddingTop.toFloat()

        val calendar = Calendar.getInstance()
        for (month in 0..11) {
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
            
            val originalTypeface = textPaint.typeface
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.textSize = spacingX * 0.65f
            canvas.drawText(monthName.uppercase(), paddingLeft + spacingX * 0.2f, currentY + spacingX * 0.8f, textPaint)
            
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = spacingX * 0.35f
            val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
            for (i in daysOfWeek.indices) {
                val x = paddingLeft + (i + 0.5f) * spacingX
                canvas.drawText(daysOfWeek[i], x, currentY + spacingX * 1.8f, textPaint)
            }
            
            currentY += spacingX * 2.5f
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val firstDayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7
            val dotRadius = spacingX * 0.35f
            
            for (day in 1..daysInMonth) {
                val gridIndex = day - 1 + firstDayOfWeek
                val row = gridIndex / 7
                val col = gridIndex % 7
                
                val cx = paddingLeft + (col + 0.5f) * spacingX
                val cy = currentY + (row + 0.5f) * spacingX
                
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
                
                val isToday = (calendar.get(Calendar.YEAR) == currentYear && month == currentMonth && day == currentDay)
                val status = completionData[dayOfYear] ?: 0
                val isPast = calendar.before(now) && !isToday
                
                drawDot(canvas, cx, cy, dotRadius, isToday, isPast, status)
            }
            
            val weeks = Math.ceil((daysInMonth + firstDayOfWeek).toDouble() / 7).toInt()
            currentY += (weeks * spacingX) + (spacingX * 0.8f)
            textPaint.typeface = originalTypeface
        }
    }

    private fun drawDot(canvas: Canvas, cx: Float, cy: Float, radius: Float, isToday: Boolean, isPast: Boolean, status: Int) {
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val todayRingColor = if (isDarkMode) Color.WHITE else Color.BLACK

        // Today always gets a ring
        if (isToday) {
            todayStrokePaint.color = todayRingColor
            canvas.drawCircle(cx, cy, radius * 1.5f, todayStrokePaint)
        }
        
        when {
            status == 2 -> canvas.drawCircle(cx, cy, radius, completedPaint)
            status == 1 -> {
                canvas.drawCircle(cx, cy, radius, pastPaint)
                canvas.drawCircle(cx, cy, radius, todayStrokePaint.apply { strokeWidth = 1f; color = accentColor })
            }
            isPast -> canvas.drawCircle(cx, cy, radius, pastPaint)
            isToday -> {
                // TODAY IS FILLED AS REQUESTED
                canvas.drawCircle(cx, cy, radius, todayPaint)
            }
            else -> {
                // OTHER EMPTY DAYS ARE LINED
                emptyPaint.color = accentColor
                canvas.drawCircle(cx, cy, radius, emptyPaint)
            }
        }
        
        // Restore defaults
        todayStrokePaint.color = todayRingColor
        todayStrokePaint.strokeWidth = 2.5f * resources.displayMetrics.density
    }
}
