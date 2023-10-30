package me.shika.wasm.parser.binary

import me.shika.wasm.def.WasmArrayType
import me.shika.wasm.def.WasmCompositeType
import me.shika.wasm.def.WasmExpr
import me.shika.wasm.def.WasmFieldType
import me.shika.wasm.def.WasmFuncType
import me.shika.wasm.def.WasmModuleDef
import me.shika.wasm.def.WasmStructType
import me.shika.wasm.def.WasmType
import me.shika.wasm.def.WasmValueType
import me.shika.wasm.parser.binary.internal.ByteBuffer
import me.shika.wasm.parser.binary.internal.debug.asWasmText
import me.shika.wasm.parser.binary.internal.readByteAsInt
import me.shika.wasm.parser.binary.internal.readS32
import me.shika.wasm.parser.binary.internal.readS64
import me.shika.wasm.parser.binary.internal.readU32

@OptIn(ExperimentalStdlibApi::class)
internal class BinaryExpressionParser(
    private val state: WasmModuleDef
) {
    private var code = IntArray(16)
    private var nextPosition = 0

    private var depth = 0
    private val elseDepth = ArrayDeque<Int>()

    fun ByteBuffer.parseExpression(const: Boolean, exprSize: Int): WasmExpr {
        val start = position
        while (true) {
            if (position - start >= exprSize) {
                error("Read out of expr bounds, instructions: ${code.asWasmText(exprSize)}")
            }
            // todo: validate const instructions
            when (val op = readByteAsInt()) {
                Unreachable -> push(Unreachable)
                NoOp -> { /* do nothing */ }
                Block, Loop, Try -> {
                    val type = parseBlockType()
                    push(op, type)
                    depth++
                }
                If -> {
                    val type = parseBlockType()
                    push(op, type)
                    depth++
                    elseDepth.addLast(depth)
                }
                Else -> {
                    while (elseDepth.last() > depth) {
                        elseDepth.removeLast()
                    }
                    require(elseDepth.removeLast() == depth) { "Encountered else without matching if" }
                    push(Else)
                }
                Branch -> {
                    val labelIdx = parseIdx()
                    push(Branch, labelIdx)
                }
                BranchIf -> {
                    val labelIdx = parseIdx()
                    push(BranchIf, labelIdx)
                }
                BranchTable -> {
                    push(BranchTable)

                    val labelCount = readU32()
                    push(labelCount)

                    repeat(labelCount) {
                        // Labels if true
                        push(readU32())
                    }

                    // label if false
                    push(readU32())
                }
                Return -> {
                    push(Return)
                }
                Call -> {
                    val funcIdx = parseIdx()
                    push(Call, funcIdx)
                }
                CallIndirect -> {
                    val typeIdx = parseIdx()
                    val tableIdx = parseIdx()
                    push(CallIndirect, typeIdx, tableIdx)
                }
                CallRef, ReturnCallRef -> {
                    val typeIdx = parseIdx()
                    push(op, typeIdx)
                }
                Catch -> {
                    val tagIdx = parseIdx()
                    push(Catch, tagIdx)
                }
                CatchAll -> {
                    push(CatchAll)
                }
                Throw, Rethrow -> {
                    val idx = parseIdx()
                    push(op, idx)
                }
                End -> {
                    while (elseDepth.isNotEmpty() && elseDepth.last() > depth) {
                        elseDepth.removeLast()
                    }

                    depth--
                    if (depth < 0) {
                        break
                    } else {
                        push(End)
                    }
                }
                RefNull -> {
                    val type = parseRefType()
                    push(RefNull, type)
                }
                IsNull -> {
                    push(IsNull)
                }
                FuncRef -> {
                    val idx = parseIdx()
                    push(FuncRef, idx)
                }
                Drop -> {
                    push(Drop)
                }
                Select -> {
                    push(Select)
                }
                SelectMany -> {
                    push(SelectMany)
                    val typeCount = readU32()
                    push(typeCount)
                    repeat(typeCount) {
                        val type = parseValueType()
                        push(type)
                    }
                }
                LocalGet,
                LocalSet,
                LocalTee,
                GlobalGet,
                GlobalSet,
                TableGet,
                TableSet -> {
                    push(op, parseIdx())
                }
                ModuleOp -> {
                    val moduleOp = readU32()
                    push((moduleOp shl 8) or (ModuleOp and 0xFF))
                    when (moduleOp) {
                        // integer truncate instructions
                        0, 1, 2, 3, 4, 5, 6, 7 -> {
                            // do nothing
                        }
                        // memory instructions
                        8, 9, 10, 11 -> {
                            val dataIdx = parseIdx()
                            push(dataIdx)

                            when (moduleOp) {
                                8, 10 -> {
                                    val idx = parseIdx() // advance counter, should always be 0x00
                                    check(idx == 0) {
                                        "Expected second index for module op $moduleOp to be 0, but it was $idx"
                                    }
                                }
                            }
                        }
                        // table instructions
                        12, 13, 14, 16, 17 -> {
                            val tableIdx = parseIdx()
                            push(tableIdx)

                            when (moduleOp) {
                                12, 14 -> {
                                    val idx = parseIdx()
                                    push(idx)
                                }
                            }
                        }
                        else -> {
                            error("Unknown module op: $moduleOp")
                        }
                    }
                }
                RefOp -> {
                    val refOp = readU32()
                    push((refOp shl 8) or (RefOp and 0xFF))
                    when (refOp) {
                        0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                        10, 11, 12, 13, 14, 16, 17, 18, 19 -> {
                            val dataIdx = parseIdx()
                            push(dataIdx)

                            when (refOp) {
                                2, 3, 4, 5, 8, 9, 10, 17, 18, 19 -> {
                                    val idx = parseIdx()
                                    push(idx)
                                }
                            }
                        }
                        20, 21, 22, 23 -> {
                            val type = parseRefType()
                            push(type)
                        }
                        15, 26, 27, 28, 29, 30 -> {
                            /* do nothing */
                        }
                        else -> {
                            error("Unknown ref op: $refOp")
                        }
                    }
                }
                MemLoadi64,
                MemLoadi32,
                MemLoadf64,
                MemLoadf32,
                MemLoadi32_8s,
                MemLoadi32_8u,
                MemLoadi32_16s,
                MemLoadi32_16u,
                MemLoadi64_8s,
                MemLoadi64_8u,
                MemLoadi64_16s,
                MemLoadi64_16u,
                MemLoadi64_32s,
                MemLoadi64_32u,
                MemStorei64,
                MemStorei32,
                MemStoref32,
                MemStoref64,
                MemStorei32_8,
                MemStorei32_16,
                MemStorei64_8,
                MemStorei64_16,
                MemStorei64_32 -> {
                    val align = readU32()
                    val offset = readU32()
                    push(op, align, offset)
                }
                MemSize, MemGrow -> {
                    val memIdx = readByteAsInt()
                    check(memIdx == 0) {
                        "Non-zero memory index for op: $op, idx: $memIdx"
                    }
                }
                Consti32 -> {
                    val value = readS32()
                    push(Consti32, value)
                }
                Consti64 -> {
                    val value = readS64()
                    push(Consti64, (value and 0xFFFFFFFF).toInt(), (value shr 32 and 0xFFFFFFFF).toInt())
                }
                Constf32 -> {
                    val value = readInt()
                    push(Constf32, value)
                }
                Constf64 -> {
                    val arg0 = readInt()
                    val arg1 = readInt()
                    push(Constf64, arg0, arg1)
                }
                RefEq, RefAsNonNull -> {
                    push(op)
                }
                else -> {
                    if (op in NumericStart..NumericEnd) {
                        push(op)
                    } else {
                        error("Unknown expr op byte: ${op.toHexString()}")
                    }
                }
            }
        }
        val expr = WasmExpr(code.copyOf(nextPosition))
        resetParser()
        return expr
    }

    fun ByteBuffer.parseFuncRefExpr(): WasmExpr {
        val idx = parseIdx()
        return WasmExpr(
            intArrayOf(
                FuncRef,
                idx
            )
        )
    }

    fun ByteBuffer.parseType(): Array<WasmType> =
        when (val byte = readByteAsInt()) {
            0x4E -> Array(readU32()) { parseSubtype(readByteAsInt()) }
            else -> arrayOf(parseSubtype(byte))
        }

    private fun ByteBuffer.parseSubtype(byte: Int): WasmType =
        when (byte) {
            0x50 -> {
                val idx = IntArray(readU32()) { parseIdx() }
                val compType = parseCompType(readByteAsInt())
                WasmType(compType, idx, final = false)
            }
            0x4F -> {
                val idx = IntArray(readU32()) { parseIdx() }
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

    private fun ByteBuffer.parseBlockType(): Int =
        parseValueType()

    private fun ByteBuffer.parseIdx(): Int =
        readU32()

    private fun push(value: Int) {
        ensureCapacity(1)
        code[nextPosition++] = value
    }

    private fun push(op: Int, arg: Int) {
        ensureCapacity(2)
        code[nextPosition++] = op
        code[nextPosition++] = arg
    }

    private fun push(op: Int, arg0: Int, arg1: Int) {
        ensureCapacity(3)
        code[nextPosition++] = op
        code[nextPosition++] = arg0
        code[nextPosition++] = arg1
    }


    private fun resetParser() {
        nextPosition = 0
        depth = 0
        elseDepth.clear()
    }

    private fun ensureCapacity(space: Int) {
        if (nextPosition + space > code.size - 1) {
            val newCode = IntArray(code.size * 2)
            code.copyInto(newCode)
            code = newCode
        }
    }

    @Suppress("ConstPropertyName")
    companion object {
        // https://webassembly.github.io/spec/core/binary/instructions.html#instructions

        // Control
        internal const val Unreachable = 0x00
        internal const val NoOp = 0x01
        internal const val Block = 0x02
        internal const val Loop = 0x03
        internal const val If = 0x04
        internal const val Else = 0x05
        internal const val Try = 0x06
        internal const val Catch = 0x07
        internal const val Throw = 0x08
        internal const val Rethrow = 0x09
        internal const val CatchAll = 0x19
        internal const val Branch = 0x0C
        internal const val BranchIf = 0x0D
        internal const val BranchTable = 0x0E
        internal const val Return = 0x0F
        internal const val Call = 0x10
        internal const val CallIndirect = 0x11
        internal const val CallRef = 0x14
        internal const val ReturnCallRef = 0x15
        internal const val End = 0x0B

        // Ref
        internal const val RefNull = 0xD0
        internal const val IsNull = 0xD1
        internal const val FuncRef = 0xD2
        internal const val RefEq = 0xD3
        internal const val RefAsNonNull = 0xD4

        // Parametric
        internal const val Drop = 0x1A
        internal const val Select = 0x1B
        internal const val SelectMany = 0x1C

        // Variable
        internal const val LocalGet = 0x20
        internal const val LocalSet = 0x21
        internal const val LocalTee = 0x22
        internal const val GlobalGet = 0x23
        internal const val GlobalSet = 0x24

        // Table
        internal const val TableGet = 0x25
        internal const val TableSet = 0x26

        // One op for different instructions
        internal const val RefOp = 0xFB
        internal const val ModuleOp = 0xFC

        // Memory
        internal const val MemLoadi32 = 0x28
        internal const val MemLoadi64 = 0x29
        internal const val MemLoadf32 = 0x2A
        internal const val MemLoadf64 = 0x2B
        internal const val MemLoadi32_8s = 0x2C
        internal const val MemLoadi32_8u = 0x2D
        internal const val MemLoadi32_16s = 0x2E
        internal const val MemLoadi32_16u = 0x2F
        internal const val MemLoadi64_8s = 0x30
        internal const val MemLoadi64_8u = 0x31
        internal const val MemLoadi64_16s = 0x32
        internal const val MemLoadi64_16u = 0x33
        internal const val MemLoadi64_32s = 0x34
        internal const val MemLoadi64_32u = 0x35
        internal const val MemStorei32 = 0x36
        internal const val MemStorei64 = 0x37
        internal const val MemStoref32 = 0x38
        internal const val MemStoref64 = 0x39
        internal const val MemStorei32_8 = 0x3A
        internal const val MemStorei32_16 = 0x3B
        internal const val MemStorei64_8 = 0x3C
        internal const val MemStorei64_16 = 0x3D
        internal const val MemStorei64_32 = 0x3E
        internal const val MemSize = 0x3F
        internal const val MemGrow = 0x40

        // Const
        internal const val Consti32 = 0x41
        internal const val Consti64 = 0x42
        internal const val Constf32 = 0x43
        internal const val Constf64 = 0x44

        // Int range
        internal const val NumericStart = 0x45
        internal const val NumericEnd = 0xC4

        internal const val i32_eqz = 0x45
        internal const val i64_eqz = 0x50
        internal const val i32_clz = 0x67
        internal const val i32_ctz = 0x68
        internal const val i32_popcnt = 0x69
        internal const val i64_clz = 0x79
        internal const val i64_ctz = 0x7A
        internal const val i64_popcnt = 0x7B
        internal const val f32_abs = 0x8B
        internal const val f32_neg = 0x8C
        internal const val f32_ceil = 0x8D
        internal const val f32_floor = 0x8E
        internal const val f32_trunc = 0x8F
        internal const val f32_nearest = 0x90
        internal const val f32_sqrt = 0x91
        internal const val f64_abs = 0x99
        internal const val f64_neg = 0x9A
        internal const val f64_ceil = 0x9B
        internal const val f64_floor = 0x9C
        internal const val f64_trunc = 0x9D
        internal const val f64_nearest = 0x9E
        internal const val f64_sqrt = 0x9F
        internal const val i32_wrap_i64 = 0xA7
        internal const val i32_trunc_f32_s = 0xA8
        internal const val i32_trunc_f32_u = 0xA9
        internal const val i32_trunc_f64_s = 0xAA
        internal const val i32_trunc_f64_u = 0xAB
        internal const val i64_extend_i32_s = 0xAC
        internal const val i64_extend_i32_u = 0xAD
        internal const val i64_trunc_f32_s = 0xAE
        internal const val i64_trunc_f32_u = 0xAF
        internal const val i64_trunc_f64_s = 0xB0
        internal const val i64_trunc_f64_u = 0xB1
        internal const val f32_convert_i32_s = 0xB2
        internal const val f32_convert_i32_u = 0xB3
        internal const val f32_convert_i64_s = 0xB4
        internal const val f32_convert_i64_u = 0xB5
        internal const val f32_demote_f64 = 0xB6
        internal const val f64_convert_i32_s = 0xB7
        internal const val f64_convert_i32_u = 0xB8
        internal const val f64_convert_i64_s = 0xB9
        internal const val f64_convert_i64_u = 0xBA
        internal const val f64_promote_f32 = 0xBB
        internal const val i32_reinterpret_f32 = 0xBC
        internal const val i64_reinterpret_f64 = 0xBD
        internal const val f32_reinterpret_i32 = 0xBE
        internal const val f64_reinterpret_i64 = 0xBF
        internal const val i32_extend8_s = 0xC0
        internal const val i32_extend16_s = 0xC1
        internal const val i64_extend8_s = 0xC2
        internal const val i64_extend16_s = 0xC3
        internal const val i64_extend32_s = 0xC4

        // Binary
        internal const val i32_eq = 0x46
        internal const val i32_ne = 0x47
        internal const val i32_lt_s = 0x48
        internal const val i32_lt_u = 0x49
        internal const val i32_gt_s = 0x4A
        internal const val i32_gt_u = 0x4B
        internal const val i32_le_s = 0x4C
        internal const val i32_le_u = 0x4D
        internal const val i32_ge_s = 0x4E
        internal const val i32_ge_u = 0x4F
        internal const val i64_eq = 0x51
        internal const val i64_ne = 0x52
        internal const val i64_lt_s = 0x53
        internal const val i64_lt_u = 0x54
        internal const val i64_gt_s = 0x55
        internal const val i64_gt_u = 0x56
        internal const val i64_le_s = 0x57
        internal const val i64_le_u = 0x58
        internal const val i64_ge_s = 0x59
        internal const val i64_ge_u = 0x5A
        internal const val f32_eq = 0x5B
        internal const val f32_ne = 0x5C
        internal const val f32_lt = 0x5D
        internal const val f32_gt = 0x5E
        internal const val f32_le = 0x5F
        internal const val f32_ge = 0x60
        internal const val f64_eq = 0x61
        internal const val f64_ne = 0x62
        internal const val f64_lt = 0x63
        internal const val f64_gt = 0x64
        internal const val f64_le = 0x65
        internal const val f64_ge = 0x66
        internal const val i32_add = 0x6A
        internal const val i32_sub = 0x6B
        internal const val i32_mul = 0x6C
        internal const val i32_div_s = 0x6D
        internal const val i32_div_u = 0x6E
        internal const val i32_rem_s = 0x6F
        internal const val i32_rem_u = 0x70
        internal const val i32_and = 0x71
        internal const val i32_or = 0x72
        internal const val i32_xor = 0x73
        internal const val i32_shl = 0x74
        internal const val i32_shr_s = 0x75
        internal const val i32_shr_u = 0x76
        internal const val i32_rotl = 0x77
        internal const val i32_rotr = 0x78
        internal const val i64_add = 0x7C
        internal const val i64_sub = 0x7D
        internal const val i64_mul = 0x7E
        internal const val i64_div_s = 0x7F
        internal const val i64_div_u = 0x80
        internal const val i64_rem_s = 0x81
        internal const val i64_rem_u = 0x82
        internal const val i64_and = 0x83
        internal const val i64_or = 0x84
        internal const val i64_xor = 0x85
        internal const val i64_shl = 0x86
        internal const val i64_shr_s = 0x87
        internal const val i64_shr_u = 0x88
        internal const val i64_rotl = 0x89
        internal const val i64_rotr = 0x8A
        internal const val f32_add = 0x92
        internal const val f32_sub = 0x93
        internal const val f32_mul = 0x94
        internal const val f32_div = 0x95
        internal const val f32_min = 0x96
        internal const val f32_max = 0x97
        internal const val f32_copysign = 0x98
        internal const val f64_add = 0xA0
        internal const val f64_sub = 0xA1
        internal const val f64_mul = 0xA2
        internal const val f64_div = 0xA3
        internal const val f64_min = 0xA4
        internal const val f64_max = 0xA5
        internal const val f64_copysign = 0xA6

        private val EmptyIntArray = IntArray(0)
    }
}
