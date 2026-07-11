package io.github.mrpotatosse.merkator.utils.game

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.mrpotatosse.merkator.projections.FixtureElementDraw
import io.github.mrpotatosse.merkator.projections.GraphicalElementDraw
import io.github.mrpotatosse.merkator.projections.MapDrawInformation
import io.github.mrpotatosse.merkator.utils.worldToScreen

fun DrawScope.drawMapCellFromCoords(
    x: Float,
    y: Float,
    originX: Float,
    originY: Float,
    cellWidth: Float,
    cellHeight: Float,
    color: Color = Color.Gray,
    style: DrawStyle = Stroke(width = 1f),
) {
    val (startX, startY) = worldToScreen(x, y, originX, originY, cellWidth, cellHeight)
    val cellHalfWidth = cellWidth / 2.0f
    val cellHalfHeight = cellHeight / 2.0f

    val path = Path().apply {
        moveTo(startX + cellHalfWidth, startY)
        lineTo(startX, startY + cellHalfHeight)
        lineTo(startX - cellHalfWidth, startY)
        lineTo(startX, startY - cellHalfHeight)
        close()
    }

    drawPath(path, color, style = style)
}

fun MapDrawInformation.extractDistinctElementsIds() = this.elements.flatMap { elementDraws ->
    elementDraws.map { element ->
        when (element) {
            is FixtureElementDraw -> element.fixtureId
            is GraphicalElementDraw -> element.gfxId
        }
    }
}.distinct()