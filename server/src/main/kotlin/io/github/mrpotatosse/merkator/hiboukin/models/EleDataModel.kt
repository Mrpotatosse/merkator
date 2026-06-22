package io.github.mrpotatosse.merkator.hiboukin.models

import io.github.mrpotatosse.merkator.enumerations.GraphicalElementTypeEnum
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object EleDataModel : LongIdTable("ref_ele_data") {
    val path = text("path")// : String,
    val elementId = integer("element_id").index()
    val elementType = enumeration("element_type", GraphicalElementTypeEnum::class).index()
    val data = blob("data")

    init {
        uniqueIndex(elementId, elementType)
    }
}