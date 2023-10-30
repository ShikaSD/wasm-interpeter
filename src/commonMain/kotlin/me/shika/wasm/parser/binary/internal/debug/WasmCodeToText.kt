package me.shika.wasm.parser.binary.internal.debug

import me.shika.wasm.def.WasmValueType
import me.shika.wasm.parser.binary.BinaryExpressionParser
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.Block
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.Branch
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.BranchIf
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.BranchTable
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.Call
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.CallIndirect
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.CallRef
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.Catch
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.CatchAll
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.Constf32
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.Constf64
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.Consti32
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.Consti64
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.Drop
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.Else
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.End
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.FuncRef
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.GlobalGet
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.GlobalSet
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.If
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.IsNull
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.LocalGet
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.LocalSet
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.LocalTee
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.Loop
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemGrow
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemLoadf32
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemLoadf64
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemLoadi32
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemLoadi32_16s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemLoadi32_16u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemLoadi32_8s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemLoadi32_8u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemLoadi64
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemLoadi64_16s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemLoadi64_16u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemLoadi64_32s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemLoadi64_32u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemLoadi64_8s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemLoadi64_8u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemSize
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemStoref32
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemStoref64
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemStorei32
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemStorei32_16
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemStorei32_8
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemStorei64
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemStorei64_16
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemStorei64_32
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.MemStorei64_8
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.ModuleOp
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.NoOp
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.RefAsNonNull
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.RefEq
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.RefNull
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.RefOp
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.Rethrow
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.Return
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.Select
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.SelectMany
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.TableGet
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.TableSet
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.Throw
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.Try
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.Unreachable
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_abs
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_add
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_ceil
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_convert_i32_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_convert_i32_u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_convert_i64_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_convert_i64_u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_copysign
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_demote_f64
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_div
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_eq
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_floor
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_ge
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_gt
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_le
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_lt
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_max
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_min
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_mul
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_ne
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_nearest
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_neg
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_reinterpret_i32
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_sqrt
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_sub
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f32_trunc
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_abs
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_add
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_ceil
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_convert_i32_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_convert_i32_u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_convert_i64_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_convert_i64_u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_copysign
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_div
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_eq
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_floor
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_ge
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_gt
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_le
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_lt
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_max
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_min
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_mul
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_ne
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_nearest
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_neg
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_promote_f32
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_reinterpret_i64
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_sqrt
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_sub
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.f64_trunc
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_add
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_and
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_clz
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_ctz
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_div_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_div_u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_eq
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_eqz
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_extend16_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_extend8_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_ge_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_ge_u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_gt_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_gt_u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_le_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_le_u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_lt_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_lt_u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_mul
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_ne
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_or
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_popcnt
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_reinterpret_f32
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_rem_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_rem_u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_rotl
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_rotr
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_shl
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_shr_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_shr_u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_sub
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_trunc_f32_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_trunc_f32_u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_trunc_f64_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_trunc_f64_u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_wrap_i64
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i32_xor
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_add
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_and
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_clz
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_ctz
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_div_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_div_u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_eq
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_eqz
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_extend16_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_extend32_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_extend8_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_extend_i32_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_extend_i32_u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_ge_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_ge_u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_gt_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_gt_u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_le_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_le_u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_lt_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_lt_u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_mul
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_ne
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_or
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_popcnt
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_reinterpret_f64
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_rem_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_rem_u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_rotl
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_rotr
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_shl
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_shr_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_shr_u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_sub
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_trunc_f32_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_trunc_f32_u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_trunc_f64_s
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_trunc_f64_u
import me.shika.wasm.parser.binary.BinaryExpressionParser.Companion.i64_xor

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
                    when (op) {
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
                    when (op) {
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
        // Unary
        i32_eqz -> "i32.eqz"
        i64_eqz -> "i64.eqz"
        i32_clz -> "i32.clz"
        i32_ctz -> "i32.ctz"
        i32_popcnt -> "i32.popcnt"
        i64_clz -> "i64.clz"
        i64_ctz -> "i64.ctz"
        i64_popcnt -> "i64.popcnt"
        f32_abs -> "f32.abs"
        f32_neg -> "f32.neg"
        f32_ceil -> "f32.ceil"
        f32_floor -> "f32.floor"
        f32_trunc -> "f32.trunc"
        f32_nearest -> "f32.nearest"
        f32_sqrt -> "f32.sqrt"
        f64_abs -> "f64.abs"
        f64_neg -> "f64.neg"
        f64_ceil -> "f64.ceil"
        f64_floor -> "f64.floor"
        f64_trunc -> "f64.trunc"
        f64_nearest -> "f64.nearest"
        f64_sqrt -> "f64.sqrt"
        i32_wrap_i64 -> "i32.wrap.i64"
        i32_trunc_f32_s -> "i32.trunc.f32.s"
        i32_trunc_f32_u -> "i32.trunc.f32.u"
        i32_trunc_f64_s -> "i32.trunc.f64.s"
        i32_trunc_f64_u -> "i32.trunc.f64.u"
        i64_extend_i32_s -> "i64.extend.i32.s"
        i64_extend_i32_u -> "i64.extend.i32.u"
        i64_trunc_f32_s -> "i64.trunc.f32.s"
        i64_trunc_f32_u -> "i64.trunc.f32.u"
        i64_trunc_f64_s -> "i64.trunc.f64.s"
        i64_trunc_f64_u -> "i64.trunc.f64.u"
        f32_convert_i32_s -> "f32.convert.i32.s"
        f32_convert_i32_u -> "f32.convert.i32.u"
        f32_convert_i64_s -> "f32.convert.i64.s"
        f32_convert_i64_u -> "f32.convert.i64.u"
        f32_demote_f64 -> "f32.demote.f64"
        f64_convert_i32_s -> "f64.convert.i32.s"
        f64_convert_i32_u -> "f64.convert.i32.u"
        f64_convert_i64_s -> "f64.convert.i64.s"
        f64_convert_i64_u -> "f64.convert.i64.u"
        f64_promote_f32 -> "f64.promote.f32"
        i32_reinterpret_f32 -> "i32.reinterpret.f32"
        i64_reinterpret_f64 -> "i64.reinterpret.f64"
        f32_reinterpret_i32 -> "f32.reinterpret.i32"
        f64_reinterpret_i64 -> "f64.reinterpret.i64"
        i32_extend8_s -> "i32.extend8.s"
        i32_extend16_s -> "i32.extend16.s"
        i64_extend8_s -> "i64.extend8.s"
        i64_extend16_s -> "i64.extend16.s"
        i64_extend32_s -> "i64.extend32.s"

        // Binary
        i32_eq -> "i32.eq"
        i32_ne -> "i32.ne"
        i32_lt_s -> "i32.lt.s"
        i32_lt_u -> "i32.lt.u"
        i32_gt_s -> "i32.gt.s"
        i32_gt_u -> "i32.gt.u"
        i32_le_s -> "i32.le.s"
        i32_le_u -> "i32.le.u"
        i32_ge_s -> "i32.ge.s"
        i32_ge_u -> "i32.ge.u"
        i64_eq -> "i64.eq"
        i64_ne -> "i64.ne"
        i64_lt_s -> "i64.lt.s"
        i64_lt_u -> "i64.lt.u"
        i64_gt_s -> "i64.gt.s"
        i64_gt_u -> "i64.gt.u"
        i64_le_s -> "i64.le.s"
        i64_le_u -> "i64.le.u"
        i64_ge_s -> "i64.ge.s"
        i64_ge_u -> "i64.ge.u"
        f32_eq -> "f32.eq"
        f32_ne -> "f32.ne"
        f32_lt -> "f32.lt"
        f32_gt -> "f32.gt"
        f32_le -> "f32.le"
        f32_ge -> "f32.ge"
        f64_eq -> "f64.eq"
        f64_ne -> "f64.ne"
        f64_lt -> "f64.lt"
        f64_gt -> "f64.gt"
        f64_le -> "f64.le"
        f64_ge -> "f64.ge"
        i32_add -> "i32.add"
        i32_sub -> "i32.sub"
        i32_mul -> "i32.mul"
        i32_div_s -> "i32.div.s"
        i32_div_u -> "i32.div.u"
        i32_rem_s -> "i32.rem.s"
        i32_rem_u -> "i32.rem.u"
        i32_and -> "i32.and"
        i32_or -> "i32.or"
        i32_xor -> "i32.xor"
        i32_shl -> "i32.shl"
        i32_shr_s -> "i32.shr.s"
        i32_shr_u -> "i32.shr.u"
        i32_rotl -> "i32.rotl"
        i32_rotr -> "i32.rotr"
        i64_add -> "i64.add"
        i64_sub -> "i64.sub"
        i64_mul -> "i64.mul"
        i64_div_s -> "i64.div.s"
        i64_div_u -> "i64.div.u"
        i64_rem_s -> "i64.rem.s"
        i64_rem_u -> "i64.rem.u"
        i64_and -> "i64.and"
        i64_or -> "i64.or"
        i64_xor -> "i64.xor"
        i64_shl -> "i64.shl"
        i64_shr_s -> "i64.shr.s"
        i64_shr_u -> "i64.shr.u"
        i64_rotl -> "i64.rotl"
        i64_rotr -> "i64.rotr"
        f32_add -> "f32.add"
        f32_sub -> "f32.sub"
        f32_mul -> "f32.mul"
        f32_div -> "f32.div"
        f32_min -> "f32.min"
        f32_max -> "f32.max"
        f32_copysign -> "f32.copysign"
        f64_add -> "f64.add"
        f64_sub -> "f64.sub"
        f64_mul -> "f64.mul"
        f64_div -> "f64.div"
        f64_min -> "f64.min"
        f64_max -> "f64.max"
        f64_copysign -> "f64.copysign"
        else -> {
            error("Unknown numeric op: ${this.toHexString()}")
        }
    }
