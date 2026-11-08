package com.developersancho.formatico.feature.workspace

import androidx.lifecycle.viewModelScope
import com.developersancho.formatico.core.base.MviViewModel
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.Repairs
import com.developersancho.formatico.core.format.convert.ConvertOptions
import com.developersancho.formatico.core.format.xml.XmlDocument
import com.developersancho.formatico.core.share.ShareLinks
import com.developersancho.formatico.core.share.SharedDocument
import com.developersancho.formatico.core.storage.MemorySmallStore
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.convert_null_comment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString

/**
 * The workspace of the document whose id is [documentId]: reads it again whenever its text or its format changes, and
 * keeps how its tree is left in the [workbench], so that its tab shows it so again.
 *
 * @param readings what the open documents were read as: a document read before is not read again.
 * @param reader where the document is read, off the main thread.
 * @param queries the queries used on this device, which the Query view offers again.
 */
internal class WorkspaceViewModel(
    private val documentId: Long,
    private val workbench: Workbench,
    private val readings: Readings = Readings(workbench),
    private val reader: CoroutineDispatcher = Dispatchers.Default,
    private val queries: QueryHistory = QueryHistory(MemorySmallStore(), CoroutineScope(Dispatchers.Unconfined))
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

    /** Whether the link is to carry the document formatted: off each time the share opens, as its boards draw it. */
    private val shareFormatted = MutableStateFlow(false)

    init {
        // The link is made while the share is shown, off the main thread, again as the text or the choice changes; the
        // link made before stays until the next is made.
        viewModelScope.launch {
            val shared = workbench.state
                .map { state -> state.documents.firstOrNull { it.id == documentId }?.takeIf { it.view.sharing } }
                .map { shown -> shown?.let { Triple(it.name, it.text, it.format) } }
                .distinctUntilChanged()
            combine(shared, shareFormatted, ::Pair).collectLatest { (shown, formatted) ->
                val (name, text, format) = shown ?: return@collectLatest setState { copy(share = null) }
                // What Format makes of it, without keeping it: none of plain text, nor of a document with errors.
                val options = workbench.formatOptions
                val formattedText = withContext(reader) {
                    val document = OpenDocument(id = documentId, name = name, text = text, format = format)
                    (edited(document, DocumentEdit.Format, options) as? Edited.Text)?.text
                }
                val sent = if (formatted && formattedText != null) formattedText else text
                setState {
                    copy(share = DocumentShare(name, format, formatted, canFormat = formattedText != null, outcome = share?.outcome))
                }
                val outcome = withContext(reader) { ShareLinks.share(SharedDocument(sent, name)) }
                setState { copy(share = share?.copy(outcome = outcome)) }
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

    init {
        // The query is asked while it is shown, off the main thread, and the first of a new query's matches chosen, as
        // the tree's search chooses its first.
        viewModelScope.launch {
            workbench.state
                .map { state -> state.documents.firstOrNull { it.id == documentId }?.takeIf { it.view.pane == DocumentPane.Query } }
                .map { shown ->
                    shown?.let { it.copy(name = null, edited = false, view = DocumentView(query = it.view.query), historyId = null) }
                }
                .distinctUntilChanged()
                .collectLatest { shown ->
                    if (shown == null) return@collectLatest setState { copy(query = null) }
                    val reading = readings.of(documentId, shown.text, shown.format)?.first
                        ?: withContext(reader) { readDocument(shown.text, shown.format) }
                    val outcome = withContext(reader) { queryOutcome(shown, reading, shown.view.query, ::xmlOf) }
                    setState { copy(query = outcome) }
                    val first = outcome.matches.firstOrNull { it.key != null } ?: return@collectLatest
                    if (outcome.matches.none { it.key == state.value.selected?.key }) select(first.key)
                }
        }
        viewModelScope.launch {
            queries.queries.collect { recent -> setState { copy(recentQueries = recent) } }
        }
    }

    init {
        // The document is converted while its Convert is shown, off the main thread, again as its text or the choice
        // changes.
        viewModelScope.launch {
            workbench.state
                .map { state -> state.documents.firstOrNull { it.id == documentId }?.takeIf { it.view.pane == DocumentPane.Convert } }
                .map { shown -> shown?.let { it.copy(edited = false, view = DocumentView(convert = it.view.convert), historyId = null) } }
                .distinctUntilChanged()
                .collectLatest { shown ->
                    if (shown == null) return@collectLatest setState { copy(conversion = null) }
                    val reading = readings.of(documentId, shown.text, shown.format)?.first
                        ?: withContext(reader) { readDocument(shown.text, shown.format) }
                    val comment = (reading as? Reading.Read)?.let { nullCommentOf(it.format, targetOf(it.format, shown.view.convert)) }
                    val outcome = withContext(reader) { conversionOf(shown, reading, shown.view.convert, comment.orEmpty()) }
                    setState { copy(conversion = outcome) }
                }
        }
    }

    init {
        // The other open documents, which the document can be compared with.
        viewModelScope.launch {
            workbench.state
                .map { state -> state.documents.filter { it.id != documentId }.map { OtherDocument(it.id, it.name) } }
                .distinctUntilChanged()
                .collect { others -> setState { copy(others = others) } }
        }
        // The document is compared while its Compare is shown, off the main thread, again as either text, the other
        // document or the choice changes.
        viewModelScope.launch {
            workbench.state
                .map { state -> compareInput(state.documents) }
                .distinctUntilChanged()
                .collectLatest { input ->
                    if (input == null) return@collectLatest setState { copy(comparison = null) }
                    val (a, b) = input
                    val aReading = readingOf(a)
                    val bReading = b?.let { readingOf(it) }
                    val outcome = withContext(reader) { comparisonOf(a, aReading, b, bReading, a.view.compare.by) }
                    setState { copy(comparison = outcome) }
                }
        }
    }

    /**
     * What the document's comparison is worked out from, while its Compare is shown: the document and the one it is
     * compared with, with no more of their views than how they are compared, so that a new choice of difference does
     * not compare them again.
     */
    private fun compareInput(documents: List<OpenDocument>): Pair<OpenDocument, OpenDocument?>? {
        val shown = documents.firstOrNull { it.id == documentId }?.takeIf { it.view.pane == DocumentPane.Compare } ?: return null
        val partner = partnerOf(documentId, documents, shown.view.compare)

        fun bare(document: OpenDocument, by: CompareBy) =
            document.copy(edited = false, view = DocumentView(compare = CompareChoice(by = by)), historyId = null)
        return bare(shown, shown.view.compare.by) to partner?.let { bare(it, CompareBy.Meaning) }
    }

    /** What [document] was read as: kept where it was read before, read again off the main thread otherwise. */
    private suspend fun readingOf(document: OpenDocument): Reading =
        readings.of(document.id, document.text, document.format)?.first
            ?: withContext(reader) { readDocument(document.text, document.format) }

    // The XML an XPath is asked of, read once for each of its texts rather than once for each query.
    private var xml: XmlDocument? = null

    private fun xmlOf(text: String): XmlDocument = xml?.takeIf { it.text == text } ?: XmlDocument.parse(text).also { xml = it }

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

            is WorkspaceIntent.Query -> workbench.changeView(documentId) { it.copy(query = intent.query) }

            is WorkspaceIntent.Convert -> workbench.changeView(documentId) { it.copy(convert = intent.choice) }

            is WorkspaceIntent.Compare -> workbench.changeView(documentId) { it.copy(compare = intent.choice) }

            WorkspaceIntent.UseQuery -> state.value.let { shown ->
                shown.queryLanguage?.let { queries.remember(it, shown.document?.view?.query.orEmpty()) }
            }

            is WorkspaceIntent.Write -> workbench.write(documentId, intent.text)

            is WorkspaceIntent.Repair -> state.value.document?.let { document ->
                document.format?.let { Repairs.of(intent.problem, document.text, it) }?.let { workbench.repair(documentId, it.edits) }
            }

            WorkspaceIntent.RepairAll -> repairAll()

            WorkspaceIntent.Close -> workbench.close(documentId)

            is WorkspaceIntent.Share -> {
                if (intent.open) shareFormatted.value = false
                workbench.changeView(documentId) { it.copy(sharing = intent.open) }
            }

            is WorkspaceIntent.ShareFormatted -> shareFormatted.value = intent.formatted
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

/**
 * The workspace of [document] once it is read, beside the [others] open: what its view model comes to, at once, for a
 * preview or a test.
 */
internal fun readState(document: OpenDocument, others: List<OpenDocument> = emptyList()): WorkspaceState {
    val reading = readDocument(document.text, document.format)
    // Its summary is worked out where it is shown, all but its gzipped size, which the platform measures in its time.
    val shown = document.view.pane == DocumentPane.Summary
    return WorkspaceState(
        document = document,
        reading = reading,
        stats = TextStats.of(document.text),
        json5Allows = json5Allows(document.text, document.format, reading),
        summary = if (shown) (reading as? Reading.Read)?.root?.let(::summaryOf) else null,
        sizes = if (shown) sizesOf(document, minifiedOf(document)) else null,
        query = if (document.view.pane == DocumentPane.Query) queryOutcome(document, reading, document.view.query) else null,
        conversion = if (document.view.pane == DocumentPane.Convert) previewConversion(document, reading) else null,
        comparison = if (document.view.pane == DocumentPane.Compare) previewComparison(document, reading, others) else null,
        others = others.map { OtherDocument(it.id, it.name) }
    ).arranged()
}

/** [document], read as [reading], compared at once with the one of [others] its view chooses, for a preview or a test. */
private fun previewComparison(document: OpenDocument, reading: Reading, others: List<OpenDocument>): CompareOutcome {
    val partner = partnerOf(document.id, listOf(document) + others, document.view.compare)
    return comparisonOf(document, reading, partner, partner?.let { readDocument(it.text, it.format) }, document.view.compare.by)
}

/** [document], read as [reading], converted at once, for a preview or a test. */
private fun previewConversion(document: OpenDocument, reading: Reading): ConvertOutcome {
    val choice = document.view.convert
    val comment = (reading as? Reading.Read)?.let { previewNullComment(it.format, targetOf(it.format, choice)) }
    return conversionOf(document, reading, choice, comment.orEmpty())
}

/**
 * The words that mark a null left out, in the reader's language, `{name}` standing for its name: a document of [from]
 * converted to [to], which has no null.
 */
private suspend fun nullCommentOf(from: Format, to: Format): String =
    getString(Res.string.convert_null_comment, ConvertOptions.NAME, from.displayName, to.displayName)

/** What marks a null left out where a preview or a test works the state out at once: the English resources' words. */
private fun previewNullComment(from: Format, to: Format): String =
    "${ConvertOptions.NAME} = null in the ${from.displayName}: ${to.displayName} has no null"
