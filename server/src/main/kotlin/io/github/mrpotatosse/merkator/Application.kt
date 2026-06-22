package io.github.mrpotatosse.merkator

import io.github.mrpotatosse.merkator.hiboukin.routes.hiboukinGetRoutes
import io.github.mrpotatosse.merkator.hiboukin.routes.hiboukinParseRoutes
import io.github.mrpotatosse.merkator.modules.merkatorModules
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    install(CORS) {
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Delete)
        // For ease of demonstration we allow any connections.
        // Don't do this in production.
        anyHost()
    }
    install(Koin) {
        slf4jLogger()
        properties(mapOf("application" to this@module))
        modules(merkatorModules)
    }
    routing {
        hiboukinParseRoutes()
        hiboukinGetRoutes()
    }
}