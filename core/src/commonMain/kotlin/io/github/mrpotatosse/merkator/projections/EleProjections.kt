package io.github.mrpotatosse.merkator

import io.github.mrpotatosse.merkator.projections.BasePoint
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class GraphicalElementData

@Serializable
@SerialName("PARTICLES")
data class ParticlesGraphicalElementData(
    val id: Int,
    val scriptId: Short
) : GraphicalElementData()

@Serializable
@SerialName("ENTITY")
data class EntityGraphicalElementData(
    val id: Int,
    val entityLook: String,
    val horizontalSymmetry: Boolean,
    val playAnimation: Boolean,
    val playAnimStatic: Boolean,
    val minDelay: Int,
    val maxDelay: Int,
) : GraphicalElementData()

@Serializable
sealed class NormalBasedElementData : GraphicalElementData() {
    abstract val id: Int
    abstract val gfxId: Int
    abstract val height: Byte
    abstract val horizontalSymmetry: Boolean
    abstract val origin: BasePoint
    abstract val size: BasePoint
}

@Serializable
@SerialName("NORMAL")
data class NormalGraphicalElementData(
    override val id: Int,
    override val gfxId: Int,
    override val height: Byte,
    override val horizontalSymmetry: Boolean,
    override val origin: BasePoint,
    override val size: BasePoint,
) : NormalBasedElementData()

@Serializable
@SerialName("BOUNDING_BOX")
data class BoundingBoxGraphicalElementData(
    override val id: Int,
    override val gfxId: Int,
    override val height: Byte,
    override val horizontalSymmetry: Boolean,
    override val origin: BasePoint,
    override val size: BasePoint,
) : NormalBasedElementData()

@Serializable
@SerialName("BLENDED")
data class BlendedGraphicalElementData(
    override val id: Int,
    override val gfxId: Int,
    override val height: Byte,
    override val horizontalSymmetry: Boolean,
    override val origin: BasePoint,
    override val size: BasePoint,
    var blendMode: String = ""
) : NormalBasedElementData()

@Serializable
@SerialName("ANIMATED")
data class AnimatedGraphicalElementData(
    override val id: Int,
    override val gfxId: Int,
    override val height: Byte,
    override val horizontalSymmetry: Boolean,
    override val origin: BasePoint,
    override val size: BasePoint,
    var minDelay: UInt = 0u,
    var maxDelay: UInt = 0u
) : NormalBasedElementData()