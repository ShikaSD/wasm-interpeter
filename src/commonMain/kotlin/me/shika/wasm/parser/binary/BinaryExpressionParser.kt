package me.shika.wasm.parser.binary

import me.shika.wasm.def.WasmExpr
import me.shika.wasm.def.WasmInstructions.Block
import me.shika.wasm.def.WasmInstructions.Branch
import me.shika.wasm.def.WasmInstructions.BranchIf
import me.shika.wasm.def.WasmInstructions.BranchTable
import me.shika.wasm.def.WasmInstructions.Call
import me.shika.wasm.def.WasmInstructions.CallIndirect
import me.shika.wasm.def.WasmInstructions.CallRef
import me.shika.wasm.def.WasmInstructions.Catch
import me.shika.wasm.def.WasmInstructions.CatchAll
import me.shika.wasm.def.WasmInstructions.Drop
import me.shika.wasm.def.WasmInstructions.Else
import me.shika.wasm.def.WasmInstructions.End
import me.shika.wasm.def.WasmInstructions.GlobalGet
import me.shika.wasm.def.WasmInstructions.GlobalSet
import me.shika.wasm.def.WasmInstructions.If
import me.shika.wasm.def.WasmInstructions.IsNull
import me.shika.wasm.def.WasmInstructions.LocalGet
import me.shika.wasm.def.WasmInstructions.LocalSet
import me.shika.wasm.def.WasmInstructions.LocalTee
import me.shika.wasm.def.WasmInstructions.Loop
import me.shika.wasm.def.WasmInstructions.MemGrow
import me.shika.wasm.def.WasmInstructions.MemLoadf32
import me.shika.wasm.def.WasmInstructions.MemLoadf64
import me.shika.wasm.def.WasmInstructions.MemLoadi32
import me.shika.wasm.def.WasmInstructions.MemLoadi32_16s
import me.shika.wasm.def.WasmInstructions.MemLoadi32_16u
import me.shika.wasm.def.WasmInstructions.MemLoadi32_8s
import me.shika.wasm.def.WasmInstructions.MemLoadi32_8u
import me.shika.wasm.def.WasmInstructions.MemLoadi64
import me.shika.wasm.def.WasmInstructions.MemLoadi64_16s
import me.shika.wasm.def.WasmInstructions.MemLoadi64_16u
import me.shika.wasm.def.WasmInstructions.MemLoadi64_32s
import me.shika.wasm.def.WasmInstructions.MemLoadi64_32u
import me.shika.wasm.def.WasmInstructions.MemLoadi64_8s
import me.shika.wasm.def.WasmInstructions.MemLoadi64_8u
import me.shika.wasm.def.WasmInstructions.MemSize
import me.shika.wasm.def.WasmInstructions.MemStoref32
import me.shika.wasm.def.WasmInstructions.MemStoref64
import me.shika.wasm.def.WasmInstructions.MemStorei32
import me.shika.wasm.def.WasmInstructions.MemStorei32_16
import me.shika.wasm.def.WasmInstructions.MemStorei32_8
import me.shika.wasm.def.WasmInstructions.MemStorei64
import me.shika.wasm.def.WasmInstructions.MemStorei64_16
import me.shika.wasm.def.WasmInstructions.MemStorei64_32
import me.shika.wasm.def.WasmInstructions.MemStorei64_8
import me.shika.wasm.def.WasmInstructions.ModuleOp
import me.shika.wasm.def.WasmInstructions.NoOp
import me.shika.wasm.def.WasmInstructions.NumericEnd
import me.shika.wasm.def.WasmInstructions.NumericStart
import me.shika.wasm.def.WasmInstructions.RefAsNonNull
import me.shika.wasm.def.WasmInstructions.RefEq
import me.shika.wasm.def.WasmInstructions.RefFunc
import me.shika.wasm.def.WasmInstructions.RefNull
import me.shika.wasm.def.WasmInstructions.RefOp
import me.shika.wasm.def.WasmInstructions.Rethrow
import me.shika.wasm.def.WasmInstructions.Return
import me.shika.wasm.def.WasmInstructions.ReturnCallRef
import me.shika.wasm.def.WasmInstructions.Select
import me.shika.wasm.def.WasmInstructions.SelectMany
import me.shika.wasm.def.WasmInstructions.TableGet
import me.shika.wasm.def.WasmInstructions.TableSet
import me.shika.wasm.def.WasmInstructions.Throw
import me.shika.wasm.def.WasmInstructions.Try
import me.shika.wasm.def.WasmInstructions.Unreachable
import me.shika.wasm.def.WasmInstructions.f32_const
import me.shika.wasm.def.WasmInstructions.f64_const
import me.shika.wasm.def.WasmInstructions.i32_const
import me.shika.wasm.def.WasmInstructions.i64_const
import me.shika.wasm.parser.binary.internal.ByteBuffer
import me.shika.wasm.parser.binary.internal.debug.asWasmText
import me.shika.wasm.parser.binary.internal.readByteAsInt
import me.shika.wasm.parser.binary.internal.readS32
import me.shika.wasm.parser.binary.internal.readS64
import me.shika.wasm.parser.binary.internal.readU32

@OptIn(ExperimentalStdlibApi::class)
internal class BinaryExpressionParser(
    private val typeParser: BinaryTypeParser
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
                RefFunc -> {
                    val idx = parseIdx()
                    push(RefFunc, idx)
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
                i32_const -> {
                    val value = readS32()
                    push(i32_const, value)
                }
                i64_const -> {
                    val value = readS64()
                    push(i64_const, (value and 0xFFFFFFFF).toInt(), (value shr 32 and 0xFFFFFFFF).toInt())
                }
                f32_const -> {
                    val value = readInt()
                    push(f32_const, value)
                }
                f64_const -> {
                    val arg0 = readInt()
                    val arg1 = readInt()
                    push(f64_const, arg0, arg1)
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

    private fun ByteBuffer.parseBlockType(): Int =
        with(typeParser) { parseValueType() }

    private fun ByteBuffer.parseRefType(): Int =
        with(typeParser) { parseRefType() }

    private fun ByteBuffer.parseValueType(): Int =
        with(typeParser) { parseValueType() }

    fun ByteBuffer.parseFuncRefExpr(): WasmExpr {
        val idx = parseIdx()
        return WasmExpr(
            intArrayOf(
                RefFunc,
                idx
            )
        )
    }

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
}

