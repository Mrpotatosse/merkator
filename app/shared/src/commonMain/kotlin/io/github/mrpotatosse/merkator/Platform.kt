package io.github.mrpotatosse.merkator

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform