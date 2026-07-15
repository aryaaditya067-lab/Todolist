package com.example.myapplication.tile

import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders.Tile
import com.example.core.domain.repository.TaskRepository
import com.example.core.util.Resource
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

@OptIn(ExperimentalHorologistApi::class)
@AndroidEntryPoint
class TaskTileService : SuspendingTileService() {

    @Inject
    lateinit var taskRepository: TaskRepository

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ResourceBuilders.Resources {
        return ResourceBuilders.Resources.Builder()
            .setVersion("1")
            .build()
    }

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ): Tile {
        val resource = taskRepository.getTasksFlow().first { it !is Resource.Loading }
        val allTasks = resource.data ?: emptyList()
        
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayEnd = todayStart + 24 * 60 * 60 * 1000

        val todayTasks = allTasks.filter { it.date in todayStart until todayEnd && !it.isRecurringTemplate }
        val completedCount = todayTasks.count { it.done }
        val totalCount = todayTasks.size
        val pendingTasks = todayTasks.filter { !it.done }.map { it.title }

        return Tile.Builder()
            .setResourcesVersion("1")
            .setFreshnessIntervalMillis(15 * 60 * 1000L) // Update every 15 mins
            .setTileTimeline(
                TimelineBuilders.Timeline.fromLayoutElement(
                    TileRenderer.buildTaskTileLayout(completedCount, totalCount, pendingTasks)
                )
            )
            .build()
    }
}
