package me.shika.wasm.parser.binary.internal

import java.nio.ByteOrder

actual class ByteBuffer(private val buffer: java.nio.ByteBuffer) {
    init {
        buffer.order(ByteOrder.LITTLE_ENDIAN)
    }

    private var stringBytes = ByteArray(0)

    actual val size: Int get() = buffer.limit()
    actual val position: Int get() = buffer.position()

    actual fun offset(bytes: Int) {
        buffer.position(buffer.position() + bytes)
    }

    actual fun readByte(): Byte = buffer.get()
    actual fun readInt(): Int = buffer.getInt()
    actual fun readString(length: Int): String {
        if (stringBytes.size < length) {
            stringBytes = ByteArray(length)
        }
        buffer.get(stringBytes, 0, length)
        return String(stringBytes, 0, length)
    }

    actual companion object {
        actual fun wrap(bytes: ByteArray): ByteBuffer =
            ByteBuffer(java.nio.ByteBuffer.wrap(bytes))
    }
}
