@file:OptIn(ExperimentalWasmJsInterop::class)

package com.developersancho.formatico.feature.workspace

import kotlinx.coroutines.await
import kotlin.js.Promise

internal actual suspend fun gzippedSize(text: String): Long? =
    runCatching { gzipLength(text.toJsString()).await<JsNumber>().toInt().toLong() }.getOrNull()

/** How many bytes [text], written in UTF-8, comes to through the browser's `CompressionStream` of gzip. */
internal expect fun gzipLength(text: JsString): Promise<JsNumber>
