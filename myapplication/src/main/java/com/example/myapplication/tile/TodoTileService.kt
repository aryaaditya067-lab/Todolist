package com.example.myapplication.tile

import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders.Tile
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@OptIn(ExperimentalHorologistApi::class)
@AndroidEntryPoint
class TodoTileService : SuspendingTileService() {

    @Inject
    lateinit var tileRepository: TileRepository

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
        val data = tileRepository.getTileData()
        return Tile.Builder()
            .setResourcesVersion("1")
            .setFreshnessIntervalMillis(30 * 60 * 1000L)
            .setTileTimeline(
                TimelineBuilders.Timeline.fromLayoutElement(
                    TileRenderer.buildLayout(data)
                )
            )
            .build()
    }
}
