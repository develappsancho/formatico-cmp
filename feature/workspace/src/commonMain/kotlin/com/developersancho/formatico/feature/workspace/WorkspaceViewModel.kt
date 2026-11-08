package com.developersancho.formatico.feature.workspace

import androidx.lifecycle.viewModelScope
import com.developersancho.formatico.core.base.MviViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The workspace of the document whose id is [documentId]: reads it again whenever its text or its format changes, and
 * keeps how its tree is left in the [workbench], so that its tab shows it so again.
 *
 * @param readings what the open documents were read as: a document read before is not read again.
 * @param reader where the document is read, off the main thread.
 */
internal class WorkspaceViewModel(
    private val documentId: Long,
    private val workbench: Workbench,
    private val readings: Readings = Readings(workbench),
    private val reader: CoroutineDispatcher = Dispatchers.Default
) : MviViewModel<WorkspaceState, WorkspaceIntent, WorkspaceEffect>(WorkspaceState()) {
    init {
        val document = workbench.state
            .map { state -> state.documents.firstOrNull { it.id == documentId } }
            .distinctUntilChanged()
        viewModelScope.launch {
            document.collect { open -> setState { copy(document = open).arranged() } }
        }
        viewModelScope.launch {
            document
                .map { open -> open?.let { it.text to it.format } }
                .distinctUntilChanged()
                .collectLatest { source ->
                    val (text, format) = source ?: return@collectLatest
                    val (reading, stats) = readings.of(documentId, text, format)
                        ?: withContext(reader) { readDocument(text, format) to TextStats.of(text) }
                            .also { readings.keep(documentId, text, format, it) }
                    setState { copy(reading = reading, stats = stats).arranged() }
                }
        }
    }

    override fun handle(intent: WorkspaceIntent) {
        when (intent) {
            is WorkspaceIntent.Expand -> workbench.changeView(documentId) { it.copy(expanded = intent.keys) }

            is WorkspaceIntent.Select -> select(intent.key)

            is WorkspaceIntent.ShowPane -> workbench.changeView(documentId) { it.copy(pane = intent.pane) }

            is WorkspaceIntent.ChangeFormat -> workbench.changeFormat(documentId, intent.format)

            is WorkspaceIntent.Edit -> viewModelScope.launch {
                sendEffect(WorkspaceEffect.Edited(intent.edit, workbench.edit(documentId, intent.edit)))
            }

            is WorkspaceIntent.SortTable -> workbench.changeView(documentId) { it.copy(tableSort = intent.sort) }

            is WorkspaceIntent.FilterTable -> workbench.changeView(documentId) { it.copy(tableFilter = intent.filter) }

            is WorkspaceIntent.HideColumns -> workbench.changeView(documentId) {
                it.copy(hiddenColumns = intent.hidden)
            }

            is WorkspaceIntent.Write -> workbench.write(documentId, intent.text)

            WorkspaceIntent.Close -> workbench.close(documentId)
        }
    }

    /** Chooses the value whose key is [key], and opens the branches it stands in, so that its row is shown. */
    private fun select(key: String?) {
        val ancestors = trail(state.value.root, key).dropLast(1).map { it.key }
        val expanded = state.value.expanded
        val opened = if (expanded.containsAll(ancestors)) null else expanded + ancestors
        workbench.changeView(documentId) { view -> view.copy(selected = key, expanded = opened ?: view.expanded) }
    }
}

/** The state with its open branches, the trail to its chosen value and its table, from its document's view and its tree. */
internal fun WorkspaceState.arranged(): WorkspaceState {
    val view = document?.view ?: DocumentView()
    return copy(
        expanded = view.expanded ?: defaultExpanded(root),
        trail = trail(root, view.selected),
        table = tableOf(root, view.selected)
    )
}

/** The workspace of [document] once it is read: what its view model comes to, at once, for a preview or a test. */
internal fun readState(document: OpenDocument): WorkspaceState = WorkspaceState(
    document = document,
    reading = readDocument(document.text, document.format),
    stats = TextStats.of(document.text)
).arranged()
