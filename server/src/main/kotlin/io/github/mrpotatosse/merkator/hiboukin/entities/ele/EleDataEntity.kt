package io.github.mrpotatosse.merkator.hiboukin.entities.ele

import io.github.mrpotatosse.merkator.enumerations.GraphicalElementTypeEnum
import io.github.mrpotatosse.merkator.hiboukin.models.EleDataModel
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import java.nio.ByteBuffer

sealed class EleDataEntity(id: EntityID<Long>) : LongEntity(id) {
    val elementId by EleDataModel.elementId
    private val data by EleDataModel.data
    fun getBuffer(): ByteBuffer = ByteBuffer.wrap(data.bytes)

    companion object : LongEntityClass<EleDataEntity>(EleDataModel) {
        override fun createInstance(entityId: EntityID<Long>, row: ResultRow?): EleDataEntity {
            return when (row?.get(EleDataModel.elementType)) {
                GraphicalElementTypeEnum.NORMAL -> NormalEntity(entityId)
                GraphicalElementTypeEnum.BOUNDING_BOX -> BoundingBoxEntity(entityId)
                GraphicalElementTypeEnum.ANIMATED -> AnimatedEntity(entityId)
                GraphicalElementTypeEnum.ENTITY -> EntityEntity(entityId)
                GraphicalElementTypeEnum.PARTICLES -> ParticlesEntity(entityId)
                GraphicalElementTypeEnum.BLENDED -> BlendedEntity(entityId)
                else -> RawEleEntity(entityId)
            }
        }
    }
}