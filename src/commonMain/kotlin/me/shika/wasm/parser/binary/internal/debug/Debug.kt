package me.shika.wasm.parser.binary.internal.debug

@OptIn(ExperimentalStdlibApi::class)
fun Byte.debug(label: String): Byte {
    println("$label ${toHexString()}")
    return this
}
@OptIn(ExperimentalStdlibApi::class)
fun Int.debug(label: String): Int {
    println("$label ${toHexString()}")
    return this
}
