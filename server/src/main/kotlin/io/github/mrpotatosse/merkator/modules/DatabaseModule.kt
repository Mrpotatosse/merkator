package io.github.mrpotatosse.merkator.modules

import io.github.mrpotatosse.merkator.extensions.initSchema
import io.ktor.server.application.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.dsl.module
import java.io.File

val databaseModule = module {
    single<Database>(createdAtStart = true) {
        val app = getProperty<Application>("application")
        val url = app.environment.config.property("db.url").getString()
        val drop = app.environment.config.propertyOrNull("db.drop")?.getString()?.toBoolean() ?: false
        require(url.startsWith("jdbc:sqlite:")) {
            "Unsupported database url: $url"
        }

        val dbPath = url.removePrefix("jdbc:sqlite:")
        val file = File(dbPath)
        if (drop) {
            if (!file.deleteRecursively()) logger.warn("Cannot delete database $dbPath")
            else logger.info("Database $dbPath dropped")
        }
        file.parentFile?.mkdirs()

        logger.info("Connecting to database: $url")
        Database.connect(url = url, driver = "org.sqlite.JDBC").apply {
            initSchema()
        }
    }
}