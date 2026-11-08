package com.developersancho.formatico.feature.workspace

import androidx.lifecycle.viewModelScope
import com.developersancho.formatico.core.base.MviViewModel
import com.developersancho.formatico.core.format.Repairs
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
                    // Only a JSON document with errors is read again, as JSON5, to say what that would leave out.
                    val invalid = reading is Reading.Read && reading.errors > 0
                    val allows = if (invalid) withContext(reader) { json5Allows(text, format, reading) } else 0
                    setState { copy(reading = reading, stats = stats, json5Allows = allows).arranged() }
                }
        }
    }

    init {
        // The summary is worked out while it is shown, off the main thread, and its gzipped size after the rest.
        viewModelScope.launch {
            workbench.state
                .map { state -> state.documents.firstOrNull { it.id == documentId }?.takeIf { it.view.pane == DocumentPane.Summary } }
                .map { shown -> shown?.let { it.copy(view = DocumentView(pane = DocumentPane.Summary)) } }
                .distinctUntilChanged()
                .collectLatest { shown ->
                    if (shown == null) return@collectLatest setState { copy(summary = null, sizes = null) }
                    val reading = readings.of(documentId, shown.text, shown.format)?.first
                        ?: withContext(reader) { readDocument(shown.text, shown.format) }
                    val (summary, minified) = withContext(reader) {
                        (reading as? Reading.Read)?.root?.let(::summaryOf) to minifiedOf(shown)
                    }
                    val sizes = sizesOf(shown, minified)
                    setState { copy(summary = summary, sizes = sizes) }
                    val compressed = withContext(reader) { gzippedSize(minified ?: shown.text) }
                    setState { copy(sizes = sizes.copy(compressed = compressed)) }
                }
        }
    }

    override fun handle(intent: WorkspaceIntent) {
        when (intent) {
            is WorkspaceIntent.Expand -> workbench.changeView(documentId) { it.copy(expanded = intent.keys) }

            is WorkspaceIntent.Select -> select(intent.key)

            is WorkspaceIntent.ShowPane -> workbench.changeView(documentId) { it.copy(pane = intent.pane) }

            is WorkspaceIntent.Search -> workbench.changeView(documentId) { it.copy(searching = intent.open) }

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

            is WorkspaceIntent.Repair -> state.value.document?.let { document ->
                document.format?.let { Repairs.of(intent.problem, document.text, it) }?.let { workbench.repair(documentId, it.edits) }
            }

            WorkspaceIntent.RepairAll -> repairAll()

            WorkspaceIntent.Close -> workbench.close(documentId)
        }
    }

    /** Repairs every problem of the document Formatico repairs, reading it again after each round, off the main thread. */
    private fun repairAll() {
        val document = state.value.document ?: return
        val format = document.format ?: return
        viewModelScope.launch {
            val repaired = withContext(reader) { Repairs.all(document.text, format) { problemsOf(it, format) } }
            workbench.write(documentId, repaired)
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
internal fun readState(document: OpenDocument): WorkspaceState {
    val reading = readDocument(document.text, document.format)
    // Its summary is worked out where it is shown, all but its gzipped size, which the platform measures in its time.
    val shown = document.view.pane == DocumentPane.Summary
    return WorkspaceState(
        document = document,
        reading = reading,
        stats = TextStats.of(document.text),
        json5Allows = json5Allows(document.text, document.format, reading),
        summary = if (shown) (reading as? Reading.Read)?.root?.let(::summaryOf) else null,
        sizes = if (shown) sizesOf(document, minifiedOf(document)) else null
    ).arranged()
}
