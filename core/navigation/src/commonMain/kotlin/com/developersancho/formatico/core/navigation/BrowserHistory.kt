package com.developersancho.formatico.core.navigation

import androidx.compose.runtime.Composable

/**
 * Keeps a browser tab's history and address with the screens the app shows, as a web page's are, where the app runs
 * in a browser; elsewhere it does nothing. [entries] are the screens from the root to the one shown, each as the part
 * of an address after its `#` (`#start`), the last of them the address's. Going back or forward in the browser hands
 * [onRestore] the screens of the entry it came to; an address written by hand, which has no screens of its own,
 * hands it its `#` part alone, and an address with none hands it an empty one.
 */
@Composable
expect fun BrowserHistory(entries: List<String>, onRestore: (List<String>) -> Unit)
