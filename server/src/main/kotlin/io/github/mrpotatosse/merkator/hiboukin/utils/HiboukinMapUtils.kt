package io.github.mrpotatosse.merkator.hiboukin.utils

fun mapIdToKey(id: UInt) = "${id % 10u}/$id.dlm"
fun gfxIdToKey(id: Int) = "png/${id}.png"
fun keyToGfxId(key: String) = key.removePrefix("png/").removeSuffix(".png").toInt()

fun worldToScreen(x: Float, y: Float, originX: Float, originY: Float): Pair<Float, Float> {
    val sx = (x - y) * (CellWidth / 2f) + originX
    val sy = (x + y) * (CellHeight / 2f) + originY
    return sx to sy
}

fun packArgb(a: Int, r: Int, g: Int, b: Int): Long =
    ((a.toLong() and 0xFF) shl 24) or
            ((r.toLong() and 0xFF) shl 16) or
            ((g.toLong() and 0xFF) shl 8) or
            (b.toLong() and 0xFF)