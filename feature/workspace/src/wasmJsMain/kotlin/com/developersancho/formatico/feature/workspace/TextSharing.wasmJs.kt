@file:OptIn(ExperimentalWasmJsInterop::class)

package com.developersancho.formatico.feature.workspace

internal actual fun browserCanShare(): Boolean = js("typeof navigator.share === 'function'")

internal actual fun browserShare(url: JsString): Unit = js("{ navigator.share({ url: url }).catch(() => {}); }")
