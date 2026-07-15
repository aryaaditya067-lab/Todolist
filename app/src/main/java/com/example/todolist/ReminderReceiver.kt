package com.example.todolist

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("TASK_ID", -1)
        val title = intent.getStringExtra("TASK_TITLE") ?: "Task"
        val action = intent.action
        
        when (action) {
            "DONE" -> {
                if (taskId != -1) {
                    // Logic to mark task as done in background
                    // This typically needs a Coroutine or a separate Worker
                    // For simplicity, we can trigger a one-time work to handle DB update
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(taskId)
                }
            }
            "SNOOZE" -> {
                if (taskId != -1) {
                    val workRequest = OneTimeWorkRequestBuilder<SmartNotificationWorker>()
                        .setInitialDelay(10, TimeUnit.MINUTES)
                        .setInputData(Data.Builder()
                            .putInt("TASK_ID", taskId)
                            .putString("TASK_TITLE", title)
                            .build())
                        .build()
                    WorkManager.getInstance(context).enqueue(workRequest)
                    
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(taskId)
                }
            }
            else -> {
                // Original reminder logic (optional if using WorkManager for all)
            }
        }
    }
}
