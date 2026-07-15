package com.example.myapplication

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.domain.model.HabitProgress
import com.example.core.domain.model.RepeatMode
import com.example.core.domain.model.Task
import com.example.core.domain.model.TaskCategory
import com.example.core.domain.model.TaskPriority
import com.example.core.domain.usecase.*
import com.example.core.util.Resource
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

data class WearUiState(
    val tasks: List<Task> = emptyList(),
    val habits: List<HabitProgress> = emptyList(),
    val isLoading: Boolean = true,
    val userName: String = "",
    val greeting: String = "",
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val isPhoneAppInstalled: Boolean = true
)

@HiltViewModel
class WearViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val getTasksUseCase: GetTasksUseCase,
    private val addTaskUseCase: AddTaskUseCase,
    private val toggleTaskUseCase: ToggleTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WearUiState())
    val uiState: StateFlow<WearUiState> = _uiState.asStateFlow()

    init {
        updateUserInfo()
        startListening()
        
        auth.addAuthStateListener { 
            updateUserInfo()
            checkLoginStatus()
        }
    }

    private fun checkLoginStatus() {
        _uiState.update { it.copy(isLoggedIn = auth.currentUser != null) }
    }

    private fun updateUserInfo() {
        val user = auth.currentUser
        val userName = user?.displayName?.split(" ")?.firstOrNull()?.uppercase() ?: "USER"

        val hour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
        val greeting = when (hour) {
            in 0..3 -> "Good Night 🌙"
            in 4..11 -> "Good Morning 🌅"
            in 12..16 -> "Good Afternoon ☀️"
            in 17..21 -> "Good Evening 🌆"
            else -> "Good Night 🌙"
        }
        
        _uiState.update { it.copy(userName = userName, greeting = greeting, isLoggedIn = user != null) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startListening() {
        viewModelScope.launch {
            _uiState.map { it.isLoggedIn }
                .distinctUntilChanged()
                .flatMapLatest { isLoggedIn ->
                    if (isLoggedIn) {
                        getTasksUseCase()
                    } else {
                        flowOf(Resource.Success(emptyList()))
                    }
                }
                .collect { resource ->
                    when (resource) {
                        is Resource.Loading -> _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                        is Resource.Success -> processData(resource.data ?: emptyList())
                        is Resource.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = resource.message) }
                    }
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

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            toggleTaskUseCase(task.remoteId, !task.done)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            deleteTaskUseCase(task.remoteId)
        }
    }

    fun addVoiceTask(title: String) {
        viewModelScope.launch {
            val today = getTodayTimestamp()
            val tempId = "temp_${System.currentTimeMillis()}"
            val initialTask = Task(
                remoteId = tempId,
                title = title,
                date = today,
                category = TaskCategory.PERSONAL,
                priority = TaskPriority.MEDIUM
            )
            addTaskUseCase(initialTask)

            try {
                val (parsedTitle, priorityStr) = GroqApiService.parseTaskDetails(title)
                val priority = try {
                    TaskPriority.valueOf(priorityStr)
                } catch (e: Exception) {
                    TaskPriority.MEDIUM
                }
                
                if (parsedTitle != title || priority != TaskPriority.MEDIUM) {
                    deleteTaskUseCase(tempId)
                    val smartTask = Task(
                        title = parsedTitle,
                        date = today,
                        category = TaskCategory.PERSONAL,
                        priority = priority
                    )
                    addTaskUseCase(smartTask)
                }
            } catch (e: Exception) {}
        }
    }

    fun setPhoneAppInstalled(installed: Boolean) {
        _uiState.update { it.copy(isPhoneAppInstalled = installed) }
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
