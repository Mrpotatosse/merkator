package io.github.mrpotatosse.merkator.hiboukin.services

import io.github.mrpotatosse.merkator.D2pMap
import io.github.mrpotatosse.merkator.GraphicalElement
import io.github.mrpotatosse.merkator.NormalGraphicalElementData
import io.github.mrpotatosse.merkator.hiboukin.utils.*
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
    ) =
        map
            .layers
            .map { layer ->
                if ((layer.layerId == 0) and !withGround) emptyList()
                else if ((layer.layerId == 2) and !withDecor) emptyList()
                else layer
                    .cells
                    .flatMap { cell -> cell.elements }
                    .filterIsInstance<GraphicalElement>()
                    .map(mapper)
            }.filter { elements -> elements.isNotEmpty() }

    fun renderGraphicalElements(
        elements: List<GraphicalElement>,
        normalElements: Map<Int, NormalGraphicalElementData>,
        gfx: Map<Int, ByteArray>,
        offsetX: Int,
        offsetY: Int,
    ): Picture {
        val recorder = PictureRecorder()
        val recordCanvas = recorder.beginRecording(
            Rect.makeWH(CanvasWidth.toFloat(), CanvasHeight.toFloat())
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
        gfx: Map<Int, ByteArray>
    ): ByteArray {
        val squareWidth = 1280
        val squareHeight = 820
        val x = (CanvasWidth - squareWidth) / 2
        val y = (CanvasHeight - squareHeight) / 2

        val surface = Surface.makeRasterN32Premul(CanvasWidth, CanvasHeight)
        val canvas = surface.canvas

        val picture = renderGraphicalElements(elements.flatten(), normalElements, gfx, x, 0)
        canvas.drawPicture(picture)
        picture.close()

        Paint().use { paint ->
            paint.color = Color.BLACK
            paint.mode = PaintMode.STROKE
            paint.strokeWidth = 1f
            canvas.drawRect(
                Rect.makeXYWH(x.toFloat(), y.toFloat(), squareWidth.toFloat(), squareHeight.toFloat()),
                paint
            )
        }

        val result = surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)!!.bytes
        surface.close()
        return result
    }
}