import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        binaries.executable()
        d8()
        applyBinaryen()
    }
}
