package io.github.mrpotatosse.merkator.projections

import kotlinx.serialization.Serializable

@Serializable
data class BasePoint(
    val x: Short,
    val y: Short,
)

@Serializable
data class BaseColor(
    val red: Byte,
    val green: Byte,
    val blue: Byte,
)