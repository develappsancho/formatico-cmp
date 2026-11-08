package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.convert.NullHandling
import com.developersancho.formatico.core.format.types.TypeLanguage
import com.developersancho.formatico.core.storage.SmallStore
import com.developersancho.formatico.core.storage.TextStore
import com.developersancho.formatico.core.storage.historyTextName
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
 * [small], with what each was left showing (its view, the tree's open branches and chosen value, the table's order,
 * hidden columns and filter, and its query), and their texts in [texts]. A document open from Start's history has its text there
 * once, under the history's name, which the history writes as it changes; any other has it under `open-<id>`. A CSV's
 * or a TSV's tab keeps whether its first row is the header. What is written follows the workbench a moment after it
 * changes; only the texts that changed are written again.
 */
class WorkbenchKeeper(
    private val workbench: Workbench,
    private val small: SmallStore,
    private val texts: TextStore,
    private val scope: CoroutineScope
) {
    // The text each document kept under `open-<id>` was last written with, by its id.
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
            val fromHistory = tab.historyId?.let { texts.read(historyTextName(it)) }
            val text = fromHistory ?: texts.read(textName(tab.id)) ?: return@mapNotNull null
            // A tab kept before its text was the history's has its own besides: it is not needed.
            if (fromHistory != null) runCatching { texts.delete(textName(tab.id)) } else written[tab.id] = text
            OpenDocument(
                id = tab.id,
                name = tab.name,
                text = text,
                format = Format.entries.firstOrNull { it.name == tab.format },
                edited = tab.edited,
                view = tab.view(),
                // A text that is no longer the history's is the tab's own.
                historyId = tab.historyId.takeIf { fromHistory != null },
                firstRowHeader = tab.firstRowHeader
            )
        }
        if (documents.isNotEmpty()) workbench.reopen(documents, kept.selectedId)
    }

    private suspend fun keep(state: WorkbenchState) {
        // A write that fails leaves the device as it was; the next change writes again.
        runCatching {
            state.documents.forEach { document ->
                when {
                    document.historyId == null && written[document.id] != document.text -> {
                        texts.write(textName(document.id), document.text)
                        written[document.id] = document.text
                    }

                    // The history keeps the text of a document opened from it.
                    document.historyId != null && document.id in written -> {
                        texts.delete(textName(document.id))
                        written.remove(document.id)
                    }
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
                val view = document.view
                KeptTab(
                    id = document.id,
                    name = document.name,
                    format = document.format?.name,
                    edited = document.edited,
                    pane = view.pane.name,
                    historyId = document.historyId,
                    expanded = view.expanded,
                    selected = view.selected,
                    sortColumn = view.tableSort?.column,
                    sortDescending = view.tableSort?.descending == true,
                    hiddenColumns = view.hiddenColumns,
                    tableFilter = view.tableFilter,
                    query = view.query,
                    convertTo = view.convert.to?.name,
                    convertIndent = view.convert.indent,
                    convertSortKeys = view.convert.sortKeys,
                    convertNulls = view.convert.nulls.name,
                    compareWith = view.compare.with,
                    compareBy = view.compare.by.name,
                    typesLanguage = view.types.language.name,
                    typesRoot = view.types.root,
                    schemaWith = view.schema.with,
                    firstRowHeader = document.firstRowHeader
                )
            },
            selectedId = state.selectedId
        )
    }
}

/**
 * A tab as it is kept, with what its document was left showing; its text is kept apart, by [id]. What a tab kept before
 * the view was is read with the view a document opens with.
 */
@Serializable
private data class KeptTab(
    val id: Long,
    val name: String?,
    val format: String?,
    val edited: Boolean,
    val pane: String,
    val historyId: Long?,
    val expanded: Set<String>? = null,
    val selected: String? = null,
    val sortColumn: String? = null,
    val sortDescending: Boolean = false,
    val hiddenColumns: Set<String> = emptySet(),
    val tableFilter: String = "",
    val query: String = "",
    val convertTo: String? = null,
    val convertIndent: Int = 2,
    val convertSortKeys: Boolean = false,
    val convertNulls: String? = null,
    val compareWith: Long? = null,
    val compareBy: String? = null,
    val typesLanguage: String? = null,
    val typesRoot: String? = null,
    val schemaWith: Long? = null,
    val firstRowHeader: Boolean = true
) {
    /** What the document was left showing: its pane by name, or the tree where that pane is no longer one. */
    fun view() = DocumentView(
        expanded = expanded,
        selected = selected,
        pane = DocumentPane.entries.firstOrNull { it.name == pane } ?: DocumentPane.Tree,
        tableSort = sortColumn?.let { TableSort(it, sortDescending) },
        hiddenColumns = hiddenColumns,
        tableFilter = tableFilter,
        query = query,
        convert = ConvertChoice(
            to = Format.entries.firstOrNull { it.name == convertTo },
            indent = convertIndent,
            sortKeys = convertSortKeys,
            nulls = NullHandling.entries.firstOrNull { it.name == convertNulls } ?: NullHandling.LeaveOut
        ),
        compare = CompareChoice(with = compareWith, by = CompareBy.entries.firstOrNull { it.name == compareBy } ?: CompareBy.Meaning),
        types = TypesChoice(
            language = TypeLanguage.entries.firstOrNull { it.name == typesLanguage } ?: TypeLanguage.Kotlin,
            root = typesRoot
        ),
        schema = SchemaChoice(with = schemaWith)
    )
}

private fun textName(id: Long) = "open-$id"
