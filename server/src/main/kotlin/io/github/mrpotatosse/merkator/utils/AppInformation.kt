package io.github.mrpotatosse.merkator.utils

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object AppInformation : LongIdTable("app_information") {
    val key = text("information_key")
    val value = text("information_value")
}