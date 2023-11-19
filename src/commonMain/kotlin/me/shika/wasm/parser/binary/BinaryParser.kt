package me.shika.wasm.parser.binary

import me.shika.wasm.def.MemPageSize
import me.shika.wasm.def.WasmExport
import me.shika.wasm.def.WasmExportDesc
import me.shika.wasm.def.WasmExpr
import me.shika.wasm.def.WasmFuncBody
import me.shika.wasm.def.WasmFuncIdx
import me.shika.wasm.def.WasmFieldType
import me.shika.wasm.def.WasmMemoryDef
import me.shika.wasm.def.WasmTableDef
import me.shika.wasm.def.WasmGlobalDef
import me.shika.wasm.def.WasmGlobalIdx
import me.shika.wasm.def.WasmImport
import me.shika.wasm.def.WasmImportDesc
import me.shika.wasm.def.WasmLimits
import me.shika.wasm.def.WasmFuncLocal
import me.shika.wasm.def.WasmMemIdx
import me.shika.wasm.def.WasmModuleData
import me.shika.wasm.def.WasmModuleDef
import me.shika.wasm.def.WasmElementDef
import me.shika.wasm.def.WasmModuleInitMode
import me.shika.wasm.def.WasmTableIdx
import me.shika.wasm.def.WasmTagIdx
import me.shika.wasm.def.WasmType
import me.shika.wasm.def.WasmValueType
import me.shika.wasm.parser.binary.internal.ByteBuffer
import me.shika.wasm.parser.binary.internal.readU32

@OptIn(ExperimentalStdlibApi::class)
private class BinaryParser(private val state: WasmModuleDef) {
    private val typeParser = BinaryTypeParser()
    private val expressionParser = BinaryExpressionParser(typeParser)

    fun parseAndValidateBinary(buffer: ByteBuffer) {
        val magic = buffer.readInt()
        require(magic == 0x6D736100) {
            "Expected magic to be 0x6D736100, but it was ${magic.toHexString()}"
        }

        val version = buffer.readInt()
        require(version == 0x1) {
            "Expected version to be 0x1, but it was ${version.toHexString()}"
        }

        while (buffer.position < buffer.size) {
            with(state) { parseNextSection(buffer) }
        }
    }

    private fun WasmModuleDef.parseNextSection(buffer: ByteBuffer) {
        val sectionIdByte = buffer.readByte()
        val sectionType = SectionType.entries.firstOrNull { it.byte == sectionIdByte }
        require(sectionType != null) {
            "Unknown section id: $sectionIdByte"
        }
        val sectionSize = buffer.readU32()
        val position = buffer.position

        try {
            when (sectionType) {
                SectionType.Custom -> {
                    buffer.offset(sectionSize)
                    // todo: log?
                }

                SectionType.Type -> {
                    require(types == null) {
                        "Encountered repeated types section."
                    }
                    types = buffer.parseTypeSection()
                }

                SectionType.Import -> {
                    require(imports == null) {
                        "Encountered repeated import section."
                    }
                    imports = buffer.parseImportSection()
                }

                SectionType.Function -> {
                    require(funcTypeIdx == null) {
                        "Encountered repeated function section."
                    }
                    funcTypeIdx = buffer.parseFunctionSection()
                }

                SectionType.Table -> {
                    require(tables == null) {
                        "Encountered repeated function section."
                    }
                    tables = buffer.parseTableSection()
                }

                SectionType.Memory -> {
                    require(memory == null) {
                        "Encountered repeated function section."
                    }
                    memory = buffer.parseMemorySection()
                }

                SectionType.Global -> {
                    require(globals == null) {
                        "Encountered repeated function section."
                    }
                    globals = buffer.parseGlobalSection()
                }

                SectionType.Export -> {
                    require(exports == null) {
                        "Encountered repeated export section."
                    }
                    exports = buffer.parseExportSection()
                }

                SectionType.Start -> {
                    require(startFuncIdx == null) {
                        "Encountered repeated start section."
                    }
                    startFuncIdx = buffer.parseStartSection()
                }

                SectionType.Element -> {
                    require(elements == null) {
                        "Encountered repeated elements section."
                    }
                    elements = buffer.parseElementsSection()
                }

                SectionType.Code -> {
                    require(code == null) {
                        "Encountered repeated code section."
                    }
                    code = buffer.parseCodeSection()
                }

                SectionType.Data -> {
                    require(data == null) {
                        "Encountered repeated data section."
                    }
                    data = buffer.parseDataSection().also {
                        require(it.size == dataCount) {
                            "Data count does not match data section size."
                        }
                    }
                }

                SectionType.DataCount -> {
                    require(dataCount == null) {
                        "Encountered repeated data count section."
                    }
                    dataCount = buffer.parseDataCountSection()
                }

                SectionType.Tag -> {
                    require(tags == null) {
                        "Encountered repeated tag section"
                    }
                    tags = buffer.parseTagSection()
                }
            }
        } catch (e: Exception) {
            val offset = buffer.position - position
            buffer.offset(-offset)
            throw IllegalStateException(
                "Error while parsing section $sectionType at offset ${offset}, the data is: ${buffer.dump(sectionSize)}",
                e
            )
        }

        require(buffer.position - position == sectionSize) {
            "Section $sectionType is malformed, read ${buffer.position - position}, expected size is $sectionSize"
        }
    }

    private fun ByteBuffer.dump(length: Int): String =
        buildString {
            append("[")
            repeat(length) { append("0x${readByte().toHexString(HexFormat.UpperCase)}, ") }
            append("]")
        }

    private fun ByteBuffer.parseTypeSection(): Array<WasmType> {
        val types = mutableListOf<WasmType>()
        repeat(readU32()) {
            val type = with(typeParser) { parseType() }
            types.addAll(type)
        }
        return types.toTypedArray()
    }

    private fun ByteBuffer.parseImportSection(): Array<WasmImport> {
        val importCount = readU32()
        return Array(importCount) {
            val moduleName = parseName()
            val name = parseName()
            val desc = parseImportDesc()
            WasmImport(moduleName, name, desc)
        }
    }

    private fun ByteBuffer.parseFunctionSection(): IntArray {
        val funcCount = readU32()
        return IntArray(funcCount) { readU32() }
    }

    private fun ByteBuffer.parseTableSection(): Array<WasmTableDef> {
        val tableCount = readU32()
        return Array(tableCount) { parseTableDef() }
    }

    private fun ByteBuffer.parseMemorySection(): Array<WasmMemoryDef> {
        val memCount = readU32()
        return Array(memCount) { parseMemType() }
    }

    private fun ByteBuffer.parseGlobalSection(): Array<WasmGlobalDef> {
        val globalCount = readU32()
        return Array(globalCount) { parseGlobal() }
    }

    private fun ByteBuffer.parseExportSection(): Array<WasmExport> {
        val exportCount = readU32()
        return Array(exportCount) {
            val name = parseName()
            val desc = parseExportDesc()
            WasmExport(name, desc)
        }
    }

    private fun ByteBuffer.parseStartSection(): Int {
        return readU32()
    }

    private fun ByteBuffer.parseElementsSection(): Array<WasmElementDef> {
        val elementCount = readU32()
        return Array(elementCount) {
            when (val elementType = readU32()) {
                // TODO: consider smart bitfield hacks:
                //  (https://webassembly.github.io/spec/core/binary/modules.html#element-section)
                0 -> {
                    val expr = parseConstantExpr()
                    WasmElementDef(
                        WasmValueType.FuncRef.toInt(),
                        parseFuncIdxIntoInitExpr(),
                        WasmModuleInitMode.Active(0, expr)
                    )
                }

                1 -> {
                    val type = parseModuleElemKind()
                    WasmElementDef(
                        type.toInt(),
                        parseFuncIdxIntoInitExpr(),
                        WasmModuleInitMode.Passive
                    )
                }

                2 -> {
                    val tableIdx = parseIdx()
                    val expr = parseConstantExpr()
                    val type = parseModuleElemKind()
                    WasmElementDef(
                        type.toInt(),
                        parseFuncIdxIntoInitExpr(),
                        WasmModuleInitMode.Active(tableIdx, expr)
                    )
                }

                3 -> {
                    val type = parseModuleElemKind()
                    WasmElementDef(
                        type.toInt(),
                        parseFuncIdxIntoInitExpr(),
                        WasmModuleInitMode.Declarative
                    )
                }

                4 -> {
                    val expr = parseConstantExpr()
                    WasmElementDef(
                        WasmValueType.FuncRef.toInt(),
                        parseFuncIdxIntoInitExpr(),
                        WasmModuleInitMode.Active(0, expr)
                    )
                }

                5 -> {
                    val type = parseRefType()
                    val expr = Array(readU32()) { parseExpr() }
                    WasmElementDef(
                        type,
                        expr,
                        WasmModuleInitMode.Passive
                    )
                }

                6 -> {
                    val tableIdx = parseIdx()
                    val modeExpr = parseConstantExpr()
                    val type = parseRefType()
                    val expr = Array(readU32()) { parseExpr() }
                    WasmElementDef(
                        type,
                        expr,
                        WasmModuleInitMode.Active(tableIdx, modeExpr)
                    )
                }

                7 -> {
                    val type = parseRefType()
                    val expr = Array(readU32()) { parseExpr() }
                    WasmElementDef(
                        type,
                        expr,
                        WasmModuleInitMode.Declarative
                    )
                }

                else -> {
                    error("Unknown element type in element section: $elementType")
                }
            }
        }
    }

    private fun ByteBuffer.parseDataCountSection(): Int =
        readU32()

    private fun ByteBuffer.parseCodeSection(): Array<WasmFuncBody> =
        Array(readU32()) {
            parseFuncBody()
        }

    private fun ByteBuffer.parseDataSection(): Array<WasmModuleData> =
        Array(readU32()) {
            when (val type = readU32()) {
                0 -> {
                    val expr = parseConstantExpr()
                    val bytes = readBytes()
                    WasmModuleData(bytes, WasmModuleInitMode.Active(0, expr))
                }
                1 -> {
                    val bytes = readBytes()
                    WasmModuleData(bytes, WasmModuleInitMode.Passive)
                }
                2 -> {
                    val memIdx = parseIdx()
                    val expr = parseConstantExpr()
                    val bytes = readBytes()
                    WasmModuleData(bytes, WasmModuleInitMode.Active(memIdx, expr))
                }
                else -> {
                    error("Unknown data type: $type")
                }
            }
        }

    private fun ByteBuffer.parseTagSection(): IntArray =
        IntArray(readU32()) {
            val attribute = readByte()
            require(attribute == 0x0.toByte()) {
                "Only exception (0x0) tags are supported (got $attribute)"
            }
            readU32()
        }

    private fun ByteBuffer.parseGlobal(): WasmGlobalDef {
        val globalType = parseFieldType()
        val expr = parseExpr()
        return WasmGlobalDef(globalType, expr)
    }

    private fun ByteBuffer.parseImportDesc(): WasmImportDesc {
        val byte = readByte()
        return when (byte.toInt()) {
            0x00 -> WasmFuncIdx(parseIdx())
            0x01 -> parseTableDef()
            0x02 -> parseMemType()
            0x03 -> parseFieldType()
            0x04 -> WasmTagIdx(parseIdx())
            else -> error {
                "Unknown import desc type ${byte.toHexString()}"
            }
        }
    }

    private fun ByteBuffer.parseExportDesc(): WasmExportDesc {
        val byte = readByte()
        return when (byte.toInt()) {
            0x00 -> WasmFuncIdx(parseIdx())
            0x01 -> WasmTableIdx(parseIdx())
            0x02 -> WasmMemIdx(parseIdx())
            0x03 -> WasmGlobalIdx(parseIdx())
            0x04 -> WasmTagIdx(parseIdx())
            else -> error {
                "Unknown export desc type ${byte.toHexString()}"
            }
        }
    }

    private fun ByteBuffer.parseMemType(): WasmMemoryDef {
        val limits = parseLimits()
        return WasmMemoryDef(initSize = limits.min, maxSize = limits.max)
    }

    private fun ByteBuffer.parseTableDef(): WasmTableDef {
        val byte = peekByte().toInt()
        when (byte) {
            0x40 -> {
                readByte().also {
                    check(it == 0x40.toByte()) { "expected 0x40, got ${it.toHexString()}" }
                }
                readByte().also {
                    check(it == 0x00.toByte()) { "expected 0x00, got ${it.toHexString()}" }
                }
                val type = parseRefType()
                val limits = parseLimits()
                val expr = parseExpr()
                return WasmTableDef(
                    type,
                    limits.min,
                    limits.max,
                    expr
                )
            }
            else -> {
                val type = parseRefType()
                val limits = parseLimits()
                return WasmTableDef(
                    type,
                    limits.min,
                    limits.max,
                    WasmExpr(IntArray(0))
                )
            }
        }
    }

    private fun ByteBuffer.parseIdx(): Int =
        readU32()

    private fun ByteBuffer.parseName(): String {
        val length = readU32()
        return readString(length)
    }

    private fun ByteBuffer.parseLimits(): WasmLimits {
        val byte = readByte()
        return when (byte.toInt()) {
            0x00 -> WasmLimits(readU32(), Int.MAX_VALUE / MemPageSize)
            0x01 -> WasmLimits(readU32(), readU32())
            else -> error {
                "Unexpected limits type ${byte.toHexString()}"
            }
        }
    }

    private fun ByteBuffer.parseFuncBody(): WasmFuncBody {
        val bodySize = readU32()
        val startPosition = position

        return WasmFuncBody(
            parseLocals(),
            parseExpr(expectedSize = bodySize - (position - startPosition))
        ).also {
            check(startPosition + bodySize == position) {
                val size = position - startPosition
                offset(-(size))
                "Malformed function body (expected: $bodySize, actual: ${size}, bytes: ${dump(bodySize)})"
            }
        }
    }

    private fun ByteBuffer.parseLocals(): IntArray {
        val localCount = readU32()
        val list = mutableListOf<Int>()
        repeat(localCount) {
            val count = readU32()
            val type = parseValueType()
            repeat(count) {
                list.add(type)
            }
        }
        return list.toIntArray()
    }

    private fun ByteBuffer.parseRefType(): Int =
        with(typeParser) { parseRefType() }

    private fun ByteBuffer.parseFieldType(): WasmFieldType =
        with(typeParser) { parseFieldType() }

    private fun ByteBuffer.parseValueType(): Int =
        with(typeParser) { parseValueType() }

    private fun ByteBuffer.parseExpr(expectedSize: Int = Int.MAX_VALUE): WasmExpr =
        with(expressionParser) { parseExpression(const = false, expectedSize) }

    private fun ByteBuffer.parseConstantExpr(expectedSize: Int = Int.MAX_VALUE): WasmExpr =
        with(expressionParser) { parseExpression(const = true, expectedSize) }

    private fun ByteBuffer.parseFuncIdxIntoInitExpr(): Array<WasmExpr> =
        Array(readU32()) { with(expressionParser) { parseFuncRefExpr() } }

    private fun ByteBuffer.parseModuleElemKind(): WasmValueType {
        val kind = readByte()
        check(kind.toInt() == 0x00) {
            "Only 0x00 (funcref) is supported as elemkind in element segment."
        }
        return WasmValueType.FuncRef
    }

    private fun ByteBuffer.readBytes(): ByteArray =
        ByteArray(readU32()) { readByte() }

    private enum class SectionType(val byte: Byte) {
        Custom(0),
        Type(1),
        Import(2),
        Function(3),
        Table(4),
        Memory(5),
        Global(6),
        Export(7),
        Start(8),
        Element(9),
        Code(10),
        Data(11),
        DataCount(12),
        Tag(13),
    }
}

fun parseBinary(buffer: ByteBuffer): WasmModuleDef {
    val def = WasmModuleDef()
    val parser = BinaryParser(def)
    parser.parseAndValidateBinary(buffer)
    return def
}



