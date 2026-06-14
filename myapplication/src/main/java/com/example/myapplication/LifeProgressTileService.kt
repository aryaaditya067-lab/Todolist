package com.example.myapplication

import android.content.Context
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.*
import androidx.wear.protolayout.LayoutElementBuilders.*
import androidx.wear.protolayout.ModifiersBuilders.*
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders.Tile
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

@OptIn(ExperimentalHorologistApi::class)
class LifeProgressTileService : SuspendingTileService() {

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
        val data = fetchData()
        return Tile.Builder()
            .setResourcesVersion("1")
            .setFreshnessIntervalMillis(30 * 60 * 1000L)
            .setTileTimeline(
                TimelineBuilders.Timeline.fromLayoutElement(
                    buildLayout(data)
                )
            )
            .build()
    }

    private suspend fun fetchData(): TileData {
        return try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
                ?: return TileData()

            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -31)
            val startTime = calendar.timeInMillis

            val snapshot = FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("tasks")
                .whereGreaterThanOrEqualTo("date", startTime)
                .get().await()

            val allTasks = snapshot.documents.map { doc ->
                Task.fromMap(doc.id, doc.data ?: emptyMap())
            }.filter { !it.isRecurringTemplate }

            val history = mutableMapOf<String, Int>()
            allTasks.groupBy {
                formatDate(it.date)
            }.forEach { (dateKey, dayTasks) ->
                history[dateKey] = when {
                    dayTasks.all { it.done } -> 2
                    dayTasks.any { it.done } -> 1
                    else -> 0
                }
            }

            val cal = Calendar.getInstance()
            val daysLeft = cal.getActualMaximum(Calendar.DAY_OF_MONTH) - cal[Calendar.DAY_OF_MONTH]
            val quote = try { MotivationalQuotes.quotes.random() } catch (e: Exception) { "Keep going." }

            TileData(history = history, daysLeft = daysLeft, quote = quote)
        } catch (e: Exception) {
            TileData()
        }
    }
}

data class TileData(
    val history: Map<String, Int> = emptyMap(),
    val daysLeft: Int = 0,
    val quote: String = ""
)

// ─── Colors ───────────────────────────────────────────────────────────────────
private val C_ORANGE      = 0xFFFF6B00.toInt()
private val C_ORANGE_HALF = 0x80FF6B00.toInt() 
private val C_WHITE       = 0xFFFFFFFF.toInt()
private val C_GRAY        = 0xFF888888.toInt()
private val C_DARK        = 0xFF222222.toInt()
private val C_AMOLED      = 0xFF000000.toInt()

private const val FONT_ROUNDED = "sans-serif-rounded"

private fun formatDate(time: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = time }
    return "${cal[Calendar.YEAR]}-${cal[Calendar.MONTH]}-${cal[Calendar.DAY_OF_MONTH]}"
}

// ─── Layout ───────────────────────────────────────────────────────────────────
private fun buildLayout(data: TileData): LayoutElement {

    val now = System.currentTimeMillis()
    val todayStr = formatDate(now)
    
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        cal.add(Calendar.DAY_OF_YEAR, -1)
    }
    val gridStart = cal.timeInMillis

    // ─── Grid: 4 rows x 7 cols ───────────────────────────────────────────
    val gridColumn = Column.Builder()
        .setWidth(wrap())
        .setHeight(wrap())
        .setHorizontalAlignment(HORIZONTAL_ALIGN_START)

    // Day headers
    gridColumn.addContent(
        Row.Builder().apply {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                addContent(
                    Box.Builder()
                        .setWidth(dp(13f))
                        .setHeight(wrap())
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                        .addContent(
                            Text.Builder()
                                .setText(day)
                                .setFontStyle(
                                    FontStyle.Builder()
                                        .setPreferredFontFamilies(FONT_ROUNDED)
                                        .setSize(sp(10f))
                                        .setColor(argb(C_GRAY))
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
            }
        }.build()
    )

    gridColumn.addContent(Spacer.Builder().setHeight(dp(3f)).build())

    val gridCal = Calendar.getInstance()
    gridCal.timeInMillis = gridStart
    
    for (weekIdx in 0..3) {
        val rowBuilder = Row.Builder()
        for (dayIdx in 0..6) {
            val dateStr = formatDate(gridCal.timeInMillis)
            val status = data.history[dateStr] ?: 0
            val isToday = dateStr == todayStr
            val isPast = gridCal.timeInMillis < now && !isToday

            val dotColor = when {
                isToday -> C_ORANGE
                status == 2 -> C_ORANGE
                isPast -> C_ORANGE_HALF
                else -> C_DARK
            }

            val dot = Box.Builder()
                .setWidth(dp(7f))
                .setHeight(dp(7f))
                .setModifiers(
                    Modifiers.Builder()
                        .setBackground(
                            Background.Builder()
                                .setColor(argb(dotColor))
                                .setCorner(Corner.Builder().setRadius(dp(3.5f)).build())
                                .build()
                        )
                        .build()
                )
                .build()

            rowBuilder.addContent(
                Box.Builder()
                    .setWidth(dp(13f))
                    .setHeight(dp(13f))
                    .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                    .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
                    .setModifiers(
                        Modifiers.Builder()
                            .apply {
                                if (isToday) {
                                    setBorder(
                                        Border.Builder()
                                            .setWidth(dp(1.2f))
                                            .setColor(argb(C_WHITE))
                                            .build()
                                    )
                                }
                            }
                            .setBackground(
                                Background.Builder()
                                    .setCorner(Corner.Builder().setRadius(dp(6.5f)).build())
                                    .build()
                            )
                            .build()
                    )
                    .addContent(dot)
                    .build()
            )
            gridCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        gridColumn.addContent(rowBuilder.build())
        gridColumn.addContent(Spacer.Builder().setHeight(dp(1f)).build())
    }

    // ─── Days left column ────────────────────────────────────────────────
    val daysLeftColumn = Column.Builder()
        .setWidth(wrap())
        .setHeight(wrap())
        .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
        .addContent(
            Text.Builder()
                .setText("${data.daysLeft}")
                .setFontStyle(
                    FontStyle.Builder()
                        .setPreferredFontFamilies(FONT_ROUNDED)
                        .setSize(sp(34f))
                        .setWeight(FONT_WEIGHT_BOLD)
                        .setColor(argb(C_WHITE))
                        .build()
                )
                .build()
        )
        .addContent(
            Text.Builder()
                .setText("DAYS LEFT")
                .setFontStyle(
                    FontStyle.Builder()
                        .setPreferredFontFamilies(FONT_ROUNDED)
                        .setSize(sp(10f))
                        .setColor(argb(C_GRAY))
                        .build()
                )
                .build()
        )
        .build()

    // ─── Grid + Days left side by side ───────────────────────────────────
    val middleRow = Row.Builder()
        .setWidth(wrap())
        .setHeight(wrap())
        .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
        .addContent(gridColumn.build())
        .addContent(Spacer.Builder().setWidth(dp(10f)).build())
        .addContent(daysLeftColumn)
        .build()

    // ─── Full layout ─────────────────────────────────────────────────────
    val quoteText = if (data.quote.isNotEmpty()) "\"${data.quote}\"" else ""

    return Box.Builder()
        .setWidth(expand())
        .setHeight(expand())
        .setModifiers(
            Modifiers.Builder()
                .setBackground(
                    Background.Builder()
                        .setColor(argb(C_AMOLED))
                        .build()
                )
                .build()
        )
        .addContent(
            Column.Builder()
                .setWidth(wrap())
                .setHeight(wrap())
                .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                .addContent(
                    Text.Builder()
                        .setText("LIFE PROGRESS")
                        .setFontStyle(
                            FontStyle.Builder()
                                .setPreferredFontFamilies(FONT_ROUNDED)
                                .setSize(sp(12f))
                                .setWeight(FONT_WEIGHT_BOLD)
                                .setColor(argb(C_ORANGE))
                                .build()
                        )
                        .build()
                )
                .addContent(Spacer.Builder().setHeight(dp(6f)).build())
                .addContent(middleRow)
                .addContent(Spacer.Builder().setHeight(dp(6f)).build())
                .apply {
                    if (quoteText.isNotEmpty()) {
                        addContent(
                            Text.Builder()
                                .setText(quoteText)
                                .setFontStyle(
                                    FontStyle.Builder()
                                        .setPreferredFontFamilies(FONT_ROUNDED)
                                        .setSize(sp(11f))
                                        .setColor(argb(C_WHITE))
                                        .build()
                                )
                                .build()
                        )
                    }
                }
                .build()
        )
        .build()
}
