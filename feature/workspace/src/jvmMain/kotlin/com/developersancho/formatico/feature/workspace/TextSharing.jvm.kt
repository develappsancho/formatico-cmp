package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable

/** The desktop has no share sheet: a link is copied. */
@Composable
internal actual fun rememberTextSharing(): ((String) -> Unit)? = null
