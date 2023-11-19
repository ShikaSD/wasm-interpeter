@file:OptIn(ExperimentalStdlibApi::class)

package me.shika.wasm.runtime

import me.shika.wasm.debug.dump
import me.shika.wasm.def.MemPageSize
import me.shika.wasm.def.WasmArrayType
import me.shika.wasm.def.WasmExpr
import me.shika.wasm.def.WasmFuncIdx
import me.shika.wasm.def.WasmFuncType
import me.shika.wasm.def.WasmOpcodes
import me.shika.wasm.def.WasmOpcodes.ArrayGet
import me.shika.wasm.def.WasmOpcodes.ArrayLen
import me.shika.wasm.def.WasmOpcodes.ArrayNewData
import me.shika.wasm.def.WasmOpcodes.ArrayNewDefault
import me.shika.wasm.def.WasmOpcodes.ArraySet
import me.shika.wasm.def.WasmOpcodes.Block
import me.shika.wasm.def.WasmOpcodes.Branch
import me.shika.wasm.def.WasmOpcodes.BranchIf
import me.shika.wasm.def.WasmOpcodes.Call
import me.shika.wasm.def.WasmOpcodes.CallRef
import me.shika.wasm.def.WasmOpcodes.Catch
import me.shika.wasm.def.WasmOpcodes.Drop
import me.shika.wasm.def.WasmOpcodes.End
import me.shika.wasm.def.WasmOpcodes.GlobalGet
import me.shika.wasm.def.WasmOpcodes.GlobalSet
import me.shika.wasm.def.WasmOpcodes.If
import me.shika.wasm.def.WasmOpcodes.IsNull
import me.shika.wasm.def.WasmOpcodes.Jump
import me.shika.wasm.def.WasmOpcodes.LocalGet
import me.shika.wasm.def.WasmOpcodes.LocalSet
import me.shika.wasm.def.WasmOpcodes.LocalTee
import me.shika.wasm.def.WasmOpcodes.Loop
import me.shika.wasm.def.WasmOpcodes.MemSize
import me.shika.wasm.def.WasmOpcodes.MemStorei32_16
import me.shika.wasm.def.WasmOpcodes.RefCast
import me.shika.wasm.def.WasmOpcodes.RefFunc
import me.shika.wasm.def.WasmOpcodes.RefNull
import me.shika.wasm.def.WasmOpcodes.Return
import me.shika.wasm.def.WasmOpcodes.Select
import me.shika.wasm.def.WasmOpcodes.StructGet
import me.shika.wasm.def.WasmOpcodes.StructNew
import me.shika.wasm.def.WasmOpcodes.StructSet
import me.shika.wasm.def.WasmOpcodes.Try
import me.shika.wasm.def.WasmOpcodes.Unreachable
import me.shika.wasm.def.WasmOpcodes.f32_const
import me.shika.wasm.def.WasmOpcodes.f64_const
import me.shika.wasm.def.WasmOpcodes.i32_add
import me.shika.wasm.def.WasmOpcodes.i32_and
import me.shika.wasm.def.WasmOpcodes.i32_const
import me.shika.wasm.def.WasmOpcodes.i32_div_s
import me.shika.wasm.def.WasmOpcodes.i32_eq
import me.shika.wasm.def.WasmOpcodes.i32_eqz
import me.shika.wasm.def.WasmOpcodes.i32_ge_s
import me.shika.wasm.def.WasmOpcodes.i32_gt_s
import me.shika.wasm.def.WasmOpcodes.i32_le_s
import me.shika.wasm.def.WasmOpcodes.i32_lt_s
import me.shika.wasm.def.WasmOpcodes.i32_mul
import me.shika.wasm.def.WasmOpcodes.i32_rem_s
import me.shika.wasm.def.WasmOpcodes.i32_shl
import me.shika.wasm.def.WasmOpcodes.i32_sub
import me.shika.wasm.def.WasmOpcodes.i32_xor
import me.shika.wasm.def.WasmOpcodes.i64_const
import me.shika.wasm.def.WasmStructType
import me.shika.wasm.def.WasmValueType


class WasmExecutor(private val runtime: WasmRuntime) {
    fun execute(
        module: WasmModule,
        function: WasmLocalFunction,
    ) {
        // todo: validate types
        val type = function.type
        val localDef = function.body.locals
        val stack = runtime.stack

        // todo: move locals to env to share between frames
        val locals = arrayOfNulls<Any?>(type.params.size + localDef.size)
        repeat(type.params.size) {
            locals[type.params.size - 1 - it] = stack.pop()
        }

        val functionIdx = module.functions.indexOf(function)
        println("executing function $functionIdx")
        println(function.body.body.code.dump())
        try {
            stack.pushFrame(function.type.returns.size, locals)
            executeFrame(module, stack.currentFrame(), function.body.body.code)
            stack.popFrame()
        } catch (e: ExecutionException) {
            throw IllegalStateException(
                "Caught error while executing function $functionIdx @ ${e.offset.toHexString()}",
                e.cause
            )
        }
    }

    private fun executeFrame(module: WasmModule, frame: WasmControl.Frame, code: IntArray) {
        val stack = runtime.stack
        val locals = frame.locals

        var ip = 0

        var instructionOffset = 0
        try {
            while (ip < code.size) {
                instructionOffset = ip
                val instruction = code[ip++]
//                print("executing ${code.dump(start = instructionOffset, maxInstructions = 1)}")
                when (val op = instruction and 0xFF) {
                    i32_const -> stack.push(
                        code[ip++]
                    )

                    i32_add -> {
                        val right = stack.pop() as Int
                        val left = stack.pop() as Int
                        stack.push(
                            left + right
                        )
                    }

                    i32_and -> {
                        val right = stack.pop() as Int
                        val left = stack.pop() as Int
                        stack.push(
                            left and right
                        )
                    }

                    i32_div_s -> {
                        val right = stack.pop() as Int
                        val left = stack.pop() as Int
                        stack.push(
                            left / right
                        )
                    }

                    i32_eq -> {
                        val right = stack.pop() as Int
                        val left = stack.pop() as Int
                        stack.push(
                            (left == right).toInt()
                        )
                    }

                    i32_eqz -> {
                        val value = stack.pop() as Int
                        stack.push(
                            (value == 0).toInt()
                        )
                    }

                    i32_ge_s -> {
                        val right = stack.pop() as Int
                        val left = stack.pop() as Int
                        stack.push(
                            (left >= right).toInt()
                        )
                    }

                    i32_gt_s -> {
                        val right = stack.pop() as Int
                        val left = stack.pop() as Int
                        stack.push(
                            (left > right).toInt()
                        )
                    }

                    i32_lt_s -> {
                        val right = stack.pop() as Int
                        val left = stack.pop() as Int
                        stack.push(
                            (left < right).toInt()
                        )
                    }

                    i32_le_s -> {
                        val right = stack.pop() as Int
                        val left = stack.pop() as Int
                        stack.push(
                            (left <= right).toInt()
                        )
                    }

                    i32_mul -> {
                        val right = stack.pop() as Int
                        val left = stack.pop() as Int
                        stack.push(
                            left * right
                        )
                    }

                    i32_rem_s -> {
                        val right = stack.pop() as Int
                        val left = stack.pop() as Int
                        stack.push(
                            left % right
                        )
                    }

                    i32_shl -> {
                        val shift = stack.pop() as Int
                        val value = stack.pop() as Int
                        stack.push(
                            value shl shift
                        )
                    }

                    MemStorei32_16 -> {
                        val memory = module.memory[0]
                        val c = stack.pop() as Int
                        val i = stack.pop() as Int
                        val align = code[ip++]
                        val offset = code[ip++]
                        require(offset == 0 && align == 0) {
                            "Only offset 0 and align 0 are supported for now"
                        }
                        memory.storeInt16(i, c.toShort())
                    }

                    i32_sub -> {
                        val right = stack.pop() as Int
                        val left = stack.pop() as Int
                        stack.push(
                            left - right
                        )
                    }

                    i32_xor -> {
                        val right = stack.pop() as Int
                        val left = stack.pop() as Int
                        stack.push(
                            left xor right
                        )
                    }

                    i64_const -> stack.push(
                        code[ip++].toLong() or (code[ip++].toLong() shl 32)
                    )

                    f32_const -> stack.push(
                        Float.fromBits(code[ip++])
                    )

                    f64_const -> stack.push(
                        Double.fromBits(code[ip++].toLong() or (code[ip++].toLong() shl 32))
                    )

                    GlobalGet -> {
                        val global = module.globals[code[ip++]]
                        stack.push(global.value)
                    }

                    GlobalSet -> {
                        val global = module.globals[code[ip++]]
                        global.value = stack.pop()
                    }

                    LocalSet -> {
                        locals[code[ip++]] = stack.pop()
                    }

                    LocalGet -> {
                        val local = locals[code[ip++]]
                        stack.push(local)
                    }

                    LocalTee -> {
                        val localIndex = code[ip++]
                        val ref = stack.peek()
                        locals[localIndex] = ref
                    }

                    RefFunc -> {
                        val funcIdx = code[ip++]
                        stack.push(WasmFuncIdx(funcIdx))
                    }

                    RefNull -> {
                        /* val type = */ code[ip++]
                        stack.push(null)
                    }

                    IsNull -> {
                        val ref = stack.pop()
                        stack.push((ref == null).toInt())
                    }

                    ArrayNewDefault -> {
                        // todo: primitive arrays
                        val size = stack.pop() as Int
                        /* val type = */code[ip++]
                        val array = arrayOfNulls<Any?>(size)
                        stack.push(array)
                    }

                    ArrayNewData -> {
                        val typeIdx = code[ip++]
                        val arrayType = module.types[typeIdx].compType as WasmArrayType
                        val dataIdx = code[ip++]
                        val data = module.data[dataIdx] ?: error("Data $dataIdx does not exist.")
                        val valueCount = stack.pop() as Int // n
                        val dataOffset = stack.pop() as Int // s
                        val ref = arrayOfNulls<Any?>(valueCount)
                        when (val valueType = WasmValueType.toValueType(arrayType.type.type)) {
                            WasmValueType.i8 -> {
                                val bytes = data.bytes
                                for (i in 0..<valueCount) {
                                    ref[i] = bytes[dataOffset + i]
                                }
                            }

                            WasmValueType.i16 -> {
                                val bytes = data.bytes
                                for (i in 0..<valueCount) {
                                    ref[i] =
                                        bytes[dataOffset + i * 2].toInt() or
                                                (bytes[dataOffset + i * 2 + 1].toInt() shl 8)
                                }
                            }

                            else -> {
                                error("Unsupported array type: $valueType")
                            }
                        }
                        stack.push(ref)
                    }

                    ArrayLen -> {
                        val array = stack.pop() as Array<*>
                        stack.push(array.size)
                    }

                    StructNew -> {
                        val typeIdx = code[ip++]
                        val structType = module.types[typeIdx].compType as WasmStructType
                        val fields = Array(structType.fields.size) { stack.pop() }
                        fields.reverse()
                        stack.push(WasmStruct(structType, fields))
                    }

                    Call -> {
                        val funcIdx = code[ip++]
                        val func = module.functions[funcIdx]
                        runtime.runInternal(module, func)
                    }

                    CallRef -> {
                        /* val typeIdx = */ code[ip++]
                        val funcIdx = stack.pop() as WasmFuncIdx
                        val func = module.functions[funcIdx.idx]
                        runtime.runInternal(module, func)
                    }

                    StructGet -> {
                        /*val typeIdx = */code[ip++]
                        val fieldIdx = code[ip++]

                        val struct = stack.pop() as WasmStruct
                        stack.push(struct.fields[fieldIdx])
                    }

                    StructSet -> {
                        /*val typeIdx = */code[ip++]
                        val fieldIdx = code[ip++]
                        val value = stack.pop()

                        val struct = stack.pop() as WasmStruct
                        struct.fields[fieldIdx] = value
                    }

                    ArrayGet -> {
                        /*val typeIdx = */code[ip++]
                        val index = stack.pop() as Int
                        val array = stack.pop() as Array<Any?>
                        stack.push(array[index])
                    }

                    ArraySet -> {
                        /*val typeIdx = */code[ip++]
                        val value = stack.pop()
                        val index = stack.pop() as Int
                        val array = stack.pop() as Array<Any?>
                        array[index] = value
                    }

                    RefCast -> {
                        // todo: validate cast is allowed
                        val typeIdx = code[ip++]
                        val type = module.types[typeIdx].compType
                        val value = stack.pop()
                        val nullsAllowed = instruction and 0xFF00 == 0

                        if (value == null && nullsAllowed) {
                            stack.push(null)
                        } else {
                            require(value != null) {
                                "ref.cast value must be non-null"
                            }
                            when (type) {
                                is WasmStructType -> {
                                    value as WasmStruct
                                    stack.push(value.cast(type))
                                }
                                is WasmArrayType -> {
                                    value as Array<*>
                                    stack.push(value)
                                }
                                else -> error(
                                    "Only struct and array types are supported for ref.cast for now, got $type"
                                )
                            }
                        }
                    }

                    If -> {
                        val typeIdx = code[ip++]
                        val elseOffset = code[ip++]
                        val endOffset = code[ip++]
                        val type = typeIdx.type(module)

                        val condition = stack.pop() as Int
                        stack.pushLabel(
                            type.inputArity(),
                            type.outputArity(),
                            endOffset
                        )

                        if (condition == 0) {
                            ip = elseOffset
                        }
                    }

                    End -> {
                        stack.popLabel(0)
                    }

                    Jump -> {
                        val offset = code[ip]
                        ip = offset
                    }

                    Drop -> {
                        stack.pop()
                    }

                    Select -> {
                        val condition = stack.pop() as Int
                        val val2 = stack.pop()
                        val val1 = stack.pop()
                        if (condition != 0) {
                            stack.push(val1)
                        } else {
                            stack.push(val2)
                        }
                    }

                    Block -> {
                        val typeIdx = code[ip++]
                        val type = typeIdx.type(module)
                        val endOffset = code[ip++]
                        stack.pushLabel(
                            type.inputArity(),
                            type.outputArity(),
                            endOffset
                        )
                    }

                    Try -> {
                        val typeIdx = code[ip++]
                        val type = typeIdx.type(module)
                        val endOffset = code[ip++]
                        val catchOffset = code[ip++]
                        stack.pushLabel(
                            type.inputArity(),
                            type.outputArity(),
                            endOffset
                        )
                        // todo: implement try-catch
                    }

                    Loop -> {
                        val typeIdx = code[ip++]
                        val type = typeIdx.type(module)
                        stack.pushLabel(
                            type.inputArity(),
                            type.outputArity(),
                            ip - 2 // loop instruction
                        )
                    }

                    Catch -> {
                        error("Must be unreachable")
                    }

                    Branch -> {
                        val labelIdx = code[ip]
                        val label = stack.popLabel(labelIdx)
                        ip = label.end
                    }

                    BranchIf -> {
                        val labelIdx = code[ip++]
                        val condition = stack.pop() as Int
                        if (condition != 0) {
                            val label = stack.popLabel(labelIdx)
                            ip = label.end
                        }
                    }

                    MemSize -> {
                        stack.push(
                            module.memory[0].bytes.size / MemPageSize
                        )
                    }

                    Return -> {
                        return
                    }

                    Unreachable -> {
                        error("This path should not be reached!")
                    }

                    else -> error("Unknown instruction: ${op.toHexString()}")
                }
            }
        } catch (e: Exception) {
            throw ExecutionException(instructionOffset, e)
        }
    }

    private fun Int.type(module: WasmModule): Any =
        if (this < 0) {
            WasmValueType.toValueType(this)
        } else {
            module.types[this].compType
        }
    private fun Any.inputArity(): Int =
        when (this) {
            is WasmFuncType -> params.size
            else -> 0
        }
    private fun Any.outputArity(): Int =
        when (this) {
            is WasmFuncType -> returns.size
            WasmValueType.Empty -> 0
            else -> 1
        }

    internal fun evaluateConstExpr(context: WasmModule, type: Int, expr: WasmExpr): Any? {
        val stack = runtime.stack
        stack.pushFrame(1, emptyArray())
        executeFrame(context, stack.currentFrame(), expr.code)
        stack.popFrame()
        return stack.pop()
    }
}

class ExecutionException(val offset: Int, override val cause: Exception) : Exception()

@Suppress("NOTHING_TO_INLINE")
internal inline fun Boolean.toInt() = if (this) 1 else 0
