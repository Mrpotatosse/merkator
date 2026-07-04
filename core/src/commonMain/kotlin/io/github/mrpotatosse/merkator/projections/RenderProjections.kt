package io.github.mrpotatosse.merkator.projections

import io.github.mrpotatosse.merkator.FixtureColor
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
enum class ElementDrawType {
    FIXTURE, ELEMENT
}

@Serializable
data class ElementDraw(
    val gfxId: Int,
    val x: Float,
    val y: Float,
    val width: Int,
    val height: Int,
    val flipped: Boolean,
    val type: ElementDrawType,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class BasicDraw

@Serializable
@SerialName("GRAPHICAL")
data class GraphicalElementDraw(
    val gfxId: Int,
    val x: Float,
    val y: Float,
    val width: Int,
    val height: Int,
    val flipped: Boolean,
) : BasicDraw()

@Serializable
@SerialName("FIXTURE")
data class FixtureElementDraw(
    val fixtureId: Int,
    val x: Float,
    val y: Float,
    val rotation: Short,
    val scale: BasePoint,
    val color: FixtureColor
) : BasicDraw()