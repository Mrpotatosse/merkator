package io.github.mrpotatosse.merkator.hiboukin.routes

import io.github.mrpotatosse.merkator.const.*
import io.github.mrpotatosse.merkator.enumerations.GraphicalElementTypeEnum
import io.github.mrpotatosse.merkator.hiboukin.entities.d2p.MapEntity
import io.github.mrpotatosse.merkator.hiboukin.entities.d2p.WorldGfxEntity
import io.github.mrpotatosse.merkator.hiboukin.entities.ele.NormalEntity
import io.github.mrpotatosse.merkator.hiboukin.models.D2pDataModel
import io.github.mrpotatosse.merkator.hiboukin.services.HiboukinMapService
import io.github.mrpotatosse.merkator.projections.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
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

    get("/hiboukin/gfx") {
        val gfxId = call.parameters.getOrFail("id").toInt()
        val quality = call.parameters["quality"]?.toInt() ?: 80
        call.respondBytes(transaction {
            val image = Image.makeFromEncoded(WorldGfxEntity.findById(gfxId).data.bytes)
            val data = image.encodeToData(EncodedImageFormat.WEBP, quality)!!
            image.close()
            data.bytes
        })
    }

    get("/hiboukin/gfxs") {
        val gfxIds = call.parameters.getOrFail("ids")
            .split(",")
            .map { it.trim().toInt() }
            .toIntArray().toList()
        call.respond(transaction {
            val gfxs = WorldGfxEntity
                .findAllByGfxIdIn(gfxIds)
                .mapValues { gfx -> gfx.value.data.bytes }

            MapGfxInformation(gfxs)
        })
    }

    get("/hiboukin/draw/{id}") {
        val mapId = call.parameters.getOrFail("id").toUInt()
        val withGround = call.queryParameters["ground"]?.toBoolean() ?: true
        val withDecor = call.queryParameters["decor"]?.toBoolean() ?: true

        call.respond(transaction {
            val map = MapEntity
                .findByMapId(mapId)
                .getMap()

            val graphicalElements = mapService
                .elements(map, withGround, withDecor)

            val normalElements = graphicalElements
                .flatMap { elements ->
                    NormalEntity
                        .findAllByElementIdInAndTypeIn(
                            elements
                                .map { element -> element.elementId.toInt() }, listOf(GraphicalElementTypeEnum.NORMAL)
                        )
                        .map { normal -> normal.getData() }
                }
                .associateBy { it.id }

            val elements = graphicalElements.map { elements ->
                elements.map { element ->
                    val normal = normalElements[element.elementId.toInt()] ?: return@map null

                    val r = BaseColor.clamp((element.hue.red + element.shadow.red + 128f) * 2f, 0f, 512f)
                    val g = BaseColor.clamp((element.hue.green + element.shadow.green + 128f) * 2f, 0f, 512f)
                    val b = BaseColor.clamp((element.hue.blue + element.shadow.blue + 128f) * 2f, 0f, 512f)

                    val col = element.cellId % MapWidth
                    val row = element.cellId / MapWidth
                    val cellX = col * CellWidth + if (row % 2 == 1) CellHalfWidth.toDouble() else 0.0
                    val cellY = row * CellHalfHeight

                    val dataX = -normal.origin.x + (CellHalfWidth + element.offset.x)
                    val dataY = -normal.origin.y + (CellHalfHeight - element.altitude * 10.0 + element.offset.y)

                    GraphicalElementDraw(
                        gfxId = normal.gfxId,
                        x = (cellX + dataX).toFloat(),
                        y = (cellY + dataY).toFloat(),
                        width = normal.size.x.toInt(),
                        height = normal.size.y.toInt(),
                        flipped = normal.horizontalSymmetry,
                        r = r / 255f,
                        g = g / 255f,
                        b = b / 255f,
                    )
                }.filterNotNull()
            }

            val backgrounds = map.background.map { fixture ->
                FixtureElementDraw(
                    fixtureId = fixture.fixtureId,
                    x = fixture.offset.x.toFloat() + CellHalfWidth,
                    y = fixture.offset.y.toFloat() + CellHeight,
                    rotation = fixture.rotation,
                    scale = fixture.scale,
                    color = fixture.color,
                )
            }

            val foregrounds = map.foreground.map { fixture ->
                FixtureElementDraw(
                    fixtureId = fixture.fixtureId,
                    x = fixture.offset.x.toFloat() + CellHalfWidth,
                    y = fixture.offset.y.toFloat() + CellHeight,
                    rotation = fixture.rotation,
                    scale = fixture.scale,
                    color = fixture.color,
                )
            }

            MapDrawInformation(map, listOf(backgrounds, *elements.toTypedArray(), foregrounds))
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
                        .findAllByElementIdInAndTypeIn(elements, listOf(GraphicalElementTypeEnum.NORMAL))
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

    /* get("/hiboukin/map/{id}/render") {
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
    } */
}