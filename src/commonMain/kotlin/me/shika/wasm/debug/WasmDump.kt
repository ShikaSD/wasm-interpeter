package me.shika.wasm.debug

import me.shika.wasm.def.WasmOpcodes.AnyConvert
import me.shika.wasm.def.WasmOpcodes.ArrayGet
import me.shika.wasm.def.WasmOpcodes.ArrayLen
import me.shika.wasm.def.WasmOpcodes.ArrayNewData
import me.shika.wasm.def.WasmOpcodes.ArrayNewDefault
import me.shika.wasm.def.WasmOpcodes.ArraySet
import me.shika.wasm.def.WasmOpcodes.Block
import me.shika.wasm.def.WasmOpcodes.Branch
import me.shika.wasm.def.WasmOpcodes.BranchIf
import me.shika.wasm.def.WasmOpcodes.BranchTable
import me.shika.wasm.def.WasmOpcodes.Call
import me.shika.wasm.def.WasmOpcodes.CallIndirect
import me.shika.wasm.def.WasmOpcodes.CallRef
import me.shika.wasm.def.WasmOpcodes.Catch
import me.shika.wasm.def.WasmOpcodes.Drop
import me.shika.wasm.def.WasmOpcodes.End
import me.shika.wasm.def.WasmOpcodes.ExternConvert
import me.shika.wasm.def.WasmOpcodes.GlobalGet
import me.shika.wasm.def.WasmOpcodes.GlobalSet
import me.shika.wasm.def.WasmOpcodes.If
import me.shika.wasm.def.WasmOpcodes.IsNull
import me.shika.wasm.def.WasmOpcodes.Jump
import me.shika.wasm.def.WasmOpcodes.LocalGet
import me.shika.wasm.def.WasmOpcodes.LocalSet
import me.shika.wasm.def.WasmOpcodes.LocalTee
import me.shika.wasm.def.WasmOpcodes.Loop
import me.shika.wasm.def.WasmOpcodes.MemGrow
import me.shika.wasm.def.WasmOpcodes.MemLoadf32
import me.shika.wasm.def.WasmOpcodes.MemLoadf64
import me.shika.wasm.def.WasmOpcodes.MemLoadi32
import me.shika.wasm.def.WasmOpcodes.MemLoadi32_16s
import me.shika.wasm.def.WasmOpcodes.MemLoadi32_16u
import me.shika.wasm.def.WasmOpcodes.MemLoadi32_8s
import me.shika.wasm.def.WasmOpcodes.MemLoadi32_8u
import me.shika.wasm.def.WasmOpcodes.MemLoadi64
import me.shika.wasm.def.WasmOpcodes.MemLoadi64_16s
import me.shika.wasm.def.WasmOpcodes.MemLoadi64_16u
import me.shika.wasm.def.WasmOpcodes.MemLoadi64_32s
import me.shika.wasm.def.WasmOpcodes.MemLoadi64_32u
import me.shika.wasm.def.WasmOpcodes.MemLoadi64_8s
import me.shika.wasm.def.WasmOpcodes.MemLoadi64_8u
import me.shika.wasm.def.WasmOpcodes.MemSize
import me.shika.wasm.def.WasmOpcodes.MemStoref32
import me.shika.wasm.def.WasmOpcodes.MemStoref64
import me.shika.wasm.def.WasmOpcodes.MemStorei32
import me.shika.wasm.def.WasmOpcodes.MemStorei32_16
import me.shika.wasm.def.WasmOpcodes.MemStorei32_8
import me.shika.wasm.def.WasmOpcodes.MemStorei64
import me.shika.wasm.def.WasmOpcodes.MemStorei64_16
import me.shika.wasm.def.WasmOpcodes.MemStorei64_32
import me.shika.wasm.def.WasmOpcodes.MemStorei64_8
import me.shika.wasm.def.WasmOpcodes.NoOp
import me.shika.wasm.def.WasmOpcodes.RefAsNonNull
import me.shika.wasm.def.WasmOpcodes.RefCast
import me.shika.wasm.def.WasmOpcodes.RefEq
import me.shika.wasm.def.WasmOpcodes.RefFunc
import me.shika.wasm.def.WasmOpcodes.RefNull
import me.shika.wasm.def.WasmOpcodes.Rethrow
import me.shika.wasm.def.WasmOpcodes.Return
import me.shika.wasm.def.WasmOpcodes.Select
import me.shika.wasm.def.WasmOpcodes.SelectMany
import me.shika.wasm.def.WasmOpcodes.StructGet
import me.shika.wasm.def.WasmOpcodes.StructNew
import me.shika.wasm.def.WasmOpcodes.StructSet
import me.shika.wasm.def.WasmOpcodes.TableGet
import me.shika.wasm.def.WasmOpcodes.TableSet
import me.shika.wasm.def.WasmOpcodes.Throw
import me.shika.wasm.def.WasmOpcodes.Try
import me.shika.wasm.def.WasmOpcodes.Unreachable
import me.shika.wasm.def.WasmOpcodes.f32_abs
import me.shika.wasm.def.WasmOpcodes.f32_add
import me.shika.wasm.def.WasmOpcodes.f32_ceil
import me.shika.wasm.def.WasmOpcodes.f32_const
import me.shika.wasm.def.WasmOpcodes.f32_convert_i32_s
import me.shika.wasm.def.WasmOpcodes.f32_convert_i32_u
import me.shika.wasm.def.WasmOpcodes.f32_convert_i64_s
import me.shika.wasm.def.WasmOpcodes.f32_convert_i64_u
import me.shika.wasm.def.WasmOpcodes.f32_copysign
import me.shika.wasm.def.WasmOpcodes.f32_demote_f64
import me.shika.wasm.def.WasmOpcodes.f32_div
import me.shika.wasm.def.WasmOpcodes.f32_eq
import me.shika.wasm.def.WasmOpcodes.f32_floor
import me.shika.wasm.def.WasmOpcodes.f32_ge
import me.shika.wasm.def.WasmOpcodes.f32_gt
import me.shika.wasm.def.WasmOpcodes.f32_le
import me.shika.wasm.def.WasmOpcodes.f32_lt
import me.shika.wasm.def.WasmOpcodes.f32_max
import me.shika.wasm.def.WasmOpcodes.f32_min
import me.shika.wasm.def.WasmOpcodes.f32_mul
import me.shika.wasm.def.WasmOpcodes.f32_ne
import me.shika.wasm.def.WasmOpcodes.f32_nearest
import me.shika.wasm.def.WasmOpcodes.f32_neg
import me.shika.wasm.def.WasmOpcodes.f32_reinterpret_i32
import me.shika.wasm.def.WasmOpcodes.f32_sqrt
import me.shika.wasm.def.WasmOpcodes.f32_sub
import me.shika.wasm.def.WasmOpcodes.f32_trunc
import me.shika.wasm.def.WasmOpcodes.f64_abs
import me.shika.wasm.def.WasmOpcodes.f64_add
import me.shika.wasm.def.WasmOpcodes.f64_ceil
import me.shika.wasm.def.WasmOpcodes.f64_const
import me.shika.wasm.def.WasmOpcodes.f64_convert_i32_s
import me.shika.wasm.def.WasmOpcodes.f64_convert_i32_u
import me.shika.wasm.def.WasmOpcodes.f64_convert_i64_s
import me.shika.wasm.def.WasmOpcodes.f64_convert_i64_u
import me.shika.wasm.def.WasmOpcodes.f64_copysign
import me.shika.wasm.def.WasmOpcodes.f64_div
import me.shika.wasm.def.WasmOpcodes.f64_eq
import me.shika.wasm.def.WasmOpcodes.f64_floor
import me.shika.wasm.def.WasmOpcodes.f64_ge
import me.shika.wasm.def.WasmOpcodes.f64_gt
import me.shika.wasm.def.WasmOpcodes.f64_le
import me.shika.wasm.def.WasmOpcodes.f64_lt
import me.shika.wasm.def.WasmOpcodes.f64_max
import me.shika.wasm.def.WasmOpcodes.f64_min
import me.shika.wasm.def.WasmOpcodes.f64_mul
import me.shika.wasm.def.WasmOpcodes.f64_ne
import me.shika.wasm.def.WasmOpcodes.f64_nearest
import me.shika.wasm.def.WasmOpcodes.f64_neg
import me.shika.wasm.def.WasmOpcodes.f64_promote_f32
import me.shika.wasm.def.WasmOpcodes.f64_reinterpret_i64
import me.shika.wasm.def.WasmOpcodes.f64_sqrt
import me.shika.wasm.def.WasmOpcodes.f64_sub
import me.shika.wasm.def.WasmOpcodes.f64_trunc
import me.shika.wasm.def.WasmOpcodes.i32_add
import me.shika.wasm.def.WasmOpcodes.i32_and
import me.shika.wasm.def.WasmOpcodes.i32_clz
import me.shika.wasm.def.WasmOpcodes.i32_const
import me.shika.wasm.def.WasmOpcodes.i32_ctz
import me.shika.wasm.def.WasmOpcodes.i32_div_s
import me.shika.wasm.def.WasmOpcodes.i32_div_u
import me.shika.wasm.def.WasmOpcodes.i32_eq
import me.shika.wasm.def.WasmOpcodes.i32_eqz
import me.shika.wasm.def.WasmOpcodes.i32_extend16_s
import me.shika.wasm.def.WasmOpcodes.i32_extend8_s
import me.shika.wasm.def.WasmOpcodes.i32_ge_s
import me.shika.wasm.def.WasmOpcodes.i32_ge_u
import me.shika.wasm.def.WasmOpcodes.i32_gt_s
import me.shika.wasm.def.WasmOpcodes.i32_gt_u
import me.shika.wasm.def.WasmOpcodes.i32_le_s
import me.shika.wasm.def.WasmOpcodes.i32_le_u
import me.shika.wasm.def.WasmOpcodes.i32_lt_s
import me.shika.wasm.def.WasmOpcodes.i32_lt_u
import me.shika.wasm.def.WasmOpcodes.i32_mul
import me.shika.wasm.def.WasmOpcodes.i32_ne
import me.shika.wasm.def.WasmOpcodes.i32_or
import me.shika.wasm.def.WasmOpcodes.i32_popcnt
import me.shika.wasm.def.WasmOpcodes.i32_reinterpret_f32
import me.shika.wasm.def.WasmOpcodes.i32_rem_s
import me.shika.wasm.def.WasmOpcodes.i32_rem_u
import me.shika.wasm.def.WasmOpcodes.i32_rotl
import me.shika.wasm.def.WasmOpcodes.i32_rotr
import me.shika.wasm.def.WasmOpcodes.i32_shl
import me.shika.wasm.def.WasmOpcodes.i32_shr_s
import me.shika.wasm.def.WasmOpcodes.i32_shr_u
import me.shika.wasm.def.WasmOpcodes.i32_sub
import me.shika.wasm.def.WasmOpcodes.i32_trunc_f32_s
import me.shika.wasm.def.WasmOpcodes.i32_trunc_f32_u
import me.shika.wasm.def.WasmOpcodes.i32_trunc_f64_s
import me.shika.wasm.def.WasmOpcodes.i32_trunc_f64_u
import me.shika.wasm.def.WasmOpcodes.i32_trunc_sat_f32_s
import me.shika.wasm.def.WasmOpcodes.i32_trunc_sat_f32_u
import me.shika.wasm.def.WasmOpcodes.i32_trunc_sat_f64_s
import me.shika.wasm.def.WasmOpcodes.i32_trunc_sat_f64_u
import me.shika.wasm.def.WasmOpcodes.i32_wrap_i64
import me.shika.wasm.def.WasmOpcodes.i32_xor
import me.shika.wasm.def.WasmOpcodes.i64_add
import me.shika.wasm.def.WasmOpcodes.i64_and
import me.shika.wasm.def.WasmOpcodes.i64_clz
import me.shika.wasm.def.WasmOpcodes.i64_const
import me.shika.wasm.def.WasmOpcodes.i64_ctz
import me.shika.wasm.def.WasmOpcodes.i64_div_s
import me.shika.wasm.def.WasmOpcodes.i64_div_u
import me.shika.wasm.def.WasmOpcodes.i64_eq
import me.shika.wasm.def.WasmOpcodes.i64_eqz
import me.shika.wasm.def.WasmOpcodes.i64_extend16_s
import me.shika.wasm.def.WasmOpcodes.i64_extend32_s
import me.shika.wasm.def.WasmOpcodes.i64_extend8_s
import me.shika.wasm.def.WasmOpcodes.i64_extend_i32_s
import me.shika.wasm.def.WasmOpcodes.i64_extend_i32_u
import me.shika.wasm.def.WasmOpcodes.i64_ge_s
import me.shika.wasm.def.WasmOpcodes.i64_ge_u
import me.shika.wasm.def.WasmOpcodes.i64_gt_s
import me.shika.wasm.def.WasmOpcodes.i64_gt_u
import me.shika.wasm.def.WasmOpcodes.i64_le_s
import me.shika.wasm.def.WasmOpcodes.i64_le_u
import me.shika.wasm.def.WasmOpcodes.i64_lt_s
import me.shika.wasm.def.WasmOpcodes.i64_lt_u
import me.shika.wasm.def.WasmOpcodes.i64_mul
import me.shika.wasm.def.WasmOpcodes.i64_ne
import me.shika.wasm.def.WasmOpcodes.i64_or
import me.shika.wasm.def.WasmOpcodes.i64_popcnt
import me.shika.wasm.def.WasmOpcodes.i64_reinterpret_f64
import me.shika.wasm.def.WasmOpcodes.i64_rem_s
import me.shika.wasm.def.WasmOpcodes.i64_rem_u
import me.shika.wasm.def.WasmOpcodes.i64_rotl
import me.shika.wasm.def.WasmOpcodes.i64_rotr
import me.shika.wasm.def.WasmOpcodes.i64_shl
import me.shika.wasm.def.WasmOpcodes.i64_shr_s
import me.shika.wasm.def.WasmOpcodes.i64_shr_u
import me.shika.wasm.def.WasmOpcodes.i64_sub
import me.shika.wasm.def.WasmOpcodes.i64_trunc_f32_s
import me.shika.wasm.def.WasmOpcodes.i64_trunc_f32_u
import me.shika.wasm.def.WasmOpcodes.i64_trunc_f64_s
import me.shika.wasm.def.WasmOpcodes.i64_trunc_f64_u
import me.shika.wasm.def.WasmOpcodes.i64_trunc_sat_f32_s
import me.shika.wasm.def.WasmOpcodes.i64_trunc_sat_f32_u
import me.shika.wasm.def.WasmOpcodes.i64_trunc_sat_f64_s
import me.shika.wasm.def.WasmOpcodes.i64_trunc_sat_f64_u
import me.shika.wasm.def.WasmOpcodes.i64_xor
import me.shika.wasm.def.WasmValueType
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.NumericEnd
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.NumericStart

@OptIn(ExperimentalStdlibApi::class)
fun IntArray.dump(
    start: Int = 0,
    length: Int = this.size,
    maxInstructions: Int = Int.MAX_VALUE
): String {
    val instructions = this

    var indent = 0

    fun StringBuilder.appendIndented(param: String) {
        repeat(indent) {
            append("  ")
        }
        append(param)
    }
    fun StringBuilder.appendIndentedLine(param: String) {
        repeat(indent) {
            append("  ")
        }
        appendLine(param)
    }

    return buildString {
        var position = start
        var printedInstructions = 0
        while (position < length && printedInstructions < maxInstructions) {
            printedInstructions ++
            append(position.toHexString())
            append(": ")

            val instruction = instructions[position++]
            when (val op = instruction and 0xFF) {
                Unreachable -> appendIndentedLine("unreachable")
                NoOp -> appendIndentedLine("noop")
                Loop -> {
                    val type = instructions[position++]
                    appendIndented("loop ")
                    appendLine(type.toTypeString())
                    indent++
                }

                Block -> {
                    val type = instructions[position++]
                    appendIndented("block ")
                    append(type.toTypeString())
                    append(" end: ")
                    appendLine(instructions[position++].toHexString())
                    indent++
                }

                Try -> {
                    val type = instructions[position++]
                    appendIndented("try ")
                    append(type.toTypeString())
                    append(" end: ")
                    append(instructions[position++].toHexString())
                    append(" catch: ")
                    append(instructions[position++].toHexString())
                    appendLine()
                    indent++
                }

                Jump -> {
                    appendIndented("jump ")
                    append(instructions[position++].toHexString())
                    appendLine()
                }

                If -> {
                    appendIndented("if ")
                    append(instructions[position++].toTypeString())
                    append(" else: ")
                    append(instructions[position++].toHexString())
                    append(" end: ")
                    append(instructions[position++].toHexString())
                    appendLine()
                    indent++
                }

                Catch -> {
                    indent--
                    appendIndented("catch ")
                    append("[")
                    val labelCount = instructions[position++]
                    repeat(labelCount) {
                        append(instructions[position++])
                        append(" at ")
                        append(instructions[position++].toHexString())
                        append(", ")
                    }
                    append("]")
                    appendLine()
                    indent++
                }

                End -> {
                    indent--
                    appendIndentedLine("end")
                }

                Branch -> {
                    appendIndented("br ")
                    append(instructions[position++])
                    appendLine()
                }

                BranchIf -> {
                    appendIndented("br.if ")
                    append(instructions[position++])
                    appendLine()
                }

                BranchTable -> {
                    appendIndented("br.table ")

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

                Throw, Rethrow -> {
                    when (op) {
                        Throw -> appendIndented("throw ")
                        Rethrow -> appendIndented("rethrow ")
                    }
                    append(instructions[position++])
                    appendLine()
                }

                RefNull -> {
                    appendIndented("ref_null ")
                    append(instructions[position++])
                    appendLine()
                }

                IsNull -> {
                    appendIndentedLine("is_null")
                }

                RefFunc -> {
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
                        LocalGet -> appendIndented("local.get ")
                        LocalSet -> appendIndented("local.set ")
                        LocalTee -> appendIndented("local.tee ")
                        GlobalGet -> appendIndented("global.get ")
                        GlobalSet -> appendIndented("global.set ")
                        TableGet -> appendIndented("table.get ")
                        TableSet -> appendIndented("table.set ")
                    }
                    append(instructions[position++])
                    appendLine()
                }

                StructNew -> {
                    appendIndented("struct.new ")
                    append(instructions[position++])
                    appendLine()
                }

                ArrayNewDefault -> {
                    appendIndented("array.new_default ")
                    append(instructions[position++])
                    appendLine()
                }
                StructGet -> {
                    val type = instruction shr 8
                    appendIndented(
                        when (type) {
                            0b00 -> "struct.get "
                            0b01 -> "struct.get_u "
                            0b10 -> "struct.get_s "
                            else -> error("Unknown struct get type: ${type.toHexString()}")
                        }
                    )
                    append(instructions[position++])
                    append(" ")
                    append(instructions[position++])
                    appendLine()
                }
                StructSet -> {
                    appendIndented("struct.set ")
                    append(instructions[position++])
                    append(" ")
                    append(instructions[position++])
                    appendLine()
                }
                ArrayGet -> {
                    val type = instruction shr 8
                    appendIndented(
                        when (type) {
                            0b00 -> "array.get "
                            0b01 -> "array.get_u "
                            0b10 -> "array.get_s "
                            else -> error("Unknown array get type: ${type.toHexString()}")
                        }
                    )
                    append(instructions[position++])
                    appendLine()
                }
                ArraySet -> {
                    appendIndented("array.set ")
                    append(instructions[position++])
                    appendLine()
                }
                ArrayNewData -> {
                    appendIndented("array.new_data ")
                    append(instructions[position++])
                    append(" ")
                    append(instructions[position++])
                    appendLine()
                }
                ArrayLen -> {
                    appendIndentedLine("array.len")
                }
                RefCast -> {
                    appendIndented("ref.cast ")
                    if (instruction and 0xFF00 != 0) {
                        append("not_null ")
                    }
                    append(instructions[position++])
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
                    appendIndented(
                        when (op) {
                            MemLoadi32 -> "i32.load"
                            MemLoadi64 -> "i64.load"
                            MemLoadf64 -> "f64.load"
                            MemLoadf32 -> "f32.load"
                            MemLoadi32_8s -> "i32.load8_s"
                            MemLoadi32_8u -> "i32.load8_u"
                            MemLoadi32_16s -> "i32.load16_s"
                            MemLoadi32_16u -> "i32.load16_u"
                            MemLoadi64_8s -> "i64.load8_s"
                            MemLoadi64_8u -> "i64.load8_u"
                            MemLoadi64_16s -> "i64.load16_s"
                            MemLoadi64_16u -> "i64.load16_u"
                            MemLoadi64_32s -> "i64.load32_s"
                            MemLoadi64_32u -> "i64.load32_u"
                            MemStorei64 -> "i64.store"
                            MemStorei32 -> "i32.store"
                            MemStoref32 -> "f32.store"
                            MemStoref64 -> "f64.store"
                            MemStorei32_8 -> "i32.store8"
                            MemStorei32_16 -> "i32.store16"
                            MemStorei64_8 -> "i64.store8"
                            MemStorei64_16 -> "i32.store16"
                            MemStorei64_32 -> "i32.store32"
                            else -> error("Unknown mem op byte: ${op.toHexString()}")
                        }
                    )

                    append(" ")
                    append(instructions[position++])
                    append(" ")
                    append(instructions[position++])
                    appendLine()
                }

                MemSize, MemGrow -> {
                    appendIndentedLine(
                        when (op) {
                            MemSize -> "mem.size"
                            MemGrow -> "mem.grow"
                            else -> error("Unknown mem op byte: ${op.toHexString()}")
                        }
                    )
                }

                i32_const -> {
                    appendIndented("const.i32 ")
                    append(instructions[position++])
                    appendLine()
                }

                i64_const -> {
                    appendIndented("const.i64 ")
                    val bits = instructions[position++].toLong() or (instructions[position++].toLong() shl 32)
                    append(bits)
                    appendLine()
                }

                f32_const -> {
                    appendIndented("const.f32 ")
                    append(Float.fromBits(instructions[position++]))
                    appendLine()
                }

                f64_const -> {
                    appendIndented("const.f64 ")
                    val bits = instructions[position++].toLong() or (instructions[position++].toLong() shl 32)
                    append(Double.fromBits(bits))
                    appendLine()
                }

                i32_trunc_sat_f32_s -> appendIndentedLine("i32_trunc_sat_f32_s")
                i32_trunc_sat_f32_u -> appendIndentedLine("i32_trunc_sat_f32_u")
                i32_trunc_sat_f64_s -> appendIndentedLine("i32_trunc_sat_f64_s")
                i32_trunc_sat_f64_u -> appendIndentedLine("i32_trunc_sat_f64_u")
                i64_trunc_sat_f32_s -> appendIndentedLine("i64_trunc_sat_f32_s")
                i64_trunc_sat_f32_u -> appendIndentedLine("i64_trunc_sat_f32_u")
                i64_trunc_sat_f64_s -> appendIndentedLine("i64_trunc_sat_f64_s")
                i64_trunc_sat_f64_u -> appendIndentedLine("i64_trunc_sat_f64_u")

                RefEq, RefAsNonNull -> {
                    when (op) {
                        RefEq -> appendIndentedLine("ref.eq")
                        RefAsNonNull -> appendIndentedLine("ref.as_non_null")
                    }
                }

                AnyConvert -> appendIndentedLine("any.convert")
                ExternConvert -> appendIndentedLine("extern.convert")

                else -> {
                    if (op in NumericStart..NumericEnd) {
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
    if (this < 0) {
        WasmValueType.toValueType(this).toString()
    } else {
        this.toString()
    }

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
