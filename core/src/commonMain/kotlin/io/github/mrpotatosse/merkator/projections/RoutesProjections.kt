package io.github.mrpotatosse.merkator.projections

import kotlinx.serialization.Serializable

@Serializable
data class MapInformation(
    val d2p: D2pMap,
    val elements: Map<Int, NormalGraphicalElementData>,
    val elementsGfx: Map<Int, ByteArray>
)

@Serializable
data class MapDrawInformation(
    val elements: List<List<BasicDraw>>
)

@Serializable
data class MapGfxInformation(
    val gfxs: Map<Int, ByteArray>
)