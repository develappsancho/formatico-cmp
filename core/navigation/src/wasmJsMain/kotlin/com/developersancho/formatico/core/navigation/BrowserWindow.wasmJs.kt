package com.developersancho.formatico.core.navigation

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun browserWindow(): BrowserWindow = js("window")
