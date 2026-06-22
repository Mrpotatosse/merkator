plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
}

group = "io.github.mrpotatosse.merkator"
version = "1.0.0"
application {
    mainClass = "io.github.mrpotatosse.merkator.ApplicationKt"
}

val osName: String = System.getProperty("os.name", "")
val targetOs = when {
    osName == "Mac OS X" -> "macos"
    osName.startsWith("Win") -> "windows"
    osName.startsWith("Linux") -> "linux"
    else -> error("Unsupported OS: $osName")
}

val targetArch = when (val osArch = System.getProperty("os.arch")) {
    "x86_64", "amd64" -> "x64"
    "aarch64" -> "arm64"
    else -> error("Unsupported arch: $osArch")
}

val skikoTarget = when ("${targetOs}-${targetArch}") {
    "linux-x64" -> libs.skiko.linux.x64
    "linux-arm64" -> libs.skiko.linux.arm64
    "macos-x64" -> libs.skiko.macos.x64
    "macos-arm64" -> libs.skiko.macos.arm64
    "windows-x64" -> libs.skiko.windows.x64
    else -> error("Unsupported target")
}

dependencies {
    api(projects.core)
    implementation(libs.logback)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.json)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.exposed.migration.core)
    implementation(libs.exposed.migration.jdbc)
    implementation(libs.sqlite.jdbc)
    implementation(libs.kotlinx.serialization.json)

    // Source: https://mvnrepository.com/artifact/org.jetbrains.skiko/skiko
    implementation(libs.skiko)
    implementation(skikoTarget)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.testJunit)
}