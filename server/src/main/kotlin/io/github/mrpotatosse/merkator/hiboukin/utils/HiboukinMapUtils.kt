package io.github.mrpotatosse.merkator.hiboukin.utils

fun mapIdToKey(id: UInt) = "${id % 10u}/$id.dlm"
fun gfxIdToKey(id: Int) = "png/${id}.png"
fun keyToGfxId(key: String) = key.removePrefix("png/").removeSuffix(".png").toInt()

fun packArgb(a: Int, r: Int, g: Int, b: Int): Long =
    ((a.toLong() and 0xFF) shl 24) or
            ((r.toLong() and 0xFF) shl 16) or
            ((g.toLong() and 0xFF) shl 8) or
            (b.toLong() and 0xFF)