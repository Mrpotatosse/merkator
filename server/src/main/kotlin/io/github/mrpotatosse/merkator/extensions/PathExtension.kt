package io.github.mrpotatosse.merkator.extensions

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption

fun Path.extract(offset: Int, size: Int): ByteBuffer {
    return FileChannel.open(
        this,
        StandardOpenOption.READ
    )
        .use { channel ->
            channel.map(
                FileChannel.MapMode.READ_ONLY,
                offset.toLong(),
                size.toLong()
            )
        }
}

fun Path.extract(): ByteBuffer {
    return FileChannel.open(
        this,
        StandardOpenOption.READ
    )
        .use { channel ->
            channel.map(
                FileChannel.MapMode.READ_ONLY,
                0,
                channel.size()
            )
        }
}