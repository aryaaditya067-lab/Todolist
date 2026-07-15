package com.example.myapplication.tile

import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.*
import androidx.wear.protolayout.LayoutElementBuilders.*
import androidx.wear.protolayout.ModifiersBuilders.*
import androidx.wear.protolayout.TimelineBuilders
import java.util.Calendar

object TileRenderer {

    private val C_ORANGE      = 0xFFFF6700.toInt()
    private val C_ORANGE_HALF = 0x80FF6700.toInt() 
    private val C_WHITE       = 0xFFFFFFFF.toInt()
    private val C_GRAY        = 0xFF888888.toInt()
    private val C_DARK        = 0xFF242526.toInt()
    private val C_AMOLED      = 0xFF121212.toInt()
    private const val FONT_ROUNDED = "sans-serif-rounded"

    fun buildTaskTileLayout(
        completedCount: Int,
        totalCount: Int,
        topTasks: List<String>
    ): LayoutElement {
        val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
        
        val progressCircle = Box.Builder()
            .setWidth(dp(44f))
            .setHeight(dp(44f))
            .setModifiers(Modifiers.Builder()
                .setBackground(Background.Builder()
                    .setColor(argb(C_DARK))
                    .setCorner(Corner.Builder().setRadius(dp(22f)).build())
                    .build())
                .build())
            .addContent(
                Text.Builder()
                    .setText("${(progress * 100).toInt()}%")
                    .setFontStyle(FontStyle.Builder()
                        .setPreferredFontFamilies(FONT_ROUNDED)
                        .setSize(sp(12f))
                        .setWeight(FONT_WEIGHT_BOLD)
                        .setColor(argb(C_ORANGE))
                        .build())
                    .build()
            ).build()

        val taskColumn = Column.Builder()
            .setWidth(expand())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_START)
            .apply {
                if (topTasks.isEmpty()) {
                    addContent(
                        Text.Builder()
                            .setText("All done! 🎉")
                            .setFontStyle(FontStyle.Builder()
                                .setPreferredFontFamilies(FONT_ROUNDED)
                                .setSize(sp(14f))
                                .setColor(argb(C_WHITE))
                                .build())
                            .build()
                    )
                } else {
                    topTasks.take(3).forEach { taskTitle ->
                        addContent(
                            Text.Builder()
                                .setText("• $taskTitle")
                                .setFontStyle(FontStyle.Builder()
                                    .setPreferredFontFamilies(FONT_ROUNDED)
                                    .setSize(sp(13f))
                                    .setColor(argb(C_WHITE))
                                    .build())
                                .setMaxLines(1)
                                .build()
                        )
                        addContent(Spacer.Builder().setHeight(dp(2f)).build())
                    }
                }
            }.build()

        return Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setModifiers(Modifiers.Builder()
                .setBackground(Background.Builder().setColor(argb(C_AMOLED)).build())
                .setPadding(Padding.Builder().setAll(dp(12f)).build())
                .build())
            .addContent(
                Column.Builder()
                    .setWidth(expand())
                    .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                    .addContent(
                        Row.Builder()
                            .setWidth(expand())
                            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
                            .addContent(progressCircle)
                            .addContent(Spacer.Builder().setWidth(dp(10f)).build())
                            .addContent(
                                Column.Builder()
                                    .addContent(Text.Builder().setText("TODAY").setFontStyle(FontStyle.Builder().setSize(sp(10f)).setColor(argb(C_GRAY)).build()).build())
                                    .addContent(Text.Builder().setText("$completedCount/$totalCount").setFontStyle(FontStyle.Builder().setSize(sp(16f)).setWeight(FONT_WEIGHT_BOLD).setColor(argb(C_WHITE)).build()).build())
                                    .build()
                            ).build()
                    )
                    .addContent(Spacer.Builder().setHeight(dp(12f)).build())
                    .addContent(taskColumn)
                    .build()
            ).build()
    }

    fun buildLayout(data: TileData): LayoutElement {
        val now = System.currentTimeMillis()
        val todayStr = formatDate(now)
        
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        val gridStart = cal.timeInMillis

        val gridColumn = Column.Builder()
            .setWidth(wrap()).setHeight(wrap())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_START)

        gridColumn.addContent(
            Row.Builder().apply {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                    addContent(
                        Box.Builder().setWidth(dp(13f)).setHeight(wrap())
                            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                            .addContent(
                                Text.Builder().setText(day)
                                    .setFontStyle(FontStyle.Builder().setPreferredFontFamilies(FONT_ROUNDED).setSize(sp(10f)).setColor(argb(C_GRAY)).build())
                                    .build()
                            ).build()
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

                val dot = Box.Builder().setWidth(dp(7f)).setHeight(dp(7f))
                    .setModifiers(Modifiers.Builder().setBackground(Background.Builder().setColor(argb(dotColor)).setCorner(Corner.Builder().setRadius(dp(3.5f)).build()).build()).build())
                    .build()

                rowBuilder.addContent(
                    Box.Builder().setWidth(dp(13f)).setHeight(dp(13f))
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                        .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
                        .setModifiers(Modifiers.Builder().apply {
                            if (isToday) setBorder(Border.Builder().setWidth(dp(1.2f)).setColor(argb(C_WHITE)).build())
                        }.setBackground(Background.Builder().setCorner(Corner.Builder().setRadius(dp(6.5f)).build()).build()).build())
                        .addContent(dot).build()
                )
                gridCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            gridColumn.addContent(rowBuilder.build())
            gridColumn.addContent(Spacer.Builder().setHeight(dp(1f)).build())
        }

        val daysLeftColumn = Column.Builder().setWidth(wrap()).setHeight(wrap()).setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
            .addContent(Text.Builder().setText("${data.daysLeft}").setFontStyle(FontStyle.Builder().setPreferredFontFamilies(FONT_ROUNDED).setSize(sp(34f)).setWeight(FONT_WEIGHT_BOLD).setColor(argb(C_WHITE)).build()).build())
            .addContent(Text.Builder().setText("DAYS LEFT").setFontStyle(FontStyle.Builder().setPreferredFontFamilies(FONT_ROUNDED).setSize(sp(10f)).setColor(argb(C_GRAY)).build()).build())
            .build()

        val middleRow = Row.Builder().setWidth(wrap()).setHeight(wrap()).setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .addContent(gridColumn.build())
            .addContent(Spacer.Builder().setWidth(dp(10f)).build())
            .addContent(daysLeftColumn).build()

        val quoteText = if (data.quote.isNotEmpty()) "\"${data.quote}\"" else ""

        return Box.Builder().setWidth(expand()).setHeight(expand())
            .setModifiers(Modifiers.Builder().setBackground(Background.Builder().setColor(argb(C_AMOLED)).build()).build())
            .addContent(
                Column.Builder().setWidth(wrap()).setHeight(wrap()).setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                    .addContent(Text.Builder().setText("LIFE PROGRESS").setFontStyle(FontStyle.Builder().setPreferredFontFamilies(FONT_ROUNDED).setSize(sp(12f)).setWeight(FONT_WEIGHT_BOLD).setColor(argb(C_ORANGE)).build()).build())
                    .addContent(Spacer.Builder().setHeight(dp(6f)).build())
                    .addContent(middleRow)
                    .addContent(Spacer.Builder().setHeight(dp(6f)).build())
                    .apply {
                        if (quoteText.isNotEmpty()) {
                            addContent(Text.Builder().setText(quoteText).setFontStyle(FontStyle.Builder().setPreferredFontFamilies(FONT_ROUNDED).setSize(sp(11f)).setColor(argb(C_WHITE)).build()).build())
                        }
                    }.build()
            ).build()
    }

    private fun formatDate(time: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = time }
        return "${cal[Calendar.YEAR]}-${cal[Calendar.MONTH]}-${cal[Calendar.DAY_OF_MONTH]}"
    }
}
