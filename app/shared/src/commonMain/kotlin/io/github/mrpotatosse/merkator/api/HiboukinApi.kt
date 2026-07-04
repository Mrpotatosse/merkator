package io.github.mrpotatosse.merkator.api

import io.github.mrpotatosse.merkator.projections.MapDrawInformation
import io.github.mrpotatosse.merkator.projections.MapGfxInformation
import io.github.mrpotatosse.merkator.projections.MapInformation
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

// shared/commonMain/kotlin/api/HiboukinApi.kt
class HiboukinApi(
    private val baseUrl: String,
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) {
    /** GET /hiboukin — health check */
    suspend fun ping(): Boolean =
        client.get("$baseUrl/hiboukin").status.isSuccess()

    /** GET /hiboukin/d2p?key=... — raw bytes */
    suspend fun d2p(key: String): ByteArray =
        client.get("$baseUrl/hiboukin/d2p") {
            parameter("key", key)
        }.body()

    /** GET /hiboukin/gfx?id=... — raw bytes */
    suspend fun gfx(id: Int, quality: Int = 80): ByteArray =
        client.get("$baseUrl/hiboukin/gfx") {
            parameter("id", id)
            parameter("quality", quality)
        }.body()

    /** GET /hiboukin/gfxs?ids=... — raw bytes */
    suspend fun gfxs(ids: List<Int>, quality: Int = 80): MapGfxInformation =
        client.get("$baseUrl/hiboukin/gfxs") {
            parameter("ids", ids.joinToString(","))
            parameter("quality", quality)
        }.body()

    /** GET /hiboukin/draw/{id}?ground&decor&gfx */
    suspend fun draw(
        mapId: UInt,
        withGround: Boolean = true,
        withDecor: Boolean = true
    ): MapDrawInformation =
        client.get("$baseUrl/hiboukin/draw/$mapId") {
            parameter("ground", withGround)
            parameter("decor", withDecor)
        }.body()

    /** GET /hiboukin/map/{id}?ground&decor&gfx */
    suspend fun map(
        mapId: UInt,
        withGround: Boolean = true,
        withDecor: Boolean = true,
        withGfx: Boolean = false,
    ): MapInformation =
        client.get("$baseUrl/hiboukin/map/$mapId") {
            parameter("ground", withGround)
            parameter("decor", withDecor)
            parameter("gfx", withGfx)
        }.body()
}