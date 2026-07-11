package io.github.mrpotatosse.merkator.components.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import io.github.mrpotatosse.merkator.const.*
import io.github.mrpotatosse.merkator.models.game.BasicDrawWithBitmap
import io.github.mrpotatosse.merkator.states.game.DofusGameState
import io.github.mrpotatosse.merkator.utils.cellIdOrNull
import io.github.mrpotatosse.merkator.utils.game.drawMapCellFromCoords
import io.github.mrpotatosse.merkator.utils.getIsoGridLines
import io.github.mrpotatosse.merkator.utils.screenToWorld
import io.github.mrpotatosse.merkator.utils.unpackArgb

@Composable
fun DofusGameMap(
    drawList: List<List<BasicDrawWithBitmap>>,
    state: DofusGameState.Ready,
    withGridLines: Boolean = true,
    withMovCell: Boolean = true,
    withPointerCell: Boolean = true,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var pointer by remember { mutableStateOf(Offset.Unspecified) }

    val moveableCells = state.info.d2p.cells.filter { it.mov }.associateBy { it.id }
    val lines = remember {
        getIsoGridLines(MapWidth, MapHeight, CellWidth, CellHeight) { cellId ->
            moveableCells.containsKey(cellId)
        }
    }
    val gridColor = remember(state) {
        val baseColor = unpackArgb(state.info.d2p.color.grid)
        Color(baseColor.red, baseColor.green, baseColor.blue, baseColor.alpha)
    }
    val backgroundColor = remember(state) {
        val baseColor = unpackArgb(state.info.d2p.color.background)
        Color(baseColor.red, baseColor.green, baseColor.blue, baseColor.alpha)
    }
    val virtualWidth = 1280f
    val virtualHeight = 1024f

    val marginX = remember(canvasSize) { ((canvasSize.width - virtualWidth) / 2f).coerceAtLeast(0f) }
    val marginY = remember(canvasSize) { ((canvasSize.height - virtualHeight) / 2f).coerceAtLeast(0f) }
    val mapCell = remember(pointer) {
        screenToWorld(
            screenX = pointer.x,
            screenY = pointer.y,
            originX = marginX + CellHalfWidth,
            originY = marginY + CellHalfHeight,
            cellW = CellWidth.toFloat(),
            cellH = CellHeight.toFloat(),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .background(backgroundColor)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        pointer = event.changes.first().position
                    }
                }
            }) {
        drawList.forEachIndexed { index, draws ->
            if (withGridLines && index == 2) {
                GridCanvas(
                    gridLinesProvider = { lines },
                    gridLinesColor = gridColor,
                    transformBlock = {
                        translate(marginX, marginY)
                    }
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
        ) {
            val mapCellId = cellIdOrNull(mapCell.first.toInt(), mapCell.second.toInt())

            withTransform({
                translate(marginX, marginY)
            }) {
                if (withPointerCell && mapCellId != null && moveableCells.containsKey(mapCellId)) {
                    drawMapCellFromCoords(
                        mapCell.first, mapCell.second,
                        CellHalfWidth, CellHalfHeight,
                        CellWidth.toFloat(), CellHeight.toFloat(),
                        color = Color.Red.copy(alpha = 0.4f),
                        style = Fill
                    )
                }

                state.info.d2p.cells.forEach { cell ->
                    if (withMovCell && !cell.mov)
                        drawMapCellFromCoords(
                            (cell.id % MapWidth).toFloat(), (cell.id / MapWidth).toFloat(),
                            CellHalfWidth, CellHalfHeight,
                            CellWidth.toFloat(), CellHeight.toFloat(),
                            color = Color.Black.copy(alpha = 0.4f),
                            style = Fill
                        )
                }
            }
        }
    }
}
