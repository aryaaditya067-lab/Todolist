package com.example.todolist

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.*
import com.example.core.domain.model.*
import com.example.core.domain.usecase.*
import com.example.core.util.Resource
import com.example.todolist.data.repository.LocalTaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    application: Application,
    private val localRepository: LocalTaskRepository,
    private val getTasksUseCase: GetTasksUseCase,
    private val addTaskUseCase: AddTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val toggleTaskUseCase: ToggleTaskUseCase
) : AndroidViewModel(application) {

    private val reminderManager = ReminderManager(application)
    private val calendarSyncManager = CalendarSyncManager(application)
    private val settingsManager = SettingsManager(application)
    
    val allTasks: LiveData<List<Task>> = localRepository.allTasks
    private val context: android.content.Context = application.applicationContext

    private var timerJob: Job? = null
    
    private val _aiRoast = MutableLiveData<String?>(null)
    val aiRoast: LiveData<String?> = _aiRoast

    private val _isRoasting = MutableLiveData<Boolean>(false)
    val isRoasting: LiveData<Boolean> = _isRoasting
    
    init {
        startTicker()
        fetchFromCloud()
        syncWithSystemCalendar()
    }

    fun syncWithSystemCalendar(): Job = viewModelScope.launch(Dispatchers.IO) {
        if (settingsManager.calendarSyncEnabled && calendarSyncManager.hasPermissions()) {
            val systemTasks = calendarSyncManager.fetchCalendarEvents()
            systemTasks.forEach { systemTask ->
                val exists = localRepository.getTaskByTitleAndDate(systemTask.title, systemTask.date)
                if (exists == null) {
                    insert(systemTask)
                }
            }
        }
    }

    private fun fetchFromCloud(): Job = viewModelScope.launch {
        getTasksUseCase().collectLatest { resource ->
            if (resource is Resource.Success) {
                val remoteTasks = resource.data ?: emptyList()
                withContext(Dispatchers.IO) {
                    remoteTasks.forEach { remoteTask ->
                        if (remoteTask.remoteId.isNotEmpty()) {
                            val exists = localRepository.existsWithRemoteId(remoteTask.remoteId)
                            if (!exists) {
                                val localMatch = localRepository.getTaskByTitleAndDate(remoteTask.title, remoteTask.date)
                                if (localMatch == null) {
                                    val id = localRepository.insert(remoteTask).toInt()
                                    reminderManager.scheduleReminder(remoteTask.copy(id = id))
                                } else if (localMatch.remoteId.isEmpty()) {
                                    localRepository.update(localMatch.copy(remoteId = remoteTask.remoteId))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startTicker() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
            }
        }
    }

    private fun updateTaskInternal(task: Task): Job = viewModelScope.launch(Dispatchers.IO) {
        localRepository.update(task)
        // Sync to cloud
        if (task.remoteId.isNotEmpty()) {
            updateTaskUseCase(task)
        } else {
            val resource = addTaskUseCase(task)
            if (resource is Resource.Success) {
                localRepository.update(task.copy(remoteId = resource.data ?: ""))
            }
        }
        
        if (task.done) {
            reminderManager.cancelReminder(task.id)
        } else {
            reminderManager.scheduleReminder(task)
        }
    }

    fun ensureRecurringTasksForToday(): Job = viewModelScope.launch(Dispatchers.IO) {
        val today = getTodayTimestamp()
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        val templates = localRepository.getRecurringTemplates()
        for (template in templates) {
            val shouldAppearToday = when (template.repeatMode) {
                RepeatMode.DAILY -> true
                RepeatMode.WEEKDAYS -> dayOfWeek in 2..6
                RepeatMode.CUSTOM -> template.daysOfWeek.split(",").contains(dayOfWeek.toString())
                else -> false
            }

            if (shouldAppearToday) {
                val existingInstance = localRepository.getInstanceForTemplate(template.id, today)
                val duplicateCheck = localRepository.getTaskByTitleAndDate(template.title, today)
                
                if (existingInstance == null && duplicateCheck == null) {
                    val newInstance = template.copy(
                        id = 0,
                        remoteId = "",
                        date = today,
                        isRecurringTemplate = false,
                        parentId = template.id,
                        parentRemoteId = template.remoteId,
                        done = false,
                        timerStatus = TimerStatus.NOT_STARTED,
                        timerSeconds = template.targetSeconds
                    )
                    insert(newInstance)
                }
            }
        }
    }

    fun startTimer(task: Task): Job = viewModelScope.launch {
        if (task.done || task.timerType == TimerType.NONE) return@launch
        
        allTasks.value?.filter { it.timerStatus == TimerStatus.RUNNING && it.id != task.id }?.forEach {
            localRepository.update(it.copy(timerStatus = TimerStatus.PAUSED))
        }
        
        val updated = task.copy(timerStatus = TimerStatus.RUNNING)
        updateTaskInternal(updated)

        val serviceIntent = Intent(context, TimerService::class.java).apply {
            action = "START"
            putExtra("TASK_ID", task.id)
        }
        context.startService(serviceIntent)
    }

    fun pauseTimer(task: Task): Job = viewModelScope.launch {
        val updated = task.copy(timerStatus = TimerStatus.PAUSED)
        updateTaskInternal(updated)
        
        val serviceIntent = Intent(context, TimerService::class.java).apply {
            action = "STOP"
        }
        context.startService(serviceIntent)
    }

    fun stopTimer(task: Task): Job = viewModelScope.launch {
        val updated = task.copy(
            timerStatus = TimerStatus.NOT_STARTED,
            timerSeconds = task.targetSeconds
        )
        updateTaskInternal(updated)

        val serviceIntent = Intent(context, TimerService::class.java).apply {
            action = "STOP"
        }
        context.startService(serviceIntent)
    }

    fun toggleTaskDone(task: Task): Job = viewModelScope.launch {
        val newDone = !task.done
        var updatedTask = task.copy(done = newDone)
        
        if (!newDone && task.timerType != TimerType.NONE) {
            updatedTask = updatedTask.copy(
                timerStatus = TimerStatus.NOT_STARTED,
                timerSeconds = task.targetSeconds
            )
        } else if (newDone && task.timerType != TimerType.NONE) {
            updatedTask = updatedTask.copy(
                timerStatus = TimerStatus.FINISHED,
                timerSeconds = 0
            )
            val serviceIntent = Intent(context, TimerService::class.java).apply {
                action = "STOP"
            }
            context.startService(serviceIntent)
        }
        updateTaskInternal(updatedTask)
        toggleTaskUseCase(task.remoteId, newDone)
    }

    fun insert(task: Task): Job = viewModelScope.launch(Dispatchers.IO) {
        val id = localRepository.insert(task).toInt()
        val updatedLocal = task.copy(id = id)
        
        // Sync to cloud
        val resource = addTaskUseCase(updatedLocal)
        if (resource is Resource.Success) {
            localRepository.update(updatedLocal.copy(remoteId = resource.data ?: ""))
        }
        
        reminderManager.scheduleReminder(updatedLocal)
        
        if (settingsManager.calendarSyncEnabled) {
            calendarSyncManager.syncTaskToCalendar(updatedLocal)
        }

        if (task.isRecurringTemplate) {
            ensureRecurringTasksForToday()
        }
    }

    fun delete(task: Task): Job = viewModelScope.launch(Dispatchers.IO) {
        deleteMultipleTasks(listOf(task))
    }

    fun deleteMultipleTasks(tasks: List<Task>): Job = viewModelScope.launch(Dispatchers.IO) {
        if (tasks.isEmpty()) return@launch
        
        localRepository.deleteMultiple(tasks)
        tasks.forEach { task ->
            reminderManager.cancelReminder(task.id)
            if (task.remoteId.isNotEmpty()) {
                deleteTaskUseCase(task.remoteId)
            }
        }
    }

    fun stopRepeatingTask(task: Task): Job = viewModelScope.launch(Dispatchers.IO) {
        val pid = if (task.isRecurringTemplate) task.id else task.parentId
        if (pid != 0) {
            val templates = localRepository.getRecurringTemplates()
            templates.find { it.id == pid }?.let { template ->
                if (template.remoteId.isNotEmpty()) {
                    deleteTaskUseCase(template.remoteId)
                }
                localRepository.delete(template)
                
                val currentTask = if (task.isRecurringTemplate) null else task
                currentTask?.let {
                    val updated = it.copy(parentId = 0, parentRemoteId = "", repeatMode = RepeatMode.ONE_TIME)
                    localRepository.update(updated)
                    // Sync update
                    if (updated.remoteId.isNotEmpty()) {
                        updateTaskUseCase(updated)
                    }
                }
            }
        }
    }

    fun getTasksForMonth(): LiveData<List<Task>> {
        val todayTimestamp: Long = getTodayTimestamp()
        val cal: Calendar = Calendar.getInstance()
        cal.timeInMillis = todayTimestamp
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val start: Long = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val end: Long = cal.timeInMillis

        val result: LiveData<List<Task>> = localRepository.getTasksForMonth(start, end)
        return result
    }

    fun generateRoast() {
        viewModelScope.launch {
            _isRoasting.postValue(true)
            try {
                val today = getTodayTimestamp()
                val tomorrow = today + 86400000
                val tasksForToday = localRepository.getTasksForDateRange(today, tomorrow)
                val completed = tasksForToday.count { it.done }
                val total = tasksForToday.size
                
                val allTasksList = allTasks.value ?: emptyList()
                val recurring = allTasksList.filter { it.repeatMode != RepeatMode.ONE_TIME || it.parentId != 0 }
                val missedHabits = recurring.count { !it.done && it.date < today }
                val streak = calculateStreak(allTasksList)

                val roast = GroqApiService.getRoast(completed, total, missedHabits, streak)
                _aiRoast.postValue(roast)
            } catch (e: Exception) {
                _aiRoast.postValue("AI is too tired to roast you right now. 😴")
            } finally {
                _isRoasting.postValue(false)
            }
        }
    }

    private fun calculateStreak(tasks: List<Task>): Int {
        if (tasks.isEmpty()) return 0
        val sortedDates: List<Long> = tasks.filter { it.done }.map { it.date }.distinct().sortedDescending()
        if (sortedDates.isEmpty()) return 0
        
        var streak: Int = 0
        var current: Long = getTodayTimestamp()
        
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

    private fun getTodayTimestamp(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
