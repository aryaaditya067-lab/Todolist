package com.example.myapplication.tile

import com.example.core.domain.model.Task
import com.example.core.domain.repository.TaskRepository
import com.example.core.util.Resource
import com.example.myapplication.MotivationalQuotes
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class TileData(
    val history: Map<String, Int> = emptyMap(),
    val daysLeft: Int = 0,
    val quote: String = ""
)

@Singleton
class TileRepository @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend fun getTileData(): TileData {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -31)
        val startTime = calendar.timeInMillis

        val resource = taskRepository.getTasksFlow().first { it !is Resource.Loading }
        val allTasks = (resource.data ?: emptyList()).filter { it.date >= startTime && !it.isRecurringTemplate }

        val history = mutableMapOf<String, Int>()
        allTasks.groupBy { formatDate(it.date) }.forEach { (dateKey, dayTasks) ->
            history[dateKey] = when {
                dayTasks.all { it.done } -> 2
                dayTasks.any { it.done } -> 1
                else -> 0
            }
        }

        val cal = Calendar.getInstance()
        val daysLeft = cal.getActualMaximum(Calendar.DAY_OF_MONTH) - cal[Calendar.DAY_OF_MONTH]
        val quote = try { MotivationalQuotes.quotes.random() } catch (e: Exception) { "Keep going." }

        return TileData(history = history, daysLeft = daysLeft, quote = quote)
    }

    private fun formatDate(time: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = time }
        return "${cal[Calendar.YEAR]}-${cal[Calendar.MONTH]}-${cal[Calendar.DAY_OF_MONTH]}"
    }
}
