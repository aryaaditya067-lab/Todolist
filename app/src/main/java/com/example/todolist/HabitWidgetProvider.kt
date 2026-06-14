package com.example.todolist

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class HabitWidgetProvider : AppWidgetProvider() {

    companion object {
        fun triggerUpdate(context: Context) {
            val intent = Intent(context, HabitWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, HabitWidgetProvider::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.habit_widget)

        // Force zero padding for Android 12+ (API 31+) to ensure edge-to-edge
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            views.setViewPadding(android.R.id.background, 0, 0, 0, 0)
        }

        // Tapping opens the app
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(android.R.id.background, pendingIntent)

        // Fetch data and update UI
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val dao = db.taskDao()
            
            val now = Calendar.getInstance()
            val todayDate = now.get(Calendar.DAY_OF_MONTH)
            val dayOfYear = now.get(Calendar.DAY_OF_YEAR)
            val daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)

            val remainingDays = daysInMonth - todayDate
            
            // Generate dots bitmap: Passed days Orange, Today Highlighted, Future Grey
            val dotsBitmap = createDotsBitmap(context, daysInMonth, todayDate)
            views.setImageViewBitmap(R.id.widget_dots, dotsBitmap)

            // Show Remaining Days in the big number
            views.setTextViewText(R.id.widget_big_number, remainingDays.toString())
            views.setTextViewText(R.id.widget_number_label, if (remainingDays == 1) "DAY LEFT" else "DAYS LEFT")
            
            val quoteIndex = dayOfYear % MotivationalQuotes.quotes.size
            val motivation = if (remainingDays == 0) "Month completed! 🏆" else MotivationalQuotes.quotes[quoteIndex]
            views.setTextViewText(R.id.widget_motivation, motivation)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun createDotsBitmap(context: Context, totalDays: Int, todayDate: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        
        val dotRadius = 8f * density
        val spacing = 26f * density
        val columns = 7
        val rows = (totalDays + columns - 1) / columns
        
        val width = (columns * spacing).toInt()
        val height = (rows * spacing).toInt()
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val accentColor = ContextCompat.getColor(context, R.color.widget_accent)
        val inactiveColor = ContextCompat.getColor(context, R.color.widget_dot_inactive)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
        val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = inactiveColor }
        val todayStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
        }

        for (i in 0 until totalDays) {
            val dayOfMonth = i + 1
            val row = i / columns
            val col = i % columns
            val cx = col * spacing + spacing / 2
            val cy = row * spacing + spacing / 2
            
            when {
                dayOfMonth == todayDate -> {
                    // Current Day: Orange with highlight ring
                    canvas.drawCircle(cx, cy, dotRadius * 1.1f, fillPaint)
                    canvas.drawCircle(cx, cy, dotRadius * 1.4f, todayStrokePaint)
                }
                dayOfMonth < todayDate -> {
                    // Passed Days: Solid Orange
                    canvas.drawCircle(cx, cy, dotRadius, fillPaint)
                }
                else -> {
                    // Future Days: Grey
                    canvas.drawCircle(cx, cy, dotRadius, inactivePaint)
                }
            }
        }

        return bitmap
    }
}
