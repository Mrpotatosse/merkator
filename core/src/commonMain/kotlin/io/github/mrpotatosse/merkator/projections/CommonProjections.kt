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
) {
    companion object {
        fun clamp(value: Float, min: Float, max: Float): Float = value.coerceIn(min, max)
    }
}