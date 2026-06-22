package io.github.mrpotatosse.merkator.hiboukin.services

import io.github.mrpotatosse.merkator.enumerations.GraphicalElementTypeEnum
import io.github.mrpotatosse.merkator.extensions.*
import org.jetbrains.exposed.v1.core.exposedLogger
import java.nio.ByteBuffer
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.pathString

class HiboukinFileService {
    val extensions = setOf("d2p", "ele", "d2o")

    fun authorizedExtension(path: Path): Boolean = extensions.any {
        path.extension.equals(it, ignoreCase = true)
    }

    data class D2pEntry(
        val key: String,
        val offset: Int,
        val size: Int
    )

    fun parseD2p(path: Path, buffer: ByteBuffer) = sequence {
        val header = buffer.readByte() + buffer.readByte()
        require(header == 3) { "Invalid d2p header ($header) | file=${path.pathString}" }
        buffer.seek(buffer.size() - 24)
        val diff = buffer.readInt()
        buffer.readInt() // skip
        val startOffset = buffer.readInt()
        val totalElements = buffer.readInt()
        exposedLogger.trace("Parsing D2P sequence | name=${path.pathString} totalElements=$totalElements")

        buffer.seek(startOffset)
        repeat(totalElements) {
            val key = buffer.readUTF()
            val offset = buffer.readInt() + diff
            val size = buffer.readInt()

            yield(D2pEntry(key, offset, size))
        }
    }

    data class EleEntry(
        val elementId: Int,
        val elementType: GraphicalElementTypeEnum,
        val offset: Int,
        val size: UInt
    )

    fun parseEle(path: Path, raw: ByteBuffer): Pair<Sequence<EleEntry>, Sequence<Int>> {
        val buffer = raw.deflate()
        val header = buffer.readByte()
        require(header.toInt() == 69) { "Invalid ele header  ($header) | file=${path.pathString}" }
        val fileVersion = buffer.readByte()

        val elementsSequence = sequence {
            val elementsCount = buffer.readUnsignedInt()
            require(fileVersion >= 9) { "Unsupported ele file version ($fileVersion) | file=${path.pathString}" }
            exposedLogger.trace("Parsing Ele sequence | version=$fileVersion elements=$elementsCount")
            repeat(elementsCount.toInt()) {
                val skip = buffer.readUnsignedShort()
                val startOffset = buffer.position()
                val elementId = buffer.readInt()
                val elementType = GraphicalElementTypeEnum.fromId(buffer.readByte())
                require(elementType != null) { "Unsupported element type ($elementType) | file=${path.pathString}" }
                val offset = buffer.position()
                val size = skip - (offset - startOffset).toUInt()

                yield(EleEntry(elementId, elementType, offset, size))
                buffer.readBytes(size.toInt())
            }
        }

        val isJpgSequence = sequence {
            val isJpgCount = buffer.readInt()
            repeat(isJpgCount) {
                yield(buffer.readInt())
            }
        }

        return Pair(elementsSequence, isJpgSequence)
    }

    fun parseD2o(path: Path, buffer: ByteBuffer) = sequence<Unit> {

    }
}