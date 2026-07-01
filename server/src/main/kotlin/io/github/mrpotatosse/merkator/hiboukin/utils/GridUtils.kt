package io.github.mrpotatosse.merkator.hiboukin.utils

import io.github.mrpotatosse.merkator.const.CellHalfHeight
import io.github.mrpotatosse.merkator.const.CellHalfWidth
import io.github.mrpotatosse.merkator.const.CellHeight
import io.github.mrpotatosse.merkator.const.CellWidth
import io.github.mrpotatosse.merkator.utils.worldToScreen
import org.jetbrains.skia.*

fun Canvas.drawIsoGrid(width: Int, height: Int, rawOriginX: Float = 0f, rawOriginY: Float = 0f) {
    val font = Font(FontMgr.default.matchFamily("Arial").getTypeface(0))
    val paint = Paint().apply {
        color = Color.WHITE
    }

    val originX = rawOriginX + CellHalfWidth
    val originY = rawOriginY + CellHalfHeight

    var lines = arrayOf<Point>()
    for (y in 0..<height * 2) {
        for (x in 0..<width) {
            val (startX, startY) = worldToScreen(
                x.toFloat(),
                y.toFloat(),
                originX,
                originY,
                CellWidth.toFloat(),
                CellHeight.toFloat()
            )
            val id = (y * width) + x
            val textDiff = font.measureText("$id", paint)
            drawString("$id", startX - (textDiff.width / 2f), startY + (textDiff.height / 2f), font, paint)
            lines += arrayOf(
                // l
                Point(startX - CellHalfWidth, startY),
                Point(startX, startY - CellHalfHeight),

                // t
                Point(startX, startY - CellHalfHeight),
                Point(startX + CellHalfWidth, startY),
            )
            if (((y % 2 == 1) and (x == width - 1)) or (y == (height * 2) - 1)) {
                // l fill
                lines += arrayOf(
                    Point(startX, startY + CellHalfHeight),
                    Point(startX + CellHalfWidth, startY),
                )
            }
            if (((y % 2 == 0) and (x == 0)) or (y == (height * 2) - 1)) {
                // t fill
                lines += arrayOf(
                    Point(startX - CellHalfWidth, startY),
                    Point(startX, startY + CellHalfHeight),
                )
            }
        }
    }
    drawLines(
        lines, paint
    )
}