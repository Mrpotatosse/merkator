package io.github.mrpotatosse.merkator.extensions

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Inflater


/*
 * Optimized big-endian binary extensions for ByteBuffer
 *
 * Notes:
 * - ByteBuffer already provides:
 *      position()
 *      position(Int)
 *      remaining()
 *      capacity()
 *      get()
 *      getShort()
 *      getInt()
 *      getLong()
 *      getFloat()
 *      getDouble()
 *
 * - These extensions avoid allocations whenever possible.
 */

private const val CHUNK_BIT_SIZE = 7
private const val MASK_10000000 = 0x80
private const val MASK_01111111 = 0x7F

/*
 * ============================================================
 * Position / Size
 * ============================================================
 */

fun ByteBuffer.seek(position: Int): ByteBuffer =
    apply { position(position) }

fun ByteBuffer.size(): Int =
    limit()

/*
 * ============================================================
 * Primitive Reads
 * ============================================================
 */

fun ByteBuffer.readBoolean(): Boolean =
    get().toInt() != 0

fun ByteBuffer.readByte(): Byte =
    get()

fun ByteBuffer.readUnsignedByte(): UByte =
    get().toUByte()

fun ByteBuffer.readSByte(): Byte =
    get()

fun ByteBuffer.readShort(): Short =
    order(ByteOrder.BIG_ENDIAN).short

fun ByteBuffer.readUnsignedShort(): UShort =
    readShort().toUShort()

fun ByteBuffer.readInt(): Int =
    order(ByteOrder.BIG_ENDIAN).int

fun ByteBuffer.readUnsignedInt(): UInt =
    readInt().toUInt()

fun ByteBuffer.readLong(): Long =
    order(ByteOrder.BIG_ENDIAN).long

fun ByteBuffer.readFloat(): Float =
    order(ByteOrder.BIG_ENDIAN).float

fun ByteBuffer.readDouble(): Double =
    order(ByteOrder.BIG_ENDIAN).double

/*
 * ============================================================
 * Byte Arrays
 * ============================================================
 */

fun ByteBuffer.readBytes(count: Int): ByteArray {
    if (count <= 0) return ByteArray(0)

    val bytes = ByteArray(count)
    get(bytes)
    return bytes
}

/*
 * ============================================================
 * UTF
 * ============================================================
 */

fun ByteBuffer.readUTF(): String {
    val length = readShort().toInt() and 0xFFFF
    return readUTFBytes(length)
}

fun ByteBuffer.readUTFUnsigned(): String {
    val length = readVarShort().toInt() and 0xFFFF
    return readUTFBytes(length)
}

fun ByteBuffer.readUTF7BitLength(): String {
    val length = readInt()
    return readUTFBytes(length)
}

fun ByteBuffer.readUTFBytes(length: Int): String {
    if (length <= 0) return ""

    val bytes = ByteArray(length)
    get(bytes)

    return bytes.decodeToString()
}

/*
 * ============================================================
 * VarInt
 * ============================================================
 */

fun ByteBuffer.readVarInt(): Int {
    var result = 0
    var shift = 0

    while (shift < 32) {
        val b = readUnsignedByte().toInt()

        result = result or ((b and MASK_01111111) shl shift)

        if ((b and MASK_10000000) == 0) {
            return result
        }

        shift += CHUNK_BIT_SIZE
    }

    error("Too much data")
}

fun ByteBuffer.readVarShort(): Short {
    var result = 0
    var shift = 0

    while (shift < 16) {
        val b = readUnsignedByte().toInt()

        result = result or ((b and MASK_01111111) shl shift)

        if ((b and MASK_10000000) == 0) {
            return result.toShort()
        }

        shift += CHUNK_BIT_SIZE
    }

    error("Too much data")
}

fun ByteBuffer.readVarLong(): Long {
    var result = 0L
    var shift = 0

    while (shift < 64) {
        val b = readUnsignedByte().toLong()

        result = result or ((b and 0x7F) shl shift)

        if ((b and 0x80) == 0L) {
            return result
        }

        shift += 7
    }

    error("Too much data")
}

/*
 * ============================================================
 * Unsigned aliases
 * ============================================================
 */

fun ByteBuffer.readVarUhInt(): Int =
    readVarInt()

fun ByteBuffer.readVarUhLong(): Long =
    readVarLong()

fun ByteBuffer.readVarUhShort(): Short =
    readVarShort()

/*
 * ============================================================
 * ZLIB Deflate
 * ============================================================
 */

fun ByteBuffer.deflate(initialCapacity: Int = 1024 * 8): ByteBuffer {
    val inflater = Inflater()
    val outputStream = ByteArrayOutputStream()
    try {
        val buffer = ByteArray(initialCapacity)
        inflater.setInput(this)
        var count = -1
        while (count != 0) {
            count = inflater.inflate(buffer)
            outputStream.write(buffer, 0, count)
        }
        val bytes = outputStream.toByteArray()
        return ByteBuffer.allocateDirect(bytes.size).put(bytes).flip()
    } finally {
        inflater.end()
        outputStream.close()
    }
}

/*
 * ============================================================
 * InputStream
 * ============================================================
 */

fun ByteBuffer.asInputStream(): InputStream {
    val src = this

    return object : InputStream() {

        override fun read(): Int {
            if (!src.hasRemaining()) return -1
            return src.get().toInt() and 0xFF
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (!src.hasRemaining()) return -1

            val actualLen = minOf(len, src.remaining())
            src.get(b, off, actualLen)
            return actualLen
        }
    }
}


fun ByteBuffer.extract(offset: Int, size: Int, keepOldOffset: Boolean = true) = position()
    .let { oldOffset ->
        seek(offset).readBytes(size).apply {
            if (keepOldOffset) seek(oldOffset)
        }
    }