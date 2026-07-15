package com.example.todolist.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.todolist.data.local.entity.TaskEntity

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isRecurringTemplate = 0 ORDER BY date DESC, done DESC, startTime ASC")
    fun getAllTasks(): LiveData<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): TaskEntity?

    @Query("SELECT * FROM tasks WHERE isRecurringTemplate = 1")
    suspend fun getRecurringTemplates(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE parentId = :templateId AND date = :date LIMIT 1")
    suspend fun getInstanceForTemplate(templateId: Int, date: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE title = :title AND date = :date AND isRecurringTemplate = 0 LIMIT 1")
    suspend fun getTaskByTitleAndDate(title: String, date: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Delete
    suspend fun deleteTasks(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE parentId = :templateId")
    suspend fun deleteInstancesOfTemplate(templateId: Int)

    @Query("SELECT remoteId FROM tasks WHERE parentId = :templateId AND remoteId != ''")
    suspend fun getRemoteIdsOfInstances(templateId: Int): List<String>

    @Transaction
    suspend fun deleteTasksWithInstances(tasks: List<TaskEntity>) {
        tasks.forEach { task ->
            if (task.isRecurringTemplate) {
                deleteInstancesOfTemplate(task.id)
            }
            deleteTask(task)
        }
    }

    @Query("SELECT * FROM tasks WHERE date >= :startOfMonth AND date <= :endOfMonth AND isRecurringTemplate = 0")
    fun getTasksForMonth(startOfMonth: Long, endOfMonth: Long): LiveData<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE date = :date AND isRecurringTemplate = 0")
    suspend fun getTasksForDate(date: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE date >= :start AND date <= :end AND isRecurringTemplate = 0")
    suspend fun getTasksForDateRange(start: Long, end: Long): List<TaskEntity>

    @Query("DELETE FROM tasks WHERE date < :today AND done = 0 AND isRecurringTemplate = 0")
    suspend fun deleteOldIncompleteTasks(today: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM tasks WHERE remoteId = :remoteId)")
    suspend fun existsWithRemoteId(remoteId: String): Boolean
}
