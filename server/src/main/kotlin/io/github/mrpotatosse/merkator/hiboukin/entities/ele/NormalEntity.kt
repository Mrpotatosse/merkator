package io.github.mrpotatosse.merkator.hiboukin.entities.ele

import io.github.mrpotatosse.merkator.NormalGraphicalElementData
import io.github.mrpotatosse.merkator.enumerations.GraphicalElementTypeEnum
import io.github.mrpotatosse.merkator.extensions.readBoolean
import io.github.mrpotatosse.merkator.extensions.readByte
import io.github.mrpotatosse.merkator.extensions.readInt
import io.github.mrpotatosse.merkator.extensions.readShort
import io.github.mrpotatosse.merkator.hiboukin.models.EleDataModel
import io.github.mrpotatosse.merkator.projections.BasePoint
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.dao.LongEntityClass
import java.nio.ByteBuffer

open class NormalEntity(id: EntityID<Long>) : EleDataEntity(id) {
    fun getData(): NormalGraphicalElementData {
        return typedData(getBuffer(), ::NormalGraphicalElementData)
    }

    companion object : LongEntityClass<NormalEntity>(EleDataModel) {
        fun findAllByElementIdIn(elementIds: Collection<Int>) = find { EleDataModel.elementId inList elementIds }
        fun findAllByElementIdInAndTypeIn(elementIds: Collection<Int>, types: Collection<GraphicalElementTypeEnum>) =
            find { (EleDataModel.elementId inList elementIds) and (EleDataModel.elementType inList types) }
    }

    fun <T> typedData(
        buffer: ByteBuffer,
        ctor: (
            id: Int,
            gfxId: Int,
            height: Byte,
            horizontalSymmetry: Boolean,
            origin: BasePoint,
            size: BasePoint
        ) -> T,
    ): T {
        val gfxId = buffer.readInt()
        val height = buffer.readByte()
        val horizontalSymmetry = buffer.readBoolean()
        val originX = buffer.readShort()
        val originY = buffer.readShort()
        val sizeX = buffer.readShort()
        val sizeY = buffer.readShort()

        return ctor(
            elementId,
            gfxId,
            height,
            horizontalSymmetry,
            BasePoint(originX, originY),
            BasePoint(sizeX, sizeY)
        )
    }
}