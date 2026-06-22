package io.github.mrpotatosse.merkator.hiboukin.utils

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import kotlin.math.ceil
import kotlin.math.floor

class IsoGridRenderer(
    var tileWidth: Float = CellWidth.toFloat(),
    var tileHeight: Float = CellHeight.toFloat(),
) {

    private val paint = Paint().apply {
        isAntiAlias = true
        color = 0xFF444444.toInt()
    }

    private val majorPaint = Paint().apply {
        isAntiAlias = true
        color = 0xFF777777.toInt()
    }

    fun draw(
        canvas: Canvas,
        screenWidth: Int,
        screenHeight: Int,
        cameraX: Float,
        cameraY: Float,
        originX: Float,
        originY: Float,
        majorStep: Int = 10
    ) {
        // how many tiles we need to cover screen (overscan included)
        val cols = (screenWidth / tileWidth).toInt() + 10
        val rows = (screenHeight / tileHeight).toInt() + 10

        fun worldToScreen(x: Float, y: Float): Pair<Float, Float> {
            val sx = (x - y) * (tileWidth / 2f) + originX - cameraX
            val sy = (x + y) * (tileHeight / 2f) + originY - cameraY
            return sx to sy
        }

        // estimate visible world bounds (rough but works well)
        val minX = floor((cameraX / tileWidth) - cols).toInt()
        val maxX = ceil((cameraX / tileWidth) + cols).toInt()
        val minY = floor((cameraY / tileHeight) - rows).toInt()
        val maxY = ceil((cameraY / tileHeight) + rows).toInt()

        // draw diagonal grid lines (isometric lattice)
        for (i in minX..maxX) {
            drawIsoLine(canvas, i, minY, i, maxY, ::worldToScreen, isMajor = (i % majorStep == 0))
        }

        for (j in minY..maxY) {
            drawIsoLine(canvas, minX, j, maxX, j, ::worldToScreen, isMajor = (j % majorStep == 0))
        }
    }

    private fun drawIsoLine(
        canvas: Canvas,
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int,
        project: (Float, Float) -> Pair<Float, Float>,
        isMajor: Boolean
    ) {
        val p = if (isMajor) majorPaint else paint

        val (sx1, sy1) = project(x1.toFloat(), y1.toFloat())
        val (sx2, sy2) = project(x2.toFloat(), y2.toFloat())

        canvas.drawLine(sx1, sy1, sx2, sy2, p)
    }
}

fun Canvas.drawIsoGrid(grid: IsoGridRenderer, width: Int, height: Int, originX: Float = 0f, originY: Float = 0f) {
    grid.draw(
        canvas = this,
        screenWidth = width,
        screenHeight = height,
        cameraX = 0f,
        cameraY = 0f,
        originX = originX + (width / 2f),
        originY = originY + (height / 2f),
    )
}