package io.github.mrpotatosse.merkator.hiboukin.entities.d2p

import io.github.mrpotatosse.merkator.hiboukin.models.D2pDataModel
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

sealed class D2pDataEntity(id: EntityID<Long>) : LongEntity(id) {
    val key by D2pDataModel.key
    val data by D2pDataModel.data

    companion object : LongEntityClass<D2pDataEntity>(D2pDataModel) {
        override fun createInstance(entityId: EntityID<Long>, row: ResultRow?): D2pDataEntity {
            if (row?.get(D2pDataModel.key)?.endsWith(".dlm") ?: false) {
                return MapEntity(entityId)
            } else if ((row?.get(D2pDataModel.key)?.endsWith(".png") ?: false) and
                (row?.get(D2pDataModel.key)?.startsWith("png/") ?: false)
            ) {
                return WorldGfxEntity(entityId)
            }
            return RawD2pEntity(entityId)
        }
    }
}