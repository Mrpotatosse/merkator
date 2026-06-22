package io.github.mrpotatosse.merkator.hiboukin.models

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object IsJpgModel : LongIdTable("ref_is_jpg") {
    val gfxId = integer("gfx_id")
}