package com.developersancho.formatico.core.navigation

import androidx.compose.runtime.Composable

// Only a browser has a history of its own to keep.
@Composable
actual fun BrowserHistory(entries: List<String>, onRestore: (List<String>) -> Unit) = Unit
