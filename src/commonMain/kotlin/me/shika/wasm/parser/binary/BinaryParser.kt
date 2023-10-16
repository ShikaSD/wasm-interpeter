package me.shika.wasm.parser.binary

import me.shika.wasm.WasmExport
import me.shika.wasm.WasmExportDesc
import me.shika.wasm.WasmExpr
import me.shika.wasm.WasmFuncBody
import me.shika.wasm.WasmFuncIdx
import me.shika.wasm.WasmFieldType
import me.shika.wasm.WasmMemType
import me.shika.wasm.WasmTableType
import me.shika.wasm.WasmGlobal
import me.shika.wasm.WasmGlobalIdx
import me.shika.wasm.WasmImport
import me.shika.wasm.WasmImportDesc
import me.shika.wasm.WasmLimits
import me.shika.wasm.WasmFuncLocal
import me.shika.wasm.WasmMemIdx
import me.shika.wasm.WasmModuleData
import me.shika.wasm.WasmModuleElement
import me.shika.wasm.WasmModuleInitMode
import me.shika.wasm.WasmRecursiveType
import me.shika.wasm.WasmTableIdx
import me.shika.wasm.WasmTagIdx
import me.shika.wasm.WasmType
import me.shika.wasm.WasmValueType
import me.shika.wasm.parser.binary.internal.BinaryExpressionParser
import me.shika.wasm.parser.binary.internal.ByteBuffer
import me.shika.wasm.parser.binary.internal.readU32

class BinaryParserState {
    var types: Array<WasmRecursiveType>? = null
    var imports: Array<WasmImport>? = null
    var funcIdx: IntArray? = null
    var tables: Array<WasmTableType>? = null
    var memory: Array<WasmMemType>? = null
    var globals: Array<WasmGlobal>? = null
    var exports: Array<WasmExport>? = null
    var startFuncIdx: Int? = null
    var elements: Array<WasmModuleElement>? = null
    var dataCount: Int? = null
    var code: Array<WasmFuncBody>? = null
    var data: Array<WasmModuleData>? = null
    var tags: IntArray? = null
}

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

@OptIn(ExperimentalStdlibApi::class)
private class BinaryParser(private val state: BinaryParserState) {
    private val expressionParser = BinaryExpressionParser(state)

    fun parseBinary(buffer: ByteBuffer) {
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

    private fun BinaryParserState.parseNextSection(buffer: ByteBuffer) {
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
                    require(funcIdx == null) {
                        "Encountered repeated function section."
                    }
                    funcIdx = buffer.parseFunctionSection()
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

    private fun ByteBuffer.parseTypeSection(): Array<WasmRecursiveType> {
        val typeCount = readU32()
        return Array(typeCount) {
            with(expressionParser) { parseType() }
        }
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

    private fun ByteBuffer.parseTableSection(): Array<WasmTableType> {
        val tableCount = readU32()
        return Array(tableCount) { parseTableType() }
    }

    private fun ByteBuffer.parseMemorySection(): Array<WasmMemType> {
        val memCount = readU32()
        return Array(memCount) { parseMemType() }
    }

    private fun ByteBuffer.parseGlobalSection(): Array<WasmGlobal> {
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

    private fun ByteBuffer.parseElementsSection(): Array<WasmModuleElement> {
        val elementCount = readU32()
        return Array(elementCount) {
            when (val elementType = readU32()) {
                // TODO: consider smart bitfield hacks:
                //  (https://webassembly.github.io/spec/core/binary/modules.html#element-section)
                0 -> {
                    val expr = parseConstantExpr()
                    WasmModuleElement(
                        WasmValueType.FuncRef.toInt(),
                        parseFuncIdxIntoInitExpr(),
                        WasmModuleInitMode.Active(0, expr)
                    )
                }

                1 -> {
                    val type = parseModuleElemKind()
                    WasmModuleElement(
                        type.toInt(),
                        parseFuncIdxIntoInitExpr(),
                        WasmModuleInitMode.Passive
                    )
                }

                2 -> {
                    val tableIdx = parseIdx()
                    val expr = parseConstantExpr()
                    val type = parseModuleElemKind()
                    WasmModuleElement(
                        type.toInt(),
                        parseFuncIdxIntoInitExpr(),
                        WasmModuleInitMode.Active(tableIdx, expr)
                    )
                }

                3 -> {
                    val type = parseModuleElemKind()
                    WasmModuleElement(
                        type.toInt(),
                        parseFuncIdxIntoInitExpr(),
                        WasmModuleInitMode.Declarative
                    )
                }

                4 -> {
                    val expr = parseConstantExpr()
                    WasmModuleElement(
                        WasmValueType.FuncRef.toInt(),
                        parseFuncIdxIntoInitExpr(),
                        WasmModuleInitMode.Active(0, expr)
                    )
                }

                5 -> {
                    val type = parseRefType()
                    val expr = Array(readU32()) { parseExpr() }
                    WasmModuleElement(
                        type.toInt(),
                        expr,
                        WasmModuleInitMode.Passive
                    )
                }

                6 -> {
                    val tableIdx = parseIdx()
                    val modeExpr = parseConstantExpr()
                    val type = parseRefType()
                    val expr = Array(readU32()) { parseExpr() }
                    WasmModuleElement(
                        type.toInt(),
                        expr,
                        WasmModuleInitMode.Active(tableIdx, modeExpr)
                    )
                }

                7 -> {
                    val type = parseRefType()
                    val expr = Array(readU32()) { parseExpr() }
                    WasmModuleElement(
                        type.toInt(),
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

    private fun ByteBuffer.parseGlobal(): WasmGlobal {
        val globalType = parseFieldType()
        val expr = parseExpr()
        return WasmGlobal(globalType, expr)
    }

    private fun ByteBuffer.parseImportDesc(): WasmImportDesc {
        val byte = readByte()
        return when (byte.toInt()) {
            0x00 -> WasmFuncIdx(parseIdx())
            0x01 -> parseTableType()
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

    private fun ByteBuffer.parseMemType(): WasmMemType =
        WasmMemType(parseLimits())

    private fun ByteBuffer.parseTableType(): WasmTableType =
        WasmTableType(parseRefType(), parseLimits())

    private fun ByteBuffer.parseIdx(): Int =
        readU32()

    private fun ByteBuffer.parseName(): String {
        val length = readU32()
        return readString(length)
    }

    private fun ByteBuffer.parseLimits(): WasmLimits {
        val byte = readByte()
        return when (byte.toInt()) {
            0x00 -> WasmLimits(readU32(), Int.MAX_VALUE)
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

    private fun ByteBuffer.parseLocals(): Array<WasmFuncLocal> =
        Array(readU32()) {
            WasmFuncLocal(readU32(), parseValueType())
        }

    private fun ByteBuffer.parseRefType(): Int =
        with(expressionParser) { parseRefType() }

    private fun ByteBuffer.parseFieldType(): WasmFieldType =
        with(expressionParser) { parseFieldType() }

    private fun ByteBuffer.parseValueType(): Int =
        with(expressionParser) { parseValueType() }

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
}

fun parseBinary(buffer: ByteBuffer): BinaryParserState {
    val state = BinaryParserState()
    val parser = BinaryParser(state)
    parser.parseBinary(buffer)
    return state
}



