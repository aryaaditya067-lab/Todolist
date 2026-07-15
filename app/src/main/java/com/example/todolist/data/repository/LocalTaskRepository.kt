package com.example.todolist.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.example.core.domain.model.Task
import com.example.todolist.data.local.dao.TaskDao
import com.example.todolist.data.mapper.toDomain
import com.example.todolist.data.mapper.toEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalTaskRepository @Inject constructor(private val taskDao: TaskDao) {

    val allTasks: LiveData<List<Task>> = taskDao.getAllTasks().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun insert(task: Task): Long {
        return taskDao.insertTask(task.toEntity())
    }

    suspend fun update(task: Task) {
        taskDao.updateTask(task.toEntity())
    }

    suspend fun getTaskById(id: Int): Task? {
        return taskDao.getTaskById(id)?.toDomain()
    }

    suspend fun delete(task: Task) {
        if (task.isRecurringTemplate) {
            taskDao.deleteInstancesOfTemplate(task.id)
        }
        taskDao.deleteTask(task.toEntity())
    }

    suspend fun deleteMultiple(tasks: List<Task>) {
        taskDao.deleteTasksWithInstances(tasks.map { it.toEntity() })
    }

    fun getTasksForMonth(start: Long, end: Long): LiveData<List<Task>> {
        return taskDao.getTasksForMonth(start, end).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun deleteOldIncompleteTasks(today: Long) {
        taskDao.deleteOldIncompleteTasks(today)
    }

    suspend fun getRecurringTemplates(): List<Task> {
        return taskDao.getRecurringTemplates().map { it.toDomain() }
    }

    suspend fun getInstanceForTemplate(templateId: Int, date: Long): Task? {
        return taskDao.getInstanceForTemplate(templateId, date)?.toDomain()
    }

    suspend fun getTaskByTitleAndDate(title: String, date: Long): Task? {
        return taskDao.getTaskByTitleAndDate(title, date)?.toDomain()
    }

    suspend fun existsWithRemoteId(remoteId: String): Boolean {
        return taskDao.existsWithRemoteId(remoteId)
    }

    suspend fun getTasksForDateRange(start: Long, end: Long): List<Task> {
        return taskDao.getTasksForDateRange(start, end).map { it.toDomain() }
    }
}
