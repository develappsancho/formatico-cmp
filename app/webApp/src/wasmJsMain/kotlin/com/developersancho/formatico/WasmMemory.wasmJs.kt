@file:OptIn(ExperimentalWasmJsInterop::class)

package com.developersancho.formatico

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.wasm.unsafe.WebAssembly
import kotlin.wasm.unsafe.wasmMemory

/**
 * Compose Multiplatform 1.12.2 copies a resource's bytes (a typeface, a sample) into the Wasm memory through
 * `wasmExports.memory`. Kotlin 2.4.20 deprecated that way: its first use writes "Accessing `memory` via `wasmExports` is
 * deprecated" to the console as an error, at every start. The supported way is `kotlin.wasm.unsafe.wasmMemory`, which
 * Compose takes from 1.13.0 on (JetBrains/compose-multiplatform#5724): once Formatico is on it, this goes, with its call
 * in `main`. Pantrix UI's site does the same (its guide "web-compatibility" names it, without its code: a finding).
 */
internal actual fun shareWasmMemoryWithResources() = shareMemory(wasmMemory)

/**
 * The compiler writes the bodies of `js(...)` into one module, where they reach the memory through a variable called
 * `wasmExports`: in Kotlin 2.4.20 a proxy that writes the error on its first use. It is replaced here with a plain object
 * holding the same memory, taken the supported way. Where the variable is gone, or cannot be changed, nothing is done.
 */
@Suppress("UnusedParameter") // The JavaScript body reads it by its name.
private fun shareMemory(memory: WebAssembly.Memory): Unit = js(
    """{
        try {
            if (typeof wasmExports !== 'undefined') wasmExports = { memory: memory };
        } catch (unchangeable) {
        }
    }"""
)
