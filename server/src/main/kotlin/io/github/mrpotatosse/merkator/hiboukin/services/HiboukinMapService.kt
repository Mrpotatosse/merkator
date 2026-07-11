package io.github.mrpotatosse.merkator.hiboukin.services

import io.github.mrpotatosse.merkator.projections.D2pMap
import io.github.mrpotatosse.merkator.projections.GraphicalElement

class HiboukinMapService {
    fun elements(
        map: D2pMap,
        withGround: Boolean = true,
        withDecor: Boolean = true,
    ) = elements(
        map,
        withGround,
        withDecor
    ) { element ->
        element
    }

    fun <T> elements(
        map: D2pMap,
        withGround: Boolean = true,
        withDecor: Boolean = true,
        mapper: (GraphicalElement) -> T
    ) = map
        .layers
        .map { layer ->
            if ((layer.layerId == 0) and !withGround) emptyList()
            else if ((layer.layerId == 2) and !withDecor) emptyList()
            else layer
                .cells
                .flatMap { cell -> cell.elements }
                .filterIsInstance<GraphicalElement>()
                .map(mapper)
        }.filter { elements -> elements.isNotEmpty() }.toTypedArray()
}