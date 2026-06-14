package com.example.todolist

import androidx.lifecycle.LiveData

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: LiveData<List<Task>> = taskDao.getAllTasks()

    suspend fun insert(task: Task): Long {
        return taskDao.insertTask(task)
    }

    suspend fun update(task: Task) {
        taskDao.updateTask(task)
    }

    suspend fun getTaskById(id: Int): Task? {
        return taskDao.getTaskById(id)
    }

    suspend fun delete(task: Task) {
        if (task.isRecurringTemplate) {
            taskDao.deleteInstancesOfTemplate(task.id)
        }
        taskDao.deleteTask(task)
    }

    suspend fun deleteMultiple(tasks: List<Task>) {
        taskDao.deleteTasksWithInstances(tasks)
    }

    fun getTasksForMonth(start: Long, end: Long): LiveData<List<Task>> {
        return taskDao.getTasksForMonth(start, end)
    }

    suspend fun deleteOldIncompleteTasks(today: Long) {
        taskDao.deleteOldIncompleteTasks(today)
    }

    suspend fun getRecurringTemplates(): List<Task> {
        return taskDao.getRecurringTemplates()
    }

    suspend fun getInstanceForTemplate(templateId: Int, date: Long): Task? {
        return taskDao.getInstanceForTemplate(templateId, date)
    }

    suspend fun getTaskByTitleAndDate(title: String, date: Long): Task? {
        return taskDao.getTaskByTitleAndDate(title, date)
    }

    suspend fun existsWithRemoteId(remoteId: String): Boolean {
        return taskDao.existsWithRemoteId(remoteId)
    }

    suspend fun getTasksForDateRange(start: Long, end: Long): List<Task> {
        return taskDao.getTasksForDateRange(start, end)
    }
}
