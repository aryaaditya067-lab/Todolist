package com.example.todolist.data.mapper

import com.example.core.domain.model.Task
import com.example.todolist.data.local.entity.TaskEntity

fun TaskEntity.toDomain(): Task {
    return Task(
        id = id,
        remoteId = remoteId,
        title = title,
        description = description,
        category = category,
        priority = priority,
        date = date,
        reminderTime = reminderTime,
        done = done,
        timerType = timerType,
        timerSeconds = timerSeconds,
        targetSeconds = targetSeconds,
        timerStatus = timerStatus,
        repeatMode = repeatMode,
        daysOfWeek = daysOfWeek,
        isRecurringTemplate = isRecurringTemplate,
        parentId = parentId,
        parentRemoteId = parentRemoteId,
        autoCompleteOnTimerEnd = autoCompleteOnTimerEnd,
        notes = notes,
        startTime = startTime,
        endTime = endTime
    )
}

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        remoteId = remoteId,
        title = title,
        description = description,
        category = category,
        priority = priority,
        date = date,
        reminderTime = reminderTime,
        done = done,
        timerType = timerType,
        timerSeconds = timerSeconds,
        targetSeconds = targetSeconds,
        timerStatus = timerStatus,
        repeatMode = repeatMode,
        daysOfWeek = daysOfWeek,
        isRecurringTemplate = isRecurringTemplate,
        parentId = parentId,
        parentRemoteId = parentRemoteId,
        autoCompleteOnTimerEnd = autoCompleteOnTimerEnd,
        notes = notes,
        startTime = startTime,
        endTime = endTime
    )
}
