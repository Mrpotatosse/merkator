package io.github.mrpotatosse.merkator.hiboukin.routes

import io.github.mrpotatosse.merkator.hiboukin.entities.d2p.MapEntity
import io.github.mrpotatosse.merkator.hiboukin.entities.d2p.WorldGfxEntity
import io.github.mrpotatosse.merkator.hiboukin.entities.ele.NormalEntity
import io.github.mrpotatosse.merkator.hiboukin.models.D2pDataModel
import io.github.mrpotatosse.merkator.hiboukin.services.HiboukinMapService
import io.github.mrpotatosse.merkator.projections.MapInformation
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.exposedLogger
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.ktor.ext.inject

fun Route.hiboukinGetRoutes() {
    val mapService by inject<HiboukinMapService>()

    get("/hiboukin") {
        call.respond("OK")
    }

    get("/hiboukin/d2p") {
        val key = call.request.queryParameters.getOrFail("key")
        call.respondBytes(transaction {
            D2pDataModel
                .select(D2pDataModel.data)
                .where { D2pDataModel.key eq key }
                .single()[D2pDataModel.data].bytes
        })
    }

    get("/hiboukin/map/{id}") {
        val mapId = call.parameters.getOrFail("id").toUInt()
        val withGround = call.queryParameters["ground"]?.toBoolean() ?: true
        val withDecor = call.queryParameters["decor"]?.toBoolean() ?: true
        val withGfx = call.queryParameters["gfx"]?.toBoolean() ?: false
        call.respond(transaction {
            val map = MapEntity
                .findByMapId(mapId)
                .getMap()

            val elements = mapService
                .elements(map, withGround, withDecor) { element ->
                    element.elementId.toInt()
                }
                .flatMap { elements ->
                    NormalEntity
                        .findAllByElementIdIn(elements)
                        .map { normal -> normal.getData() }
                }
                .associateBy { it.id }

            val elementsGfx = if (withGfx) WorldGfxEntity
                .findAllByGfxIdIn(elements.map { it.value.gfxId })
                .mapValues { it.value.data.bytes } else emptyMap()

            MapInformation(
                map,
                elements,
                elementsGfx
            )
        })
    }

    get("/hiboukin/map/{id}/render") {
        val mapId = call.parameters.getOrFail("id").toUInt()
        val withGround = call.queryParameters["ground"]?.toBoolean() ?: true
        val withDecor = call.queryParameters["decor"]?.toBoolean() ?: true
        val gridLayer = call.queryParameters["gridLayer"]?.toInt() ?: 100

        val startCom = System.currentTimeMillis()
        val (graphical, normals, gfx) = transaction {
            val map = MapEntity
                .findByMapId(mapId)
                .getMap()

            val graphicalElements = mapService
                .elements(map, withGround, withDecor)

            val normalElements = graphicalElements
                .flatMap { elements ->
                    NormalEntity
                        .findAllByElementIdIn(elements.map { element -> element.elementId.toInt() })
                        .map { normal -> normal.getData() }
                }
                .associateBy { it.id }

            val elementsGfx = WorldGfxEntity
                .findAllByGfxIdIn(normalElements.map { it.value.gfxId })
                .mapValues { it.value.data.bytes }

            Triple(graphicalElements, normalElements, elementsGfx)
        }
        exposedLogger.info("computing took ${System.currentTimeMillis() - startCom}ms")

        val startRed = System.currentTimeMillis()
        val imageBytes = mapService.render(graphical, normals, gfx, gridLayer)
        exposedLogger.info("render took ${System.currentTimeMillis() - startRed}ms")
        call.respond(imageBytes)
    }
}