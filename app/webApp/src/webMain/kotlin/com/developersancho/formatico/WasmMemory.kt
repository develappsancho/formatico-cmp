package com.developersancho.formatico

/**
 * Hands Compose's resource reader the Wasm memory the way Kotlin supports, before the first resource is read; nothing
 * in the JavaScript build, which has no such memory.
 */
internal expect fun shareWasmMemoryWithResources()
