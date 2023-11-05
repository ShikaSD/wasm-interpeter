package me.shika.wasm.parser.binary

import me.shika.wasm.def.WasmArrayType
import me.shika.wasm.def.WasmCompositeType
import me.shika.wasm.def.WasmFieldType
import me.shika.wasm.def.WasmFuncType
import me.shika.wasm.def.WasmStructType
import me.shika.wasm.def.WasmType
import me.shika.wasm.def.WasmValueType
import me.shika.wasm.parser.binary.internal.ByteBuffer
import me.shika.wasm.parser.binary.internal.readByteAsInt
import me.shika.wasm.parser.binary.internal.readS32
import me.shika.wasm.parser.binary.internal.readU32

@OptIn(ExperimentalStdlibApi::class)
class BinaryTypeParser {
    fun ByteBuffer.parseType(): Array<WasmType> =
        when (val byte = readByteAsInt()) {
            0x4E -> Array(readU32()) { parseSubtype(readByteAsInt()) }
            else -> arrayOf(parseSubtype(byte))
        }

    private fun ByteBuffer.parseSubtype(byte: Int): WasmType =
        when (byte) {
            0x50 -> {
                val idx = IntArray(readU32()) { readU32() }
                val compType = parseCompType(readByteAsInt())
                WasmType(compType, idx, final = false)
            }
            0x4F -> {
                val idx = IntArray(readU32()) { readU32() }
                val compType = parseCompType(readByteAsInt())
                WasmType(compType, idx, final = true)
            }
            else -> WasmType(parseCompType(byte), EmptyIntArray, final = true)
        }

    private fun ByteBuffer.parseCompType(byte: Int): WasmCompositeType =
        when (byte) {
            0x5E -> WasmArrayType(parseFieldType())
            0x5F -> WasmStructType(Array(readU32()) { parseFieldType() })
            0x60 -> parseFuncType()
            else -> {
                error("Unknown comp type: ${byte.toHexString()}")
            }
        }

    private fun ByteBuffer.parseFuncType(): WasmFuncType {
        val paramTypes = parseResultType()
        val returnTypes = parseResultType()
        return WasmFuncType(paramTypes, returnTypes)
    }

    private fun ByteBuffer.parseResultType(): IntArray {
        val count = readU32()
        return IntArray(count) { parseValueType() }
    }

    fun ByteBuffer.parseFieldType(): WasmFieldType =
        WasmFieldType(
            parseValueType(),
            when (val byte = readByteAsInt()) {
                0x00 -> false
                0x01 -> true
                else -> error("Unexpected value for mut: ${byte.toHexString()}")
            }
        )

    fun ByteBuffer.parseValueType(): Int {
        val value = readS32()
        return if (value < 0) {
            when (value) {
                -0x01 -> WasmValueType.i32.toInt()
                -0x02 -> WasmValueType.i64.toInt()
                -0x03 -> WasmValueType.f32.toInt()
                -0x04 -> WasmValueType.f64.toInt()
                -0x05 -> WasmValueType.v128.toInt()
                -0x08 -> WasmValueType.i8.toInt()
                -0x09 -> WasmValueType.i16.toInt()
                -0x0e -> WasmValueType.NullExternRef.toInt()
                -0x0f -> WasmValueType.NullRef.toInt()
                -0x10 -> WasmValueType.FuncRef.toInt()
                -0x11 -> WasmValueType.ExternRef.toInt()
                -0x12 -> WasmValueType.AnyRef.toInt()
                -0x13 -> WasmValueType.EqRef.toInt()
                -0x14 -> WasmValueType.i31.toInt()
                -0x15 -> WasmValueType.Struct.toInt()
                -0x1c -> parseRefType() // todo: not null
                -0x1d -> parseRefType() // todo: nullable
                -0x40 -> WasmValueType.Empty.toInt()
                else -> {
                    error("Unknown type $value")
                }
            }
        } else {
            value
        }
    }

    fun ByteBuffer.parseRefType(): Int =
        parseValueType()
            .also {
                require(it >= 0 || it < WasmValueType.v128.toInt()) {
                    "Encountered $it in place of ref type"
                }
            }

    companion object {
        private val EmptyIntArray = IntArray(0)
    }
}
