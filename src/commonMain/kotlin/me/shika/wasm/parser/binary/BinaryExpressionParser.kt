package me.shika.wasm.parser.binary

import me.shika.wasm.debug.dump
import me.shika.wasm.def.WasmExpr
import me.shika.wasm.def.WasmOpcodes
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.AnyConvert
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.ArrayCopy
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.ArrayFill
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.ArrayGet
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.ArrayGetS
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.ArrayGetU
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.ArrayInitData
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.ArrayInitElem
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.ArrayLen
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.ArrayNew
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.ArrayNewData
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.ArrayNewDefault
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.ArrayNewElem
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.ArrayNewFixed
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.ArraySet
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.Block
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.Branch
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.BranchIf
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.BranchTable
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.Call
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.CallIndirect
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.CallRef
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.Catch
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.CatchAll
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.DataDrop
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.Drop
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.Else
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.End
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.GlobalGet
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.GlobalSet
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.If
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.IsNull
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.LocalGet
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.LocalSet
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.LocalTee
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.Loop
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemGrow
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemLoadf32
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemLoadf64
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemLoadi32
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemLoadi32_16s
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemLoadi32_16u
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemLoadi32_8s
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemLoadi32_8u
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemLoadi64
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemLoadi64_16s
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemLoadi64_16u
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemLoadi64_32s
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemLoadi64_32u
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemLoadi64_8s
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemLoadi64_8u
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemSize
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemStoref32
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemStoref64
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemStorei32
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemStorei32_16
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemStorei32_8
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemStorei64
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemStorei64_16
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemStorei64_32
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemStorei64_8
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.DataOp
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.ElemDrop
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.ExternConvert
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemoryCopy
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemoryFill
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.MemoryInit
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.NoOp
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.NumericEnd
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.NumericStart
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.RefAsNonNull
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.RefCast
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.RefCastNotNull
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.RefEq
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.RefFunc
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.RefNull
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.RefOp
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.RefTest
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.RefTestNotNull
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.Rethrow
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.Return
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.Select
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.SelectMany
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.StructGet
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.StructGetS
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.StructGetU
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.StructNew
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.StructNewDefault
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.StructSet
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.TableCopy
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.TableFill
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.TableGet
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.TableGrow
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.TableInit
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.TableSet
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.TableSize
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.Throw
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.Try
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.Unreachable
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.f32_const
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.f64_const
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.i32_const
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.i32_trunc_f32_s
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.i32_trunc_f32_u
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.i32_trunc_f64_s
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.i32_trunc_f64_u
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.i32_trunc_sat_f32_s
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.i32_trunc_sat_f32_u
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.i32_trunc_sat_f64_s
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.i32_trunc_sat_f64_u
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.i64_const
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.i64_trunc_f32_s
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.i64_trunc_f32_u
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.i64_trunc_f64_s
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.i64_trunc_f64_u
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.i64_trunc_sat_f32_s
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.i64_trunc_sat_f32_u
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.i64_trunc_sat_f64_s
import me.shika.wasm.parser.binary.BinaryWasmOpcodes.i64_trunc_sat_f64_u
import me.shika.wasm.parser.binary.internal.ByteBuffer
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

    private var blocks = ArrayDeque<BlockInfo>()

    fun ByteBuffer.parseExpression(const: Boolean, exprSize: Int): WasmExpr {
        val start = position
        while (true) {
            if (position - start >= exprSize) {
                error("Read out of expr bounds, instructions: ${code.dump(exprSize)}")
            }
            // todo: validate const instructions
            when (val op = readByteAsInt()) {
                Unreachable -> push(WasmOpcodes.Unreachable)
                NoOp -> { /* do nothing */ }
                Block, Loop -> {
                    val type = parseBlockType()
                    val block = when (op) {
                        Block -> {
                            push(WasmOpcodes.Block)
                            push(type)
                            push(0xbadca5e) // placeholder for offset to end
                            BlockInfo.Block(nextPosition - 1)
                        }
                        Loop -> {
                            push(WasmOpcodes.Loop)
                            push(type)
                            BlockInfo.Loop
                        }
                        else -> error("Unknown block type: $op")
                    }
                    blocks.add(block)
                }
                Try -> {
                    val type = parseBlockType()
                    push(WasmOpcodes.Try)
                    push(type)
                    blocks.add(BlockInfo.Try(nextPosition, mutableMapOf()))
                    push(0xbadca5e) // placeholder for end position
                    push(0xbadca5e) // placeholder for offset to catch count
                }
                If -> {
                    val type = parseBlockType()
                    push(WasmOpcodes.If)
                    push(type)
                    push(0xbade15e) // placeholder for offset to else
                    push(0xbadca5e) // placeholder for offset to end
                    blocks.add(
                        BlockInfo.If(nextPosition - 2)
                    )
                }
                Else -> {
                    val block = blocks.lastOrNull() ?: error("Else is not allowed outside of block")
                    if (block !is BlockInfo.If) {
                        error("Else is not allowed in $block")
                    }
                    if (block.ifEndOffset != -1) {
                        error("Else is already defined for block")
                    }
                    // push jump to end for if block
                    push(WasmOpcodes.Jump)
                    block.ifEndOffset = nextPosition
                    push(0xbade15e) // placeholder for end position

                    // start else block
                    // patch else start position in if block
                    code[block.offset] = nextPosition

                }
                Branch -> {
                    val labelIdx = parseIdx()
                    push(WasmOpcodes.Branch, labelIdx)
                }
                BranchIf -> {
                    val labelIdx = parseIdx()
                    push(WasmOpcodes.BranchIf, labelIdx)
                }
                BranchTable -> {
                    push(WasmOpcodes.BranchTable)

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
                    push(WasmOpcodes.Return)
                }
                Call -> {
                    val funcIdx = parseIdx()
                    push(WasmOpcodes.Call, funcIdx)
                }
                CallIndirect -> {
                    val typeIdx = parseIdx()
                    val tableIdx = parseIdx()
                    push(WasmOpcodes.CallIndirect, typeIdx, tableIdx)
                }
                CallRef -> {
                    val typeIdx = parseIdx()
                    push(WasmOpcodes.CallRef, typeIdx)
                }
                Catch -> {
                    val tryBlock = blocks.lastOrNull() as? BlockInfo.Try
                        ?: error("Catch is not allowed outside of try block")
                    val tagIdx = parseIdx()

                    // make sure we don't fall-through into this catch
                    push(WasmOpcodes.Jump)
                    push(0xbade15e) // placeholder for end position

                    tryBlock.catchOffsets[tagIdx] = nextPosition
                }
                CatchAll -> {
                    val tryBlock = blocks.lastOrNull() as? BlockInfo.Try
                        ?: error("Catch is not allowed outside of try block")

                    // make sure we don't fall-through into this catch
                    push(WasmOpcodes.Jump)
                    push(0xbade15e) // placeholder for end position

                    tryBlock.catchOffsets[BlockInfo.Try.CatchAll] = nextPosition
                }
                Throw -> {
                    val idx = parseIdx()
                    push(WasmOpcodes.Throw, idx)
                }
                Rethrow -> {
                    val idx = parseIdx()
                    push(WasmOpcodes.Rethrow, idx)
                }
                End -> {
                    val block = blocks.removeLastOrNull() ?: break
                    when (block) {
                        is BlockInfo.If -> {
                            if (block.ifEndOffset != -1) {
                                // if else block exists, patch end of if to jump here
                                code[block.ifEndOffset] = nextPosition
                            } else {
                                // else patch if itself to jump here
                                code[block.offset] = nextPosition
                            }
                            push(End)
                            // patch branch on if to jump here
                            code[block.offset + 1] = nextPosition
                        }

                        is BlockInfo.Try -> {
                            // encode catch blocks
                            val catchOffsets = block.catchOffsets
                            if (catchOffsets.isNotEmpty()) {
                                // This instruction should never be executed,
                                // but is here to delimit catches for code dump
                                push(WasmOpcodes.Catch)

                                code[block.offset + 1] = nextPosition

                                push(block.catchOffsets.size)
                                block.catchOffsets.forEach { (tagIdx, offset) ->
                                    push(tagIdx)
                                    push(offset)
                                }
                                // patch end of all catches + try block to jump here
                                block.catchOffsets.forEach { (_, offset) ->
                                    code[offset - 1] = nextPosition
                                }
                            } else {
                                // no catches, indicate that to try block
                                code[block.offset + 1] = -1
                            }

                            push(End)
                            // patch end of try block to jump here
                            code[block.offset] = nextPosition
                        }

                        is BlockInfo.Block -> {
                            push(End)
                            // patch block to jump here
                            code[block.offset] = nextPosition
                        }

                        is BlockInfo.Loop -> {
                            // do nothing, jump is always backwards
                            push(End)
                        }
                    }
                }
                RefNull -> {
                    val type = parseRefType()
                    push(WasmOpcodes.RefNull, type)
                }
                IsNull -> {
                    push(WasmOpcodes.IsNull)
                }
                RefFunc -> {
                    val idx = parseIdx()
                    push(WasmOpcodes.RefFunc, idx)
                }
                Drop -> {
                    push(WasmOpcodes.Drop)
                }
                Select -> {
                    push(WasmOpcodes.Select)
                }
                SelectMany -> {
                    push(WasmOpcodes.SelectMany)
                    val typeCount = readU32()
                    push(typeCount)
                    repeat(typeCount) {
                        val type = parseValueType()
                        push(type)
                    }
                }
                LocalGet -> push(WasmOpcodes.LocalGet, parseIdx())
                LocalSet -> push(WasmOpcodes.LocalSet, parseIdx())
                LocalTee -> push(WasmOpcodes.LocalTee, parseIdx())
                GlobalGet -> push(WasmOpcodes.GlobalGet, parseIdx())
                GlobalSet -> push(WasmOpcodes.GlobalSet, parseIdx())
                TableGet -> push(WasmOpcodes.TableGet, parseIdx())
                TableSet -> push(WasmOpcodes.TableSet, parseIdx())
                DataOp -> {
                    when (val dataOp = readU32()) {
                        // integer truncate instructions
                        i32_trunc_sat_f32_s -> WasmOpcodes.i32_trunc_sat_f32_s
                        i32_trunc_sat_f32_u -> WasmOpcodes.i32_trunc_sat_f32_u
                        i32_trunc_sat_f64_s -> WasmOpcodes.i32_trunc_sat_f64_s
                        i32_trunc_sat_f64_u -> WasmOpcodes.i32_trunc_sat_f64_u
                        i64_trunc_sat_f32_s -> WasmOpcodes.i64_trunc_sat_f32_s
                        i64_trunc_sat_f32_u -> WasmOpcodes.i64_trunc_sat_f32_u
                        i64_trunc_sat_f64_s -> WasmOpcodes.i64_trunc_sat_f64_s
                        i64_trunc_sat_f64_u -> WasmOpcodes.i64_trunc_sat_f64_u
                        MemoryInit -> {
                            val dataIdx = parseIdx()
                            val segIdx = readByteAsInt()
                            require(segIdx == 0) {
                                "Non-zero segment index for memory init: $segIdx"
                            }
                            push(WasmOpcodes.MemInit, dataIdx)
                        }

                        DataDrop -> {
                            val dataIdx = parseIdx()
                            push(WasmOpcodes.DataDrop, dataIdx)
                        }

                        MemoryCopy -> {
                            val srcIdx = readByteAsInt()
                            val dstIdx = readByteAsInt()
                            require(srcIdx == 0) {
                                "Non-zero source index for memory copy: $srcIdx"
                            }
                            require(dstIdx == 0) {
                                "Non-zero destination index for memory copy: $dstIdx"
                            }
                            push(WasmOpcodes.MemCopy)
                        }

                        MemoryFill -> {
                            val memIdx = readByteAsInt()
                            require(memIdx == 0) {
                                "Non-zero memory index for memory fill: $memIdx"
                            }
                            push(WasmOpcodes.MemFill)
                        }

                        TableInit -> {
                            val elemIdx = parseIdx()
                            val tableIdx = parseIdx()
                            push(WasmOpcodes.TableInit, elemIdx, tableIdx)
                        }

                        ElemDrop -> {
                            val elemIdx = parseIdx()
                            push(WasmOpcodes.ElemDrop, elemIdx)
                        }

                        TableCopy -> {
                            val srcIdx = parseIdx()
                            val dstIdx = parseIdx()
                            push(WasmOpcodes.TableCopy, srcIdx, dstIdx)
                        }

                        TableGrow -> {
                            val tableIdx = parseIdx()
                            push(WasmOpcodes.TableGrow, tableIdx)
                        }

                        TableSize -> {
                            val tableIdx = parseIdx()
                            push(WasmOpcodes.TableSize, tableIdx)
                        }

                        TableFill -> {
                            val tableIdx = parseIdx()
                            push(WasmOpcodes.TableFill, tableIdx)
                        }

                        else -> {
                            error("Unknown data op: $dataOp")
                        }
                    }
                }
                RefOp -> {
                    when (val refOp = readU32()) {
                        StructNew -> {
                            val typeIdx = parseIdx()
                            push(WasmOpcodes.StructNew, typeIdx)
                        }
                        StructNewDefault -> {
                            val typeIdx = parseIdx()
                            push(WasmOpcodes.StructNewDefault, typeIdx)
                        }
                        StructGet -> {
                            val typeIdx = parseIdx()
                            val fieldIdx = parseIdx()
                            push(WasmOpcodes.StructGet, typeIdx, fieldIdx)
                        }
                        StructGetU -> {
                            val typeIdx = parseIdx()
                            val fieldIdx = parseIdx()
                            push(WasmOpcodes.StructGet or (0b01 shl 8), typeIdx, fieldIdx)
                        }
                        StructGetS -> {
                            val typeIdx = parseIdx()
                            val fieldIdx = parseIdx()
                            push(WasmOpcodes.StructGet or (0b10 shl 8), typeIdx, fieldIdx)
                        }
                        StructSet -> {
                            val typeIdx = parseIdx()
                            val fieldIdx = parseIdx()
                            push(WasmOpcodes.StructSet, typeIdx, fieldIdx)
                        }
                        ArrayNew -> {
                            val typeIdx = parseIdx()
                            push(WasmOpcodes.ArrayNew, typeIdx)
                        }
                        ArrayNewDefault -> {
                            val typeIdx = parseIdx()
                            push(WasmOpcodes.ArrayNewDefault, typeIdx)
                        }
                        ArrayNewFixed -> {
                            val typeIdx = parseIdx()
                            val size = readU32()
                            push(WasmOpcodes.ArrayNewFixed, typeIdx, size)
                        }
                        ArrayNewData -> {
                            val typeIdx = parseIdx()
                            val dataIdx = parseIdx()
                            push(WasmOpcodes.ArrayNewData, typeIdx, dataIdx)
                        }
                        ArrayNewElem -> {
                            val typeIdx = parseIdx()
                            val elemIdx = parseIdx()
                            push(WasmOpcodes.ArrayNewElem, typeIdx, elemIdx)
                        }
                        ArrayGet -> {
                            val typeIdx = parseIdx()
                            push(WasmOpcodes.ArrayGet, typeIdx)
                        }
                        ArrayGetU -> {
                            val typeIdx = parseIdx()
                            push(WasmOpcodes.ArrayGet or (0b01 shl 8), typeIdx)
                        }
                        ArrayGetS -> {
                            val typeIdx = parseIdx()
                            push(WasmOpcodes.ArrayGet or (0b10 shl 8), typeIdx)
                        }
                        ArraySet -> {
                            val typeIdx = parseIdx()
                            push(WasmOpcodes.ArraySet, typeIdx)
                        }
                        ArrayLen -> {
                            push(WasmOpcodes.ArrayLen)
                        }
                        ArrayFill -> {
                            val typeIdx = parseIdx()
                            push(WasmOpcodes.ArrayFill, typeIdx)
                        }
                        ArrayCopy -> {
                            val typeIdxFrom = parseIdx()
                            val typeIdxTo = parseIdx()
                            push(WasmOpcodes.ArrayCopy, typeIdxFrom, typeIdxTo)
                        }
                        ArrayInitData -> {
                            val typeIdx = parseIdx()
                            val dataIdx = parseIdx()
                            push(WasmOpcodes.ArrayInitData, typeIdx, dataIdx)
                        }
                        ArrayInitElem -> {
                            val typeIdx = parseIdx()
                            val elemIdx = parseIdx()
                            push(WasmOpcodes.ArrayInitElem, typeIdx, elemIdx)
                        }
                        RefTestNotNull -> {
                            push(WasmOpcodes.RefTest or (1 shl 8))
                            push(parseRefType())
                        }
                        RefTest -> {
                            push(WasmOpcodes.RefTest)
                            push(parseRefType())
                        }
                        RefCastNotNull -> {
                            push(WasmOpcodes.RefCast or (1 shl 8))
                            push(parseRefType())
                        }
                        RefCast -> {
                            push(WasmOpcodes.RefCast)
                            push(parseRefType())
                        }
                        AnyConvert -> {
                            push(WasmOpcodes.AnyConvert)
                        }
                        ExternConvert -> {
                            push(WasmOpcodes.ExternConvert)
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
                    val internalOp = when (op) {
                        MemLoadi64 -> WasmOpcodes.MemLoadi64
                        MemLoadi32 -> WasmOpcodes.MemLoadi32
                        MemLoadf64 -> WasmOpcodes.MemLoadf64
                        MemLoadf32 -> WasmOpcodes.MemLoadf32
                        MemLoadi32_8s -> WasmOpcodes.MemLoadi32_8s
                        MemLoadi32_8u -> WasmOpcodes.MemLoadi32_8u
                        MemLoadi32_16s -> WasmOpcodes.MemLoadi32_16s
                        MemLoadi32_16u -> WasmOpcodes.MemLoadi32_16u
                        MemLoadi64_8s -> WasmOpcodes.MemLoadi64_8s
                        MemLoadi64_8u -> WasmOpcodes.MemLoadi64_8u
                        MemLoadi64_16s -> WasmOpcodes.MemLoadi64_16s
                        MemLoadi64_16u -> WasmOpcodes.MemLoadi64_16u
                        MemLoadi64_32s -> WasmOpcodes.MemLoadi64_32s
                        MemLoadi64_32u -> WasmOpcodes.MemLoadi64_32u
                        MemStorei64 -> WasmOpcodes.MemStorei64
                        MemStorei32 -> WasmOpcodes.MemStorei32
                        MemStoref32 -> WasmOpcodes.MemStoref32
                        MemStoref64 -> WasmOpcodes.MemStoref64
                        MemStorei32_8 -> WasmOpcodes.MemStorei32_8
                        MemStorei32_16 -> WasmOpcodes.MemStorei32_16
                        MemStorei64_8 -> WasmOpcodes.MemStorei64_8
                        MemStorei64_16 -> WasmOpcodes.MemStorei64_16
                        MemStorei64_32 -> WasmOpcodes.MemStorei64_32
                        else -> error("Unknown memory op: $op")
                    }
                    push(internalOp, align, offset)
                }
                MemSize -> {
                    val memIdx = readByteAsInt()
                    check(memIdx == 0) {
                        "Non-zero memory index for op: $op, idx: $memIdx"
                    }
                    push(WasmOpcodes.MemSize)
                }
                MemGrow -> {
                    val memIdx = readByteAsInt()
                    check(memIdx == 0) {
                        "Non-zero memory index for op: $op, idx: $memIdx"
                    }
                    push(WasmOpcodes.MemGrow)
                }
                i32_const -> {
                    val value = readS32()
                    push(WasmOpcodes.i32_const, value)
                }
                i64_const -> {
                    val value = readS64()
                    push(
                        WasmOpcodes.i64_const,
                        (value and 0xFFFFFFFF).toInt(), (value shr 32 and 0xFFFFFFFF).toInt()
                    )
                }
                f32_const -> {
                    val value = readInt()
                    push(WasmOpcodes.f32_const, value)
                }
                f64_const -> {
                    val arg0 = readInt()
                    val arg1 = readInt()
                    push(WasmOpcodes.f64_const, arg0, arg1)
                }
                RefEq -> {
                    push(WasmOpcodes.RefEq)
                }
                RefAsNonNull -> {
                    push(WasmOpcodes.RefAsNonNull)
                }
                else -> {
                    if (op in NumericStart..NumericEnd) {
                        push(op) // guaranteed to be preserved as is
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
        check(blocks.isEmpty()) {
            "Blocks stack is not empty: $blocks"
        }
    }

    private fun ensureCapacity(space: Int) {
        if (nextPosition + space > code.size - 1) {
            val newCode = IntArray(code.size * 2)
            code.copyInto(newCode)
            code = newCode
        }
    }

    sealed interface BlockInfo {
        data class Block(val offset: Int) : BlockInfo
        data object Loop : BlockInfo

        data class If(
            val offset: Int,
            var ifEndOffset: Int = -1
        ) : BlockInfo

        data class Try(
            val offset: Int,
            val catchOffsets: MutableMap<Int, Int>
        ) : BlockInfo {
            companion object {
                const val CatchAll = -1
            }
        }
    }
}

