package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable

/**
 * The platform's way of handing a text to another app, its share sheet: Android's chooser, iOS's activity sheet, and a
 * browser's own where it has one. Null where there is none, as on the desktop. Pantrix leaves the share sheet to the
 * app (its chart export guide, section 3).
 */
@Composable
internal expect fun rememberTextSharing(): ((String) -> Unit)?
