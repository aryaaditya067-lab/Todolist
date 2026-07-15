package com.example.todolist.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.todolist.data.local.dao.TaskDao
import com.example.todolist.data.local.entity.TaskEntity

@Database(entities = [TaskEntity::class], version = 11, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
