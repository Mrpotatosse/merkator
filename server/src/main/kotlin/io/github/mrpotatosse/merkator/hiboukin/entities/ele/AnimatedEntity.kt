package io.github.mrpotatosse.merkator.hiboukin.entities.ele

import io.github.mrpotatosse.merkator.projections.BoundingBoxGraphicalElementData
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class AnimatedEntity(id: EntityID<Long>) : NormalEntity(id) {
    fun getAnimatedData(): BoundingBoxGraphicalElementData {
        return typedData(getBuffer(), ::BoundingBoxGraphicalElementData)
    }
}