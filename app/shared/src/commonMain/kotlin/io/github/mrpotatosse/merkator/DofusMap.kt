package io.github.mrpotatosse.merkator
/*
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
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mrpotatosse.merkator.api.HiboukinApi
import io.github.mrpotatosse.merkator.components.game.ElementsCanvas
import io.github.mrpotatosse.merkator.components.game.GridCanvas
import io.github.mrpotatosse.merkator.const.CellHalfHeight
import io.github.mrpotatosse.merkator.const.CellHalfWidth
import io.github.mrpotatosse.merkator.const.MapWidth
import io.github.mrpotatosse.merkator.models.game.BasicDrawWithBitmap
import io.github.mrpotatosse.merkator.projections.FixtureElementDraw
import io.github.mrpotatosse.merkator.projections.GraphicalElementDraw
import io.github.mrpotatosse.merkator.projections.MapDrawInformation
import io.github.mrpotatosse.merkator.utils.game.drawMapCellFromCoords
import io.github.mrpotatosse.merkator.utils.getIsoGridLines
import io.github.mrpotatosse.merkator.utils.screenToWorld
import io.github.mrpotatosse.merkator.utils.unpackArgb
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration

sealed interface MapDrawState {
    data object Loading : MapDrawState
    data class Ready(val info: MapDrawInformation) : MapDrawState
    data class Error(val message: String) : MapDrawState
}

@Composable
@Preview
fun DofusMap() {
    val api = remember { HiboukinApi(baseUrl = "http://localhost:8080") }
    val cellWidth = 86
    val cellHeight = 43
    val mapWidth = 14
    val mapHeight = 20
    // zaap astrub : 191105026
    // map eleveur : 73403912
    var currentMapId by remember { mutableStateOf(191105026u) }
    var pointer by remember { mutableStateOf(Offset.Unspecified) }
    var serverOk by remember { mutableStateOf<Boolean?>(null) }
    var timeToRender by remember { mutableStateOf(Duration.ZERO) }
    var withGrid by remember { mutableStateOf(true) }
    var mapDrawState by remember { mutableStateOf<MapDrawState>(MapDrawState.Loading) }
    var drawList by remember { mutableStateOf<List<List<BasicDrawWithBitmap>>>(emptyList()) }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var marginX by remember { mutableStateOf(0f) }
    var marginY by remember { mutableStateOf(0f) }
    val mapCell = remember(pointer, marginX, marginY) {
        screenToWorld(
            screenX = pointer.x,
            screenY = pointer.y,
            originX = marginX,
            originY = marginY,
            cellW = cellWidth.toFloat(),
            cellH = cellHeight.toFloat(),
        )
    }

    val lines = remember(mapWidth, mapHeight, cellWidth, cellHeight) {
        getIsoGridLines(mapWidth, mapHeight, cellWidth, cellHeight)
    }

    LaunchedEffect(Unit) {
        serverOk = runCatching { api.ping() }.getOrDefault(false)
    }

    LaunchedEffect(currentMapId) {
        mapDrawState = MapDrawState.Loading
        mapDrawState = runCatching { api.draw(currentMapId) }.fold(
            onSuccess = { MapDrawState.Ready(it) },
            onFailure = { MapDrawState.Error(it.message ?: "unknown error") }
        )
    }

    LaunchedEffect(mapDrawState) {
        drawList = emptyList()
        when (val state = mapDrawState) {
            is MapDrawState.Ready -> {
                val currentTime = Clock.System.now()
                drawList = withContext(Dispatchers.Default) {
                    val ids = state.info.elements.flatMap { elementDraws ->
                        elementDraws.map { element ->
                            when (element) {
                                is FixtureElementDraw -> element.fixtureId
                                is GraphicalElementDraw -> element.gfxId
                            }
                        }
                    }.distinct()
                    val semaphore = Semaphore(16)
                    val gfxs = coroutineScope {
                        ids.chunked(16).map { chunk ->
                            async {
                                semaphore.withPermit {
                                    try {
                                        api.gfxs(chunk).gfxs
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (_: Exception) {
                                        emptyMap()
                                    }
                                }
                            }
                        }.awaitAll().fold(mutableMapOf<Int, ByteArray>()) { acc, m -> acc.apply { putAll(m) } }
                    }

                    val bitmaps: Map<Int, ImageBitmap> = coroutineScope {
                        gfxs.map { (id, bytes) ->
                            async(Dispatchers.Default) {
                                val img = Image.makeFromEncoded(bytes)
                                val bitmap = Bitmap().apply {
                                    allocPixels(ImageInfo.makeN32Premul(img.width, img.height))
                                }
                                img.readPixels(bitmap, 0, 0)  // forces the decode now, off the UI thread
                                val composeBitmap = bitmap.asComposeImageBitmap()

                                id to composeBitmap
                            }
                        }.awaitAll().toMap()
                    }

                    state.info.elements.map { elementDraws ->
                        elementDraws.mapNotNull { element ->
                            val id = when (element) {
                                is FixtureElementDraw -> element.fixtureId
                                is GraphicalElementDraw -> element.gfxId
                            }
                            bitmaps[id]?.let { BasicDrawWithBitmap(element, it) }
                        }
                    }
                }
                timeToRender = Clock.System.now() - currentTime
            }

            else -> {}
        }
    }
    Box(Modifier.fillMaxSize()) {
        val virtualWidth = 1280f
        val virtualHeight = 1024f

        when (val state = mapDrawState) {
            is MapDrawState.Ready -> {
                val baseColor = unpackArgb(state.info.d2p.color.grid)
                val gridColor = Color(baseColor.red, baseColor.green, baseColor.blue, baseColor.alpha)
                drawList.forEachIndexed { index, draws ->
                    if ((index == 2) and withGrid) {
                        GridCanvas(
                            gridLinesProvider = { lines },
                            gridLinesColor = gridColor,
                            transformBlock = { translate(marginX, marginY) }
                        )
                    }

                    ElementsCanvas(
                        drawsProvider = { draws },
                        transformBlock = {
                            translate(marginX, marginY)
                        }
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged {
                            canvasSize = it

                            marginX = ((canvasSize.width - virtualWidth) / 2f).coerceAtLeast(0f)
                            marginY = ((canvasSize.height - virtualHeight) / 2f).coerceAtLeast(0f)
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    pointer = event.changes.first().position
                                }
                            }
                        }
                ) {
                    withTransform({ translate(marginX, marginY) }) {
                        drawMapCellFromCoords(
                            mapCell.first, mapCell.second,
                            0f, 0f,
                            cellWidth.toFloat(), cellHeight.toFloat(),
                            color = Color.Red.copy(alpha = 0.4f),
                            style = Fill
                        )

                        state.info.d2p.cells.forEach { cell ->
                            if (!cell.mov)
                                drawMapCellFromCoords(
                                    (cell.id % MapWidth).toFloat(), (cell.id / MapWidth).toFloat(),
                                    CellHalfWidth, CellHalfHeight,
                                    cellWidth.toFloat(), cellHeight.toFloat(),
                                    color = Color.Black.copy(alpha = 0.4f),
                                    style = Fill
                                )
                        }
                    }
                    if (pointer.isSpecified) drawCircle(Color.Green, 6f, pointer)
                }

                InfoWatcher(
                    pointer = pointer,
                    originX = marginX + cellWidth / 2f,
                    originY = marginY + cellHeight / 2f,
                    cellWidth = cellWidth.toFloat(),
                    cellHeight = cellHeight.toFloat(),
                    timeToRender = timeToRender,
                    serverOk = serverOk,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )

                MapController(
                    currentMapId = currentMapId,
                    onNavigate = { currentMapId = it },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }

            else -> {}
        }
    }
}

@Composable
fun InfoWatcher(
    pointer: Offset,
    originX: Float,
    originY: Float,
    cellWidth: Float,
    cellHeight: Float,
    timeToRender: Duration,
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
        Text(
            "time to render: $timeToRender",
            color = Color.White, fontSize = 12.sp
        )

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
fun MapController(
    currentMapId: UInt,
    onNavigate: (UInt) -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("map $currentMapId", color = Color.White, fontSize = 12.sp)

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
} */