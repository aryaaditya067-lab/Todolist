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
import com.example.core.domain.model.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.work.*

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: TaskViewModel by viewModels()
    private lateinit var adapter: TaskAdapter
    private lateinit var auth: FirebaseAuth

    @Inject
    lateinit var wearManager: WearCommunicationManager

    // AI Suggestion ke liye job
    private var aiSuggestionJob: Job? = null

    // Fix 2: TimerState data class — switch aur state sync rahega
    private data class TimerState(var type: TimerType = TimerType.NONE, var seconds: Int = 0)

    // Fix 1: Word boundary check — uses startsWith for stemming support
    private fun String.containsWord(word: String): Boolean {
        return this.split(Regex("[\\s,\\.!\\?:;\\-/]+")).any { it.startsWith(word, ignoreCase = true) }
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

        wearManager.sendAuthStatus()
        handleIntent(intent)

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

    override fun onResume() {
        super.onResume()
        // Har baar app foreground aaye, watch ko auth bhejo
        wearManager.sendAuthStatus()
        updateUserHeader()
    }

    private fun updateUserHeader() {
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
            // Bug Fix 1: Filter out duplicate task IDs (Phone + Cloud sync)
            val distinctTasks = tasks.distinctBy { it.id }
            adapter.submitList(distinctTasks)
            binding.txtEmptyState.visibility = if (distinctTasks.isEmpty()) View.VISIBLE else View.GONE

            // Log duplicates for debugging
            val grouped = tasks.groupBy { it.id }
            grouped.filter { it.value.size > 1 }.forEach { (id, dupes) ->
                Log.e("MainActivity", "Duplicate tasks found for ID: $id (count: ${dupes.size})")
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
        var selectedPriority = TaskPriority.MEDIUM

        // AI Suggestion Logic (Triggers as you type with delay)
        dialogBinding.editTaskTitle.doAfterTextChanged { text ->
            val title = text?.toString()?.trim() ?: ""
            if (title.isNotEmpty()) {
                val lowerTitle = title.lowercase()
                
                // Priority Keyword Suggestion
                selectedPriority = when {
                    lowerTitle.containsWord("urgent") || lowerTitle.containsWord("asap") || lowerTitle.containsWord("important") -> TaskPriority.HIGH
                    lowerTitle.containsWord("later") || lowerTitle.containsWord("low") -> TaskPriority.LOW
                    else -> TaskPriority.MEDIUM
                }
                
                // Priority UI feedback
                dialogBinding.txtPrioritySuggestion.visibility = if (selectedPriority != TaskPriority.MEDIUM) View.VISIBLE else View.GONE
                dialogBinding.txtPrioritySuggestion.text = "Priority: ${selectedPriority.name} ⚡"

                // Category Keyword Suggestion using containsWord fix
                val suggestedCategory = when {
                    lowerTitle.containsWord("work") || lowerTitle.containsWord("meeting") -> TaskCategory.WORK
                    lowerTitle.containsWord("study") || lowerTitle.containsWord("exam") || lowerTitle.containsWord("read") -> TaskCategory.STUDY
                    lowerTitle.containsWord("gym") || lowerTitle.containsWord("workout") || lowerTitle.containsWord("fit") -> TaskCategory.FITNESS
                    lowerTitle.containsWord("code") || lowerTitle.containsWord("dev") || lowerTitle.containsWord("bug") -> TaskCategory.CODING
                    lowerTitle.containsWord("game") || lowerTitle.containsWord("play") -> TaskCategory.GAMING
                    else -> null
                }
                
                selectedCategory = suggestedCategory ?: TaskCategory.PERSONAL
                if (suggestedCategory != null) {
                    dialogBinding.txtCategorySuggestion.visibility = View.VISIBLE
                    dialogBinding.txtCategorySuggestion.text = "Suggesting: ${suggestedCategory.name} ✨"
                } else {
                    dialogBinding.txtCategorySuggestion.visibility = View.GONE
                }

                aiSuggestionJob?.cancel()
                if (title.length >= 8) { // Optimization: Only call AI for meaningful length
                    aiSuggestionJob = lifecycleScope.launch {
                        delay(700) // Debounce delay
                        try {
                            dialogBinding.progressAiSuggestion.visibility = View.VISIBLE
                            val suggestion = GroqApiService.getTaskSuggestion(title)
                            if (suggestion.isNotEmpty()) {
                                dialogBinding.cardAiSuggestion.visibility = View.VISIBLE
                                dialogBinding.txtAiSuggestion.text = suggestion
                            }
                        } finally {
                            dialogBinding.progressAiSuggestion.visibility = View.GONE
                        }
                    }
                } else {
                    dialogBinding.cardAiSuggestion.visibility = View.GONE
                }
            } else {
                selectedCategory = TaskCategory.PERSONAL // Fix: Reset category
                selectedPriority = TaskPriority.MEDIUM
                dialogBinding.txtPrioritySuggestion.visibility = View.GONE
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

        // Timer Switch Logic
        dialogBinding.switchTimer.setOnCheckedChangeListener { _, isChecked ->
            dialogBinding.layoutDurationPicker.visibility = if (isChecked) View.VISIBLE else View.GONE
            timerState.type = if (isChecked) TimerType.COUNTDOWN else TimerType.NONE
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
                timerType = timerState.type, // Use sync state
                targetSeconds = timerState.seconds, // Use sync state
                timerSeconds = timerState.seconds,
                repeatMode = repeatMode,
                notes = notes,
                isRecurringTemplate = repeatMode != RepeatMode.ONE_TIME,
                autoCompleteOnTimerEnd = dialogBinding.switchAutoComplete.isChecked
            )

            viewModel.insert(newTask)
            dialog.dismiss()
        }

        // Fix: Cancel AI job when dialog is dismissed
        dialog.setOnDismissListener { 
            aiSuggestionJob?.cancel()
        }

        dialog.show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data?.scheme == "todolist") {
            lifecycleScope.launch {
                repeat(3) { // Send 3 times with a delay to ensure watch is listening
                    wearManager.sendAuthStatus()
                    delay(1000)
                }
            }
            Log.d("MainActivity", "Deep link received: sending auth status to watch")
        }
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
