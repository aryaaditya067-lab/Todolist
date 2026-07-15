package com.example.myapplication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.example.core.domain.repository.TaskRepository
import com.example.core.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class TaskComplicationService : SuspendingComplicationDataSourceService() {

    @Inject
    lateinit var taskRepository: TaskRepository

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("5").build(),
            contentDescription = PlainComplicationText.Builder("5 tasks remaining").build()
        ).setTitle(PlainComplicationText.Builder("Tasks").build()).build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ShortTextComplicationData.TYPE) return null

        val count = fetchRemainingTaskCount()
        
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("$count").build(),
            contentDescription = PlainComplicationText.Builder("$count tasks remaining").build()
        ).setTitle(PlainComplicationText.Builder("Tasks").build()).build()
    }

    private suspend fun fetchRemainingTaskCount(): Int {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val tomorrow = today + 24 * 60 * 60 * 1000

        return try {
            val resource = taskRepository.getTasksFlow().first { it !is Resource.Loading }
            val tasks = resource.data ?: emptyList()
            tasks.count { it.date in today until tomorrow && !it.done && !it.isRecurringTemplate }
        } catch (e: Exception) {
            0
        }
    }
}
