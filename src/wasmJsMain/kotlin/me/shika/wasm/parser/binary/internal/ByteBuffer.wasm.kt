package me.shika.wasm.parser.binary.internal

actual class ByteBuffer(private val buffer: ByteArray) {
    actual val size: Int = buffer.size

    private var current: Int = 0
    actual val position: Int get() = current

    actual fun offset(bytes: Int) {
        require(current + bytes < size) {
            "Skipping out of bounds from: $current, count: $bytes, size: $size"
        }
        current += bytes
    }

    actual fun readByte(): Byte =
        buffer[current++]

    actual fun readInt(): Int =
        buffer[current++].toInt() and 0xFF or
            buffer[current++].toInt() and 0xFF shl 8 or
            buffer[current++].toInt() and 0xFF shl 16 or
            buffer[current++].toInt() and 0xFF shl 24

    actual fun readString(length: Int): String =
        TODO()

    actual companion object {
        actual fun wrap(bytes: ByteArray): ByteBuffer =
            ByteBuffer(bytes)
    }
}
