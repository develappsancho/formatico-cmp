package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.designsystem.LocalSyntaxColors
import com.developersancho.formatico.core.designsystem.SyntaxColors
import com.developersancho.formatico.core.format.json.jsonPath
import com.pantrixui.components.data.lazytreeview.PantrixLazyTreeView
import com.pantrixui.components.data.lazytreeview.rememberTreeViewState
import com.pantrixui.components.data.treeview.TreeNode
import com.pantrixui.components.data.treeview.TreeSelection
import com.pantrixui.components.data.treeview.TreeViewStyle
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
)

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
 * The rows of [root]: each value as `name: value` in the document's colours, the [selected] one in the chosen row's
 * own colour, and what a search [marks] marked in each.
 */
internal fun documentRows(root: ValueNode, inks: RowInks, selected: String?, marks: TreeMarks?): DocumentRows {
    val ends = HashMap<String, RowEnd>()

    fun nodeOf(node: ValueNode): TreeNode {
        endOf(node)?.let { ends[node.key] = it }
        return TreeNode(
            key = node.key,
            label = rowLabel(node),
            children = node.children.map(::nodeOf),
            tagKey = tagOf(node),
            styledLabel = styledLabelOf(node, inks, chosen = node.key == selected, marks = marks)
        )
    }
    return DocumentRows(listOf(nodeOf(root)), ends)
}

/** A value as its row writes it: `name: value`, an object or an array by its size; the root's `$` with no colon. */
internal fun rowLabel(node: ValueNode): String = node.name + colonOf(node) + node.text

private fun colonOf(node: ValueNode): String = if (node.path.isEmpty()) "" else ": "

/**
 * [rowLabel] in the document's colours: the key, the colon and the value each in its kind's, or none on a [chosen]
 * row, which is written in its own; and over them what [marks] found in the key and in the value.
 */
internal fun styledLabelOf(node: ValueNode, inks: RowInks, chosen: Boolean, marks: TreeMarks?): AnnotatedString {
    val colon = colonOf(node)
    val syntax = inks.syntax

    fun AnnotatedString.Builder.part(text: String, color: Color) {
        if (chosen) append(text) else withStyle(SpanStyle(color = color)) { append(text) }
    }
    return buildAnnotatedString {
        part(node.name, syntax.key)
        part(colon, syntax.punctuation)
        part(node.text, inkOf(node.kind, inks))
        if (marks == null) return@buildAnnotatedString
        val mark = if (node.key == marks.current) {
            SpanStyle(color = inks.onCurrent, background = inks.current)
        } else {
            SpanStyle(background = inks.match)
        }
        if (nameHolds(node, marks.sought, marks.language)) {
            placesOf(marks.sought, node.name, marks.language).forEach { addStyle(mark, it.first, it.last + 1) }
        }
        if (valueHolds(node, marks.sought, marks.language)) {
            val start = node.name.length + colon.length
            placesOf(marks.sought, node.text, marks.language).forEach { addStyle(mark, start + it.first, start + it.last + 1) }
        }
    }
}

/** The colour a value of [kind] is written in: its syntax's, and the size of an object or an array quieter. */
private fun inkOf(kind: ValueKind, inks: RowInks): Color = when (kind) {
    ValueKind.Object, ValueKind.Array -> inks.size
    ValueKind.String -> inks.syntax.string
    ValueKind.Number -> inks.syntax.number
    ValueKind.Boolean, ValueKind.Null, ValueKind.Alias -> inks.syntax.literal
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
 * end, and what a search found, as its [marks] say. The chosen value's row is brought into view as it is chosen, in
 * the text, by the search or by going to an anchor.
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
    val rows = remember(root, inks, selected, marks) { documentRows(root, inks, selected, marks) }
    val words = previewWords()
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
