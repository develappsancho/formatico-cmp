package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.json.JsonDialect
import com.developersancho.formatico.core.format.json.JsonDocument
import com.developersancho.formatico.core.format.json.formatted
import com.developersancho.formatico.core.format.json.minified
import com.developersancho.formatico.core.format.text.Indentation
import com.developersancho.formatico.core.format.text.TextLayout
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * A document open in a tab.
 *
 * @property id what tells it from the other open documents.
 * @property name its file's name, or null for a new document or a text that was pasted.
 * @property text what it says now.
 * @property format the format it is read as, or null for plain text.
 * @property edited whether its text was changed since it was opened.
 * @property view how its tree was left, to be found so again when its tab is chosen.
 */
data class OpenDocument(
    val id: Long,
    val name: String?,
    val text: String,
    val format: Format?,
    val edited: Boolean = false,
    val view: DocumentView = DocumentView()
)

/**
 * How a document's tree was left.
 *
 * @property expanded the keys of the open branches, or null for those a document opens with.
 * @property selected the key of the chosen value, or null for none.
 * @property pane what a phone shows of it: its text or its tree.
 */
data class DocumentView(
    val expanded: Set<String>? = null,
    val selected: String? = null,
    val pane: DocumentPane = DocumentPane.Tree
)

/** What a phone shows of a document, one at a time. */
enum class DocumentPane { Text, Tree }

/**
 * The open documents, in the order of their tabs.
 *
 * @property selectedId the document whose tab is chosen, or null for a new tab: the Start screen.
 */
data class WorkbenchState(val documents: List<OpenDocument> = emptyList(), val selectedId: Long? = null) {
    /** The document whose tab is chosen, or null on a new tab. */
    val selected: OpenDocument? get() = documents.firstOrNull { it.id == selectedId }
}

/** A change Formatico makes to a whole document. */
enum class DocumentEdit {
    /** A value to a line, each level indented. */
    Format,

    /** One line, no space and no comment. */
    Minify,

    /** Every object's keys sorted by name, a value to a line. */
    SortKeys
}

/** What came of a [DocumentEdit]. */
sealed interface EditResult {
    /** The text changed; [previous] is the document as it was, to undo it with [Workbench.restore]. */
    data class Changed(val previous: OpenDocument) : EditResult

    /** The text was as the edit would make it already. */
    data object Unchanged : EditResult

    /** The document is not one Formatico can change so: a format it does not read yet, or one with errors. */
    data object NotPossible : EditResult
}

/** The JSON a format is read as, or null for a format Formatico does not read yet. */
internal val Format.jsonDialect: JsonDialect?
    get() = when (this) {
        Format.Json -> JsonDialect.Json
        Format.Jsonc -> JsonDialect.Jsonc
        Format.Json5 -> JsonDialect.Json5
        else -> null
    }

/**
 * The documents open in Formatico's tabs, and the changes made to them: what the bar's tabs and every workspace
 * show. It lives as long as the app does.
 *
 * @param worker where a document is read and written out again, off the main thread.
 */
class Workbench(private val worker: CoroutineDispatcher = Dispatchers.Default) {
    private val mutableState = MutableStateFlow(WorkbenchState())
    private var nextId = 1L

    /** The open documents and the chosen tab. */
    val state: StateFlow<WorkbenchState> = mutableState.asStateFlow()

    /**
     * Opens a document in a tab of its own and chooses it, in the place of the new tab it was opened from. A document
     * open already with the same name and text is chosen instead of opened twice.
     *
     * @return the document's id.
     */
    fun open(name: String?, text: String, format: Format?): Long {
        val open = mutableState.value.documents.firstOrNull { it.name == name && it.text == text }
        if (open != null) {
            select(open.id)
            return open.id
        }
        val document = OpenDocument(id = nextId++, name = name, text = text, format = format)
        mutableState.update { it.copy(documents = it.documents + document, selectedId = document.id) }
        return document.id
    }

    /** Chooses the tab of the document whose id is [id], or a new tab where it is null. */
    fun select(id: Long?) {
        mutableState.update { state -> state.copy(selectedId = id?.takeIf { id in state.documents.map { it.id } }) }
    }

    /** Closes the document whose id is [id]. Where its tab was chosen, the next one is, or else the one before. */
    fun close(id: Long) {
        mutableState.update { state ->
            val index = state.documents.indexOfFirst { it.id == id }
            if (index < 0) return@update state
            val documents = state.documents - state.documents[index]
            val selectedId = if (state.selectedId != id) {
                state.selectedId
            } else {
                documents.getOrNull(index)?.id ?: documents.getOrNull(index - 1)?.id
            }
            state.copy(documents = documents, selectedId = selectedId)
        }
    }

    /** Reads the document whose id is [id] as [format] from now on, or as plain text where it is null. */
    fun changeFormat(id: Long, format: Format?) {
        change(id) { it.copy(format = format, view = it.view.copy(expanded = null, selected = null)) }
    }

    /** Keeps how the document whose id is [id] is shown: its open branches, its chosen value, its pane. */
    fun changeView(id: Long, transform: (DocumentView) -> DocumentView) {
        change(id) { it.copy(view = transform(it.view)) }
    }

    /** Makes [edit] to the document whose id is [id], reading it and writing it out off the main thread. */
    suspend fun edit(id: Long, edit: DocumentEdit): EditResult {
        val document = mutableState.value.documents.firstOrNull { it.id == id } ?: return EditResult.NotPossible
        val text = withContext(worker) { edited(document, edit) } ?: return EditResult.NotPossible
        if (text == document.text) return EditResult.Unchanged
        change(id) { it.copy(text = text, edited = true) }
        return EditResult.Changed(previous = document)
    }

    /** Gives the document [previous]'s text back, as an edit's undo does, where it is still open. */
    fun restore(previous: OpenDocument) {
        change(previous.id) { it.copy(text = previous.text, edited = previous.edited) }
    }

    private fun change(id: Long, change: (OpenDocument) -> OpenDocument) {
        mutableState.update { state ->
            state.copy(documents = state.documents.map { if (it.id == id) change(it) else it })
        }
    }

    /** The text [edit] makes of [document], or null where it cannot be made. */
    private fun edited(document: OpenDocument, edit: DocumentEdit): String? {
        val dialect = document.format?.jsonDialect ?: return null
        val json = JsonDocument.parse(document.text, dialect)
        if (!json.isValid) return null
        val indentation = TextLayout.of(document.text).indentation ?: Indentation.Spaces(2)
        return when (edit) {
            DocumentEdit.Format -> json.formatted(indentation)
            DocumentEdit.Minify -> json.minified()
            DocumentEdit.SortKeys -> json.formatted(indentation, sortKeys = true)
        }
    }
}
