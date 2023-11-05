package me.shika.wasm.def

class WasmModuleDef {
    var types: Array<WasmType>? = null
    var imports: Array<WasmImport>? = null
    var funcTypeIdx: IntArray? = null
    var tables: Array<WasmTableDef>? = null
    var memory: Array<WasmMemoryDef>? = null
    var globals: Array<WasmGlobalDef>? = null
    var exports: Array<WasmExport>? = null
    var startFuncIdx: Int? = null
    var elements: Array<WasmElementDef>? = null
    var dataCount: Int? = null
    var code: Array<WasmFuncBody>? = null
    var data: Array<WasmModuleData>? = null
    var tags: IntArray? = null
}

class WasmType(
    val compType: WasmCompositeType,
    val types: IntArray,
    val final: Boolean
)

sealed interface WasmCompositeType
class WasmArrayType(val type: WasmFieldType) : WasmCompositeType
class WasmStructType(val fields: Array<WasmFieldType>) : WasmCompositeType

class WasmFuncType(
    val params: IntArray,
    val returns: IntArray,
) : WasmCompositeType

class WasmFuncLocal(
    val idx: Int,
    val type: Int
)

class WasmFuncBody(
    val locals: Array<WasmFuncLocal>,
    val body: WasmExpr
)

@Suppress("EnumEntryName")
enum class WasmValueType {
    @Suppress("unused")
    Zero,

    // Packed types
    i8,
    i16,

    // Primitive types
    i32,
    i64,
    f32,
    f64,

    // Vector type
    v128,

    // Ref types
    FuncRef,
    ExternRef,
    None,
    AnyRef,
    EqRef,
    Struct,
    Any,
    i31,
    Array,
    NullRef,
    NullExternRef,

    // Absence of a type
    Empty;

    fun toInt(): Int = -ordinal
}

class WasmImport(val moduleName: String, val name: String, val desc: WasmImportDesc)
class WasmExport(val name: String, val desc: WasmExportDesc)

sealed interface WasmImportDesc
sealed interface WasmExportDesc
class WasmFuncIdx(val typeIdx: Int) : WasmImportDesc, WasmExportDesc
class WasmTagIdx(val typeIdx: Int) : WasmImportDesc, WasmExportDesc
class WasmTableIdx(val typeIdx: Int) : WasmExportDesc
class WasmMemIdx(val typeIdx: Int) : WasmExportDesc
class WasmGlobalIdx(val typeIdx: Int) : WasmExportDesc
class WasmTableDef(val refType: Int, val initSize: Int, val maxSize: Int, val initExpr: WasmExpr?) : WasmImportDesc
class WasmMemoryDef(val initSize: Int, val maxSize: Int) : WasmImportDesc
class WasmFieldType(val type: Int, val mutable: Boolean) : WasmImportDesc

class WasmLimits(val min: Int, val max: Int)

class WasmGlobalDef(val type: WasmFieldType, val init: WasmExpr)

class WasmElementDef(val type: Int, val init: Array<out WasmExpr>, val mode: WasmModuleInitMode)
sealed class WasmModuleInitMode {
    data object Passive : WasmModuleInitMode()
    data class Active(val idx: Int, val offset: WasmExpr) : WasmModuleInitMode()
    data object Declarative : WasmModuleInitMode()
}

class WasmModuleData(val bytes: ByteArray, val mode: WasmModuleInitMode)

class WasmExpr(val code: IntArray)

internal const val MemPageSize = 65536
