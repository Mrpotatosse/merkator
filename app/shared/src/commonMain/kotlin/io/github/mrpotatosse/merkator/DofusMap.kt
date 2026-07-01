package io.github.mrpotatosse.merkator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mrpotatosse.merkator.api.HiboukinApi
import io.github.mrpotatosse.merkator.projections.MapInformation
import io.github.mrpotatosse.merkator.utils.getIsoGridLines
import io.github.mrpotatosse.merkator.utils.screenToWorld

@Composable
@Preview
fun DofusMap() {
    val api = remember { HiboukinApi(baseUrl = "http://localhost:8080") }
    val cellWidth = 86
    val cellHeight = 43
    val mapWidth = 14
    val mapHeight = 20
    val currentMapId = 73403912u

    val lines = remember(mapWidth, mapHeight, cellWidth, cellHeight) {
        getIsoGridLines(mapWidth, mapHeight, cellWidth, cellHeight)
    }

    var pointer by remember { mutableStateOf(Offset.Unspecified) }
    var serverOk by remember { mutableStateOf<Boolean?>(null) }
    var mapInfo by remember { mutableStateOf<MapInformation?>(null) }

    LaunchedEffect(Unit) {
        serverOk = runCatching { api.ping() }.getOrDefault(false)
        mapInfo = runCatching { api.map(currentMapId) }.getOrNull()
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            pointer = event.changes.first().position
                        }
                    }
                }
        ) {
            lines.forEach { (start, end) ->
                drawLine(
                    color = Color.Gray,
                    start = Offset(start.first, start.second),
                    end = Offset(end.first, end.second),
                    strokeWidth = 1f
                )
            }
        }
        val cellHalfWidth = cellWidth / 2.0f
        val cellHalfHeight = cellHeight / 2.0f

        InfoWatcher(
            pointer = pointer,
            originX = cellHalfWidth,          // adjust if your grid is offset
            originY = cellHalfHeight,
            cellWidth = cellWidth.toFloat(),
            cellHeight = cellHeight.toFloat(),
            serverOk = serverOk,
            modifier = Modifier
                .padding(8.dp)
        )
    }
}

@Composable
fun InfoWatcher(
    pointer: Offset,
    originX: Float,
    originY: Float,
    cellWidth: Float,
    cellHeight: Float,
    serverOk: Boolean?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        val status = when (serverOk) {
            null -> "server: ..."
            true -> "server: OK"
            false -> "server: DOWN"
        }
        Text(status, color = Color.White, fontSize = 12.sp)

        if (pointer.isSpecified) {
            val (worldX, worldY) = screenToWorld(
                screenX = pointer.x,
                screenY = pointer.y,
                originX = originX,
                originY = originY,
                cellW = cellWidth,
                cellH = cellHeight
            )
            Text(
                "screen: ${pointer.x.toInt()}, ${pointer.y.toInt()}",
                color = Color.White, fontSize = 12.sp
            )
            Text(
                "world: ${worldX.toInt()}, ${worldY.toInt()}",
                color = Color(0xFF8CE99A), fontSize = 12.sp
            )
        }
    }
}
