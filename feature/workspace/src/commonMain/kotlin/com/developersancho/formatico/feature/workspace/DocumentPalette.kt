package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.Key
import com.developersancho.formatico.core.format.Format
import com.pantrixui.components.overlay.commandpalette.PantrixCommand
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.keyboard.PantrixShortcut
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.palette_compare
import formatico.feature.workspace.generated.resources.palette_compare_keywords
import formatico.feature.workspace.generated.resources.palette_convert_to
import formatico.feature.workspace.generated.resources.palette_format
import formatico.feature.workspace.generated.resources.palette_format_keywords
import formatico.feature.workspace.generated.resources.palette_group_convert
import formatico.feature.workspace.generated.resources.palette_group_document
import formatico.feature.workspace.generated.resources.palette_group_view
import formatico.feature.workspace.generated.resources.palette_minify_keywords
import formatico.feature.workspace.generated.resources.palette_query_jsonpath
import formatico.feature.workspace.generated.resources.palette_query_keywords
import formatico.feature.workspace.generated.resources.palette_query_xpath
import formatico.feature.workspace.generated.resources.palette_sort_keywords
import formatico.feature.workspace.generated.resources.palette_table
import formatico.feature.workspace.generated.resources.palette_tree
import formatico.feature.workspace.generated.resources.workspace_copy_document
import formatico.feature.workspace.generated.resources.workspace_minify
import formatico.feature.workspace.generated.resources.workspace_save_file
import formatico.feature.workspace.generated.resources.workspace_sort_keys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The keys of the palette's document commands, the same in every language: what [DocumentPalette.run] is given. */
internal object DocumentCommandKeys {
    const val FORMAT = "format"
    const val MINIFY = "minify"
    const val SORT_KEYS = "sort"
    const val COPY = "copy"
    const val SAVE = "save"
    const val COMPARE = "compare"
    const val TREE = "tree"
    const val TABLE = "table"
    const val QUERY = "query"

    /** The key of converting to [format]: `convert_yaml`. */
    fun convert(format: Format): String = "convert_${format.name.lowercase()}"
}

/**
 * The keys that run the palette's document commands, as the Palette boards write them: Format and Minify with Option
 * and Shift, as an editor's own Format does, and the views by their numbers. A browser keeps Command and a number for
 * its own tabs: there the views have none.
 */
internal object DocumentShortcuts {
    val Format = PantrixShortcut(Key.F, shift = true, alt = true)
    val Minify = PantrixShortcut(Key.M, shift = true, alt = true)
    val Copy = PantrixShortcut(Key.C, command = true, shift = true)
    val Save = PantrixShortcut(Key.S, command = true)
    val Tree = PantrixShortcut(Key.One, command = true)
    val Table = PantrixShortcut(Key.Two, command = true)
    val Query = PantrixShortcut(Key.F, command = true, shift = true)
}

/** The formats the palette offers to convert to, the most converted to: every one of them but the document's own. */
private val PaletteTargets = listOf(Format.Json, Format.Yaml, Format.Toml, Format.Xml, Format.Csv)

/**
 * The command palette's commands for [document], the one shown, under their groups: Document's (Format, Minify, Sort
 * keys, Copy, Save and Compare), View's (the tree, the table and the query) and Convert's (to the formats most converted
 * to), as the Palette boards list them. A phone lists fewer, the ones its screen has no button for at hand, and writes
 * no shortcuts; a command the document cannot take, as Sort keys for a CSV or a query of plain text, is listed faint.
 */
@Composable
fun documentPaletteCommands(document: OpenDocument): List<PantrixCommand> =
    documentAndViewCommands(document.format) + convertGroup(document.format)

/**
 * The palette's commands of a document that have a shortcut where the app runs, under their groups, as Settings'
 * Keyboard shortcuts lists them: those of a JSON document, whose query is JSONPath's. A phone writes none, and a
 * browser none of the keys it keeps for itself.
 */
@Composable
fun documentShortcutCommands(): List<PantrixCommand> = documentAndViewCommands(Format.Json).filter { it.shortcut != null }

/** The Document and the View groups of a document of [format], with the keys the window and the platform give them. */
@Composable
private fun documentAndViewCommands(format: Format?): List<PantrixCommand> {
    val phone = workspaceLayout() == WorkspaceLayout.Phone
    val browser = PantrixTheme.platform.type == PlatformType.Web
    val keys: (PantrixShortcut) -> PantrixShortcut? = { if (phone) null else it }
    return documentGroup(format, phone, keys) + viewGroup(format, phone, browser, keys)
}

/** The Document group: Format, Minify, Sort keys (not on a phone), Copy, Save and Compare (not on a phone). */
@Composable
private fun documentGroup(format: Format?, phone: Boolean, keys: (PantrixShortcut) -> PantrixShortcut?): List<PantrixCommand> {
    val group = stringResource(Res.string.palette_group_document)
    val edits = format?.edits.orEmpty()
    return listOfNotNull(
        PantrixCommand(
            key = DocumentCommandKeys.FORMAT,
            label = stringResource(Res.string.palette_format),
            group = group,
            shortcut = keys(DocumentShortcuts.Format),
            keywords = keywords(Res.string.palette_format_keywords),
            enabled = DocumentEdit.Format in edits
        ),
        PantrixCommand(
            key = DocumentCommandKeys.MINIFY,
            label = stringResource(Res.string.workspace_minify),
            group = group,
            shortcut = keys(DocumentShortcuts.Minify),
            keywords = keywords(Res.string.palette_minify_keywords),
            enabled = DocumentEdit.Minify in edits
        ),
        PantrixCommand(
            key = DocumentCommandKeys.SORT_KEYS,
            label = stringResource(Res.string.workspace_sort_keys),
            group = group,
            keywords = keywords(Res.string.palette_sort_keywords),
            enabled = DocumentEdit.SortKeys in edits
        ).takeUnless { phone },
        PantrixCommand(
            key = DocumentCommandKeys.COPY,
            label = stringResource(Res.string.workspace_copy_document),
            group = group,
            icon = PantrixIcons.Copy,
            shortcut = keys(DocumentShortcuts.Copy)
        ),
        PantrixCommand(
            key = DocumentCommandKeys.SAVE,
            label = stringResource(Res.string.workspace_save_file),
            group = group,
            icon = PantrixIcons.Download,
            shortcut = keys(DocumentShortcuts.Save)
        ),
        PantrixCommand(
            key = DocumentCommandKeys.COMPARE,
            label = stringResource(Res.string.palette_compare),
            group = group,
            keywords = keywords(Res.string.palette_compare_keywords)
        ).takeUnless { phone }
    )
}

/**
 * The View group: the tree and the table (not on a phone, whose screen has them at hand), and the query, JSONPath's or
 * XPath's; none of them of plain text. A [browser] keeps Command and a number for its own tabs.
 */
@Composable
private fun viewGroup(
    format: Format?,
    phone: Boolean,
    browser: Boolean,
    keys: (PantrixShortcut) -> PantrixShortcut?
): List<PantrixCommand> {
    val group = stringResource(Res.string.palette_group_view)
    val read = format != null
    return listOfNotNull(
        PantrixCommand(
            key = DocumentCommandKeys.TREE,
            label = stringResource(Res.string.palette_tree),
            group = group,
            shortcut = keys(DocumentShortcuts.Tree).takeUnless { browser },
            enabled = read
        ).takeUnless { phone },
        PantrixCommand(
            key = DocumentCommandKeys.TABLE,
            label = stringResource(Res.string.palette_table),
            group = group,
            shortcut = keys(DocumentShortcuts.Table).takeUnless { browser },
            enabled = read
        ).takeUnless { phone },
        PantrixCommand(
            key = DocumentCommandKeys.QUERY,
            label = stringResource(if (format == Format.Xml) Res.string.palette_query_xpath else Res.string.palette_query_jsonpath),
            group = group,
            icon = PantrixIcons.Search,
            shortcut = keys(DocumentShortcuts.Query),
            keywords = keywords(Res.string.palette_query_keywords),
            enabled = read
        )
    )
}

/** The Convert group: to each of the formats most converted to but the document's own; none of plain text. */
@Composable
private fun convertGroup(format: Format?): List<PantrixCommand> {
    val group = stringResource(Res.string.palette_group_convert)
    return PaletteTargets.filter { it != format }.map { target ->
        PantrixCommand(
            key = DocumentCommandKeys.convert(target),
            label = stringResource(Res.string.palette_convert_to, target.displayName),
            group = group,
            icon = PantrixIcons.ArrowForward,
            enabled = format != null
        )
    }
}

/** The other words [words] lists, by commas, that find a command. */
@Composable
private fun keywords(words: StringResource): List<String> = stringResource(words).split(',').map { it.trim() }.filter { it.isNotEmpty() }

/**
 * What running a document command of the palette does to the documents of [workbench]: an edit is made and reported
 * as the toolbar's is, a copy and a save are the toolbar's own, and a view is shown as its button shows it.
 */
@Stable
class DocumentPalette internal constructor(
    private val workbench: Workbench,
    private val commands: DocumentCommands,
    private val scope: CoroutineScope
) {
    /** Runs the palette's command [key] on the document whose id is [documentId]: false where [key] is none of its. */
    fun run(key: String, documentId: Long): Boolean {
        val document = workbench.state.value.documents.firstOrNull { it.id == documentId } ?: return false
        val target = PaletteTargets.firstOrNull { DocumentCommandKeys.convert(it) == key }
        when {
            key == DocumentCommandKeys.FORMAT -> edit(documentId, DocumentEdit.Format)

            key == DocumentCommandKeys.MINIFY -> edit(documentId, DocumentEdit.Minify)

            key == DocumentCommandKeys.SORT_KEYS -> edit(documentId, DocumentEdit.SortKeys)

            key == DocumentCommandKeys.COPY -> commands.copy(document)

            key == DocumentCommandKeys.SAVE -> commands.save(document)

            key == DocumentCommandKeys.COMPARE -> show(documentId, DocumentPane.Compare)

            key == DocumentCommandKeys.TREE -> show(documentId, DocumentPane.Tree)

            key == DocumentCommandKeys.TABLE -> show(documentId, DocumentPane.Table)

            key == DocumentCommandKeys.QUERY -> show(documentId, DocumentPane.Query)

            target != null -> workbench.changeView(documentId) { view ->
                view.copy(pane = DocumentPane.Convert, convert = view.convert.copy(to = target))
            }

            else -> return false
        }
        return true
    }

    private fun edit(documentId: Long, edit: DocumentEdit) {
        scope.launch { commands.report(edit, workbench.edit(documentId, edit)) }
    }

    private fun show(documentId: Long, pane: DocumentPane) = workbench.changeView(documentId) { it.copy(pane = pane) }
}

/** What runs the palette's document commands on the documents of [workbench]. */
@Composable
fun rememberDocumentPalette(workbench: Workbench): DocumentPalette {
    val commands = rememberDocumentCommands(workbench)
    val scope = rememberCoroutineScope()
    return remember(workbench, commands, scope) { DocumentPalette(workbench, commands, scope) }
}
