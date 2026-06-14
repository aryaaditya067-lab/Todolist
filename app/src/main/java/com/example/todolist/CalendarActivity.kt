package com.example.todolist

import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.viewModels

class CalendarActivity : BaseActivity() {

    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        val habitView = findViewById<HabitTrackerView>(R.id.calendarHabitView)
        habitView.isYearlyMode = true // Set to yearly vertical scroll mode

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        viewModel.allTasks.observe(this) { tasks ->
            habitView.setTasks(tasks)
        }
    }

    override fun applySettings() {
        super.applySettings()
        findViewById<HabitTrackerView>(R.id.calendarHabitView)?.setAccentColor(settingsManager.accentColor)
    }
}
