package io.github.mrpotatosse.merkator.hiboukin.entities.d2p

import io.github.mrpotatosse.merkator.hiboukin.models.D2pDataModel
import io.github.mrpotatosse.merkator.hiboukin.utils.gfxIdToKey
import io.github.mrpotatosse.merkator.hiboukin.utils.keyToGfxId
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.dao.LongEntityClass

class WorldGfxEntity(id: EntityID<Long>) : D2pDataEntity(id) {
    companion object : LongEntityClass<WorldGfxEntity>(D2pDataModel) {
        fun findAllByGfxIdIn(elementIds: Collection<Int>): Map<Int, WorldGfxEntity> =
            find { D2pDataModel.key inList elementIds.map(::gfxIdToKey) }
                .associateBy { keyToGfxId(it.key) }
    }
}