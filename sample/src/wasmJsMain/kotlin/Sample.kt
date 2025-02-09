import kotlinx.coroutines.delay

suspend fun main() {
    while (true) {
        delay(1000)
        println("Hello world!")
    }
}
