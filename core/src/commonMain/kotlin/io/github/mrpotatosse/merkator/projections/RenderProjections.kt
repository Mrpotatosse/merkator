package io.github.mrpotatosse.merkator.projections

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
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
    val r: Float,
    val g: Float,
    val b: Float,
) : BasicDraw()

@Serializable
@SerialName("FIXTURE")
data class FixtureElementDraw(
    val fixtureId: Int,
    val x: Float,
    val y: Float,
    val rotation: Short,
    val scale: BasePoint,
    val color: FixtureColor,
) : BasicDraw()