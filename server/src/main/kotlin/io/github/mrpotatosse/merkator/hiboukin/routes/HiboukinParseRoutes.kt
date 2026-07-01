package io.github.mrpotatosse.merkator.hiboukin.routes

import io.github.mrpotatosse.merkator.const.AppHiboukinPathDefault
import io.github.mrpotatosse.merkator.const.AppHiboukinPathKey
import io.github.mrpotatosse.merkator.enumerations.GraphicalElementTypeEnum
import io.github.mrpotatosse.merkator.extensions.deflate
import io.github.mrpotatosse.merkator.extensions.extract
import io.github.mrpotatosse.merkator.extensions.readBytes
import io.github.mrpotatosse.merkator.hiboukin.models.D2pDataModel
import io.github.mrpotatosse.merkator.hiboukin.models.EleDataModel
import io.github.mrpotatosse.merkator.hiboukin.models.IsJpgModel
import io.github.mrpotatosse.merkator.hiboukin.services.HiboukinFileReaderService
import io.github.mrpotatosse.merkator.hiboukin.services.HiboukinFileService
import io.github.mrpotatosse.merkator.utils.AppInformation
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.ktor.ext.inject
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension

fun Route.hiboukinParseRoutes() {
    @Serializable
    data class D2pIndexEntry(val path: String, val key: String, val offset: Int, val size: Int)

    @Serializable
    data class EleElementIndexEntry(
        val elementId: Int,
        val elementType: GraphicalElementTypeEnum,
        val offset: Int,
        val size: UInt
    )

    @Serializable
    data class EleIndexesEntry(val path: String, val elements: MutableList<EleElementIndexEntry>)

    @Serializable
    data class HiboukinIndexes(
        val d2pIndexes: MutableList<D2pIndexEntry>,
        val eleIndexes: MutableList<EleIndexesEntry>,
        val isJpgEntries: MutableList<Int>
    )

    post("/hiboukin") {
        val fs by inject<HiboukinFileService>()
        val frs by inject<HiboukinFileReaderService>()

        val folder = call.parameters.getOrFail("folder")
        val chunkSize = call.parameters["chunk-size"]?.toInt() ?: 512
        val hiboukinName = call.parameters["hiboukin"] ?: AppHiboukinPathDefault

        val indexes = frs.readIndexes(folder) {
            fs.authorizedExtension(it)
        }
        val groupedIndexes = indexes.collect(Collectors.groupingBy { it.extension })
        val hiboukinIndexes = HiboukinIndexes(
            mutableListOf(),
            mutableListOf(),
            mutableListOf(),
        )
        for ((extension, paths) in groupedIndexes) {
            for (path in paths) {
                when (extension) {
                    "d2p" -> hiboukinIndexes
                        .d2pIndexes
                        .addAll(
                            frs.readFile(path, fs::parseD2p)
                                .map {
                                    D2pIndexEntry(
                                        path.absolutePathString(),
                                        it.key,
                                        it.offset,
                                        it.size
                                    )
                                }
                        )

                    "ele" -> frs.readFile(path, fs::parseEle).let { (elements, isJpg) ->
                        hiboukinIndexes.eleIndexes.add(
                            EleIndexesEntry(
                                path.absolutePathString(),
                                elements.map {
                                    EleElementIndexEntry(
                                        it.elementId,
                                        it.elementType,
                                        it.offset,
                                        it.size
                                    )
                                }.toMutableList()
                            )
                        )

                        hiboukinIndexes.isJpgEntries.addAll(isJpg)
                    }
                }
            }
        }

        transaction {
            hiboukinIndexes.d2pIndexes.chunked(chunkSize).forEach { chunk ->
                D2pDataModel.batchInsert(chunk) {
                    this[D2pDataModel.path] = it.path
                    this[D2pDataModel.key] = it.key
                    this[D2pDataModel.data] = ExposedBlob(
                        Path.of(it.path)
                            .extract(it.offset, it.size)
                            .readBytes(it.size)
                    )
                }
            }
        }

        transaction {
            hiboukinIndexes.eleIndexes.forEach { ele ->
                val buffer = Path.of(ele.path)
                    .extract()
                    .deflate()
                ele.elements.chunked(chunkSize).forEach { chunk ->
                    EleDataModel.batchInsert(chunk) {
                        this[EleDataModel.path] = ele.path
                        this[EleDataModel.elementId] = it.elementId
                        this[EleDataModel.elementType] = it.elementType
                        this[EleDataModel.data] = ExposedBlob(
                            buffer
                                .extract(it.offset, it.size.toInt())
                        )
                    }
                }

            }
        }

        transaction {
            IsJpgModel.batchInsert(hiboukinIndexes.isJpgEntries) {
                this[IsJpgModel.gfxId] = it
            }
        }

        transaction {
            AppInformation.insert {
                it[AppInformation.key] = "$AppHiboukinPathKey:${hiboukinName}"
                it[AppInformation.value] = folder
            }
        }

        call.respond("inserted")
    }
}