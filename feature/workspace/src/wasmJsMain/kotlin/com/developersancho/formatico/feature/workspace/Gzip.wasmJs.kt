package com.developersancho.formatico.feature.workspace

import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun gzipLength(text: JsString): Promise<JsNumber> = js(
    "new Response(new Blob([text]).stream().pipeThrough(new CompressionStream('gzip'))).arrayBuffer().then(b => b.byteLength)"
)
