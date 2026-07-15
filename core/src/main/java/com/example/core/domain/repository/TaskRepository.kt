package com.example.core.domain.repository

import com.example.core.domain.model.Task
import com.example.core.util.Resource
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getTasksFlow(): Flow<Resource<List<Task>>>
    suspend fun addTask(task: Task): Resource<Unit>
    suspend fun updateTask(task: Task): Resource<Unit>
    suspend fun deleteTask(remoteId: String): Resource<Unit>
    suspend fun toggleTaskDone(remoteId: String, done: Boolean): Resource<Unit>
}
