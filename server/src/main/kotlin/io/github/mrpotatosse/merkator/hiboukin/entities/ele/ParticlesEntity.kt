package io.github.mrpotatosse.merkator.hiboukin.entities.ele

import io.github.mrpotatosse.merkator.ParticlesGraphicalElementData
import io.github.mrpotatosse.merkator.extensions.readShort
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class ParticlesEntity(id: EntityID<Long>) : EleDataEntity(id) {
    fun getData(): ParticlesGraphicalElementData {
        val buffer = getBuffer()
        val scriptId = buffer.readShort()
        return ParticlesGraphicalElementData(
            elementId,
            scriptId
        )
    }
}