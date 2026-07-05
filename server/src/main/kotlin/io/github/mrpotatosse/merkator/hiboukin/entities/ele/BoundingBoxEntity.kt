package io.github.mrpotatosse.merkator.hiboukin.entities.ele

import io.github.mrpotatosse.merkator.projections.BoundingBoxGraphicalElementData
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class BoundingBoxEntity(id: EntityID<Long>) : NormalEntity(id) {
    fun getBoundingBoxData(): BoundingBoxGraphicalElementData {
        return typedData(getBuffer(), ::BoundingBoxGraphicalElementData)
    }
}