package com.example.todolist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.domain.model.*

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val remoteId: String = "",
    val title: String,
    val description: String = "",
    val category: TaskCategory = TaskCategory.PERSONAL,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val date: Long,
    val reminderTime: Long? = null,
    val done: Boolean = false,
    val timerType: TimerType = TimerType.NONE,
    val timerSeconds: Int = 0,
    val targetSeconds: Int = 0,
    val timerStatus: TimerStatus = TimerStatus.NOT_STARTED,
    val repeatMode: RepeatMode = RepeatMode.ONE_TIME,
    val daysOfWeek: String = "",
    val isRecurringTemplate: Boolean = false,
    val parentId: Int = 0,
    val parentRemoteId: String = "",
    val autoCompleteOnTimerEnd: Boolean = false,
    val notes: String = "",
    val startTime: String = "",
    val endTime: String = ""
)
