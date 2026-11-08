package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.designsystem.LocalSyntaxColors
import com.developersancho.formatico.core.designsystem.SyntaxColors
import com.developersancho.formatico.core.format.json.jsonPath
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.data.lazytreeview.PantrixLazyTreeView
import com.pantrixui.components.data.lazytreeview.TreeSource
import com.pantrixui.components.data.lazytreeview.rememberTreeViewState
import com.pantrixui.components.data.treeview.TreeNode
import com.pantrixui.components.data.treeview.TreeRowAction
import com.pantrixui.components.data.treeview.TreeSelection
import com.pantrixui.components.data.treeview.TreeViewStyle
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.l10n.PantrixLocale
import com.pantrixui.foundation.theme.PantrixTheme
import com.pantrixui.foundation.token.typography.TypeRole

/**
 * What a search marks in the tree's rows: what is [sought], in small letters as [language] writes them, and the key of
 * the [current] match.
 */
@Immutable
internal data class TreeMarks(val sought: String, val locale: PantrixLocale, val current: String?)

/**
 * The colours a row's label is written in: the document's [syntax], an object's or an array's size in [size], a
 * search's match on [match] and the current match in [onCurrent] on [current].
 */
@Immutable
internal data class RowInks(
    val syntax: SyntaxColors,
    val size: Color,
    val match: Color,
    val current: Color,
    val onCurrent: Color
) {
    /** A key's style, made once for every row: a large document's tree has a hundred thousand of them. */
    val keyStyle = SpanStyle(color = syntax.key)

    /** The colon's style. */
    val colonStyle = SpanStyle(color = syntax.punctuation)

    private val kindStyles = ValueKind.entries.map { SpanStyle(color = syntaxInk(it, syntax, size)) }

    /** The style a value of [kind] is written in: its syntax's, and the size of an object or an array quieter. */
    fun styleOf(kind: ValueKind): SpanStyle = kindStyles[kind.ordinal]
}

@Composable
private fun rowInks(): RowInks {
    val syntax = LocalSyntaxColors.current
    val colors = PantrixTheme.colors
    return remember(syntax, colors) {
        RowInks(syntax, colors.textSecondary, colors.warning.container, colors.warning.color, colors.warning.onColor)
    }
}

/**
 * What a row draws at its end for its value: what its string looks like it holds, its YAML [anchor], and the path an
 * alias points to. [written] is a string's value, as a colour's swatch says it.
 */
@Immutable
internal data class RowEnd(val preview: ValuePreview?, val written: String?, val anchor: String?, val target: String?)

/**
 * The tree of [root] as [PantrixLazyTreeView] asks for it, a row at a time: each value as `name: value` in the document's
 * colours [inks], which the tree writes a chosen row's in its own, and what a search [marks] marked in it. A row is made
 * only as it is about to be shown: in a large document the values are a hundred thousand, and the screen shows thirty.
 * Another search is another source; the value chosen and the branches opened are the tree's, and make none.
 */
internal class DocumentSource(private val root: ValueNode, private val inks: RowInks, private val marks: TreeMarks?) : TreeSource {
    // The values of the rows made, by their keys: a row the tree asks about is one it was given.
    private val values = HashMap<String, ValueNode>()

    // What each row made ends in, looked up for the rows shown alone.
    private val ends = HashMap<String, RowEnd?>()

    override fun childCount(parent: TreeNode?): Int = if (parent == null) 1 else valueOf(parent).children.size

    override fun childAt(parent: TreeNode?, index: Int): TreeNode = rowOf(if (parent == null) root else valueOf(parent).children[index])

    override fun indexOf(parent: TreeNode?, key: String): Int =
        if (parent == null) (if (key == root.key) 0 else -1) else valueOf(parent).children.indexOfFirst { it.key == key }

    /** What the row of [key] draws at its end, or null for nothing. */
    fun endOf(key: String): RowEnd? = ends.getOrPut(key) { values[key]?.let(::endOf) }

    /** The row of [node]: its label and its colours, and whether it is a branch, its children the tree's to ask for. */
    fun rowOf(node: ValueNode): TreeNode {
        values[node.key] = node
        val label = rowLabel(node)
        return TreeNode(
            key = node.key,
            label = label,
            hasChildren = node.children.isNotEmpty(),
            tagKey = tagOf(node),
            styledLabel = styledLabelOf(node, inks, marks = marks, label = label)
        )
    }

    private fun valueOf(row: TreeNode): ValueNode = values.getOrPut(row.key) { trail(root, row.key).last() }

    // Another search, or another document, is another source; one value chosen or another is not. A document is the
    // same one by its identity: weighing its values would walk all of them.
    override fun equals(other: Any?): Boolean = other is DocumentSource && other.root === root && other.inks == inks && other.marks == marks

    override fun hashCode(): Int = 31 * inks.hashCode() + marks.hashCode()
}

/**
 * A value as its row writes it: `name: value`, an object or an array by its size; the root's `$` with a space and no
 * colon, `$ {4}`, which written together would read as a template (the owner's decision, Faz 21).
 */
internal fun rowLabel(node: ValueNode): String = node.name + colonOf(node) + node.text

private fun colonOf(node: ValueNode): String = if (node.path.isEmpty()) " " else ": "

/**
 * [rowLabel] in the document's colours: the key, the colon and the value each in its kind's, which the tree writes a
 * chosen row's in its own; and over them what [marks] found in the key and in the value, the current match stronger,
 * whose own colours the tree keeps on a chosen row too. [label] is the row's label, which the styled one is written
 * over.
 */
internal fun styledLabelOf(
    node: ValueNode,
    inks: RowInks,
    marks: TreeMarks?,
    label: String = rowLabel(node)
): AnnotatedString {
    val nameEnd = node.name.length
    val valueStart = nameEnd + colonOf(node).length
    val parts = listOf(
        AnnotatedString.Range(inks.keyStyle, 0, nameEnd),
        AnnotatedString.Range(inks.colonStyle, nameEnd, valueStart),
        AnnotatedString.Range(inks.styleOf(node.kind), valueStart, label.length)
    )
    val styled = AnnotatedString(label, parts)
    if (marks == null) return styled
    return buildAnnotatedString {
        append(styled)
        val mark = if (node.key == marks.current) {
            SpanStyle(color = inks.onCurrent, background = inks.current)
        } else {
            SpanStyle(background = inks.match)
        }
        if (nameHolds(node, marks.sought, marks.locale)) {
            placesOf(marks.sought, node.name, marks.locale).forEach { addStyle(mark, it.first, it.last + 1) }
        }
        if (valueHolds(node, marks.sought, marks.locale)) {
            placesOf(marks.sought, node.text, marks.locale).forEach { addStyle(mark, valueStart + it.first, valueStart + it.last + 1) }
        }
    }
}

/**
 * Where [sought], in small letters as [locale] writes them, stands in [text]: the ranges of [text]'s own letters, each
 * found after the one before it. A text folded is as long as itself, so that a place in it is the same place in [text].
 */
internal fun placesOf(sought: String, text: String, locale: PantrixLocale): List<IntRange> {
    if (sought.isEmpty()) return emptyList()
    val small = locale.fold(text)
    val places = mutableListOf<IntRange>()
    var at = small.indexOf(sought)
    while (at >= 0) {
        places += at until at + sought.length
        at = small.indexOf(sought, at + sought.length)
    }
    return places
}

private fun endOf(node: ValueNode): RowEnd? {
    val end = RowEnd(
        preview = node.preview,
        written = node.value,
        anchor = node.anchor?.let { "&$it" },
        target = node.target?.let { jsonPath(it.path) }
    )
    return end.takeIf { it.preview != null || it.anchor != null || it.target != null }
}

/**
 * The document's tree: a value to a row in the document's colours, what its strings look like they hold at the row's
 * end, and what a search found, as its [marks] say. A link's row ends in the button that opens it, in the browser or
 * the app that handles the address, without choosing the row. The chosen value's row is brought into view as it is
 * chosen, in the text, by the search or by going to an anchor.
 */
@Composable
internal fun DocumentTree(
    state: WorkspaceState,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    modifier: Modifier = Modifier,
    marks: TreeMarks? = null
) {
    val root = state.root ?: return
    val inks = rowInks()
    val selected = state.selected?.key
    val source = remember(root, inks, marks) { DocumentSource(root, inks, marks) }
    val words = previewWords()
    val links = LocalUriHandler.current
    val openLink = workspaceStrings.openLink
    val tree = rememberTreeViewState()
    LaunchedEffect(tree, selected) {
        tree.scrollToKey(selected ?: return@LaunchedEffect)
    }
    PantrixLazyTreeView(
        source = source,
        expanded = state.expanded,
        onExpandedChange = { onIntent(WorkspaceIntent.Expand(it)) },
        modifier = modifier,
        state = tree,
        selection = TreeSelection.Single,
        selected = setOfNotNull(selected),
        onSelectionChange = { onIntent(WorkspaceIntent.Select(it.firstOrNull())) },
        trailing = { node -> source.endOf(node.key)?.let { RowEndOf(it, layout, chosen = node.key == selected, words = words) } },
        rowAction = { node ->
            (source.endOf(node.key)?.preview as? ValuePreview.Link)?.let { link ->
                TreeRowAction(icon = PantrixIcons.ExternalLink, label = openLink) { links.open(link.url) }
            }
        },
        style = TreeViewStyle(
            rowHeight = when (layout) {
                WorkspaceLayout.Wide -> 32.dp
                WorkspaceLayout.Tablet -> 36.dp
                WorkspaceLayout.Phone -> 44.dp
            },
            indent = PantrixTheme.spacing.lg,
            labelRole = TypeRole.Code
        ),
        tagKey = "document"
    )
}

/** Opens [url] in the browser, or the app that handles it; an address nothing on the device opens is let go. */
private fun UriHandler.open(url: String) {
    try {
        openUri(url)
    } catch (@Suppress("SwallowedException") unopened: IllegalArgumentException) {
        // No app opens the address: the press does nothing rather than end the app, as Pantrix's own links do.
    }
}
