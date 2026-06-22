package io.github.mrpotatosse.merkator

import io.github.mrpotatosse.merkator.enumerations.MapTypeEnum
import io.github.mrpotatosse.merkator.projections.BaseColor
import io.github.mrpotatosse.merkator.projections.BasePoint
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
data class D2pMap(
    val version: Byte,
    val id: UInt,
    val relativeId: UInt,
    val subareaId: Int,
    val mapType: MapTypeEnum,
    val neighbours: MapNeighbours,
    val shadowBonusOnEntities: UInt,
    val color: MapColor,
    val zoom: MapZoom,
    val tacticalModeTemplateId: Int,
    val background: MutableList<Fixture>,
    val foreground: MutableList<Fixture>,
    val groundCRC: Int,
    val layers: MutableList<Layer>
)

@Serializable
data class MapNeighbours(
    val topId: Int,
    val bottomId: Int,
    val leftId: Int,
    val rightId: Int,
)

@Serializable
data class MapColor(
    val grid: Long,
    val background: Long
)

@Serializable
data class MapZoom(
    val scale: Double,
    val offset: BasePoint
)

@Serializable
data class FixtureColor(
    val color: BaseColor,
    val alpha: UByte
)

@Serializable
data class Fixture(
    val fixtureId: Int,
    val offset: BasePoint,
    val rotation: Short,
    val scale: BasePoint,
    val color: FixtureColor
)

@Serializable
data class Layer(
    val layerId: Int,
    val cellsCount: Short,
    val cells: MutableList<Cell>
)

@Serializable
data class Cell(
    val cellId: Short,
    val elementsCount: Short,
    val elements: MutableList<BasicElement>
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class BasicElement

@Serializable
@SerialName("GRAPHICAL")
data class GraphicalElement(
    val cellId: Short,
    val layerId: Int,
    val elementId: UInt,
    val hue: BaseColor,
    val shadow: BaseColor,
    val offset: BasePoint,
    val altitude: Byte,
    val identifier: UInt
) : BasicElement()

@Serializable
@SerialName("SOUND")
data class SoundElement(
    val cellId: Short,
    val layerId: Int,
    val soundId: Int,
    val baseVolume: Short,
    val fullVolumeDistance: Short,
    val nullVolumeDistance: Short,
    val minDelayBetweenLoops: Short,
    val maxDelayBetweenLoops: Short
) : BasicElement()