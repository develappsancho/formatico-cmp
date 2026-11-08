package com.developersancho.formatico

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.developersancho.formatico.core.navigation.BrowserHistory
import com.developersancho.formatico.feature.workspace.Workbench

/** Start, as a browser's address writes it after its `#`. */
internal const val START_ENTRY = "#start"

/** The workspace of a document, as a browser's address writes it, its id after it: `#document?id=3`. */
internal const val DOCUMENT_ENTRY = "#document?id="

/**
 * Keeps the browser's history and address with the screen the app shows, where the app runs in a browser: going back
 * or forward in the browser goes from tab to tab, and an address written by hand opens its document. Elsewhere it
 * does nothing.
 */
@Composable
internal fun FollowBrowserHistory(workbench: Workbench) {
    val state by workbench.state.collectAsState()
    BrowserHistory(entries = browserEntries(state.selectedId), onRestore = workbench::showBrowserEntries)
}

/** The app's screens, as a browser's addresses write them after their `#`: Start, and the document shown over it. */
internal fun browserEntries(selectedId: Long?): List<String> =
    listOfNotNull(START_ENTRY, selectedId?.let { "$DOCUMENT_ENTRY$it" })

/**
 * Shows what the browser's [entries] come to, the last of them the screen: Start for Start, and the workspace of the
 * document it names while that document is open, or else Start. An entry that names neither changes nothing, as an
 * address with no `#` part, or one Formatico did not write, does not.
 */
internal fun Workbench.showBrowserEntries(entries: List<String>) {
    val last = entries.lastOrNull() ?: return
    when {
        last == START_ENTRY -> {
            select(null)
        }

        last.startsWith(DOCUMENT_ENTRY) -> {
            val id = last.removePrefix(DOCUMENT_ENTRY).toLongOrNull() ?: return
            select(id.takeIf { open -> state.value.documents.any { it.id == open } })
        }
    }
}
