package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.feature.workspace.strings.WorkspaceStrings
import com.developersancho.formatico.feature.workspace.strings.convertStrings
import com.developersancho.formatico.feature.workspace.strings.queryStrings
import com.developersancho.formatico.feature.workspace.strings.schemaStrings
import com.developersancho.formatico.feature.workspace.strings.shareStrings
import com.developersancho.formatico.feature.workspace.strings.summaryStrings
import com.developersancho.formatico.feature.workspace.strings.typesStrings
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.divider.PantrixDivider
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.overlay.menu.PantrixMenu
import com.pantrixui.components.overlay.menu.PantrixMenuItem
import com.pantrixui.components.overlay.popover.PopoverAlignment
import com.pantrixui.components.overlay.snackbar.LocalPantrixSnackbars
import com.pantrixui.components.overlay.snackbar.PantrixSnackbarState
import com.pantrixui.components.overlay.snackbar.SnackbarDuration
import com.pantrixui.components.overlay.snackbar.SnackbarResult
import com.pantrixui.components.overlay.snackbar.SnackbarTone
import com.pantrixui.foundation.files.FileSaveResult
import com.pantrixui.foundation.files.PantrixFile
import com.pantrixui.foundation.files.rememberFileSaver
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.platform.rememberPantrixClipboard
import kotlinx.coroutines.launch

/** A document made from another, to be opened in a tab of its own: what a query found, or a conversion. */
internal data class NewDocument(val name: String, val text: String, val format: Format)

/** What is copied from a document besides the whole of it, each said in words of its own. */
internal enum class Copied { Value, Path, Table, Summary, Results, Converted, Types, Link }

/**
 * What a document's buttons do that is not the workspace's state: copying it, or a part of it ([copyText]), saving as
 * a file, saying what an edit did, with its undo, and opening what a query found or a conversion as a document of its
 * own ([open]).
 */
internal class DocumentCommands(
    val copy: (OpenDocument) -> Unit,
    val copyText: (Copied, String) -> Unit,
    val save: (OpenDocument) -> Unit,
    val saveCsv: (OpenDocument, String) -> Unit,
    val report: (DocumentEdit, EditResult) -> Unit,
    val open: (NewDocument) -> Unit = {}
)

/** What a copy says: what is copied, for the system's own preview of it, and how it went. */
private data class CopyWords(val label: String, val done: String, val failed: String, val tag: String)

/** What an edit says: what it did, that the document was so already, or that it cannot be made, and its [strings]. */
private data class EditWords(
    val done: Map<DocumentEdit, String>,
    val already: Map<DocumentEdit, String>,
    val undo: String,
    val impossible: String,
    val aliasBeforeAnchor: String,
    val strings: WorkspaceStrings
)

/** The words [DocumentCommands] say, read once from the resources. */
private data class CommandWords(
    val document: CopyWords,
    val parts: Map<Copied, CopyWords>,
    val edits: EditWords,
    val saveFailed: String,
    val untitled: String
)

@Composable
private fun commandWords() = CommandWords(
    document = CopyWords(
        label = workspaceStrings.documentLabel,
        done = workspaceStrings.copied,
        failed = workspaceStrings.copyFailed,
        tag = "copied"
    ),
    parts = partWords(),
    edits = EditWords(
        done = mapOf(
            DocumentEdit.Format to workspaceStrings.formatted,
            DocumentEdit.Minify to workspaceStrings.minified,
            DocumentEdit.SortKeys to workspaceStrings.keysSorted
        ),
        already = mapOf(
            DocumentEdit.Format to workspaceStrings.alreadyFormatted,
            DocumentEdit.Minify to workspaceStrings.alreadyMinified,
            DocumentEdit.SortKeys to workspaceStrings.alreadySorted
        ),
        undo = workspaceStrings.undo,
        impossible = workspaceStrings.editImpossible,
        aliasBeforeAnchor = workspaceStrings.aliasBeforeAnchor,
        strings = workspaceStrings
    ),
    saveFailed = workspaceStrings.saveFailed,
    untitled = workspaceStrings.untitled
)

/** The name a document is saved under: its own, or "Untitled" with its format's extension. */
internal fun fileNameOf(document: OpenDocument, untitled: String): String =
    document.name ?: "$untitled.${document.format?.extensions?.first() ?: "txt"}"

/** The commands of the documents in [workbench], with the snackbars, the clipboard and the file saver of this place. */
@Composable
internal fun rememberDocumentCommands(workbench: Workbench): DocumentCommands {
    val clipboard = rememberPantrixClipboard()
    val saver = rememberFileSaver()
    val snackbars = LocalPantrixSnackbars.current
    val scope = rememberCoroutineScope()
    val words = commandWords()

    fun copy(text: String, copyWords: CopyWords) {
        scope.launch {
            val copied = clipboard.copyText(text, label = copyWords.label)
            // A system that shows the copy itself is not echoed.
            if (!copied || !clipboard.traits.announcedBySystem) {
                snackbars?.show(
                    message = if (copied) copyWords.done else copyWords.failed,
                    tone = if (copied) SnackbarTone.Neutral else SnackbarTone.Danger,
                    tagKey = copyWords.tag
                )
            }
        }
    }

    fun save(file: PantrixFile) {
        saver.save(file) { result ->
            if (result == FileSaveResult.Failed) {
                scope.launch { snackbars?.show(message = words.saveFailed, tone = SnackbarTone.Danger, tagKey = "save_failed") }
            }
        }
    }

    return remember(clipboard, saver, snackbars, scope, words, workbench) {
        DocumentCommands(
            copy = { copy(it.text, words.document) },
            copyText = { part, text -> copy(text, words.parts.getValue(part)) },
            save = { document ->
                save(PantrixFile(fileNameOf(document, words.untitled), document.text.encodeToByteArray()))
            },
            open = { results -> workbench.open(results.name, results.text, results.format) },
            saveCsv = { document, csv ->
                val name = fileNameOf(document, words.untitled).substringBeforeLast('.') + ".csv"
                save(PantrixFile(name, csv.encodeToByteArray(), mimeType = "text/csv"))
            },
            report = { edit, result -> scope.launch { reportEdit(edit, result, snackbars, words.edits, workbench) } }
        )
    }
}

/** The words of what is copied from a document besides the whole of it. */
@Composable
private fun partWords(): Map<Copied, CopyWords> = mapOf(
    Copied.Value to CopyWords(
        label = workspaceStrings.valueLabel,
        done = workspaceStrings.valueCopied,
        failed = workspaceStrings.valueCopyFailed,
        tag = "value_copied"
    ),
    Copied.Path to CopyWords(
        label = workspaceStrings.pathLabel,
        done = workspaceStrings.pathCopied,
        failed = workspaceStrings.pathCopyFailed,
        tag = "path_copied"
    ),
    Copied.Table to CopyWords(
        label = workspaceStrings.tableLabel,
        done = workspaceStrings.tableCopied,
        failed = workspaceStrings.tableCopyFailed,
        tag = "table_copied"
    ),
    Copied.Summary to CopyWords(
        label = summaryStrings.label,
        done = summaryStrings.copied,
        failed = summaryStrings.copyFailed,
        tag = "summary_copied"
    ),
    Copied.Results to CopyWords(
        label = queryStrings.resultsLabel,
        done = queryStrings.resultsCopied,
        failed = queryStrings.resultsCopyFailed,
        tag = "results_copied"
    ),
    Copied.Converted to CopyWords(
        label = convertStrings.label,
        done = convertStrings.copied,
        failed = convertStrings.copyFailed,
        tag = "converted_copied"
    ),
    Copied.Types to CopyWords(
        label = typesStrings.label,
        done = typesStrings.copied,
        failed = typesStrings.copyFailed,
        tag = "types_copied"
    ),
    Copied.Link to CopyWords(
        label = shareStrings.linkLabel,
        done = shareStrings.linkCopied,
        failed = shareStrings.linkCopyFailed,
        tag = "link_copied"
    )
)

/** Says what [edit] came to, [result], in [snackbars]: a change with its undo, which gives [workbench] the document back. */
private suspend fun reportEdit(
    edit: DocumentEdit,
    result: EditResult,
    snackbars: PantrixSnackbarState?,
    words: EditWords,
    workbench: Workbench
) {
    when (result) {
        is EditResult.Changed -> {
            val removed = result.commentsRemoved
            val answer = snackbars?.show(
                message = if (removed > 0) {
                    words.strings.minifiedComments(removed, removed)
                } else {
                    words.done.getValue(edit)
                },
                actionLabel = words.undo,
                // It leaves by itself, its undo with it, in Pantrix's short time (the owner's decision, 2026-11-13):
                // the editor's own undo still takes the edit back.
                duration = SnackbarDuration.Short,
                closeButton = true,
                tagKey = "edited"
            )
            if (answer == SnackbarResult.ActionPerformed) workbench.restore(result.previous)
        }

        EditResult.Unchanged -> {
            snackbars?.show(
                message = words.already.getValue(edit),
                tagKey = "unchanged"
            )
        }

        EditResult.NotPossible -> {
            snackbars?.show(
                message = words.impossible,
                tone = SnackbarTone.Warning,
                tagKey = "not_possible"
            )
        }

        EditResult.AliasBeforeAnchor -> {
            snackbars?.show(
                message = words.aliasBeforeAnchor,
                tone = SnackbarTone.Warning,
                tagKey = "alias_before_anchor"
            )
        }
    }
}

/**
 * The "more" button of a phone's bar while the document of [workbench] whose id is [documentId] is shown, and the menu
 * it opens: finding in the text and in the tree, showing the Summary, querying, converting, writing types, sorting the
 * keys, comparing and closing the document. A phone has no room for the search's button over its tree, nor a status bar
 * whose size shows the Summary, and often no keyboard to open the editor's find bar with.
 */
@Composable
fun DocumentMoreMenu(documentId: Long, workbench: Workbench) {
    val commands = rememberDocumentCommands(workbench)
    val scope = rememberCoroutineScope()
    val state by workbench.state.collectAsState()
    val format = state.documents.firstOrNull { it.id == documentId }?.format
    DocumentMenu(
        size = ButtonSize.Large,
        // Plain text has no tree to find in, nor values to sum up.
        read = format != null,
        onFindText = { workbench.changeView(documentId) { it.copy(pane = DocumentPane.Text, findingText = true) } },
        onFind = { workbench.changeView(documentId) { it.copy(pane = DocumentPane.Tree, searching = true) } },
        onSummary = { workbench.changeView(documentId) { it.copy(pane = DocumentPane.Summary) } },
        onQuery = { workbench.changeView(documentId) { it.copy(pane = DocumentPane.Query) } },
        onConvert = { workbench.changeView(documentId) { it.copy(pane = DocumentPane.Convert) } },
        onTypes = { workbench.changeView(documentId) { it.copy(pane = DocumentPane.Types) } },
        onSchema = { workbench.changeView(documentId) { it.copy(pane = DocumentPane.Schema) } },
        onCompare = { workbench.changeView(documentId) { it.copy(pane = DocumentPane.Compare) } },
        // Whether the document is valid is the workspace's to know; one with errors is told why it is not sorted.
        canSort = format?.edits?.contains(DocumentEdit.SortKeys) == true,
        onSortKeys = {
            scope.launch { commands.report(DocumentEdit.SortKeys, workbench.edit(documentId, DocumentEdit.SortKeys)) }
        },
        onShare = { workbench.changeView(documentId) { it.copy(sharing = true) } },
        onClose = { workbench.close(documentId) }
    )
}

/**
 * A phone's "more" button and its menu: what its bars have no room for, as Workspace-390 draws it, where Format,
 * Minify, Copy and Save stand at the bottom. A tablet's and a wide window's are their toolbar's ([WorkspaceToolbar]).
 */
@Composable
internal fun DocumentMenu(
    size: ButtonSize,
    read: Boolean = true,
    onFindText: () -> Unit = {},
    onFind: () -> Unit = {},
    onSummary: () -> Unit = {},
    onQuery: () -> Unit = {},
    onConvert: () -> Unit = {},
    onTypes: () -> Unit = {},
    onSchema: () -> Unit = {},
    canSort: Boolean,
    onSortKeys: () -> Unit,
    onCompare: () -> Unit = {},
    onShare: () -> Unit = {},
    onClose: () -> Unit
) {
    var open by rememberSaveable { mutableStateOf(false) }
    PantrixMenu(
        visible = open,
        onDismissRequest = { open = false },
        anchor = {
            PantrixIconButton(
                imageVector = PantrixIcons.MoreHorizontal,
                contentDescription = workspaceStrings.moreActions,
                onClick = { open = true },
                size = size,
                tagKey = "more"
            )
        },
        alignment = PopoverAlignment.End,
        tagKey = "document"
    ) {
        FindAndSummaryItems(read, onFindText, onFind, onSummary)
        QueryConvertTypesItems(read, onQuery, onConvert, onTypes, onSchema)
        PantrixMenuItem(
            text = workspaceStrings.sortKeys,
            onClick = onSortKeys,
            enabled = canSort,
            tagKey = "sort_keys"
        )
        PantrixMenuItem(
            text = workspaceStrings.compare,
            onClick = onCompare,
            tagKey = "compare"
        )
        PantrixMenuItem(
            text = workspaceStrings.share,
            onClick = onShare,
            tagKey = "share"
        )
        PantrixDivider()
        PantrixMenuItem(
            text = workspaceStrings.closeDocument,
            onClick = onClose,
            icon = PantrixIcons.Close,
            tagKey = "close"
        )
    }
}

/**
 * A phone's menu items for what its bar has no room for: finding in the text, which opens the editor's find bar, as
 * Workspace-390's `more` state has it (the owner's decision, Faz 21); finding in the tree, and the Summary, which plain
 * text has neither of.
 */
@Composable
private fun FindAndSummaryItems(read: Boolean, onFindText: () -> Unit, onFind: () -> Unit, onSummary: () -> Unit) {
    PantrixMenuItem(
        text = workspaceStrings.findInText,
        onClick = onFindText,
        icon = PantrixIcons.Search,
        tagKey = "find_text"
    )
    PantrixMenuItem(
        text = workspaceStrings.findInTree,
        onClick = onFind,
        icon = PantrixIcons.Search,
        enabled = read,
        tagKey = "find"
    )
    PantrixMenuItem(
        text = workspaceStrings.showSummaryItem,
        onClick = onSummary,
        enabled = read,
        tagKey = "summary"
    )
}

/**
 * A phone's menu items for what is made of the document: its query, its conversion, its types and its validation
 * against a schema. Plain text has none.
 */
@Composable
private fun QueryConvertTypesItems(read: Boolean, onQuery: () -> Unit, onConvert: () -> Unit, onTypes: () -> Unit, onSchema: () -> Unit) {
    PantrixMenuItem(
        text = workspaceStrings.query,
        onClick = onQuery,
        enabled = read,
        tagKey = "query"
    )
    PantrixMenuItem(
        text = workspaceStrings.convert,
        onClick = onConvert,
        icon = PantrixIcons.ArrowForward,
        enabled = read,
        tagKey = "convert"
    )
    PantrixMenuItem(
        text = typesStrings.types,
        onClick = onTypes,
        enabled = read,
        tagKey = "types"
    )
    PantrixMenuItem(
        text = schemaStrings.schema,
        onClick = onSchema,
        enabled = read,
        tagKey = "schema"
    )
}
