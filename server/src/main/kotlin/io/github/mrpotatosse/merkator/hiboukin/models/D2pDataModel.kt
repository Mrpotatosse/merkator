package io.github.mrpotatosse.merkator.hiboukin.models

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object D2pDataModel : LongIdTable("ref_d2p_data") {
    val path = text("path")//: String,
    val key = text("key").index()//: String,
    val data = blob("data")
}