package com.example.todolist

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class ReminderManager(private val context: Context) {

    fun scheduleReminder(task: Task) {
        if (task.done || task.reminderTime == null) {
            cancelReminder(task.id)
            return
        }

        val delay = task.reminderTime - System.currentTimeMillis()
        if (delay <= 0) return // Time has already passed

        val data = workDataOf(
            "TASK_ID" to task.id,
            "TASK_TITLE" to task.title
        )

        val reminderWork = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("reminder_${task.id}")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "reminder_${task.id}",
            ExistingWorkPolicy.REPLACE,
            reminderWork
        )
    }

    fun cancelReminder(taskId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork("reminder_$taskId")
    }
}
