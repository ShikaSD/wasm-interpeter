package me.shika.wasm.parser.binary.internal

expect class ByteBuffer {
    val size: Int
    val position: Int

    fun offset(bytes: Int)

    fun readByte(): Byte
    fun readInt(): Int
    fun readString(length: Int): String

    companion object {
        fun wrap(bytes: ByteArray): ByteBuffer
    }
}

// todo: double-check LEB encodings
/**
 * Reads a LEB128-encoded signed integer from [ByteBuffer].
 */
fun ByteBuffer.readS32(): Int {
    var result = 0
    var cur: Int
    var count = 0
    var signBits = -1
    do {
        cur = readByte().toInt() and 0xff
        result = result or (cur and 0x7f shl count * 7)
        signBits = signBits shl 7
        count++
    } while (cur and 0x80 == 0x80 && count < 6)

    if (cur and 0x80 == 0x80) {
        error("invalid LEB128 sequence")
    }

    // Sign extend if appropriate
    if (signBits shr 1 and result != 0) {
        result = result or signBits
    }
    return result
}

/**
 * Reads a LEB128-encoded unsigned integer from [ByteBuffer].
 */
fun ByteBuffer.readU32(): Int {
    var result = 0
    var cur: Int
    var count = 0
    do {
        cur = readByte().toInt() and 0xff
        result = result or (cur and 0x7f shl count * 7)
        count++
    } while (cur and 0x80 == 0x80 && count < 6)
    if (cur and 0x80 == 0x80) {
        error("invalid LEB128 sequence")
    }
    return result
}

fun ByteBuffer.readS64(): Long {
    var result = 0L
    var cur: Long
    var count = 0
    var signBits = -1L
    do {
        cur = readByte().toLong() and 0xffL
        result = result or (cur and 0x7fL shl count * 7)
        signBits = signBits shl 7
        count++
    } while (cur and 0x80L == 0x80L && count < 11)

    if (cur and 0x80L == 0x80L) {
        error("invalid LEB128 sequence")
    }

    // Sign extend if appropriate
    if (signBits shr 1 and result != 0L) {
        result = result or signBits
    }
    return result
}

fun ByteBuffer.readByteAsInt(): Int =
    readByte().toUByte().toInt()
