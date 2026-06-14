package com.example.todolist

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    protected lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        settingsManager = SettingsManager(this)
        applyFontScale()
        settingsManager.applyTheme()
        super.onCreate(savedInstanceState)
    }

    private fun applyFontScale() {
        val scale = when (settingsManager.fontSize) {
            SettingsManager.FONT_SMALL -> 0.85f
            SettingsManager.FONT_LARGE -> 1.25f
            else -> 1.0f // Medium/Default
        }
        
        val configuration = resources.configuration
        if (configuration.fontScale != scale) {
            configuration.fontScale = scale
            val metrics = resources.displayMetrics
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, metrics)
        }
    }

    override fun onResume() {
        super.onResume()
        applySettings()
    }

    /**
     * Override this to apply activity-specific colors/settings.
     * Always call super.applySettings() to apply the background color.
     */
    open fun applySettings() {
        val root = findViewById<View>(android.R.id.content)
        root?.setBackgroundColor(settingsManager.backgroundColor)
    }
}
