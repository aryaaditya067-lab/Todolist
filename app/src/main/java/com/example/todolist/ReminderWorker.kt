package com.example.todolist

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getInt("TASK_ID", -1)
        val taskTitle = inputData.getString("TASK_TITLE") ?: "Task Reminder"
        
        // 1. Get database instance
        val db = AppDatabase.getDatabase(applicationContext)
        val task = db.taskDao().getTaskById(taskId) // You'll need to add this to TaskDao

        // 2. Check if task exists and is NOT done
        if (task != null && !task.done) {
            showNotification(taskTitle, "Don't forget to complete your task!")
            
            // 3. Smart Repetition: If still incomplete, remind again in 1 hour
            scheduleNextReminder(taskId, taskTitle)
        }

        return Result.success()
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "task_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Task Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun scheduleNextReminder(id: Int, title: String) {
        val nextWork = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(1, TimeUnit.HOURS) // Repeat after 1 hour
            .setInputData(workDataOf("TASK_ID" to id, "TASK_TITLE" to title))
            .addTag("reminder_$id")
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "reminder_$id", 
            ExistingWorkPolicy.REPLACE, 
            nextWork
        )
    }
}
