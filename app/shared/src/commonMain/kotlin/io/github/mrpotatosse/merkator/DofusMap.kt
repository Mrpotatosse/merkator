package io.github.mrpotatosse.merkator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mrpotatosse.merkator.api.HiboukinApi
import io.github.mrpotatosse.merkator.projections.*
import io.github.mrpotatosse.merkator.utils.getIsoGridLines
import io.github.mrpotatosse.merkator.utils.screenToWorld
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image

sealed interface MapState {
    data object Loading : MapState
    data class Ready(val info: MapInformation) : MapState
    data class Error(val message: String) : MapState
}

sealed interface MapDrawState {
    data object Loading : MapDrawState
    data class Ready(val info: MapDrawInformation) : MapDrawState
    data class Error(val message: String) : MapDrawState
}

data class BasicDrawWithBitmap(
    val elementDraw: BasicDraw,
    val bitmap: ImageBitmap
)


@Composable
@Preview
fun DofusMap() {
    val api = remember { HiboukinApi(baseUrl = "http://localhost:8080") }
    val cellWidth = 86
    val cellHeight = 43
    val mapWidth = 14
    val mapHeight = 20
    // zaap astrub : 191105026
    var currentMapId by remember { mutableStateOf(191105026u) }
    var pointer by remember { mutableStateOf(Offset.Unspecified) }
    var serverOk by remember { mutableStateOf<Boolean?>(null) }
    var mapState by remember { mutableStateOf<MapState>(MapState.Loading) }
    var mapDrawState by remember { mutableStateOf<MapDrawState>(MapDrawState.Loading) }
    var drawList by remember { mutableStateOf<List<List<BasicDrawWithBitmap>>>(emptyList()) }

    val lines = remember(mapWidth, mapHeight, cellWidth, cellHeight) {
        getIsoGridLines(mapWidth, mapHeight, cellWidth, cellHeight)
    }

    LaunchedEffect(Unit) {
        serverOk = runCatching { api.ping() }.getOrDefault(false)
    }

    LaunchedEffect(currentMapId) {
        mapState = MapState.Loading
        mapState = runCatching { api.map(currentMapId) }.fold(
            onSuccess = { MapState.Ready(it) },
            onFailure = { MapState.Error(it.message ?: "unknown error") }
        )

        mapDrawState = MapDrawState.Loading
        mapDrawState = runCatching { api.draw(currentMapId) }.fold(
            onSuccess = { MapDrawState.Ready(it) },
            onFailure = { MapDrawState.Error(it.message ?: "unknown error") }
        )
    }

    LaunchedEffect(mapDrawState) {
        drawList = emptyList()
        val decoded = HashMap<Int, ImageBitmap>()
        when (val state = mapDrawState) {
            is MapDrawState.Ready -> {
                drawList = withContext(Dispatchers.Default) {
                    suspend fun bitmapFor(gfxId: Int): ImageBitmap? =
                        decoded.getOrPut(gfxId) {
                            val bytes = runCatching { api.gfx(gfxId) }
                                .fold(
                                    onSuccess = { b -> b },
                                    onFailure = { null }
                                ) ?: return null
                            Image.makeFromEncoded(bytes).toComposeImageBitmap()
                        }
                    state.info.elements.map { elementDraws ->
                        elementDraws.map { element ->
                            when (element) {
                                is FixtureElementDraw -> {
                                    val bitmap = bitmapFor(element.fixtureId) ?: return@map null
                                    BasicDrawWithBitmap(element, bitmap)
                                }

                                is GraphicalElementDraw -> {
                                    val bitmap = bitmapFor(element.gfxId) ?: return@map null
                                    BasicDrawWithBitmap(element, bitmap)
                                }
                            }
                        }.filterNotNull()
                    }
                }
            }

            else -> {}
        }
    }

    Box(Modifier.fillMaxSize()) {
        val virtualWidth = 1280f
        val virtualHeight = 1024f

        var canvasSize by remember { mutableStateOf(IntSize.Zero) }

        // computed once at the composable level, usable everywhere
        val marginX = ((canvasSize.width - virtualWidth) / 2f).coerceAtLeast(0f)
        val marginY = ((canvasSize.height - virtualHeight) / 2f).coerceAtLeast(0f)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            pointer = event.changes.first().position
                        }
                    }
                }
        ) {
            translate(marginX, marginY) {
                drawList.forEach { draws ->
                    draws.forEach { draw ->
                        when (draw.elementDraw) {
                            is FixtureElementDraw -> {
                                val w = draw.bitmap.width
                                val h = draw.bitmap.height
                                val halfW = w * 0.5f
                                val halfH = h * 0.5f

                                // AS3: (fixture.offset.x + CELL_HALF_WIDTH) + halfWidth, (fixture.offset.y + CELL_HEIGHT) + halfHeight
                                // assuming elementDraw.x/y already include the cell constants like your other elements
                                val posX = draw.elementDraw.x + halfW
                                val posY = draw.elementDraw.y + halfH

                                withTransform({
                                    // written in REVERSE of the AS3 matrix order:
                                    translate(
                                        posX,
                                        posY
                                    )                                  // AS3 last:  translate(final pos)
                                    rotate(
                                        degrees = draw.elementDraw.rotation / 100f,        // degrees, no PI/180
                                        pivot = Offset.Zero
                                    )
                                    scale(
                                        draw.elementDraw.scale.x / 1000f,
                                        draw.elementDraw.scale.y / 1000f,
                                        pivot = Offset.Zero
                                    )
                                    translate(
                                        -halfW,
                                        -halfH
                                    )                              // AS3 first: translate(-half)
                                }) {
                                    drawImage(
                                        image = draw.bitmap,
                                        dstOffset = IntOffset.Zero,                        // position lives in the transform
                                        dstSize = IntSize(w, h)
                                    )
                                }
                            }

                            is GraphicalElementDraw -> {
                                val size = IntSize(
                                    draw.elementDraw.width,
                                    draw.elementDraw.height
                                )
                                val offset = IntOffset(
                                    draw.elementDraw.x.toInt(),
                                    draw.elementDraw.y.toInt()
                                )

                                if (draw.elementDraw.flipped) {
                                    withTransform({
                                        scale(
                                            -1f,
                                            1f,
                                            pivot = Offset(draw.elementDraw.x + draw.elementDraw.width / 2f, 0f)
                                        )
                                    }) {
                                        drawImage(
                                            image = draw.bitmap,
                                            dstSize = size,
                                            dstOffset = offset
                                        )
                                    }
                                } else {
                                    drawImage(
                                        image = draw.bitmap,
                                        dstSize = size,
                                        dstOffset = offset
                                    )
                                }
                            }
                        }
                    }
                }

                lines.forEach { (start, end) ->
                    drawLine(
                        color = Color.Gray,
                        start = Offset(start.first, start.second),
                        end = Offset(end.first, end.second),
                        strokeWidth = 1f
                    )
                }
            }
        }

        InfoWatcher(
            pointer = pointer,
            originX = marginX + cellWidth / 2f,
            originY = marginY + cellHeight / 2f,
            cellWidth = cellWidth.toFloat(),
            cellHeight = cellHeight.toFloat(),
            serverOk = serverOk,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        )

        MapInfoPanel(
            mapState = mapState,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        )

        MapController(
            mapState = mapState,
            currentMapId = currentMapId,
            onNavigate = { currentMapId = it },
            modifier = Modifier
                .align(Alignment.TopEnd)
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

@Composable
fun MapInfoPanel(
    mapState: MapState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        when (mapState) {
            is MapState.Loading -> Text("map: loading...", color = Color.White, fontSize = 12.sp)
            is MapState.Error -> Text("map: ${mapState.message}", color = Color(0xFFFF8787), fontSize = 12.sp)
            is MapState.Ready -> {
                val d2p = mapState.info.d2p
                InfoLine("id", d2p.id.toString())
                InfoLine("subarea", d2p.subareaId.toString())
                InfoLine("type", d2p.mapType.toString())
                InfoLine("layers", d2p.layers.size.toString())
                InfoLine("elements", mapState.info.elements.size.toString())
                InfoLine("gfx", mapState.info.elementsGfx.size.toString())
                InfoLine("bg / fg", "${d2p.background.size} / ${d2p.foreground.size}")
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row {
        Text("$label: ", color = Color.Gray, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun MapController(
    mapState: MapState,
    currentMapId: UInt,
    onNavigate: (UInt) -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    val neighbours = (mapState as? MapState.Ready)?.info?.d2p?.neighbours

    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("map $currentMapId", color = Color.White, fontSize = 12.sp)

        // neighbor cross
        NeighborButton("↑", neighbours?.topId, onNavigate)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NeighborButton("←", neighbours?.leftId, onNavigate)
            NeighborButton("→", neighbours?.rightId, onNavigate)
        }
        NeighborButton("↓", neighbours?.bottomId, onNavigate)

        // direct map id input
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.filter(Char::isDigit) },
                placeholder = { Text("map id", fontSize = 12.sp) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color.White),
                modifier = Modifier.width(120.dp)
            )
            Button(
                onClick = { input.toUIntOrNull()?.let(onNavigate) },
                enabled = input.toUIntOrNull() != null,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Go", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun NeighborButton(
    label: String,
    neighbourId: Int?,
    onNavigate: (UInt) -> Unit
) {
    val valid = neighbourId != null && neighbourId > 0
    Button(
        onClick = { if (valid) onNavigate(neighbourId.toUInt()) },
        enabled = valid,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(36.dp)
    ) {
        Text(label, fontSize = 14.sp)
    }
}