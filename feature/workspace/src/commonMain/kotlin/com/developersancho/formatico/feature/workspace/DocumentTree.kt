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
import com.pantrixui.components.data.lazytreeview.rememberTreeViewState
import com.pantrixui.components.data.treeview.TreeNode
import com.pantrixui.components.data.treeview.TreeRowAction
import com.pantrixui.components.data.treeview.TreeSelection
import com.pantrixui.components.data.treeview.TreeViewStyle
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme
import com.pantrixui.foundation.token.typography.TypeRole

/**
 * What a search marks in the tree's rows: what is [sought], in small letters as [language] writes them, and the key of
 * the [current] match.
 */
@Immutable
internal data class TreeMarks(val sought: String, val language: String, val current: String?)

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

/** The tree's [nodes] for a document, and what each row draws at its end, by the node's key. */
internal class DocumentRows(val nodes: List<TreeNode>, val ends: Map<String, RowEnd>)

/**
 * The rows of [root] that the [expanded] branches show: each value as `name: value` in the document's colours, which
 * the tree writes a chosen row's in its own, and what a search [marks] marked in each. A closed branch's rows are made
 * as it opens: in a large document they are most of it, and the tree shows none of them.
 */
internal fun documentRows(root: ValueNode, inks: RowInks, marks: TreeMarks?, expanded: Set<String>): DocumentRows =
    RowMaker(root, inks).rows(marks, expanded)

/**
 * Makes the rows of [root] in [inks], as [documentRows] says, again each time the search or the open branches change,
 * keeping every row that did not change: a branch opened makes that row again and the rows it stands in, not the
 * hundred thousand beside them. A new search makes them all again; a value chosen, none.
 */
internal class RowMaker(private val root: ValueNode, private val inks: RowInks) {
    private var made: TreeNode? = null
    private var marks: TreeMarks? = null
    private var expanded: Set<String> = emptySet()

    // What each row made ends in: the same for a key while the document is, and only looked up for the rows shown.
    private val ends = HashMap<String, RowEnd>()

    /** The rows for [marks] and [expanded], the rows made last kept where nothing of theirs changed. */
    fun rows(marks: TreeMarks?, expanded: Set<String>): DocumentRows {
        val before = made.takeIf { marks == this.marks }
        // The keys whose rows are made again: the branches opened or closed.
        val changed = HashSet<String>()
        expanded.filterTo(changed) { it !in this.expanded }
        this.expanded.filterTo(changed) { it !in expanded }
        val keeping = before.takeIf { changed.size <= MOST_CHANGED }

        fun nodeOf(node: ValueNode, old: TreeNode?): TreeNode {
            // A row is kept where no key that changed is its own or one under it, as a key under it starts with its.
            if (old != null && changed.none { it.startsWith(node.key) }) return old
            endOf(node)?.let { ends[node.key] = it }
            val label = rowLabel(node)
            val open = node.key in expanded
            // The rows under it made before stand in the same order as its children, where it was open.
            val oldChildren = old?.children?.takeIf { it.size == node.children.size }
            return TreeNode(
                key = node.key,
                label = label,
                children = if (open) node.children.mapIndexed { index, child -> nodeOf(child, oldChildren?.get(index)) } else emptyList(),
                hasChildren = node.children.isNotEmpty(),
                tagKey = tagOf(node),
                styledLabel = styledLabelOf(node, inks, marks = marks, label = label)
            )
        }
        val top = nodeOf(root, keeping)
        made = top
        this.marks = marks
        this.expanded = expanded
        return DocumentRows(listOf(top), ends)
    }

    private companion object {
        // Past so many changed keys, as "All" opens every branch, each row is made again rather than weighed.
        const val MOST_CHANGED = 64
    }
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
        if (nameHolds(node, marks.sought, marks.language)) {
            placesOf(marks.sought, node.name, marks.language).forEach { addStyle(mark, it.first, it.last + 1) }
        }
        if (valueHolds(node, marks.sought, marks.language)) {
            placesOf(marks.sought, node.text, marks.language).forEach { addStyle(mark, valueStart + it.first, valueStart + it.last + 1) }
        }
    }
}

/**
 * Where [sought], in small letters as [language] writes them, stands in [text]: the ranges of [text]'s own letters,
 * each found after the one before it.
 */
internal fun placesOf(sought: String, text: String, language: String): List<IntRange> {
    if (sought.isEmpty()) return emptyList()
    // A letter can grow in small letters, as "İ" does outside Turkish: each small letter keeps where it came from.
    val small = StringBuilder()
    val from = ArrayList<Int>(text.length)
    text.forEachIndexed { index, letter ->
        folded(letter.toString(), language).forEach {
            small.append(it)
            from += index
        }
    }
    val places = mutableListOf<IntRange>()
    var at = small.indexOf(sought)
    while (at >= 0) {
        places += from[at]..from[at + sought.length - 1]
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
    val maker = remember(root, inks) { RowMaker(root, inks) }
    val rows = remember(maker, marks, state.expanded) { maker.rows(marks, state.expanded) }
    val words = previewWords()
    val links = LocalUriHandler.current
    val openLink = workspaceStrings.openLink
    val tree = rememberTreeViewState()
    LaunchedEffect(tree, selected) {
        tree.scrollToKey(selected ?: return@LaunchedEffect)
    }
    PantrixLazyTreeView(
        nodes = rows.nodes,
        expanded = state.expanded,
        onExpandedChange = { onIntent(WorkspaceIntent.Expand(it)) },
        modifier = modifier,
        state = tree,
        selection = TreeSelection.Single,
        selected = setOfNotNull(selected),
        onSelectionChange = { onIntent(WorkspaceIntent.Select(it.firstOrNull())) },
        trailing = { node -> rows.ends[node.key]?.let { RowEndOf(it, layout, chosen = node.key == selected, words = words) } },
        rowAction = { node ->
            (rows.ends[node.key]?.preview as? ValuePreview.Link)?.let { link ->
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
