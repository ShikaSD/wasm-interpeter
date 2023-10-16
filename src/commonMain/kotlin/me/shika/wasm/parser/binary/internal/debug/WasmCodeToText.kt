package me.shika.wasm.parser.binary.internal.debug

import me.shika.wasm.WasmValueType
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.Block
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.Branch
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.BranchIf
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.BranchTable
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.Call
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.CallIndirect
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.CallRef
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.Catch
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.CatchAll
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.Constf32
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.Constf64
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.Consti32
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.Consti64
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.Drop
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.Else
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.End
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.FuncRef
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.GlobalGet
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.GlobalSet
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.If
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.IsNull
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.LocalGet
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.LocalSet
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.LocalTee
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.Loop
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemGrow
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemLoadf32
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemLoadf64
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemLoadi32
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemLoadi32_16s
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemLoadi32_16u
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemLoadi32_8s
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemLoadi32_8u
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemLoadi64
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemLoadi64_16s
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemLoadi64_16u
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemLoadi64_32s
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemLoadi64_32u
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemLoadi64_8s
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemLoadi64_8u
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemSize
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemStoref32
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemStoref64
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemStorei32
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemStorei32_16
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemStorei32_8
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemStorei64
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemStorei64_16
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemStorei64_32
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.MemStorei64_8
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.ModuleOp
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.NoOp
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.RefAsNonNull
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.RefEq
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.RefNull
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.RefOp
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.Rethrow
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.Return
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.Select
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.SelectMany
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.TableGet
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.TableSet
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.Throw
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.Try
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser.Companion.Unreachable

@OptIn(ExperimentalStdlibApi::class)
fun IntArray.asWasmText(length: Int = this.size): String {
    val instructions = this
    var indent = 0

    fun StringBuilder.appendIndentedLine(str: String) {
        repeat(indent * 2) {
            append(' ')
        }
        appendLine(str)
    }

    fun StringBuilder.appendIndented(str: String) {
        repeat(indent * 2) {
            append(' ')
        }
        append(str)
    }

    return buildString {
        var position = 0
        while (position < length) {
            val instruction = instructions[position++]
            when (val op = (instruction and 0xFF)) {
                Unreachable -> appendIndentedLine("unreachable")
                NoOp -> appendIndentedLine("noop")
                Block, Loop, Try, If -> {
                    val type = instructions[position++]
                    when (op) {
                        Block -> appendIndented("block")
                        Loop -> appendIndented("loop")
                        Try -> appendIndented("try")
                        If -> appendIndented("if")
                    }
                    append(" ")
                    append(type.toTypeString())
                    appendLine()

                    indent++
                }

                Else -> {
                    indent--
                    appendIndentedLine("else")
                    indent++
                }

                Branch -> {
                    appendIndented("br ")
                    append(instructions[position++])
                    appendLine()
                }

                BranchIf -> {
                    appendIndented("br_if ")
                    append(instructions[position++])
                    appendLine()
                }

                BranchTable -> {
                    appendIndented("br_table ")

                    append("[")
                    val labelCount = instructions[position++]
                    repeat(labelCount) {
                        // Labels if true
                        append(instructions[position++])

                        if (it < labelCount - 1) append(", ")
                    }
                    append("]")

                    // label if false
                    append(" ")
                    append(instructions[position++])
                    appendLine()
                }

                Return -> {
                    appendIndentedLine("return")
                }

                Call -> {
                    appendIndented("call ")
                    append(instructions[position++])
                    appendLine()
                }

                CallIndirect -> {
                    appendIndented("call_indirect ")
                    append(instructions[position++])
                    append(", ")
                    append(instructions[position++])
                    appendLine()
                }

                CallRef -> {
                    appendIndented("call_ref ")
                    append(instructions[position++])
                    appendLine()
                }

                Catch -> {
                    indent--
                    appendIndented("catch ")
                    append(instructions[position++])
                    appendLine()
                    indent++
                }

                CatchAll -> {
                    indent--
                    appendIndentedLine("catch_all")
                    indent++
                }

                Throw, Rethrow -> {
                    when (op) {
                        Throw -> appendIndented("throw ")
                        Rethrow -> appendIndented("throw ")
                    }
                    append(instructions[position++])
                    appendLine()
                }

                End -> {
                    indent--
                    appendIndentedLine("end")
                }

                RefNull -> {
                    appendIndented("ref_null ")
                    append(instructions[position++])
                    appendLine()
                }

                IsNull -> {
                    appendIndentedLine("is_null")
                }

                FuncRef -> {
                    appendIndented("func_ref ")
                    append(instructions[position++])
                    appendLine()
                }

                Drop -> {
                    appendIndentedLine("drop")
                }

                Select -> {
                    appendIndentedLine("select")
                }

                SelectMany -> {
                    TODO()
                }

                LocalGet,
                LocalSet,
                LocalTee,
                GlobalGet,
                GlobalSet,
                TableGet,
                TableSet -> {
                    when (op) {
                        LocalGet -> appendIndented("local_get ")
                        LocalSet -> appendIndented("local_set ")
                        LocalTee -> appendIndented("local_tee ")
                        GlobalGet -> appendIndented("global_get ")
                        GlobalSet -> appendIndented("global_set ")
                        TableGet -> appendIndented("table_get ")
                        TableSet -> appendIndented("table_set ")
                    }
                    append(instructions[position++])
                    appendLine()
                }

                ModuleOp -> {
                    when (val moduleOp = instruction shr 8) {
                        // integer truncate instructions
                        0, 1, 2, 3, 4, 5, 6, 7 -> {
                            when (moduleOp) {
                                0 -> appendIndented("i32.trunc_sat_f32_s")
                                1 -> appendIndented("i32.trunc_sat_f32_u")
                                2 -> appendIndented("i32.trunc_sat_f64_s")
                                3 -> appendIndented("i32.trunc_sat_f64_u")
                                4 -> appendIndented("i64.trunc_sat_f32_s")
                                5 -> appendIndented("i64.trunc_sat_f32_u")
                                6 -> appendIndented("i64.trunc_sat_f64_s")
                                7 -> appendIndented("i64.trunc_sat_f64_u")
                            }
                        }
                        // memory instructions
                        8, 9, 10, 11 -> {
                            when (moduleOp) {
                                8 -> appendIndented("module.init")
                                9 -> appendIndented("data.drop")
                                10 -> appendIndented("memory.copy")
                                11 -> appendIndented("memory.fill")
                            }
                            append(" ")
                            append(instructions[position++])
                        }
                        // table instructions
                        12, 13, 14, 16, 17 -> {
                            when (moduleOp) {
                                12 -> appendIndented("table.init")
                                13 -> appendIndented("elem.drop")
                                14 -> appendIndented("table.copy")
                                15 -> appendIndented("table.grow")
                                16 -> appendIndented("table.size")
                                17 -> appendIndented("table.fill")
                            }
                            append(" ")
                            append(instructions[position++])

                            when (moduleOp) {
                                12, 14 -> {
                                    append(" ")
                                    append(instructions[position++])
                                }
                            }
                        }

                        else -> {
                            error("Unknown module op: $moduleOp")
                        }
                    }

                    appendLine()
                }

                RefOp -> {
                    when (val refOp = instruction shr 8) {
                        0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                        10, 11, 12, 13, 14, 16, 17, 18, 19 -> {
                            when (refOp) {
                                0 -> appendIndented("struct.new")
                                1 -> appendIndented("struct.new_default")
                                2 -> appendIndented("struct.get")
                                3 -> appendIndented("struct.get_s")
                                4 -> appendIndented("struct.get_u")
                                5 -> appendIndented("struct.set")
                                6 -> appendIndented("array.new")
                                7 -> appendIndented("array.new_default")
                                8 -> appendIndented("array.new_fixed")
                                9 -> appendIndented("array.new_data")
                                10 -> appendIndented("array.new_elem")
                                11 -> appendIndented("array.get")
                                12 -> appendIndented("array.get_s")
                                13 -> appendIndented("array.get_u")
                                14 -> appendIndented("array.set")
                                16 -> appendIndented("array.fill")
                                17 -> appendIndented("array.copy")
                                18 -> appendIndented("array.init_data")
                                19 -> appendIndented("array.init_elem")
                            }

                            append(" ")
                            append(instructions[position++])

                            when (refOp) {
                                2, 3, 4, 5, 8, 9, 10, 17, 18, 19 -> {
                                    append(" ")
                                    append(instructions[position++])
                                }
                            }
                        }

                        20, 21, 22, 23 -> {
                            when (refOp) {
                                20 -> appendIndented("ref.test")
                                21 -> appendIndented("ref.test (not-null)")
                                22 -> appendIndented("ref.cast")
                                23 -> appendIndented("ref.cast (not-null)")
                            }
                            append(" ")
                            append(instructions[position++])
                        }

                        15, 26, 27, 28, 29, 30 -> {
                            when (refOp) {
                                15 -> appendIndented("array.len")
                                26 -> appendIndented("extern.initialize")
                                27 -> appendIndented("extern.externalize")
                                28 -> appendIndented("i31.new")
                                29 -> appendIndented("i31.get_s")
                                30 -> appendIndented("i31.get_u")
                            }
                        }

                        else -> {
                            error("Unknown ref op: $refOp")
                        }
                    }
                    appendLine()
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
                    when (op) {
                        MemLoadi32 -> appendIndented("i32.load")
                        MemLoadi64 -> appendIndented("i64.load")
                        MemLoadf64 -> appendIndented("f64.load")
                        MemLoadf32 -> appendIndented("f32.load")
                        MemLoadi32_8s -> appendIndented("i32.load8_s")
                        MemLoadi32_8u -> appendIndented("i32.load8_u")
                        MemLoadi32_16s -> appendIndented("i32.load16_s")
                        MemLoadi32_16u -> appendIndented("i32.load16_u")
                        MemLoadi64_8s -> appendIndented("i64.load8_s")
                        MemLoadi64_8u -> appendIndented("i64.load8_u")
                        MemLoadi64_16s -> appendIndented("i64.load16_s")
                        MemLoadi64_16u -> appendIndented("i64.load16_u")
                        MemLoadi64_32s -> appendIndented("i64.load32_s")
                        MemLoadi64_32u -> appendIndented("i64.load32_u")
                        MemStorei64 -> appendIndented("i64.store")
                        MemStorei32 -> appendIndented("i32.store")
                        MemStoref32 -> appendIndented("f32.store")
                        MemStoref64 -> appendIndented("f64.store")
                        MemStorei32_8 -> appendIndented("i32.store8")
                        MemStorei32_16 -> appendIndented("i32.store16")
                        MemStorei64_8 -> appendIndented("i64.store8")
                        MemStorei64_16 -> appendIndented("i32.store16")
                        MemStorei64_32 -> appendIndented("i32.store32")
                    }

                    append(" ")
                    append(instructions[position++])
                    append(" ")
                    append(instructions[position++])
                    appendLine()
                }

                MemSize, MemGrow -> {
                    when(op) {
                        MemSize -> appendIndentedLine("mem.size")
                        MemGrow -> appendIndentedLine("mem.grow")
                    }
                }

                Consti32 -> {
                    appendIndented("const.i32 ")
                    append(instructions[position++])
                    appendLine()
                }

                Consti64 -> {
                    appendIndented("const.i64 ")
                    val bits = instructions[position++].toLong() or (instructions[position++].toLong() shl 32)
                    append(bits)
                    appendLine()
                }

                Constf32 -> {
                    appendIndented("const.f32 ")
                    append(Float.fromBits(instructions[position++]))
                    appendLine()
                }

                Constf64 -> {
                    appendIndented("const.f64 ")
                    val bits = instructions[position++].toLong() or (instructions[position++].toLong() shl 32)
                    append(Double.fromBits(bits))
                    appendLine()
                }

                RefEq, RefAsNonNull -> {
                    when(op) {
                        RefEq -> appendIndentedLine("ref.eq")
                        RefAsNonNull -> appendIndentedLine("ref.as_non_null")
                    }
                }

                else -> {
                    if (op in BinaryExpressionParser.NumericStart..BinaryExpressionParser.NumericEnd) {
                        appendIndentedLine(op.asNumOp())
                    } else {
                        error("Unknown expr op byte: ${op.toHexString()}")
                    }
                }
            }
        }
    }
}

fun Int.toTypeString(): String =
    WasmValueType.entries[this].toString()

@OptIn(ExperimentalStdlibApi::class)
fun Int.asNumOp(): String =
    when (this) {
        // todo use proper names instead of hardcoded values
        // Unary
        0x45 -> "i32.eqz"
        0x50 -> "i64.eqz"
        0x67 -> "i32.clz"
        0x68 -> "i32.ctz"
        0x69 -> "i32.popcnt"
        0x79 -> "i64.clz"
        0x7A -> "i64.ctz"
        0x7B -> "i64.popcnt"
        0x8B -> "f32.abs"
        0x8C -> "f32.neg"
        0x8D -> "f32.ceil"
        0x8E -> "f32.floor"
        0x8F -> "f32.trunc"
        0x90 -> "f32.nearest"
        0x91 -> "f32.sqrt"
        0x99 -> "f64.abs"
        0x9A -> "f64.neg"
        0x9B -> "f64.ceil"
        0x9C -> "f64.floor"
        0x9D -> "f64.trunc"
        0x9E -> "f64.nearest"
        0x9F -> "f64.sqrt"
        0xA7 -> "i32.wrap.i64"
        0xA8 -> "i32.trunc.f32.s"
        0xA9 -> "i32.trunc.f32.u"
        0xAA -> "i32.trunc.f64.s"
        0xAB -> "i32.trunc.f64.u"
        0xAC -> "i64.extend.i32.s"
        0xAD -> "i64.extend.i32.u"
        0xAE -> "i64.trunc.f32.s"
        0xAF -> "i64.trunc.f32.u"
        0xB0 -> "i64.trunc.f64.s"
        0xB1 -> "i64.trunc.f64.u"
        0xB2 -> "f32.convert.i32.s"
        0xB3 -> "f32.convert.i32.u"
        0xB4 -> "f32.convert.i64.s"
        0xB5 -> "f32.convert.i64.u"
        0xB6 -> "f32.demote.f64"
        0xB7 -> "f64.convert.i32.s"
        0xB8 -> "f64.convert.i32.u"
        0xB9 -> "f64.convert.i64.s"
        0xBA -> "f64.convert.i64.u"
        0xBB -> "f64.promote.f32"
        0xBC -> "i32.reinterpret.f32"
        0xBD -> "i64.reinterpret.f64"
        0xBE -> "f32.reinterpret.i32"
        0xBF -> "f64.reinterpret.i64"
        0xC0 -> "i32.extend8.s"
        0xC1 -> "i32.extend16.s"
        0xC2 -> "i64.extend8.s"
        0xC3 -> "i64.extend16.s"
        0xC4 -> "i64.extend32.s"

        // Binary
        0x46 -> "i32.eq"
        0x47 -> "i32.ne"
        0x48 -> "i32.lt.s"
        0x49 -> "i32.lt.u"
        0x4A -> "i32.gt.s"
        0x4B -> "i32.gt.u"
        0x4C -> "i32.le.s"
        0x4D -> "i32.le.u"
        0x4E -> "i32.ge.s"
        0x4F -> "i32.ge.u"
        0x51 -> "i64.eq"
        0x52 -> "i64.ne"
        0x53 -> "i64.lt.s"
        0x54 -> "i64.lt.u"
        0x55 -> "i64.gt.s"
        0x56 -> "i64.gt.u"
        0x57 -> "i64.le.s"
        0x58 -> "i64.le.u"
        0x59 -> "i64.ge.s"
        0x5A -> "i64.ge.u"
        0x5B -> "f32.eq"
        0x5C -> "f32.ne"
        0x5D -> "f32.lt"
        0x5E -> "f32.gt"
        0x5F -> "f32.le"
        0x60 -> "f32.ge"
        0x61 -> "f64.eq"
        0x62 -> "f64.ne"
        0x63 -> "f64.lt"
        0x64 -> "f64.gt"
        0x65 -> "f64.le"
        0x66 -> "f64.ge"
        0x6A -> "i32.add"
        0x6B -> "i32.sub"
        0x6C -> "i32.mul"
        0x6D -> "i32.div.s"
        0x6E -> "i32.div.u"
        0x6F -> "i32.rem.s"
        0x70 -> "i32.rem.u"
        0x71 -> "i32.and"
        0x72 -> "i32.or"
        0x73 -> "i32.xor"
        0x74 -> "i32.shl"
        0x75 -> "i32.shr.s"
        0x76 -> "i32.shr.u"
        0x77 -> "i32.rotl"
        0x78 -> "i32.rotr"
        0x7C -> "i64.add"
        0x7D -> "i64.sub"
        0x7E -> "i64.mul"
        0x7F -> "i64.div.s"
        0x80 -> "i64.div.u"
        0x81 -> "i64.rem.s"
        0x82 -> "i64.rem.u"
        0x83 -> "i64.and"
        0x84 -> "i64.or"
        0x85 -> "i64.xor"
        0x86 -> "i64.shl"
        0x87 -> "i64.shr.s"
        0x88 -> "i64.shr.u"
        0x89 -> "i64.rotl"
        0x8A -> "i64.rotr"
        0x92 -> "f32.add"
        0x93 -> "f32.sub"
        0x94 -> "f32.mul"
        0x95 -> "f32.div"
        0x96 -> "f32.min"
        0x97 -> "f32.max"
        0x98 -> "f32.copysign"
        0xA0 -> "f64.add"
        0xA1 -> "f64.sub"
        0xA2 -> "f64.mul"
        0xA3 -> "f64.div"
        0xA4 -> "f64.min"
        0xA5 -> "f64.max"
        0xA6 -> "f64.copysign"
        else -> {
            error("Unknown numeric op: ${this.toHexString()}")
        }
    }
