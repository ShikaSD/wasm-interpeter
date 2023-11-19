package me.shika.wasm.intrinsics

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.ShortBuffer

actual fun createString(bytes: ByteArray, offset: Int, length: Int): String =
    String(bytes, offset, length * 2, Charsets.UTF_16LE)
