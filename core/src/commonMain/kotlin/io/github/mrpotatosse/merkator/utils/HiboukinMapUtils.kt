package io.github.mrpotatosse.merkator.utils

import io.github.mrpotatosse.merkator.const.MapHeight
import io.github.mrpotatosse.merkator.const.MapWidth
import io.github.mrpotatosse.merkator.projections.BaseARGB

fun mapIdToKey(id: UInt) = "${id % 10u}/$id.dlm"
fun gfxIdToKey(id: Int) = "png/${id}.png"
fun keyToGfxId(key: String) = key.removePrefix("png/").removeSuffix(".png").toInt()

fun packArgb(a: Int, r: Int, g: Int, b: Int): Long =
    ((a.toLong() and 0xFF) shl 24) or
            ((r.toLong() and 0xFF) shl 16) or
            ((g.toLong() and 0xFF) shl 8) or
            (b.toLong() and 0xFF)

fun unpackArgb(raw: Long) =
    BaseARGB(
        ((raw shr 24) and 0xFF).toInt(),
        ((raw shr 16) and 0xFF).toInt(),
        ((raw shr 8) and 0xFF).toInt(),
        ((raw) and 0xFF).toInt()
    )

fun cellIdOrNull(x: Int, y: Int): Int? =
    if (x in 0 until MapWidth && y in 0 until MapHeight * 2) y * MapWidth + x
    else null