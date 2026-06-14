package com.example.todolist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todolist.databinding.ActivityMainBinding
import com.example.todolist.databinding.BottomSheetAddTaskBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.work.*
import java.util.concurrent.TimeUnit

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: TaskViewModel by viewModels()
    private lateinit var adapter: TaskAdapter
    private lateinit var auth: FirebaseAuth

    // AI Suggestion ke liye job
    private var aiSuggestionJob: Job? = null

    // Fix 2: TimerState data class — switch aur state sync rahega
    private data class TimerState(var type: TimerType = TimerType.NONE, var seconds: Int = 0)

    // Fix 1: Word boundary check — "display" ko "gaming" detect nahi karega
    private fun String.containsWord(word: String): Boolean {
        return this.split(Regex("[\\s,\\.!\\?]+")).any { it == word }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        adapter = TaskAdapter(
            onTaskToggled = { viewModel.toggleTaskDone(it) },
            onStartTimer = { viewModel.startTimer(it) },
            onPauseTimer = { viewModel.pauseTimer(it) },
            onStopTimer = { viewModel.stopTimer(it) },
            onTaskLongClicked = { },
            onSelectionChanged = { count -> updateSelectionUI(count) },
            onDeleteTask = { task -> viewModel.delete(task) },
            onTaskClicked = { task -> handleTaskClick(task) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.btnAdd.setOnClickListener { showAddTaskDialog() }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        viewModel.aiRoast.observe(this) { roast ->
            binding.txtAiRoast.text = roast
            binding.cardAiRoast.visibility = if (roast.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.isRoasting.observe(this) { isRoasting ->
            binding.progressRoast.visibility = if (isRoasting) View.VISIBLE else View.GONE
        }

        schedulePeriodicRoast()

        binding.btnEnterSelection.setOnClickListener {
            adapter.enterSelectionModeWithoutTask()
        }

        binding.summaryCard.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }

        binding.btnDeleteSelected.setOnClickListener {
            val selected = adapter.getSelectedTasks()
            if (selected.isNotEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Delete ${selected.size} tasks?")
                    .setMessage("Are you sure you want to delete the selected tasks?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteMultipleTasks(selected)
                        adapter.clearSelection()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        binding.btnCloseSelection.setOnClickListener {
            adapter.clearSelection()
        }

        binding.btnSelectAll.setOnClickListener {
            adapter.selectAll()
        }

        observeViewModel()
    }

    private fun schedulePeriodicRoast() {
        val roastWorkRequest = PeriodicWorkRequestBuilder<RoastWorker>(4, TimeUnit.HOURS)
            .addTag("periodic_roast")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "periodic_roast",
            ExistingPeriodicWorkPolicy.KEEP,
            roastWorkRequest
        )
    }

    private fun handleTaskClick(task: Task) {
        if (task.parentId != 0) {
            AlertDialog.Builder(this)
                .setTitle("Recurring Task")
                .setMessage("Do you want to stop this repeating task or just delete this instance?")
                .setPositiveButton("Stop Repeating") { _, _ ->
                    viewModel.stopRepeatingTask(task)
                }
                .setNegativeButton("Delete Instance") { _, _ ->
                    viewModel.delete(task)
                }
                .setNeutralButton("Cancel", null)
                .show()
        }
    }

    private fun updateSelectionUI(count: Int) {
        if (adapter.isSelectionMode) {
            binding.layoutSelectionHeader.visibility = View.VISIBLE
            binding.layoutUserHeader.visibility = View.GONE
            binding.cardDeleteSelected.visibility = View.VISIBLE
            binding.cardSelect.visibility = View.GONE
            binding.txtSelectionCount.text = "$count Selected"
        } else {
            binding.layoutSelectionHeader.visibility = View.GONE
            binding.layoutUserHeader.visibility = View.VISIBLE
            binding.cardDeleteSelected.visibility = View.GONE
            binding.cardSelect.visibility = View.VISIBLE
        }
    }

    override fun applySettings() {
        super.applySettings()
        binding.habitTrackerView.setAccentColor(settingsManager.accentColor)
    }

    private fun observeViewModel() {
        viewModel.allTasks.observe(this) { tasks ->
            adapter.submitList(tasks)
            binding.txtEmptyState.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE

            // Habit Tracker Update (Today only)
            val today = getTodayTimestamp()
            val todayTasks = tasks.filter { it.date == today && !it.isRecurringTemplate }
            
            // User Header Update
            val userName = auth.currentUser?.displayName?.split(" ")?.get(0) ?: "User"
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greeting = when (hour) {
                in 0..11 -> "Good Morning"
                in 12..16 -> "Good Afternoon"
                else -> "Good Evening"
            }
            binding.txtGreeting.text = "$greeting, $userName ✨"
            binding.txtDate.text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())

            // Bug Fix 1: Filter out duplicate task IDs (Phone + Cloud sync)
            val grouped = tasks.groupBy { it.id }
            grouped.filter { it.value.size > 1 }.forEach { (key, dupes) ->
                Log.e("MainActivity", "Duplicate tasks found: key=$key, count=${dupes.size}, ids=${dupes.map { it.id }}")
            }
        }

        viewModel.getTasksForMonth().observe(this) { tasks ->
            binding.habitTrackerView.setTasks(tasks)
            
            val doneCount = tasks.count { it.done }
            binding.txtHabitStatus.text = if (doneCount > 0) 
                "You've completed $doneCount tasks this month! 🔥" 
            else "Start your journey today!"
        }
    }

    private fun showAddTaskDialog() {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val dialogBinding = BottomSheetAddTaskBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val timerState = TimerState()
        var selectedCategory = TaskCategory.PERSONAL

        // AI Suggestion Logic (Triggers as you type with delay)
        dialogBinding.editTaskTitle.doAfterTextChanged { text ->
            val title = text?.toString()?.trim() ?: ""
            if (title.isNotEmpty()) {
                // Category Keyword Suggestion
                val suggestedCategory = when {
                    title.contains("work", true) || title.contains("meeting", true) -> TaskCategory.WORK
                    title.contains("study", true) || title.contains("exam", true) || title.contains("read", true) -> TaskCategory.STUDY
                    title.contains("gym", true) || title.contains("workout", true) || title.contains("fit", true) -> TaskCategory.FITNESS
                    title.contains("code", true) || title.contains("dev", true) || title.contains("bug", true) -> TaskCategory.CODING
                    title.contains("game", true) || title.contains("play", true) -> TaskCategory.GAMING
                    else -> null
                }
                
                if (suggestedCategory != null) {
                    selectedCategory = suggestedCategory
                    dialogBinding.txtCategorySuggestion.visibility = View.VISIBLE
                    dialogBinding.txtCategorySuggestion.text = "Suggesting: ${suggestedCategory.name} ✨"
                } else {
                    dialogBinding.txtCategorySuggestion.visibility = View.GONE
                }

                aiSuggestionJob?.cancel()
                aiSuggestionJob = lifecycleScope.launch {
                    delay(700) // Snapier delay
                    dialogBinding.progressAiSuggestion.visibility = View.VISIBLE
                    val suggestion = GroqApiService.getTaskSuggestion(title)
                    dialogBinding.progressAiSuggestion.visibility = View.GONE
                    
                    if (suggestion.isNotEmpty()) {
                        dialogBinding.cardAiSuggestion.visibility = View.VISIBLE
                        dialogBinding.txtAiSuggestion.text = suggestion
                    }
                }
            } else {
                dialogBinding.txtCategorySuggestion.visibility = View.GONE
                dialogBinding.cardAiSuggestion.visibility = View.GONE
            }
        }

        dialogBinding.btnAddSuggestion.setOnClickListener {
            val suggestion = dialogBinding.txtAiSuggestion.text.toString()
            val currentNotes = dialogBinding.editNotes.text.toString()
            dialogBinding.editNotes.setText("$currentNotes\nAI Tips: $suggestion".trim())
            dialogBinding.editNotes.visibility = View.VISIBLE
            dialogBinding.cardAiSuggestion.visibility = View.GONE
        }

        // Priority Selection - Defaulting to MEDIUM as it's not in this version of layout
        val selectedPriority = TaskPriority.MEDIUM

        // Timer Switch Logic
        dialogBinding.switchTimer.setOnCheckedChangeListener { _, isChecked ->
            dialogBinding.layoutDurationPicker.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked && timerState.type == TimerType.NONE) {
                timerState.type = TimerType.COUNTDOWN
            }
        }

        dialogBinding.btnCreateTask.setOnClickListener {
            val title = dialogBinding.editTaskTitle.text.toString().trim()
            if (title.isEmpty()) return@setOnClickListener

            // Recurring Logic
            val repeatMode = if (dialogBinding.switchRepeat.isChecked) {
                if (dialogBinding.chipWeekdays.isChecked) RepeatMode.WEEKDAYS else RepeatMode.DAILY
            } else {
                RepeatMode.ONE_TIME
            }

            val notes = dialogBinding.editNotes.text.toString()
            val minutes = dialogBinding.editCustomMinutes.text.toString().toIntOrNull() ?: 0
            val seconds = minutes * 60
            timerState.seconds = seconds

            val newTask = Task(
                title = title,
                date = getTodayTimestamp(),
                category = selectedCategory,
                priority = selectedPriority,
                timerType = if (dialogBinding.switchTimer.isChecked) TimerType.COUNTDOWN else TimerType.NONE,
                targetSeconds = seconds,
                timerSeconds = seconds,
                repeatMode = repeatMode,
                notes = notes,
                isRecurringTemplate = repeatMode != RepeatMode.ONE_TIME,
                autoCompleteOnTimerEnd = dialogBinding.switchAutoComplete.isChecked
            )

            viewModel.insert(newTask)
            dialog.dismiss()
        }

        dialog.show()
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
