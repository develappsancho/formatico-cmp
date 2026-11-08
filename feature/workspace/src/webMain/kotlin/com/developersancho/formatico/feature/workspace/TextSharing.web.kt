@file:OptIn(ExperimentalWasmJsInterop::class)

package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberTextSharing(): ((String) -> Unit)? = remember {
    if (browserCanShare()) { text -> browserShare(text.toJsString()) } else null
}

/** Whether the browser hands a link to another app itself: `navigator.share`, which a phone's browsers have. */
internal expect fun browserCanShare(): Boolean

/** Hands [url] to the browser's share sheet; a sheet closed without sending is no failure. */
internal expect fun browserShare(url: JsString)
