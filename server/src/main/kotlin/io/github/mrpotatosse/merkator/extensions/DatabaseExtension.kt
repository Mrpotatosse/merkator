package io.github.mrpotatosse.merkator.extensions

import io.github.mrpotatosse.merkator.hiboukin.tables.hiboukinTables
import io.github.mrpotatosse.merkator.utils.AppInformation
import org.jetbrains.exposed.v1.core.exposedLogger
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils

object DatabaseSchema {
    val tables = arrayOf(
        AppInformation,

        *hiboukinTables
    )
}

fun initSchema() = transaction {
    exposedLogger.info("Initializing database schema ...")
    val migrationSchema = migrateSchema()
    if (!migrationSchema.isEmpty()) execInBatch(migrationSchema)
}

private fun migrateSchema() = transaction {
    MigrationUtils.statementsRequiredForDatabaseMigration(*DatabaseSchema.tables, withLogs = true)
}