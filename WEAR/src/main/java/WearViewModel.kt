package com.example.todolist.wear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WearViewModel : ViewModel() {

    private val repository = WearTaskRepository()

    // Tasks state
    private val _tasks = MutableStateFlow<List<WearTask>>(emptyList())
    val tasks: StateFlow<List<WearTask>> = _tasks.asStateFlow()

    // Habit dots data
    private val _habitData = MutableStateFlow<Map<Long, Float>>(emptyMap())
    val habitData: StateFlow<Map<Long, Float>> = _habitData.asStateFlow()

    // Loading / error state
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        collectTasks()
        collectHabitData()
    }

    private fun collectTasks() {
        viewModelScope.launch {
            try {
                repository.getTodayTasksFlow().collect { taskList ->
                    _tasks.value = taskList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Sync failed"
                _isLoading.value = false
            }
        }
    }

    private fun collectHabitData() {
        viewModelScope.launch {
            try {
                repository.getLast30DaysFlow().collect { data ->
                    _habitData.value = data
                }
            } catch (e: Exception) {
                // Habit data optional — silently fail
            }
        }
    }

    fun toggleTask(task: WearTask) {
        viewModelScope.launch {
            try {
                repository.toggleTaskDone(task)
            } catch (e: Exception) {
                _errorMessage.value = "Could not update task"
            }
        }
    }

    fun deleteTask(task: WearTask) {
        viewModelScope.launch {
            try {
                repository.deleteTask(task)
            } catch (e: Exception) {
                _errorMessage.value = "Could not delete task"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}