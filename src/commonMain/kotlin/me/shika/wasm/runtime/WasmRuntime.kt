package me.shika.wasm.runtime

import me.shika.wasm.def.WasmCompositeType
import me.shika.wasm.def.WasmExport
import me.shika.wasm.def.WasmFieldType
import me.shika.wasm.def.WasmFuncBody
import me.shika.wasm.def.WasmFuncIdx
import me.shika.wasm.def.WasmFuncType
import me.shika.wasm.def.WasmGlobalDef
import me.shika.wasm.def.WasmImport
import me.shika.wasm.def.WasmMemoryDef
import me.shika.wasm.def.WasmModuleData
import me.shika.wasm.def.WasmModuleDef
import me.shika.wasm.def.WasmTableDef
import me.shika.wasm.def.WasmTableIdx
import me.shika.wasm.def.WasmTagIdx
import me.shika.wasm.def.WasmType

class WasmEnvironment {
    val modules: MutableMap<String, WasmModule> = mutableMapOf()

    fun instantiate(name: String, moduleDef: WasmModuleDef): WasmModule =
        WasmModule(this, moduleDef).also {
            modules[name] = it
        }
}

class WasmModule(
    env: WasmEnvironment,
    def: WasmModuleDef
) {
    val functions: Array<WasmFunction>
    val tables: Array<WasmTable>
    val memory: Array<WasmMemory>
    val globals: Array<WasmGlobal>
    val elems: Array<WasmElement>
    val data: Array<WasmModuleData>
    val tags: Array<WasmType>

    init {
        val types = def.types ?: emptyArray()
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
                    types[it.desc.typeIdx].compType as WasmFuncType,
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
            val def = tablesDef[it]
            val type = types[def.refType].compType
            WasmTable(
                type,
                def.maxSize,
                Array(def.initSize) {
                    // todo: init
                    WasmRef()
                }
            )
        }

        check(imports.none { it.desc is WasmMemoryDef }) { "Memory import not supported" }
        val memoryDefs = def.memory ?: emptyArray()
        memory = Array(memoryDefs.size) {
            val memoryDef = memoryDefs[it]
            WasmMemory(
                memoryDef.maxSize,
                ByteArray(memoryDef.initSize)
            )
        }

        check(imports.none { it.desc is WasmFieldType }) { "Global import not supported" }
        val globalsDef = def.globals  ?: emptyArray()
        globals = Array(globalsDef.size) {
            val globalDef = globalsDef[it]
            WasmGlobal(
                globalDef.type,
                // todo: init
                WasmRef()
            )
        }

        val elemsDef = def.elements ?: emptyArray()
        elems = Array(elemsDef.size) {
            val elemDef = elemsDef[it]
            WasmElement(
                elemDef.type,
                // todo: init
                WasmRef()
            )
        }

        val dataDef = def.data ?: emptyArray()
        dataDef.forEach {
            // todo: init data in memory
        }
        data = dataDef

        check(imports.none { it.desc is WasmTagIdx }) { "Tag import not supported" }
        val tagDef = def.tags ?: EmptyIntArray
        tags = Array(tagDef.size) {
            types[tagDef[it]]
        }
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
    val type: WasmCompositeType,
    val maxSize: Int,
    val refs: Array<WasmRef>
)

class WasmMemory(
    val maxSize: Int,
    val bytes: ByteArray
)

class WasmGlobal(
    val type: WasmFieldType,
    val value: WasmRef
)

class WasmElement(
    val type: Int,
    val ref: WasmRef
)

class WasmRef {
// todo
}
