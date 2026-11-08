package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.storage.SmallStore
import com.developersancho.formatico.core.storage.TextStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.milliseconds

/**
 * Keeps the documents open in [workbench] on this device, and gives them back when the app opens again: their tabs in
 * [small], each text in [texts] under `open-<id>`. What is written follows the workbench a moment after it changes;
 * only the texts that changed are written again.
 *
 * What a document's tree was left showing is not kept: it opens on its tree, or its table, as a document opens.
 */
class WorkbenchKeeper(
    private val workbench: Workbench,
    private val small: SmallStore,
    private val texts: TextStore,
    private val scope: CoroutineScope
) {
    // The text each document was last written with, by its id.
    private val written = mutableMapOf<Long, String>()
    private val mutableRestored = MutableStateFlow(false)

    /**
     * Whether what was open has been given back: what has to wait for it, as an address that names a document, can go
     * on.
     */
    val restored: StateFlow<Boolean> = mutableRestored.asStateFlow()

    /** Gives back what was open, then keeps what is, for as long as [scope] lives. */
    @OptIn(FlowPreview::class)
    fun start(): Job = scope.launch {
        try {
            reopen()
        } finally {
            mutableRestored.value = true
        }
        workbench.state.drop(1).debounce(KEEP_AFTER).collect { keep(it) }
    }

    private suspend fun reopen() {
        val kept = small.get(KEY, KeptWorkbench.serializer()) ?: return
        val documents = kept.tabs.mapNotNull { tab ->
            val text = texts.read(textName(tab.id)) ?: return@mapNotNull null
            written[tab.id] = text
            OpenDocument(
                id = tab.id,
                name = tab.name,
                text = text,
                format = Format.entries.firstOrNull { it.name == tab.format },
                edited = tab.edited,
                view = DocumentView(
                    pane = DocumentPane.entries.firstOrNull {
                        it.name == tab.pane
                    } ?: DocumentPane.Tree
                ),
                historyId = tab.historyId
            )
        }
        if (documents.isNotEmpty()) workbench.reopen(documents, kept.selectedId)
    }

    private suspend fun keep(state: WorkbenchState) {
        // A write that fails leaves the device as it was; the next change writes again.
        runCatching {
            state.documents.forEach { document ->
                if (written[document.id] != document.text) {
                    texts.write(textName(document.id), document.text)
                    written[document.id] = document.text
                }
            }
            val open = state.documents.map { it.id }.toSet()
            (written.keys - open).forEach { closed ->
                texts.delete(textName(closed))
                written.remove(closed)
            }
            small.put(KEY, KeptWorkbench.of(state), KeptWorkbench.serializer())
        }
    }

    private companion object {
        const val KEY = "workbench"
        val KEEP_AFTER = 400.milliseconds
    }
}

/** The open documents' tabs as they are kept, in their order, and the one chosen. */
@Serializable
private data class KeptWorkbench(val tabs: List<KeptTab>, val selectedId: Long?) {
    companion object {
        fun of(state: WorkbenchState) = KeptWorkbench(
            tabs = state.documents.map { document ->
                KeptTab(
                    id = document.id,
                    name = document.name,
                    format = document.format?.name,
                    edited = document.edited,
                    pane = document.view.pane.name,
                    historyId = document.historyId
                )
            },
            selectedId = state.selectedId
        )
    }
}

/** A tab as it is kept; its document's text is kept apart, by [id]. */
@Serializable
private data class KeptTab(
    val id: Long,
    val name: String?,
    val format: String?,
    val edited: Boolean,
    val pane: String,
    val historyId: Long?
)

private fun textName(id: Long) = "open-$id"
