// Adapted from navigation3-browser 1.1.0 (ChronologicalBrowserNavigation.kt),
// https://github.com/terrakok/navigation3-browser, Copyright (c) 2024 Konstantin Tskhovrebov (@terrakok), under the MIT
// License: app/webApp/src/webMain/resources/licenses/navigation3-browser.txt, which goes out with the web app.
package com.developersancho.formatico.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

// A change the app makes adds an entry to the tab's history; the change that follows the browser's own move replaces
// the entry the tab is on. The screens are kept in each entry's state, a line to a screen.
//
// The first change, on an entry the app did not write (a new tab, an address), gives each screen an entry of its
// own, so that the browser's back goes from the screen the app opens on to the screens under it. An entry it wrote
// before a reload already has the entries of the screens under it before it.
@Composable
actual fun BrowserHistory(entries: List<String>, onRestore: (List<String>) -> Unit) {
    val shown by rememberUpdatedState(entries)
    val restore by rememberUpdatedState(onRestore)
    LaunchedEffect(Unit) {
        val window = browserWindow()
        coroutineScope {
            launch {
                window.popStateEvents()
                    .map { it.state }
                    // The entry the tab is on when the app opens: one it wrote before a reload, or an address.
                    .onStart { emit(window.history.state) }
                    .collect { state -> restore(state?.lines() ?: listOf(window.location.hash)) }
            }
            launch {
                var first = true
                snapshotFlow { shown }.collect { screens ->
                    if (screens.isEmpty()) return@collect
                    val written = window.history.state
                    when {
                        // The first on an entry the app did not write: the screens from the root, each in an entry.
                        written == null && first -> screens.indices.forEach { last ->
                            window.write(screens.subList(0, last + 1), push = last > 0)
                        }

                        // What followed the browser's own move, or an address written by hand: the entry the tab is on.
                        written == null || written == screens.joinToString("\n") -> window.write(screens, push = false)

                        // The app's own move: a new entry.
                        else -> window.write(screens, push = true)
                    }
                    first = false
                }
            }
        }
    }
}

/** Writes [screens] into the tab's history, as a new entry when [push] says so and else into the entry it is on. */
private fun BrowserWindow.write(screens: List<String>, push: Boolean) {
    val state = screens.joinToString("\n")
    val address = location.origin + location.pathname + screens.last()
    if (push) history.pushState(state, "", address) else history.replaceState(state, "", address)
}
