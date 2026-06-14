package com.example.todolist

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat

class SettingsManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_DEFAULT_DURATION = "default_duration"
        const val KEY_ASK_BEFORE_START = "ask_before_start"
        const val KEY_TIMER_MODE = "timer_mode"
        const val KEY_ONLY_ONE_TIMER = "only_one_timer"
        const val KEY_AUTO_COMPLETE = "auto_complete"
        const val KEY_REMINDER_NOTIF = "reminder_notif"
        const val KEY_TIMER_FINISHED_NOTIF = "timer_finished_notif"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_FONT_SIZE = "font_size"
        const val KEY_CALENDAR_SYNC = "calendar_sync"

        const val THEME_LIGHT = 0
        const val THEME_DARK = 1
        const val THEME_SYSTEM = 2

        const val FONT_SMALL = 0
        const val FONT_MEDIUM = 1
        const val FONT_LARGE = 2
        
        const val TIMER_COUNTDOWN = 0
        const val TIMER_STOPWATCH = 1
    }

    var defaultDuration: Int
        get() = prefs.getInt(KEY_DEFAULT_DURATION, 30)
        set(value) = prefs.edit().putInt(KEY_DEFAULT_DURATION, value).apply()

    var askBeforeStart: Boolean
        get() = prefs.getBoolean(KEY_ASK_BEFORE_START, true)
        set(value) = prefs.edit().putBoolean(KEY_ASK_BEFORE_START, value).apply()

    var timerMode: Int
        get() = prefs.getInt(KEY_TIMER_MODE, TIMER_COUNTDOWN)
        set(value) = prefs.edit().putInt(KEY_TIMER_MODE, value).apply()

    var onlyOneTimer: Boolean
        get() = prefs.getBoolean(KEY_ONLY_ONE_TIMER, true)
        set(value) = prefs.edit().putBoolean(KEY_ONLY_ONE_TIMER, value).apply()

    var autoComplete: Boolean
        get() = prefs.getBoolean(KEY_AUTO_COMPLETE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_COMPLETE, value).apply()

    var reminderNotif: Boolean
        get() = prefs.getBoolean(KEY_REMINDER_NOTIF, true)
        set(value) = prefs.edit().putBoolean(KEY_REMINDER_NOTIF, value).apply()

    var timerFinishedNotif: Boolean
        get() = prefs.getBoolean(KEY_TIMER_FINISHED_NOTIF, true)
        set(value) = prefs.edit().putBoolean(KEY_TIMER_FINISHED_NOTIF, value).apply()

    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM) // Default to System
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value).apply()

    var fontSize: Int
        get() = prefs.getInt(KEY_FONT_SIZE, FONT_MEDIUM)
        set(value) = prefs.edit().putInt(KEY_FONT_SIZE, value).apply()

    var calendarSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_CALENDAR_SYNC, false)
        set(value) = prefs.edit().putBoolean(KEY_CALENDAR_SYNC, value).apply()

    // Dynamic colors fetched from resources based on current theme
    val accentColor: Int get() = ContextCompat.getColor(context, R.color.accent_primary)
    val backgroundColor: Int get() = ContextCompat.getColor(context, R.color.bg_main)

    fun applyTheme() {
        when (themeMode) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}
