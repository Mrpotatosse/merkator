package io.github.mrpotatosse.merkator.hiboukin.entities.ele

import io.github.mrpotatosse.merkator.extensions.readBoolean
import io.github.mrpotatosse.merkator.extensions.readInt
import io.github.mrpotatosse.merkator.extensions.readUTFBytes
import io.github.mrpotatosse.merkator.projections.EntityGraphicalElementData
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class EntityEntity(id: EntityID<Long>) : EleDataEntity(id) {
    fun getData(): EntityGraphicalElementData {
        val buffer = getBuffer()
        val entityLook = buffer.readUTFBytes(buffer.readInt())
        val horizontalSymmetry = buffer.readBoolean()
        val playAnimation = buffer.readBoolean()
        val playAnimStatic = buffer.readBoolean()
        val minDelay = buffer.readInt()
        val maxDelay = buffer.readInt()
        return EntityGraphicalElementData(
            elementId,
            entityLook,
            horizontalSymmetry,
            playAnimation,
            playAnimStatic,
            minDelay,
            maxDelay
        )
    }
}