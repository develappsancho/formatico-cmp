package com.developersancho.formatico.feature.workspace

import androidx.lifecycle.viewModelScope
import com.developersancho.formatico.core.base.MviViewModel
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.Repairs
import com.developersancho.formatico.core.format.convert.ConvertOptions
import com.developersancho.formatico.core.format.xml.XmlDocument
import com.developersancho.formatico.core.network.linkSite
import com.developersancho.formatico.core.share.ShareKeep
import com.developersancho.formatico.core.share.ShareLinks
import com.developersancho.formatico.core.share.SharedDocument
import com.developersancho.formatico.core.storage.MemorySmallStore
import com.developersancho.formatico.feature.workspace.strings.convertStringsIn
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

/**
 * The workspace of the document whose id is [documentId]: reads it again whenever its text or its format changes, and
 * keeps how its tree is left in the [workbench], so that its tab shows it so again.
 *
 * @param readings what the open documents were read as: a document read before is not read again.
 * @param reader where the document is read, off the main thread.
 * @param queries the queries used on this device, which the Query view offers again.
 * @param language the language the words a conversion writes into the document are in.
 * @param sharing the encrypted links the share makes, and those it made before.
 */
@Suppress("LongParameterList") // Each is the view model's own, and a test gives each a stand-in: none is a part of another.
internal class WorkspaceViewModel(
    private val documentId: Long,
    private val workbench: Workbench,
    private val readings: Readings = Readings(workbench),
    private val reader: CoroutineDispatcher = Dispatchers.Default,
    private val queries: QueryHistory = QueryHistory(MemorySmallStore(), CoroutineScope(Dispatchers.Unconfined)),
    private val language: () -> String = { "en" },
    private val sharing: EncryptedSharing = EncryptedSharing()
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
                    // A large array's table is made off the main thread too: the view keeps it while its array is shown.
                    val source = tableSource((reading as? Reading.Read)?.root, state.value.document?.view?.selected)
                    val table = source?.let { withContext(reader) { tableFrom(it) } }
                    setState { copy(reading = reading, stats = stats, json5Allows = allows, table = table).arranged() }
                }
        }
    }

    /** Whether the link is to carry the document formatted: off each time the share opens, as its boards draw it. */
    private val shareFormatted = MutableStateFlow(false)

    /** The document as the share sends it: its text, formatted where the reader chose, and its name. */
    private var shared: SharedDocument? = null

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
                val sent = SharedDocument(if (formatted && formattedText != null) formattedText else text, name)
                this@WorkspaceViewModel.shared = sent
                setState {
                    val before = share
                    copy(
                        share = DocumentShare(
                            name = name,
                            format = format,
                            formatted = formatted,
                            canFormat = formattedText != null,
                            outcome = before?.outcome,
                            way = before?.way ?: ShareWay.Link,
                            keep = before?.keep ?: ShareKeep.Week
                        )
                    )
                }
                val outcome = withContext(reader) { ShareLinks.share(sent, linkSite()) }
                setState { copy(share = share?.copy(outcome = outcome)) }
                // An encrypted link made of this very text before, and not expired, is shown again. It is looked for apart:
                // a browser's fingerprint of the text is a promise, which the next text or choice should not wait for.
                viewModelScope.launch {
                    val made = sharing.find(sent.text) ?: return@launch
                    if (this@WorkspaceViewModel.shared == sent) {
                        setState { copy(share = share?.copy(way = ShareWay.Encrypted, encrypted = EncryptedShare.Created(made))) }
                    }
                }
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
                    val comment = (reading as? Reading.Read)?.let {
                        nullCommentOf(it.format, targetOf(it.format, shown.view.convert), language())
                    }
                    val outcome = withContext(reader) { conversionOf(shown, reading, shown.view.convert, comment.orEmpty()) }
                    setState { copy(conversion = outcome) }
                }
        }
    }

    init {
        // The document's types are written while its Types is shown, off the main thread, again as its text, its name
        // or the choice changes.
        viewModelScope.launch {
            workbench.state
                .map { state -> state.documents.firstOrNull { it.id == documentId }?.takeIf { it.view.pane == DocumentPane.Types } }
                .map { shown -> shown?.let { it.copy(edited = false, view = DocumentView(types = it.view.types), historyId = null) } }
                .distinctUntilChanged()
                .collectLatest { shown ->
                    if (shown == null) return@collectLatest setState { copy(types = null) }
                    val reading = readings.of(documentId, shown.text, shown.format)?.first
                        ?: withContext(reader) { readDocument(shown.text, shown.format) }
                    val outcome = withContext(reader) { typesOf(shown, reading, shown.view.types) }
                    setState { copy(types = outcome) }
                }
        }
    }

    init {
        // The open documents that are JSON Schemas, and the document validated against the one chosen, or the one its
        // $schema names: while its Schema is shown or a schema is chosen, off the main thread, again as its text, a
        // schema's text or the choice changes.
        viewModelScope.launch {
            workbench.state
                .map { state -> schemaInput(state.documents) }
                .distinctUntilChanged()
                .collectLatest { input ->
                    if (input == null) return@collectLatest setState { copy(schema = null, schemas = emptyList()) }
                    val (shown, candidates) = input
                    val schemas = candidates.mapNotNull { other ->
                        readingOf(other).let { withContext(reader) { schemaDocumentOf(other, it) } }
                    }
                    val reading = readingOf(shown)
                    val validation = withContext(reader) { schemaCheckOf(shown, reading, shown.view.schema, schemas) }
                    setState { copy(schema = validation.check, schemas = schemas.map { OfferedSchema(it.id, it.name, it.schemaId) }) }
                    // The schema the document's $schema names is chosen for it, as though the reader had chosen it.
                    val named = validation.schema?.takeIf { shown.view.schema.with == null }
                    if (named != null) workbench.changeView(documentId) { it.copy(schema = SchemaChoice(with = named.id)) }
                }
        }
    }

    /**
     * What the document's validation is worked out from, while its Schema is shown or a schema is chosen: the document,
     * with no more of its view than its choice, and the other open documents a schema may be among, with none of theirs.
     */
    private fun schemaInput(documents: List<OpenDocument>): Pair<OpenDocument, List<OpenDocument>>? {
        val document = documents.firstOrNull { it.id == documentId } ?: return null
        val view = document.view
        if (view.pane != DocumentPane.Schema && view.schema.with == null) return null
        val shown = document.copy(edited = false, view = DocumentView(schema = view.schema), historyId = null)
        val candidates = documents.filter { it.id != documentId && it.format in SchemaFormats }
            .map { it.copy(edited = false, view = DocumentView(), historyId = null) }
        return shown to candidates
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

            is WorkspaceIntent.Types -> workbench.changeView(documentId) { it.copy(types = intent.choice) }

            is WorkspaceIntent.Compare -> workbench.changeView(documentId) { it.copy(compare = intent.choice) }

            is WorkspaceIntent.Schema -> workbench.changeView(documentId) { it.copy(schema = intent.choice) }

            is WorkspaceIntent.UseQuery -> state.value.let { shown ->
                shown.queryLanguage?.let { queries.remember(it, intent.query ?: shown.document?.view?.query.orEmpty()) }
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

            is WorkspaceIntent.ShareBy -> setState { copy(share = share?.copy(way = intent.way)) }

            is WorkspaceIntent.ShareKept -> setState { copy(share = share?.copy(keep = intent.keep)) }

            WorkspaceIntent.CreateEncryptedLink -> createEncryptedLink()

            WorkspaceIntent.DeleteEncryptedLink -> deleteEncryptedLink()
        }
    }

    /**
     * Makes the document's encrypted link: encrypted off the screen's thread, sent to Formatico's server, and kept on
     * this device with what deleting it takes.
     */
    private fun createEncryptedLink() {
        val share = state.value.share ?: return
        val document = shared ?: return
        if (share.encrypted == EncryptedShare.Creating || share.encrypted is EncryptedShare.Created) return
        setState { copy(share = this.share?.copy(encrypted = EncryptedShare.Creating)) }
        viewModelScope.launch {
            val encrypted = withContext(reader) { sharing.create(document, share.keep) }
            // The share may have closed meanwhile, or now send another text.
            if (shared == document) setState { copy(share = this.share?.copy(encrypted = encrypted)) }
        }
    }

    /** Deletes the encrypted link from Formatico's server, and forgets it here; it stays where the server is not reached. */
    private fun deleteEncryptedLink() {
        val created = state.value.share?.encrypted as? EncryptedShare.Created ?: return
        viewModelScope.launch {
            val encrypted = sharing.delete(created)
            setState { copy(share = share?.copy(encrypted = encrypted)) }
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

/**
 * The state with its open branches, the trail to its chosen value and its table, from its document's view and its tree.
 * The table is made again only for another array: a view changes with each branch opened and each value chosen.
 */
internal fun WorkspaceState.arranged(): WorkspaceState {
    val view = document?.view ?: DocumentView()
    val source = tableSource(root, view.selected)
    return copy(
        expanded = view.expanded ?: defaultExpanded(root),
        trail = trail(root, view.selected),
        table = table?.takeIf { it.source === source } ?: source?.let(::tableFrom)
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
        types = if (document.view.pane == DocumentPane.Types) typesOf(document, reading, document.view.types) else null,
        comparison = if (document.view.pane == DocumentPane.Compare) previewComparison(document, reading, others) else null,
        others = others.map { OtherDocument(it.id, it.name) }
    ).withSchema(document, reading, others).arranged()
}

/**
 * The state with its document validated at once against the schema its view chooses among [others], where its Schema
 * is shown or a schema is chosen, for a preview or a test.
 */
private fun WorkspaceState.withSchema(document: OpenDocument, reading: Reading, others: List<OpenDocument>): WorkspaceState {
    val view = document.view
    if (view.pane != DocumentPane.Schema && view.schema.with == null) return this
    val schemas = others.mapNotNull { other -> schemaDocumentOf(other, readDocument(other.text, other.format)) }
    val validation = schemaCheckOf(document, reading, view.schema, schemas)
    return copy(schema = validation.check, schemas = schemas.map { OfferedSchema(it.id, it.name, it.schemaId) })
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
 * The words that mark a null left out, in [language], the reader's, `{name}` standing for its name: a document of
 * [from] converted to [to], which has no null.
 */
private fun nullCommentOf(from: Format, to: Format, language: String): String =
    convertStringsIn(language).nullComment(ConvertOptions.NAME, from.displayName, to.displayName)

/** What marks a null left out where a preview or a test works the state out at once: the English words. */
private fun previewNullComment(from: Format, to: Format): String = nullCommentOf(from, to, "en")
