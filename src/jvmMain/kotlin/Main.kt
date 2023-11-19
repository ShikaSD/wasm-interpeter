import me.shika.wasm.parser.binary.internal.ByteBuffer
import me.shika.wasm.parser.binary.parseBinary
import me.shika.wasm.runtime.WasmRuntime
import java.io.File

fun main() {
    val file = File("/Users/shika/projects/kotlin-wasm-interpreter/sample/build/compileSync/wasmJs/main/developmentExecutable/kotlin/kotlin-wasm-interpreter-sample-wasm-js.wasm")
    val def = parseBinary(ByteBuffer.wrap(file.readBytes()))
    val runtime = WasmRuntime()
    val module = runtime.instantiate("main", def)
    runtime.run(module, "_initialize")
    println(module)
}
