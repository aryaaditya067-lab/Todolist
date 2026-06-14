package com.example.todolist

import android.app.Application

class ApplicationController : Application() {
    lateinit var repository: TaskRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(this)
        repository = TaskRepository(database.taskDao())
    }
}
