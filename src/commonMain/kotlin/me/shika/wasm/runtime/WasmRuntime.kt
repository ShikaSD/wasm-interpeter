package me.shika.wasm.runtime

import me.shika.wasm.def.MemPageSize
import me.shika.wasm.def.WasmExportDesc
import me.shika.wasm.def.WasmFieldType
import me.shika.wasm.def.WasmFuncBody
import me.shika.wasm.def.WasmFuncIdx
import me.shika.wasm.def.WasmFuncType
import me.shika.wasm.def.WasmImport
import me.shika.wasm.def.WasmMemoryDef
import me.shika.wasm.def.WasmModuleData
import me.shika.wasm.def.WasmModuleDef
import me.shika.wasm.def.WasmModuleInitMode
import me.shika.wasm.def.WasmStructType
import me.shika.wasm.def.WasmTableDef
import me.shika.wasm.def.WasmTagIdx
import me.shika.wasm.def.WasmType
import me.shika.wasm.def.WasmValueType
import me.shika.wasm.intrinsics.createString

sealed interface WasmControl {
    data class Frame(
        val arity: Int,
        val locals: Array<Any?>,
        val stackPtr: Int
    ) : WasmControl

    data class Label(
        val arity: Int,
        val stackPtr: Int,
        val end: Int
    ) : WasmControl
}

class WasmRuntimeStack {
    private val stack: Array<Any?> = arrayOfNulls<Any?>(MAX_STACK_SIZE)
    private val controls: ArrayDeque<WasmControl> = ArrayDeque()

    private var top = 0
    private var frameIdx = -1

    fun push(ref: Any?) {
        stack[top++] = ref
    }

    fun pop(): Any? {
        require(top > 0) {
            "Stack is empty"
        }
        return stack[--top].also {
            stack[top] = null
        }
    }

    fun peek(): Any? {
        return stack[top - 1]
    }

    fun pushFrame(arity: Int, locals: Array<Any?>) {
        controls.addLast(
            WasmControl.Frame(arity = arity, locals = locals, stackPtr = top)
        )
        frameIdx = controls.size - 1
    }

    fun currentFrame(): WasmControl.Frame =
        controls[frameIdx] as WasmControl.Frame

    fun popFrame() {
        var frame = controls.removeLast()
        while (frame !is WasmControl.Frame) {
            frame = controls.removeLast()
        }
        updateStack(frame.stackPtr, frame.arity)
        frameIdx = controls.indexOfLast { it is WasmControl.Frame }
    }

    fun pushLabel(input: Int, output: Int, end: Int) {
        top -= input
        if (input > 0) {
            stack.fill(null, top, top + input)
        }
        val label = WasmControl.Label(
            arity = output,
            stackPtr = top,
            end = end
        )
        controls.addLast(label)
//        println("pushing $label")
    }

    fun popLabel(labelIndex: Int): WasmControl.Label {
        var count = labelIndex
        var label = controls.removeLast()
        while (label is WasmControl.Label && count-- > 0) {
            label = controls.removeLast()
        }
        require(label is WasmControl.Label) {
            "Label with index $labelIndex not found"
        }
        updateStack(label.stackPtr, label.arity)
//        println("popping $label")
        return label
    }

    private fun updateStack(stackPtr: Int, arity: Int) {
        val oldTop = top
        top = stackPtr + arity
        if (top < oldTop) {
            stack.copyInto(
                destination = stack,
                destinationOffset = stackPtr + 1,
                startIndex = oldTop - arity,
                endIndex = oldTop
            )
            stack.fill(null, top, oldTop)
        }
    }

    fun print(): String = buildString {
        appendLine("Stack: ")
        for (i in top - 1 downTo 0) {
            append(stack[i])
            appendLine()
        }
    }

    companion object {
        private const val MAX_STACK_SIZE = 512
    }
}

class WasmRuntime {
    val modules: MutableMap<String, WasmModule> = mutableMapOf()
    val stack = WasmRuntimeStack()
    val executor = WasmExecutor(this)

    fun instantiate(name: String, moduleDef: WasmModuleDef): WasmModule =
        WasmModule(this, moduleDef).also {
            modules[name] = it
        }

    fun run(module: WasmModule, funcName: String, vararg args: Any?): Any? {
        val funcIdx = module.exports[funcName]
        if (funcIdx !is WasmFuncIdx) {
            error("Function $funcName is not exported")
        }
        try {
            return when (val function = module.functions[funcIdx.idx]) {
                is WasmLocalFunction -> {
                    args.forEach {
                        // todo: check host types against function type
                        stack.push(it)
                    }
                    executor.execute(module, function)
                    if (function.type.returns.isNotEmpty()) {
                        stack.pop()
                    } else {
                        Unit
                    }
                }

                is WasmExternalFunction -> TODO("external function call")
            }
        } catch (e: Exception) {
            println(stack.print())
            throw e
        }
    }

    internal fun runInternal(module: WasmModule, function: WasmFunction) {
        when (function) {
            is WasmLocalFunction -> executor.execute(module, function)
            is WasmExternalFunction -> when (val moduleName = function.declaration.moduleName) {
                "js_code" -> {
                    when (val name = function.declaration.name) {
                        "kotlin.wasm.internal.importStringFromWasm" -> {
                            val memory = module.memory[0]
                            val prefix = stack.pop() as? String
                            val length = stack.pop() as Int
                            val offset = stack.pop() as Int
                            val string = createString(memory.bytes, offset, length)
                            stack.push(if (prefix == null) string else prefix + string)
                        }
                        "kotlin.wasm.internal.isNullish" -> {
                            val value = stack.pop()
                            stack.push((value == null).toInt())
                        }
                        "kotlin.io.printlnImpl" -> {
                            val value = stack.pop()
                            println(value)
                        }
                        else -> error("Unknown function $name")
                    }
                }
                else -> error("External module calls are supported only for intrinsics (called $moduleName)")
            }
        }
    }
}

class WasmModule(
    env: WasmRuntime,
    def: WasmModuleDef
) {
    val types: Array<WasmType>
    val functions: Array<WasmFunction>
    val tables: Array<WasmTable>
    val memory: Array<WasmMemory>
    val globals: Array<WasmGlobal>
    val elems: Array<WasmElement>
    val data: Array<WasmModuleData?>
    val tags: Array<WasmType>
    val exports: Map<String, WasmExportDesc>

    init {
        types = def.types ?: emptyArray()
        val funcTypeIdx = def.funcTypeIdx ?: EmptyIntArray
        val funcBodies = def.code ?: emptyArray()
        val imports = def.imports ?: emptyArray()

        val funcImportCount = imports.count { it.desc is WasmFuncIdx }
        // stupid hack to avoid array of nullable
        @Suppress("UNCHECKED_CAST")
        functions = arrayOfNulls<WasmFunction?>(funcImportCount + funcTypeIdx.size) as Array<WasmFunction>
        var funcIdx = 0
        imports.forEach {
            if (it.desc is WasmFuncIdx) {
                functions[funcIdx++] = WasmExternalFunction(
                    types[it.desc.idx].compType as WasmFuncType,
                    it
                )
            }
        }

        funcTypeIdx.forEach { typeIdx ->
            val index = funcIdx++
            functions[index] = WasmLocalFunction(
                types[typeIdx].compType as WasmFuncType,
                funcBodies[index - funcImportCount]
            )
        }

        check(imports.none { it.desc is WasmTableDef }) { "Tables import not supported" }
        val tablesDef = def.tables ?: emptyArray()
        tables = Array(tablesDef.size) {
            val tableDef = tablesDef[it]
            val type = tableDef.refType
            WasmTable(
                type,
                tableDef.maxSize,
                arrayOfNulls(tableDef.initSize)
            )
        }

        check(imports.none { it.desc is WasmMemoryDef }) { "Memory import not supported" }
        val memoryDefs = def.memory ?: emptyArray()
        memory = Array(memoryDefs.size) {
            val memoryDef = memoryDefs[it]
            WasmMemory(
                memoryDef.maxSize,
                ByteArray(memoryDef.initSize * MemPageSize)
            )
        }

        check(imports.none { it.desc is WasmFieldType }) { "Global import not supported" }
        val globalsDef = def.globals  ?: emptyArray()
        globals = Array(globalsDef.size) {
            val globalDef = globalsDef[it]
            WasmGlobal(
                globalDef.type.mutable,
                globalDef.type.type,
                env.executor.evaluateConstExpr(this, globalDef.type.type, globalDef.init)
            )
        }

        val elemsDef = def.elements ?: emptyArray()
        elems = Array(elemsDef.size) {
            val elemDef = elemsDef[it]
            WasmElement(
                elemDef.type,
                TODO("element init is not implemented")
                //WasmRef(elemDef.type)
            )
        }

        val dataDef = def.data ?: emptyArray()
        data = Array(dataDef.size) {
            val data = dataDef[it]
            when (val mode = data.mode) {
                is WasmModuleInitMode.Active -> {
                    require(mode.idx == 0) { "Only memory index 0 is supported" }
                    val offset = env.executor.evaluateConstExpr(
                        this,
                        WasmValueType.i32.toInt(),
                        mode.offset
                    ) as Int
                    data.bytes.copyInto(memory[0].bytes, offset)
                    null
                }
                WasmModuleInitMode.Declarative -> TODO("Declarative data init is not supported")
                WasmModuleInitMode.Passive -> {
                    data
                }
            }
        }
        check(imports.none { it.desc is WasmTagIdx }) { "Tag import not supported" }
        val tagDef = def.tags ?: EmptyIntArray
        tags = Array(tagDef.size) {
            types[tagDef[it]]
        }

        val exportsDef = def.exports ?: emptyArray()
        exports = exportsDef.associate { it.name to it.desc }
    }

    companion object {
        private val EmptyIntArray = IntArray(0)
    }
}

sealed interface WasmFunction
class WasmLocalFunction(
    val type: WasmFuncType,
    val body: WasmFuncBody
) : WasmFunction

class WasmExternalFunction(
    val type: WasmFuncType,
    val declaration: WasmImport
) : WasmFunction

class WasmTable(
    val type: Int,
    val maxSize: Int,
    val refs: Array<Any?>
)

class WasmMemory(
    val maxPages: Int,
    val bytes: ByteArray
) {
    fun storeInt16(offset: Int, value: Short) {
        require(offset + 2 <= bytes.size) { "Memory access out of bounds" }
        bytes[offset] = value.toByte()
        bytes[offset + 1] = (value.toInt() shr 8).toByte()
    }
}

class WasmGlobal(
    val mutable: Boolean,
    val type: Int,
    var value: Any?
)

class WasmElement(
    val type: Int,
    val value: Any
)

class WasmStruct(
    val type: WasmStructType,
    val fields: Array<Any?>
) {
    fun cast(type: WasmStructType): WasmStruct = WasmStruct(type, fields)
}
