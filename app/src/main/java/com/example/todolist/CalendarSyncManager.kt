package com.example.todolist

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.core.domain.model.*
import java.util.*

class CalendarSyncManager(private val context: Context) {

    private val APP_IDENTIFIER = "[FocusFlow Task]"

    fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }

    fun syncTaskToCalendar(task: Task): Long? {
        if (!hasPermissions() || task.done) return null

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, task.date)
            put(CalendarContract.Events.DTEND, task.date + 3600000) // Default 1 hour
            put(CalendarContract.Events.TITLE, task.title)
            put(CalendarContract.Events.DESCRIPTION, "$APP_IDENTIFIER\n${task.description}")
            put(CalendarContract.Events.CALENDAR_ID, getDefaultCalendarId())
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }

        val uri: Uri? = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        return uri?.lastPathSegment?.toLong()
    }

    private fun getDefaultCalendarId(): Long {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val cursor: Cursor? = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            CalendarContract.Calendars.VISIBLE + " = 1",
            null,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getLong(0)
            }
        }
        return 1 // Fallback to 1
    }

    fun fetchCalendarEvents(): List<Task> {
        if (!hasPermissions()) return emptyList()

        val tasks = mutableListOf<Task>()
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART
        )

        val selection = "${CalendarContract.Events.DESCRIPTION} NOT LIKE ?"
        val selectionArgs = arrayOf("%$APP_IDENTIFIER%")

        val cursor: Cursor? = context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            val titleIdx = it.getColumnIndex(CalendarContract.Events.TITLE)
            val descIdx = it.getColumnIndex(CalendarContract.Events.DESCRIPTION)
            val startIdx = it.getColumnIndex(CalendarContract.Events.DTSTART)

            while (it.moveToNext()) {
                val title = it.getString(titleIdx) ?: "Untitled Event"
                val description = it.getString(descIdx) ?: ""
                val startTime = it.getLong(startIdx)

                tasks.add(Task(
                    title = title,
                    description = description,
                    date = startTime,
                    category = TaskCategory.PERSONAL,
                    priority = TaskPriority.LOW
                ))
            }
        }
        return tasks
    }
}
