@file:OptIn(ExperimentalWasmJsInterop::class)

package com.developersancho.formatico.feature.workspace

import kotlin.js.Promise

internal actual fun gzipLength(text: JsString): Promise<JsNumber> = js(
    "new Response(new Blob([text]).stream().pipeThrough(new CompressionStream('gzip')))" +
        ".arrayBuffer().then(function (b) { return b.byteLength; })"
)
