package me.shika.wasm

import me.shika.wasm.parser.binary.internal.ByteBuffer
import me.shika.wasm.parser.binary.internal.readU32

fun main() {
    val bytes = byteArrayOf(0x40)
    val buffer = ByteBuffer.wrap(bytes)
    println("Decoded: ${buffer.readU32()}")
}
