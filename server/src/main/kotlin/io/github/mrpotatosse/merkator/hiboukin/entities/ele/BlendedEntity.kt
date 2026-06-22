package io.github.mrpotatosse.merkator.hiboukin.entities.ele

import io.github.mrpotatosse.merkator.BlendedGraphicalElementData
import io.github.mrpotatosse.merkator.extensions.readInt
import io.github.mrpotatosse.merkator.extensions.readUTFBytes
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class BlendedEntity(id: EntityID<Long>) : NormalEntity(id) {
    fun getBlendedData(): BlendedGraphicalElementData {
        val buffer = getBuffer()
        val data = typedData(buffer, ::BlendedGraphicalElementData)

        data.blendMode = buffer.readUTFBytes(buffer.readInt())

        return data
    }
}