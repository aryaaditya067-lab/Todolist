package com.example.todolist

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

enum class TimerType {
    NONE, COUNTDOWN
}

enum class TimerStatus {
    NOT_STARTED, RUNNING, PAUSED, FINISHED
}

enum class TaskPriority {
    LOW, MEDIUM, HIGH
}

enum class TaskCategory {
    WORK, STUDY, FITNESS, PERSONAL, GAMING, CODING, FOOD, ART, CYCLING, CLEANING, SLEEP, READING, TRAVEL, MENTAL, FINANCE, SOCIAL, GROOMING, MUSIC, WRITING, TECH, NATURE, LANGUAGE
}

enum class RepeatMode {
    ONE_TIME, DAILY, WEEKDAYS, CUSTOM
}

@Parcelize
@Entity(tableName = "tasks")
data class Task(
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
) : Parcelable {
    val isTimerRunning: Boolean
        get() = timerStatus == TimerStatus.RUNNING

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "title" to title,
            "description" to description,
            "category" to category.name,
            "priority" to priority.name,
            "date" to date,
            "reminderTime" to reminderTime,
            "done" to done,
            "timerType" to timerType.name,
            "timerSeconds" to timerSeconds,
            "targetSeconds" to targetSeconds,
            "timerStatus" to timerStatus.name,
            "repeatMode" to repeatMode.name,
            "daysOfWeek" to daysOfWeek,
            "isRecurringTemplate" to isRecurringTemplate,
            "parentRemoteId" to parentRemoteId,
            "autoCompleteOnTimerEnd" to autoCompleteOnTimerEnd,
            "notes" to notes,
            "startTime" to startTime,
            "endTime" to endTime
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Task {
            return Task(
                remoteId = id,
                title = map["title"] as? String ?: "",
                description = map["description"] as? String ?: "",
                category = TaskCategory.valueOf(map["category"] as? String ?: "PERSONAL"),
                priority = TaskPriority.valueOf(map["priority"] as? String ?: "MEDIUM"),
                date = (map["date"] as? Long) ?: 0L,
                reminderTime = map["reminderTime"] as? Long,
                done = (map["done"] as? Boolean) ?: false,
                timerType = TimerType.valueOf(map["timerType"] as? String ?: "NONE"),
                timerSeconds = (map["timerSeconds"] as? Long)?.toInt() ?: 0,
                targetSeconds = (map["targetSeconds"] as? Long)?.toInt() ?: 0,
                timerStatus = TimerStatus.valueOf(map["timerStatus"] as? String ?: "NOT_STARTED"),
                repeatMode = RepeatMode.valueOf(map["repeatMode"] as? String ?: "ONE_TIME"),
                daysOfWeek = map["daysOfWeek"] as? String ?: "",
                isRecurringTemplate = (map["isRecurringTemplate"] as? Boolean) ?: false,
                parentRemoteId = map["parentRemoteId"] as? String ?: "",
                autoCompleteOnTimerEnd = (map["autoCompleteOnTimerEnd"] as? Boolean) ?: false,
                notes = map["notes"] as? String ?: "",
                startTime = map["startTime"] as? String ?: "",
                endTime = map["endTime"] as? String ?: ""
            )
        }
    }
}
