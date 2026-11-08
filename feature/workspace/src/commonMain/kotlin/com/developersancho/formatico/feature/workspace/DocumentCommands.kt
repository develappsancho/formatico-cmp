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
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.divider.PantrixDivider
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.overlay.menu.PantrixMenu
import com.pantrixui.components.overlay.menu.PantrixMenuItem
import com.pantrixui.components.overlay.popover.PopoverAlignment
import com.pantrixui.components.overlay.snackbar.LocalPantrixSnackbars
import com.pantrixui.components.overlay.snackbar.PantrixSnackbarState
import com.pantrixui.components.overlay.snackbar.SnackbarResult
import com.pantrixui.components.overlay.snackbar.SnackbarTone
import com.pantrixui.foundation.files.FileSaveResult
import com.pantrixui.foundation.files.PantrixFile
import com.pantrixui.foundation.files.rememberFileSaver
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.platform.rememberPantrixClipboard
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.convert_copied
import formatico.feature.workspace.generated.resources.convert_copy_failed
import formatico.feature.workspace.generated.resources.convert_label
import formatico.feature.workspace.generated.resources.query_results_copied
import formatico.feature.workspace.generated.resources.query_results_copy_failed
import formatico.feature.workspace.generated.resources.query_results_label
import formatico.feature.workspace.generated.resources.summary_copied
import formatico.feature.workspace.generated.resources.summary_copy_failed
import formatico.feature.workspace.generated.resources.summary_label
import formatico.feature.workspace.generated.resources.workspace_alias_before_anchor
import formatico.feature.workspace.generated.resources.workspace_already_formatted
import formatico.feature.workspace.generated.resources.workspace_already_minified
import formatico.feature.workspace.generated.resources.workspace_already_sorted
import formatico.feature.workspace.generated.resources.workspace_close_document
import formatico.feature.workspace.generated.resources.workspace_compare
import formatico.feature.workspace.generated.resources.workspace_convert
import formatico.feature.workspace.generated.resources.workspace_copied
import formatico.feature.workspace.generated.resources.workspace_copy_failed
import formatico.feature.workspace.generated.resources.workspace_document_label
import formatico.feature.workspace.generated.resources.workspace_edit_impossible
import formatico.feature.workspace.generated.resources.workspace_find_in_tree
import formatico.feature.workspace.generated.resources.workspace_formatted
import formatico.feature.workspace.generated.resources.workspace_keys_sorted
import formatico.feature.workspace.generated.resources.workspace_minified
import formatico.feature.workspace.generated.resources.workspace_minified_comments
import formatico.feature.workspace.generated.resources.workspace_more_actions
import formatico.feature.workspace.generated.resources.workspace_more_actions_sort_compare_save
import formatico.feature.workspace.generated.resources.workspace_path_copied
import formatico.feature.workspace.generated.resources.workspace_path_copy_failed
import formatico.feature.workspace.generated.resources.workspace_path_label
import formatico.feature.workspace.generated.resources.workspace_query
import formatico.feature.workspace.generated.resources.workspace_save_failed
import formatico.feature.workspace.generated.resources.workspace_save_file
import formatico.feature.workspace.generated.resources.workspace_show_summary_item
import formatico.feature.workspace.generated.resources.workspace_sort_keys
import formatico.feature.workspace.generated.resources.workspace_table_copied
import formatico.feature.workspace.generated.resources.workspace_table_copy_failed
import formatico.feature.workspace.generated.resources.workspace_table_label
import formatico.feature.workspace.generated.resources.workspace_undo
import formatico.feature.workspace.generated.resources.workspace_untitled
import formatico.feature.workspace.generated.resources.workspace_value_copied
import formatico.feature.workspace.generated.resources.workspace_value_copy_failed
import formatico.feature.workspace.generated.resources.workspace_value_label
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.stringResource

/** A document made from another, to be opened in a tab of its own: what a query found, or a conversion. */
internal data class NewDocument(val name: String, val text: String, val format: Format)

/** What is copied from a document besides the whole of it, each said in words of its own. */
internal enum class Copied { Value, Path, Table, Summary, Results, Converted }

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

/** What an edit says: what it did, that the document was so already, or that it cannot be made. */
private data class EditWords(
    val done: Map<DocumentEdit, String>,
    val already: Map<DocumentEdit, String>,
    val undo: String,
    val impossible: String,
    val aliasBeforeAnchor: String
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
        label = stringResource(Res.string.workspace_document_label),
        done = stringResource(Res.string.workspace_copied),
        failed = stringResource(Res.string.workspace_copy_failed),
        tag = "copied"
    ),
    parts = partWords(),
    edits = EditWords(
        done = mapOf(
            DocumentEdit.Format to stringResource(Res.string.workspace_formatted),
            DocumentEdit.Minify to stringResource(Res.string.workspace_minified),
            DocumentEdit.SortKeys to stringResource(Res.string.workspace_keys_sorted)
        ),
        already = mapOf(
            DocumentEdit.Format to stringResource(Res.string.workspace_already_formatted),
            DocumentEdit.Minify to stringResource(Res.string.workspace_already_minified),
            DocumentEdit.SortKeys to stringResource(Res.string.workspace_already_sorted)
        ),
        undo = stringResource(Res.string.workspace_undo),
        impossible = stringResource(Res.string.workspace_edit_impossible),
        aliasBeforeAnchor = stringResource(Res.string.workspace_alias_before_anchor)
    ),
    saveFailed = stringResource(Res.string.workspace_save_failed),
    untitled = stringResource(Res.string.workspace_untitled)
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
        label = stringResource(Res.string.workspace_value_label),
        done = stringResource(Res.string.workspace_value_copied),
        failed = stringResource(Res.string.workspace_value_copy_failed),
        tag = "value_copied"
    ),
    Copied.Path to CopyWords(
        label = stringResource(Res.string.workspace_path_label),
        done = stringResource(Res.string.workspace_path_copied),
        failed = stringResource(Res.string.workspace_path_copy_failed),
        tag = "path_copied"
    ),
    Copied.Table to CopyWords(
        label = stringResource(Res.string.workspace_table_label),
        done = stringResource(Res.string.workspace_table_copied),
        failed = stringResource(Res.string.workspace_table_copy_failed),
        tag = "table_copied"
    ),
    Copied.Summary to CopyWords(
        label = stringResource(Res.string.summary_label),
        done = stringResource(Res.string.summary_copied),
        failed = stringResource(Res.string.summary_copy_failed),
        tag = "summary_copied"
    ),
    Copied.Results to CopyWords(
        label = stringResource(Res.string.query_results_label),
        done = stringResource(Res.string.query_results_copied),
        failed = stringResource(Res.string.query_results_copy_failed),
        tag = "results_copied"
    ),
    Copied.Converted to CopyWords(
        label = stringResource(Res.string.convert_label),
        done = stringResource(Res.string.convert_copied),
        failed = stringResource(Res.string.convert_copy_failed),
        tag = "converted_copied"
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
                    getPluralString(Res.plurals.workspace_minified_comments, removed, removed)
                } else {
                    words.done.getValue(edit)
                },
                actionLabel = words.undo,
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

/** Which of a document's actions its "more" menu holds, by where it stands. */
internal enum class DocumentMenuItems {
    /** In a wide window's toolbar, where every other action has a button of its own. */
    Close,

    /** In a tablet's toolbar: sorting the keys, comparing, saving and closing. */
    SortCompareSaveClose,

    /**
     * In a phone's bar, where Format, Minify, Copy and Save stand at the bottom: finding in the tree, the Summary, the
     * query and Convert too.
     */
    FindSummarySortCompareClose
}

/**
 * The "more" button of a phone's bar while the document of [workbench] whose id is [documentId] is shown, and the menu
 * it opens: finding in the tree, showing the Summary, querying, converting, sorting the keys, comparing and closing the
 * document. A phone has no room for the search's button over its tree, nor a status bar whose size shows the Summary.
 */
@Composable
fun DocumentMoreMenu(documentId: Long, workbench: Workbench) {
    val commands = rememberDocumentCommands(workbench)
    val scope = rememberCoroutineScope()
    val state by workbench.state.collectAsState()
    val format = state.documents.firstOrNull { it.id == documentId }?.format
    DocumentMenu(
        items = DocumentMenuItems.FindSummarySortCompareClose,
        size = ButtonSize.Large,
        // Plain text has no tree to find in, nor values to sum up.
        read = format != null,
        onFind = { workbench.changeView(documentId) { it.copy(pane = DocumentPane.Tree, searching = true) } },
        onSummary = { workbench.changeView(documentId) { it.copy(pane = DocumentPane.Summary) } },
        onQuery = { workbench.changeView(documentId) { it.copy(pane = DocumentPane.Query) } },
        onConvert = { workbench.changeView(documentId) { it.copy(pane = DocumentPane.Convert) } },
        onCompare = { workbench.changeView(documentId) { it.copy(pane = DocumentPane.Compare) } },
        // Whether the document is valid is the workspace's to know; one with errors is told why it is not sorted.
        canSort = format?.edits?.contains(DocumentEdit.SortKeys) == true,
        onSortKeys = {
            scope.launch { commands.report(DocumentEdit.SortKeys, workbench.edit(documentId, DocumentEdit.SortKeys)) }
        },
        onSave = {},
        onClose = { workbench.close(documentId) }
    )
}

/** A document's "more" button and its menu, holding [items]. */
@Composable
internal fun DocumentMenu(
    items: DocumentMenuItems,
    size: ButtonSize,
    read: Boolean = true,
    onFind: () -> Unit = {},
    onSummary: () -> Unit = {},
    onQuery: () -> Unit = {},
    onConvert: () -> Unit = {},
    canSort: Boolean,
    onSortKeys: () -> Unit,
    onCompare: () -> Unit = {},
    onSave: () -> Unit,
    onClose: () -> Unit
) {
    var open by rememberSaveable { mutableStateOf(false) }
    PantrixMenu(
        visible = open,
        onDismissRequest = { open = false },
        anchor = {
            PantrixIconButton(
                imageVector = PantrixIcons.MoreHorizontal,
                // What the menu holds, where a tablet has moved its actions there, as Workspace-834 names it.
                contentDescription = stringResource(
                    when (items) {
                        DocumentMenuItems.SortCompareSaveClose -> Res.string.workspace_more_actions_sort_compare_save
                        else -> Res.string.workspace_more_actions
                    }
                ),
                onClick = { open = true },
                size = size,
                tagKey = "more"
            )
        },
        alignment = PopoverAlignment.End,
        tagKey = "document"
    ) {
        if (items == DocumentMenuItems.FindSummarySortCompareClose) FindSummaryAndQueryItems(read, onFind, onSummary, onQuery, onConvert)
        if (items != DocumentMenuItems.Close) {
            PantrixMenuItem(
                text = stringResource(Res.string.workspace_sort_keys),
                onClick = onSortKeys,
                enabled = canSort,
                tagKey = "sort_keys"
            )
            PantrixMenuItem(
                text = stringResource(Res.string.workspace_compare),
                onClick = onCompare,
                tagKey = "compare"
            )
        }
        if (items == DocumentMenuItems.SortCompareSaveClose) {
            PantrixMenuItem(
                text = stringResource(Res.string.workspace_save_file),
                onClick = onSave,
                icon = PantrixIcons.Download,
                tagKey = "save"
            )
        }
        if (items != DocumentMenuItems.Close) PantrixDivider()
        PantrixMenuItem(
            text = stringResource(Res.string.workspace_close_document),
            onClick = onClose,
            icon = PantrixIcons.Close,
            tagKey = "close"
        )
    }
}

/**
 * The rows only a phone's menu holds: finding in the tree, showing the Summary, querying and converting, which a [read]
 * document has. A phone has no room for the toolbar's Query and Convert.
 */
@Composable
private fun FindSummaryAndQueryItems(
    read: Boolean,
    onFind: () -> Unit,
    onSummary: () -> Unit,
    onQuery: () -> Unit,
    onConvert: () -> Unit
) {
    PantrixMenuItem(
        text = stringResource(Res.string.workspace_find_in_tree),
        onClick = onFind,
        icon = PantrixIcons.Search,
        enabled = read,
        tagKey = "find"
    )
    PantrixMenuItem(
        text = stringResource(Res.string.workspace_show_summary_item),
        onClick = onSummary,
        enabled = read,
        tagKey = "summary"
    )
    PantrixMenuItem(
        text = stringResource(Res.string.workspace_query),
        onClick = onQuery,
        enabled = read,
        tagKey = "query"
    )
    PantrixMenuItem(
        text = stringResource(Res.string.workspace_convert),
        onClick = onConvert,
        icon = PantrixIcons.ArrowForward,
        enabled = read,
        tagKey = "convert"
    )
}
