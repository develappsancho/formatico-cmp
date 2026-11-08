package com.developersancho.formatico

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.developersancho.formatico.core.navigation.BrowserHistory
import com.developersancho.formatico.feature.workspace.Workbench

/** Start, as a browser's address writes it after its `#`. */
internal const val START_ENTRY = "#start"

/** The workspace of a document, as a browser's address writes it, its id after it: `#document?id=3`. */
internal const val DOCUMENT_ENTRY = "#document?id="

/** Settings, over the screen it was opened from. */
internal const val SETTINGS_ENTRY = "#settings"

/**
 * Keeps the browser's history and address with the screen the app shows, where the app runs in a browser: going back
 * or forward in the browser goes from tab to tab, and an address written by hand opens its document. Elsewhere it
 * does nothing.
 */
@Composable
internal fun FollowBrowserHistory(workbench: Workbench, settings: MutableState<Boolean>) {
    val state by workbench.state.collectAsState()
    BrowserHistory(
        entries = browserEntries(state.selectedId, settings.value),
        onRestore = { entries -> workbench.showBrowserEntries(entries) { settings.value = it } }
    )
}

/**
 * The app's screens, as a browser's addresses write them after their `#`: Start, the document shown over it, and
 * Settings over them where [settings] says it is shown.
 */
internal fun browserEntries(selectedId: Long?, settings: Boolean = false): List<String> =
    listOfNotNull(START_ENTRY, selectedId?.let { "$DOCUMENT_ENTRY$it" }, SETTINGS_ENTRY.takeIf { settings })

/**
 * Shows what the browser's [entries] come to, the last of them the screen: Start for Start, and the workspace of the
 * document it names while that document is open, or else Start; Settings over the one before it, which [onSettings]
 * is told of. An entry that names none of them changes nothing, as an address with no `#` part, or one Formatico did
 * not write, does not.
 */
internal fun Workbench.showBrowserEntries(entries: List<String>, onSettings: (Boolean) -> Unit = {}) {
    val settings = entries.lastOrNull() == SETTINGS_ENTRY
    val last = (if (settings) entries.dropLast(1) else entries).lastOrNull()
    val document = last?.takeIf { it.startsWith(DOCUMENT_ENTRY) }?.removePrefix(DOCUMENT_ENTRY)
    if (!settings && last != START_ENTRY && document == null) return
    onSettings(settings)
    when {
        last == START_ENTRY -> select(null)

        document != null -> {
            val id = document.toLongOrNull() ?: return
            select(id.takeIf { open -> state.value.documents.any { it.id == open } })
        }
    }
}
