package com.example.todolist

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.*

class SmartNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getInt("TASK_ID", -1)
        val taskTitle = inputData.getString("TASK_TITLE") ?: "Task"
        
        if (taskId == -1) return Result.failure()

        sendSmartNotification(taskId, taskTitle)
        return Result.success()
    }

    private fun sendSmartNotification(taskId: Int, title: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "smart_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Smart Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val smartMessage = getSmartMessage(title)

        // Done Action
        val doneIntent = Intent(applicationContext, ReminderReceiver::class.java).apply {
            action = "DONE"
            putExtra("TASK_ID", taskId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            applicationContext, taskId * 2, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze Action
        val snoozeIntent = Intent(applicationContext, ReminderReceiver::class.java).apply {
            action = "SNOOZE"
            putExtra("TASK_ID", taskId)
            putExtra("TASK_TITLE", title)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            applicationContext, taskId * 2 + 1, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_checked) // Use your app icon
            .setContentTitle("Smart Reminder")
            .setContentText(smartMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, "Done", donePendingIntent)
            .addAction(0, "Snooze", snoozePendingIntent)
            .build()

        notificationManager.notify(taskId, notification)
    }

    private fun getSmartMessage(title: String): String {
        val templates = listOf(
            "✨ Time to hit the $title! Your future self will thank you.",
            "🚀 Hey! It's $title time. Let's get things done!",
            "💡 Don't forget your $title. You're doing great!",
            "🔥 Focus mode: ON. Time for $title.",
            "🌟 Success is built one task at a time. $title is waiting!"
        )
        return templates.random()
    }
}
