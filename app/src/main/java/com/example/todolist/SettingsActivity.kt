package com.example.todolist

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private val viewModel: TaskViewModel by viewModels()

    companion object {
        private const val PERMISSION_REQUEST_CALENDAR = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        auth = FirebaseAuth.getInstance()
        setupUI()
    }

    override fun applySettings() {
        super.applySettings()
        findViewById<View>(R.id.settingsRoot)?.setBackgroundColor(settingsManager.backgroundColor)
    }

    private fun setupUI() {
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        // Account
        val txtUserEmail = findViewById<TextView>(R.id.txtUserEmail)
        txtUserEmail.text = auth.currentUser?.email ?: "Not logged in"

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            showConfirmDialog("Logout", "Are you sure you want to logout? Your data will remain synced in the cloud.") {
                logout()
            }
        }

        // Appearance
        val spinnerTheme = findViewById<Spinner>(R.id.spinnerTheme)
        val themes = listOf("Light", "Dark", "System Default")
        val themeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, themes)
        spinnerTheme.adapter = themeAdapter
        spinnerTheme.setSelection(settingsManager.themeMode)
        spinnerTheme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (settingsManager.themeMode != position) {
                    settingsManager.themeMode = position
                    settingsManager.applyTheme()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val spinnerFontSize = findViewById<Spinner>(R.id.spinnerFontSize)
        val fontSizes = listOf("Small", "Medium", "Large")
        val fontAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fontSizes)
        spinnerFontSize.adapter = fontAdapter
        spinnerFontSize.setSelection(settingsManager.fontSize)
        spinnerFontSize.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (settingsManager.fontSize != position) {
                    settingsManager.fontSize = position
                    recreate() // Restart activity to apply font changes immediately
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // General Section
        val spinnerDuration = findViewById<Spinner>(R.id.spinnerDuration)
        val durations = listOf("15 min", "30 min", "45 min", "60 min")
        val durationValues = listOf(15, 30, 45, 60)
        val durationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, durations)
        spinnerDuration.adapter = durationAdapter
        spinnerDuration.setSelection(durationValues.indexOf(settingsManager.defaultDuration).coerceAtLeast(0))
        spinnerDuration.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                settingsManager.defaultDuration = durationValues[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val switchAskStart = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchAskStart)
        switchAskStart.isChecked = settingsManager.askBeforeStart
        switchAskStart.setOnCheckedChangeListener { _, isChecked -> settingsManager.askBeforeStart = isChecked }

        val switchCalendarSync = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchCalendarSync)
        switchCalendarSync.isChecked = settingsManager.calendarSyncEnabled
        switchCalendarSync.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (checkCalendarPermissions()) {
                    settingsManager.calendarSyncEnabled = true
                    viewModel.syncWithSystemCalendar()
                } else {
                    requestCalendarPermissions()
                    switchCalendarSync.isChecked = false
                }
            } else {
                settingsManager.calendarSyncEnabled = false
            }
        }

        // Timer Section
        val rgTimerMode = findViewById<RadioGroup>(R.id.rgTimerMode)
        rgTimerMode.check(if (settingsManager.timerMode == SettingsManager.TIMER_COUNTDOWN) R.id.rbCountdown else R.id.rbStopwatch)
        rgTimerMode.setOnCheckedChangeListener { _, checkedId ->
            settingsManager.timerMode = if (checkedId == R.id.rbCountdown) SettingsManager.TIMER_COUNTDOWN else SettingsManager.TIMER_STOPWATCH
        }

        val switchOneTimer = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchOneTimer)
        switchOneTimer.isChecked = settingsManager.onlyOneTimer
        switchOneTimer.setOnCheckedChangeListener { _, isChecked -> settingsManager.onlyOneTimer = isChecked }

        val switchAutoComplete = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchAutoComplete)
        switchAutoComplete.isChecked = settingsManager.autoComplete
        switchAutoComplete.setOnCheckedChangeListener { _, isChecked -> settingsManager.autoComplete = isChecked }

        // Notifications
        val switchReminder = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchReminderNotif)
        switchReminder.isChecked = settingsManager.reminderNotif
        switchReminder.setOnCheckedChangeListener { _, isChecked -> settingsManager.reminderNotif = isChecked }

        val switchTimerNotif = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchTimerNotif)
        switchTimerNotif.isChecked = settingsManager.timerFinishedNotif
        switchTimerNotif.setOnCheckedChangeListener { _, isChecked -> settingsManager.timerFinishedNotif = isChecked }

        // Data Section
        findViewById<Button>(R.id.btnResetToday).setOnClickListener {
            showConfirmDialog("Reset Today's Progress", "Are you sure you want to reset all tasks for today?") {
                Toast.makeText(this, "Today's progress reset", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnResetStreak).setOnClickListener {
            showConfirmDialog("Reset Monthly Streak", "Are you sure you want to reset your streak?") {
                val prefs = getSharedPreferences("todo_prefs", MODE_PRIVATE)
                prefs.edit().putInt("current_streak", 0).putInt("best_streak", 0).apply()
                Toast.makeText(this, "Streak reset", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logout() {
        auth.signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun showConfirmDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> onConfirm() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun checkCalendarPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCalendarPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR),
            PERMISSION_REQUEST_CALENDAR
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CALENDAR) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                settingsManager.calendarSyncEnabled = true
                findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchCalendarSync).isChecked = true
                viewModel.syncWithSystemCalendar()
                Toast.makeText(this, "Calendar Sync Enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Calendar permissions are required for sync", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
