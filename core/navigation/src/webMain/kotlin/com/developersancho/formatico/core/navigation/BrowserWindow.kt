// Adapted from navigation3-browser 1.1.0 (BrowserApi.kt), https://github.com/terrakok/navigation3-browser,
// Copyright (c) 2024 Konstantin Tskhovrebov (@terrakok), under the MIT License:
// app/webApp/src/webMain/resources/licenses/navigation3-browser.txt, which goes out with the web app.
package com.developersancho.formatico.core.navigation

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** The page's address: what Formatico reads of `window.location`. */
internal external interface BrowserLocation {
    val origin: String
    val pathname: String
    val hash: String
}

/** The tab's history: what Formatico does with `window.history`. */
internal external interface BrowserHistoryApi {
    val state: String?

    fun pushState(data: String?, title: String, url: String?)

    fun replaceState(data: String?, title: String, url: String?)

    fun go(delta: Int)
}

internal external interface BrowserEvent

/** A move in the tab's history, with the state of the entry moved to. */
internal external interface BrowserPopStateEvent : BrowserEvent {
    val state: String?
}

/** The page's window, with what Formatico reads of it and listens to. */
internal external interface BrowserWindow {
    val location: BrowserLocation
    val history: BrowserHistoryApi

    fun addEventListener(type: String, callback: ((BrowserEvent) -> Unit)?)

    fun removeEventListener(type: String, callback: ((BrowserEvent) -> Unit)?)
}

/** The page's `window`. */
internal expect fun browserWindow(): BrowserWindow

/** Each move in the tab's history, from the browser's back and forward buttons and from `history.go`. */
@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
internal fun BrowserWindow.popStateEvents(): Flow<BrowserPopStateEvent> = callbackFlow {
    val callback: (BrowserEvent) -> Unit = { event -> (event as? BrowserPopStateEvent)?.let { trySend(it) } }
    addEventListener("popstate", callback)
    awaitClose { removeEventListener("popstate", callback) }
}
