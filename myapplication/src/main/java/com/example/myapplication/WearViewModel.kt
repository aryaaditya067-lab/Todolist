package com.example.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar

data class HabitProgress(
    val name: String,
    val progress: Float,
    val streak: String,
)

data class WearUiState(
    val tasks: List<Task> = emptyList(),
    val habits: List<HabitProgress> = emptyList(),
    val isLoading: Boolean = true,
    val userName: String = "",
    val greeting: String = "",
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val aiRoast: String? = null,
    val isRoasting: Boolean = false
)

class WearViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val repository = FirestoreRepository()

    private val _uiState = MutableStateFlow(WearUiState())
    val uiState: StateFlow<WearUiState> = _uiState.asStateFlow()

    init {
        updateUserInfo()
        startListening()
    }

    private fun updateUserInfo() {
        val user = auth.currentUser
        val userName = user?.displayName?.split(" ")?.firstOrNull()?.uppercase() ?: "ADITYA"

        val hour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
        val greeting = when (hour) {
            in 0..3 -> "Good Night 🌙"
            in 4..11 -> "Good Morning 🌅"
            in 12..16 -> "Good Afternoon ☀️"
            in 17..21 -> "Good Evening 🌆"
            else -> "Good Night 🌙"
        }
        
        _uiState.update { it.copy(userName = userName, greeting = greeting) }
    }

    private fun startListening() {
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                viewModelScope.launch {
                    getTasksFlow().collect { allTasks ->
                        processData(allTasks)
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun processData(allTasks: List<Task>) = withContext(Dispatchers.Default) {
        val todayStart = getTodayTimestamp()
        val todayEnd = todayStart + 24L * 60 * 60 * 1000

        val todayTasks = allTasks.asSequence()
            .filter { (it.date in todayStart until todayEnd) && !it.isRecurringTemplate }
            .sortedWith(
                compareBy<Task> { it.done }
                    .thenByDescending { it.priority.ordinal }
            )
            .toList()

        // Efficient habit calculation
        val tasksByTitle = allTasks.groupBy { it.title }
        val habits = allTasks.asSequence()
            .filter { it.repeatMode != RepeatMode.ONE_TIME || it.parentId != 0 }
            .distinctBy { it.title }
            .take(2)
            .map { template ->
                val instances = tasksByTitle[template.title]?.filter { !it.isRecurringTemplate } ?: emptyList()
                val doneCount = instances.count { it.done }
                val totalCount = instances.size.coerceAtLeast(1)
                HabitProgress(
                    name = template.title,
                    progress = doneCount.toFloat() / totalCount,
                    streak = if (doneCount > 0) "$doneCount day streak 🔥" else "Ready?"
                )
            }
            .toList()

        _uiState.update {
            it.copy(
                tasks = todayTasks,
                habits = habits,
                isLoading = false,
                completedCount = todayTasks.count { t -> t.done },
                totalCount = todayTasks.size
            )
        }
    }

    fun generateRoast() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRoasting = true) }
            try {
                val state = _uiState.value
                val allTasks = firestore.collection("users")
                    .document(auth.currentUser?.uid ?: "")
                    .collection("tasks")
                    .get().await().documents.map { Task.fromMap(it.id, it.data ?: emptyMap()) }

                val completed = state.completedCount
                val total = state.totalCount
                val recurring = allTasks.filter { it.repeatMode != RepeatMode.ONE_TIME || it.parentId != 0 }
                val missedHabits = recurring.count { !it.done && it.date < getTodayTimestamp() }
                val streak = calculateStreak(allTasks)

                val roast = GroqApiService.getRoast(completed, total, missedHabits, streak)
                _uiState.update { it.copy(aiRoast = roast, isRoasting = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(aiRoast = "AI can't roast you now. 😴", isRoasting = false) }
            }
        }
    }

    private fun calculateStreak(tasks: List<Task>): Int {
        if (tasks.isEmpty()) return 0
        val sortedDates = tasks.filter { it.done }.map { it.date }.distinct().sortedDescending()
        if (sortedDates.isEmpty()) return 0
        var streak = 0
        var current = getTodayTimestamp()
        for (date in sortedDates) {
            if (date == current || date == current - 86400000) {
                streak++
                current = date
            } else if (date < current - 86400000) {
                break
            }
        }
        return streak
    }

    private fun getTasksFlow(): Flow<List<Task>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: return@callbackFlow
        val listener = firestore.collection("users").document(uid).collection("tasks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val tasksList = snapshot?.documents?.map { doc ->
                    Task.fromMap(doc.id, doc.data ?: emptyMap())
                } ?: emptyList()
                trySend(tasksList)
            }
        awaitClose { listener.remove() }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.toggleTaskDone(task.remoteId, !task.done)
            } catch (_: Exception) {}
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.deleteTask(task.remoteId)
            } catch (_: Exception) {}
        }
    }

    fun addVoiceTask(title: String) {
        val uid = auth.currentUser?.uid ?: return
        val today = getTodayTimestamp()
        val newTask = Task(
            title = title,
            date = today,
            category = TaskCategory.PERSONAL,
            priority = TaskPriority.MEDIUM
        )
        viewModelScope.launch {
            firestore.collection("users").document(uid).collection("tasks")
                .add(newTask.toMap())
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
