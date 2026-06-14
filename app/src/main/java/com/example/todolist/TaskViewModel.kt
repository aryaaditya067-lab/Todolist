package com.example.todolist

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.*
import java.util.*

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository
    private val firestoreRepository = FirestoreRepository()
    private val reminderManager = ReminderManager(application)
    private val calendarSyncManager = CalendarSyncManager(application)
    private val settingsManager = SettingsManager(application)
    val allTasks: LiveData<List<Task>>
    private val context = application.applicationContext

    private var timerJob: Job? = null
    
    // AI Roast State
    private val _aiRoast = MutableLiveData<String?>(null)
    val aiRoast: LiveData<String?> = _aiRoast

    private val _isRoasting = MutableLiveData<Boolean>(false)
    val isRoasting: LiveData<Boolean> = _isRoasting
    
    companion object {
        private var isInitialCloudFetchDone = false
    }
    
    init {
        val taskDao = AppDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(taskDao)
        allTasks = repository.allTasks
        startTicker()
        
        if (!isInitialCloudFetchDone) {
            fetchFromCloud()
            isInitialCloudFetchDone = true
        }
        syncWithSystemCalendar()
    }

    fun syncWithSystemCalendar() = viewModelScope.launch(Dispatchers.IO) {
        if (settingsManager.calendarSyncEnabled && calendarSyncManager.hasPermissions()) {
            val systemTasks = calendarSyncManager.fetchCalendarEvents()
            systemTasks.forEach { systemTask ->
                val exists = repository.getTaskByTitleAndDate(systemTask.title, systemTask.date)
                if (exists == null) {
                    repository.insert(systemTask)
                }
            }
        }
    }

    private fun fetchFromCloud() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val remoteTasks = firestoreRepository.fetchTasks()
            remoteTasks.forEach { remoteTask ->
                if (remoteTask.remoteId.isNotEmpty()) {
                    val exists = repository.existsWithRemoteId(remoteTask.remoteId)
                    if (!exists) {
                        val localMatch = repository.getTaskByTitleAndDate(remoteTask.title, remoteTask.date)
                        if (localMatch == null) {
                            val id = repository.insert(remoteTask).toInt()
                            reminderManager.scheduleReminder(remoteTask.copy(id = id))
                        } else if (localMatch.remoteId.isEmpty()) {
                            repository.update(localMatch.copy(remoteId = remoteTask.remoteId))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TaskViewModel", "Cloud fetch failed", e)
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

    private suspend fun syncToCloudSynchronous(task: Task) {
        try {
            val remoteId = firestoreRepository.syncTask(task)
            if (task.remoteId != remoteId) {
                repository.update(task.copy(remoteId = remoteId))
            }
        } catch (e: Exception) {
            Log.e("TaskViewModel", "Sync failed", e)
        }
    }

    private fun updateTaskInternal(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(task)
        syncToCloudSynchronous(task)
        if (task.done) {
            reminderManager.cancelReminder(task.id)
        } else {
            reminderManager.scheduleReminder(task)
        }
    }

    fun ensureRecurringTasksForToday() = viewModelScope.launch(Dispatchers.IO) {
        val today = getTodayTimestamp()
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        val templates = repository.getRecurringTemplates()
        for (template in templates) {
            val shouldAppearToday = when (template.repeatMode) {
                RepeatMode.DAILY -> true
                RepeatMode.WEEKDAYS -> dayOfWeek in 2..6
                RepeatMode.CUSTOM -> template.daysOfWeek.split(",").contains(dayOfWeek.toString())
                else -> false
            }

            if (shouldAppearToday) {
                val existingInstance = repository.getInstanceForTemplate(template.id, today)
                val duplicateCheck = repository.getTaskByTitleAndDate(template.title, today)
                
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
                    val newId = repository.insert(newInstance).toInt()
                    val taskWithId = newInstance.copy(id = newId)
                    syncToCloudSynchronous(taskWithId)
                    reminderManager.scheduleReminder(taskWithId)
                }
            }
        }
    }

    fun startTimer(task: Task, @Suppress("UNUSED_PARAMETER") onlyOneRunning: Boolean = true) = viewModelScope.launch {
        if (task.done || task.timerType == TimerType.NONE) return@launch
        
        allTasks.value?.filter { it.timerStatus == TimerStatus.RUNNING && it.id != task.id }?.forEach {
            repository.update(it.copy(timerStatus = TimerStatus.PAUSED))
        }
        
        val updated = task.copy(timerStatus = TimerStatus.RUNNING)
        updateTaskInternal(updated)

        val serviceIntent = Intent(context, TimerService::class.java).apply {
            action = "START"
            putExtra("TASK_ID", task.id)
        }
        context.startService(serviceIntent)
    }

    fun pauseTimer(task: Task) = viewModelScope.launch {
        val updated = task.copy(timerStatus = TimerStatus.PAUSED)
        updateTaskInternal(updated)
        
        val serviceIntent = Intent(context, TimerService::class.java).apply {
            action = "STOP"
        }
        context.startService(serviceIntent)
    }

    fun stopTimer(task: Task) = viewModelScope.launch {
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

    fun toggleTaskDone(task: Task) = viewModelScope.launch {
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
    }

    fun insert(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        val id = repository.insert(task).toInt()
        val updated = task.copy(id = id)
        syncToCloudSynchronous(updated)
        reminderManager.scheduleReminder(updated)
        
        if (settingsManager.calendarSyncEnabled) {
            calendarSyncManager.syncTaskToCalendar(updated)
        }

        if (task.isRecurringTemplate) {
            ensureRecurringTasksForToday()
        }
    }

    fun delete(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        deleteMultipleTasks(listOf(task))
    }

    fun deleteMultipleTasks(tasks: List<Task>) = viewModelScope.launch(Dispatchers.IO) {
        if (tasks.isEmpty()) return@launch
        
        val ridsToDelete = mutableSetOf<String>()
        val tasksToDeleteLocally = mutableSetOf<Task>()
        val templates = repository.getRecurringTemplates()
        val taskDao = AppDatabase.getDatabase(context).taskDao()

        // 1. Bhai saari IDs ek saath collect karo (Phone + Cloud)
        tasks.forEach { task ->
            tasksToDeleteLocally.add(task)
            if (task.remoteId.isNotEmpty()) ridsToDelete.add(task.remoteId)

            // Recurring template chain deletion taaki restart pe wapas na aaye
            if (task.parentId != 0) {
                templates.find { it.id == task.parentId }?.let { template ->
                    tasksToDeleteLocally.add(template)
                    if (template.remoteId.isNotEmpty()) ridsToDelete.add(template.remoteId)
                    // Bachon ki remote IDs bhi le lo
                    val instanceRids = taskDao.getRemoteIdsOfInstances(template.id)
                    ridsToDelete.addAll(instanceRids)
                }
            }
            if (task.isRecurringTemplate) {
                val instanceRids = taskDao.getRemoteIdsOfInstances(task.id)
                ridsToDelete.addAll(instanceRids)
            }
        }

        // --- STEP 1: PHONE KI SAFAI EK JHATKE MEIN ---
        // repository.deleteMultiple use karenge taaki UI ek saath update ho
        repository.deleteMultiple(tasksToDeleteLocally.toList())
        tasksToDeleteLocally.forEach { reminderManager.cancelReminder(it.id) }
        Log.d("TaskViewModel", "Local batch delete finished instantly")

        // --- STEP 2: BADAL (CLOUD) KI SAFAI PARALLEL MEIN ---
        ridsToDelete.forEach { rid ->
            launch {
                try {
                    firestoreRepository.deleteTask(rid)
                } catch (e: Exception) {
                    Log.e("TaskViewModel", "Cloud multi-delete error for $rid", e)
                }
            }
        }
    }

    fun stopRepeatingTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        val pid = if (task.isRecurringTemplate) task.id else task.parentId
        if (pid != 0) {
            val template = repository.getRecurringTemplates().find { it.id == pid }
            if (template != null) {
                if (template.remoteId.isNotEmpty()) {
                    try { firestoreRepository.deleteTask(template.remoteId) } catch (e: Exception) {}
                }
                repository.delete(template)
                
                val currentTask = if (task.isRecurringTemplate) null else task
                currentTask?.let {
                    val updated = it.copy(parentId = 0, parentRemoteId = "", repeatMode = RepeatMode.ONE_TIME)
                    repository.update(updated)
                    syncToCloudSynchronous(updated)
                }
            }
        }
    }

    fun addSuggestionAsSubTask(suggestion: String, parentTask: Task) {
        val subTask = Task(
            title = suggestion,
            date = parentTask.date,
            category = parentTask.category,
            priority = parentTask.priority,
            parentId = parentTask.id,
            parentRemoteId = parentTask.remoteId,
            startTime = parentTask.startTime,
            endTime = parentTask.endTime
        )
        insert(subTask)
    }

    fun getTasksForMonth(): LiveData<List<Task>> {
        val today = getTodayTimestamp()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = today
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfMonth = calendar.timeInMillis

        return repository.getTasksForMonth(startOfMonth, endOfMonth)
    }

    fun generateRoast() {
        viewModelScope.launch {
            _isRoasting.postValue(true)
            try {
                val today = getTodayTimestamp()
                val tomorrow = today + 86400000
                val tasksForToday = repository.getTasksForDateRange(today, tomorrow)
                val completed = tasksForToday.count { it.done }
                val total = tasksForToday.size
                
                // For simplicity, using task-based streak logic or basic habit count
                val allTasksList = allTasks.value ?: emptyList()
                val recurring = allTasksList.filter { it.repeatMode != RepeatMode.ONE_TIME || it.parentId != 0 }
                val missedHabits = recurring.count { !it.done && it.date < today } // Simplified missed logic
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

    private fun getTodayTimestamp(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
