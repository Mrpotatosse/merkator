package io.github.mrpotatosse.merkator.hiboukin.services

import io.github.mrpotatosse.merkator.const.*
import io.github.mrpotatosse.merkator.hiboukin.utils.drawIsoGrid
import io.github.mrpotatosse.merkator.projections.D2pMap
import io.github.mrpotatosse.merkator.projections.GraphicalElement
import io.github.mrpotatosse.merkator.projections.NormalGraphicalElementData
import org.jetbrains.skia.*

class HiboukinMapService {
    fun elements(
        map: D2pMap,
        withGround: Boolean = true,
        withDecor: Boolean = true,
    ) = elements(
        map,
        withGround,
        withDecor
    ) { element ->
        element
    }

    fun <T> elements(
        map: D2pMap,
        withGround: Boolean = true,
        withDecor: Boolean = true,
        mapper: (GraphicalElement) -> T
    ) = map
        .layers
        .map { layer ->
            if ((layer.layerId == 0) and !withGround) emptyList()
            else if ((layer.layerId == 2) and !withDecor) emptyList()
            else layer
                .cells
                .flatMap { cell -> cell.elements }
                .filterIsInstance<GraphicalElement>()
                .map(mapper)
        }.filter { elements -> elements.isNotEmpty() }.toTypedArray()

    fun renderGraphicalElements(
        elements: List<GraphicalElement>,
        normalElements: Map<Int, NormalGraphicalElementData>,
        gfx: Map<Int, ByteArray>,
        offsetX: Float,
        offsetY: Float,
    ): Picture {
        val recorder = PictureRecorder()
        val recordCanvas = recorder.beginRecording(
            Rect.makeWH(CanvasWidth, CanvasHeight.toFloat())
        )

        for (element in elements.sortedWith(
            compareBy<GraphicalElement> { it.layerId }
                .thenBy { it.cellId }
        )) {
            val col = element.cellId % MapWidth
            val row = element.cellId / MapWidth
            val cellX = col * CellWidth + if (row % 2 == 1) CellHalfWidth.toDouble() else 0.0
            val cellY = row * CellHalfHeight

            val normal = normalElements[element.elementId.toInt()]
            require(normal != null) { "The element ${element.elementId} is null" }

            val originOffsetX = -normal.origin.x
            val originOffsetY = -normal.origin.y
            val dataX = originOffsetX + (CellHalfWidth + element.offset.x)
            val dataY = originOffsetY + (CellHalfHeight - element.altitude * 10.0 + element.offset.y)

            val imgBytes = gfx[normal.gfxId] ?: continue
            val image = Image.makeFromEncoded(imgBytes)

            val destX = (offsetX + cellX + dataX).toFloat()
            val destY = (offsetY + cellY + dataY).toFloat()
            val destW = normal.size.x.toFloat()
            val destH = normal.size.y.toFloat()

            if (normal.horizontalSymmetry) {
                recordCanvas.save()
                recordCanvas.scale(-1f, 1f)
                recordCanvas.drawImageRect(
                    image,
                    Rect.makeXYWH(-destX - destW, destY, destW, destH)
                )
                recordCanvas.restore()
            } else {
                recordCanvas.drawImageRect(image, Rect.makeXYWH(destX, destY, destW, destH))
            }

            image.close()
        }

        return recorder.finishRecordingAsPicture()
    }

    fun render(
        elements: List<List<GraphicalElement>>,
        normalElements: Map<Int, NormalGraphicalElementData>,
        gfx: Map<Int, ByteArray>,
        gridLayer: Int
    ): ByteArray {
        val squareWidth = ((CellWidth * MapWidth) + CellHalfWidth).toFloat()
        val squareHeight = ((CellHeight * MapHeight) + CellHalfHeight)
        val x = (CanvasWidth - squareWidth) / 2f
        val y = (CanvasHeight - squareHeight) / 2f

        val surface = Surface.makeRasterN32Premul(CanvasWidth.toInt(), CanvasHeight)
        val canvas = surface.canvas

        for (layer in elements) {
            val picture = renderGraphicalElements(layer, normalElements, gfx, x, 0f)
            canvas.drawPicture(picture)
            picture.close()
            val first = layer.first()
            if (first.layerId == gridLayer)
                canvas.drawIsoGrid(MapWidth, MapHeight, x, 0f)

        }

        if (gridLayer > elements.size)
            canvas.drawIsoGrid(MapWidth, MapHeight, x, 0f)

        Paint().use { paint ->
            paint.color = Color.BLACK
            paint.mode = PaintMode.STROKE
            paint.strokeWidth = 1f
            canvas.drawRect(
                Rect.makeLTRB(
                    x, 0f,
                    x + squareWidth, squareHeight,
                ),
                paint
            )
        }

        val result = surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)!!.bytes
        surface.close()
        return result
    }
}