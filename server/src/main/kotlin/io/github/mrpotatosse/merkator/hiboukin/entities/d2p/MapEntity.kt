package io.github.mrpotatosse.merkator.hiboukin.entities.d2p

import io.github.mrpotatosse.merkator.const.CellHalfHeight
import io.github.mrpotatosse.merkator.const.CellHalfWidth
import io.github.mrpotatosse.merkator.enumerations.ElementTypeEnum
import io.github.mrpotatosse.merkator.enumerations.MapTypeEnum
import io.github.mrpotatosse.merkator.extensions.*
import io.github.mrpotatosse.merkator.hiboukin.models.D2pDataModel
import io.github.mrpotatosse.merkator.hiboukin.utils.DecryptionKeyBytes
import io.github.mrpotatosse.merkator.projections.*
import io.github.mrpotatosse.merkator.utils.mapIdToKey
import io.github.mrpotatosse.merkator.utils.packArgb
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.LongEntityClass
import java.nio.ByteBuffer

class MapEntity(id: EntityID<Long>) : D2pDataEntity(id) {

    companion object : LongEntityClass<MapEntity>(D2pDataModel) {
        fun findByMapId(id: UInt) = find { D2pDataModel.key eq mapIdToKey(id) }.single()
    }

    fun getMapBuffer(): ByteBuffer = ByteBuffer.wrap(data.bytes).deflate()
    fun getMap(): D2pMap {
        val buffer = getMapBuffer()
        val header = buffer.readByte()
        require(header.toInt() == 77) { "Invalid map header $header" }
        val mapVersion = buffer.readByte()
        val id = buffer.readUnsignedInt()

        val decryptedBuffer: ByteBuffer = if (mapVersion >= 7) {
            val encrypted = buffer.readBoolean()
            buffer.readByte()
            val dataLen = buffer.readInt()
            if (encrypted) {
                val enc = buffer.readBytes(dataLen)
                for (i in enc.indices)
                    enc[i] = (enc[i].toInt() xor DecryptionKeyBytes[i % DecryptionKeyBytes.size].toInt()).toByte()
                ByteBuffer.wrap(enc)
            } else {
                buffer
            }
        } else {
            buffer
        }


        val relativeId = decryptedBuffer.readUnsignedInt()
        val mapType = MapTypeEnum.fromId(decryptedBuffer.readByte())
        val subareaId = decryptedBuffer.readInt()
        val topNeighbourId = decryptedBuffer.readInt()
        val bottomNeighbourId = decryptedBuffer.readInt()
        val leftNeighbourId = decryptedBuffer.readInt()
        val rightNeighbourId = decryptedBuffer.readInt()
        val shadowBonusOnEntities = decryptedBuffer.readUnsignedInt()

        var backgroundAlpha = 0
        var backgroundRed = 0
        var backgroundGreen = 0
        var backgroundBlue = 0
        var gridColor = 0L

        if (mapVersion >= 9) {
            var c = decryptedBuffer.readInt()
            backgroundAlpha = (c shr 24) and 0xFF
            backgroundRed = (c shr 16) and 0xFF
            backgroundGreen = (c shr 8) and 0xFF
            backgroundBlue = c and 0xFF
            c = decryptedBuffer.readInt()
            gridColor = packArgb((c shr 24) and 0xFF, (c shr 16) and 0xFF, (c shr 8) and 0xFF, c and 0xFF)
        } else if (mapVersion >= 3) {
            backgroundRed = decryptedBuffer.readByte().toInt()
            backgroundGreen = decryptedBuffer.readByte().toInt()
            backgroundBlue = decryptedBuffer.readByte().toInt()
        }

        val backgroundColor = packArgb(backgroundAlpha, backgroundRed, backgroundGreen, backgroundBlue)

        var zoomScale = 1.0
        var zoomOffsetX: Short = 0
        var zoomOffsetY: Short = 0
        if (mapVersion >= 4) {
            zoomScale = decryptedBuffer.readUnsignedShort().toDouble() / 100.0
            zoomOffsetX = decryptedBuffer.readShort()
            zoomOffsetY = decryptedBuffer.readShort()
            if (zoomScale < 1.0) {
                zoomScale = 1.0; zoomOffsetX = 0; zoomOffsetY = 0
            }
        }
        var tacticalModeTemplateId = 0
        if (mapVersion > 10) {
            tacticalModeTemplateId = decryptedBuffer.readInt()
        }

        val background = getFixtures(decryptedBuffer)
        val foreground = getFixtures(decryptedBuffer)
        decryptedBuffer.readInt()
        val groundCRC = decryptedBuffer.readInt()
        val layers = getLayers(decryptedBuffer, mapVersion)

        return D2pMap(
            mapVersion,
            id,
            relativeId,
            subareaId,
            mapType,
            MapNeighbours(
                topNeighbourId,
                bottomNeighbourId,
                leftNeighbourId,
                rightNeighbourId
            ),
            shadowBonusOnEntities,
            MapColor(
                gridColor,
                backgroundColor,
            ),
            MapZoom(
                zoomScale,
                BasePoint(zoomOffsetX, zoomOffsetY),
            ),
            tacticalModeTemplateId,
            background,
            foreground,
            groundCRC,
            layers
        )
    }

    private fun getFixtures(buffer: ByteBuffer): MutableList<Fixture> {
        val result = mutableListOf<Fixture>()

        val count = buffer.readByte()
        repeat(count.toInt()) {
            result.add(getFixture(buffer))
        }

        return result
    }

    private fun getFixture(buffer: ByteBuffer): Fixture {
        val fixtureId = buffer.readInt()
        val offsetX = buffer.readShort()
        val offsetY = buffer.readShort()
        val rotation = buffer.readShort()
        val xScale = buffer.readShort()
        val yScale = buffer.readShort()
        val redMultiplier = buffer.readByte()
        val greenMultiplier = buffer.readByte()
        val blueMultiplier = buffer.readByte()
        val alpha = buffer.readUnsignedByte()

        return Fixture(
            fixtureId,
            BasePoint(offsetX, offsetY),
            rotation,
            BasePoint(xScale, yScale),
            FixtureColor(
                BaseColor(
                    redMultiplier,
                    greenMultiplier,
                    blueMultiplier
                ),
                alpha
            )

        )
    }

    private fun getLayers(buffer: ByteBuffer, mapVersion: Byte): MutableList<Layer> {
        val result = mutableListOf<Layer>()

        val count = buffer.readByte()
        repeat(count.toInt()) {
            result.add(getLayer(buffer, mapVersion))
        }

        return result
    }

    private fun getLayer(buffer: ByteBuffer, mapVersion: Byte): Layer {
        val layerId = if (mapVersion >= 9) buffer.readByte().toInt() else buffer.readInt()
        val cellsCount = buffer.readShort()

        val cells = mutableListOf<Cell>()

        repeat(cellsCount.toInt()) {
            cells.add(getCell(buffer, mapVersion, layerId))
        }

        return Layer(
            layerId,
            cellsCount,
            cells
        )
    }

    private fun getCell(buffer: ByteBuffer, mapVersion: Byte, layerId: Int): Cell {
        val cellId = buffer.readShort()
        val elementsCount = buffer.readShort()

        val elements = mutableListOf<BasicElement>()
        repeat(elementsCount.toInt()) {
            elements.add(getElement(buffer, mapVersion, cellId, layerId))
        }

        return Cell(
            cellId,
            elementsCount,
            elements
        )
    }

    private fun getElement(buffer: ByteBuffer, mapVersion: Byte, cellId: Short, layerId: Int): BasicElement {
        val elementType = ElementTypeEnum.fromId(buffer.readByte())
        check(elementType != null) { "Invalid element type: $elementType" }
        return when (elementType) {
            ElementTypeEnum.GRAPHICAL -> {
                val elementId = buffer.readUnsignedInt()
                val rHue = buffer.readByte()
                val gHue = buffer.readByte()
                val bHue = buffer.readByte()
                val rShadow = buffer.readByte()
                val gShadow = buffer.readByte()
                val bShadow = buffer.readByte()
                val pixelOffsetX = if (mapVersion <= 4) (buffer.readByte() * CellHalfWidth).toInt().toShort()
                else buffer.readShort()
                val pixelOffsetY = if (mapVersion <= 4) (buffer.readByte() * CellHalfHeight).toInt().toShort()
                else buffer.readShort()
                val altitude = buffer.readByte()
                val identifier = buffer.readUnsignedInt()

                GraphicalElement(
                    cellId,
                    layerId,
                    elementId,
                    BaseColor(
                        rHue,
                        gHue,
                        bHue,
                    ),
                    BaseColor(
                        rShadow,
                        gShadow,
                        bShadow,
                    ),
                    BasePoint(
                        pixelOffsetX,
                        pixelOffsetY
                    ),
                    altitude,
                    identifier,
                )
            }

            ElementTypeEnum.SOUND -> {
                val soundId = buffer.readInt()
                val baseVolume = buffer.readShort()
                val fullVolumeDistance = buffer.readShort()
                val nullVolumeDistance = buffer.readShort()
                val minDelayBetweenLoops = buffer.readShort()
                val maxDelayBetweenLoops = buffer.readShort()

                SoundElement(
                    cellId,
                    layerId,
                    soundId,
                    baseVolume,
                    fullVolumeDistance,
                    nullVolumeDistance,
                    minDelayBetweenLoops,
                    maxDelayBetweenLoops,
                )
            }
        }
    }
}