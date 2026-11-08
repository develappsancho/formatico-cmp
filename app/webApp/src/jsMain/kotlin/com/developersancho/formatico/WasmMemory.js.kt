package com.developersancho.formatico

/** The JavaScript build reads its resources with no Wasm memory: there is nothing to hand over. */
internal actual fun shareWasmMemoryWithResources() = Unit
