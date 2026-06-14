package com.example.todolist

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.util.*

class TimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var timerJob: Job? = null
    private lateinit var taskDao: TaskDao
    private var currentTaskId: Int = -1

    override fun onCreate() {
        super.onCreate()
        taskDao = AppDatabase.getDatabase(this).taskDao()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val taskId = intent?.getIntExtra("TASK_ID", -1) ?: -1

        when (action) {
            "START" -> {
                if (taskId != -1) {
                    currentTaskId = taskId
                    startForeground(1, createNotification("Timer starting..."))
                    startTicker(taskId)
                }
            }
            "PAUSE" -> {
                pauseTimer()
            }
            "STOP" -> {
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startTicker(taskId: Int) {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                val task = taskDao.getTaskById(taskId)
                
                if (task == null || task.timerStatus != TimerStatus.RUNNING || task.done) {
                    stopSelf()
                    break
                }

                if (task.timerSeconds > 0) {
                    val newSeconds = task.timerSeconds - 1
                    taskDao.updateTask(task.copy(timerSeconds = newSeconds))
                    updateNotification(task.title, newSeconds)
                    delay(1000)
                } else {
                    taskDao.updateTask(task.copy(
                        timerStatus = TimerStatus.FINISHED,
                        timerSeconds = 0,
                        done = task.autoCompleteOnTimerEnd
                    ))
                    showFinishedNotification(task.title)
                    stopSelf()
                    break
                }
            }
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        serviceScope.launch {
            val task = taskDao.getTaskById(currentTaskId)
            task?.let {
                taskDao.updateTask(it.copy(timerStatus = TimerStatus.PAUSED))
                updateNotification(it.title, it.timerSeconds, isPaused = true)
            }
        }
    }

    private fun formatTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format("%02d:%02d:%02d", h, m, s)
        } else {
            String.format("%02d:%02d", m, s)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "timer_channel",
                "Task Timer",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, seconds: Int = 0, isPaused: Boolean = false): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val contentText = if (isPaused) "Paused - ${formatTime(seconds)}" else "Remaining: ${formatTime(seconds)}"

        val builder = NotificationCompat.Builder(this, "timer_channel")
            .setContentTitle("Focusing on: $title")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(!isPaused)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        // Add Pause/Resume Action
        if (isPaused) {
            val resumeIntent = Intent(this, TimerService::class.java).apply { 
                action = "START"
                putExtra("TASK_ID", currentTaskId)
            }
            val resumePendingIntent = PendingIntent.getService(this, 1, resumeIntent, PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePendingIntent)
        } else {
            val pauseIntent = Intent(this, TimerService::class.java).apply { action = "PAUSE" }
            val pausePendingIntent = PendingIntent.getService(this, 2, pauseIntent, PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
        }

        // Add Stop Action
        val stopIntent = Intent(this, TimerService::class.java).apply { action = "STOP" }
        val stopPendingIntent = PendingIntent.getService(this, 3, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)

        return builder.build()
    }

    private fun updateNotification(title: String, seconds: Int, isPaused: Boolean = false) {
        val notification = createNotification(title, seconds, isPaused)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, notification)
    }

    private fun showFinishedNotification(title: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "timer_channel")
            .setContentTitle("Timer Finished")
            .setContentText("Task \"$title\" timer has ended.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(2, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        timerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
