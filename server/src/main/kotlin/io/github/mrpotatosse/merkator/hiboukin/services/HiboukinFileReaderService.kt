package io.github.mrpotatosse.merkator.hiboukin.services

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.function.Predicate
import java.util.stream.Stream

class HiboukinFileReaderService {
    fun readIndexes(path: String, filter: Predicate<Path>) = readIndexes(Path.of(path), filter)

    fun readIndexes(path: Path, filter: Predicate<Path>? = null): Stream<Path> = Files
        .walk(path)
        .filter { Files.isRegularFile(it) and (filter?.test(it) ?: true) }

    fun <R> readFile(
        path: Path,
        action: (path: Path, buffer: ByteBuffer) -> R
    ): R {
        return FileChannel.open(
            path,
            StandardOpenOption.READ
        )
            .use { channel ->
                action(
                    path, channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        0,
                        channel.size()
                    )
                )
            }
    }
}