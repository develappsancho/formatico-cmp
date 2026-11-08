package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.json.jsonPath
import com.pantrixui.components.data.treeview.PantrixTreeView
import com.pantrixui.components.data.treeview.TreeNode
import com.pantrixui.components.data.treeview.TreeSelection
import com.pantrixui.components.data.treeview.TreeViewStyle
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.elements.scrollbar.pantrixVerticalScroll
import com.pantrixui.components.elements.segmentbutton.PantrixSegmentButton
import com.pantrixui.components.elements.segmentbutton.SegmentOption
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.workspace_editor_body
import formatico.feature.workspace.generated.resources.workspace_editor_body_read
import formatico.feature.workspace.generated.resources.workspace_editor_body_table
import formatico.feature.workspace.generated.resources.workspace_editor_body_unread
import formatico.feature.workspace.generated.resources.workspace_editor_title
import formatico.feature.workspace.generated.resources.workspace_empty_body
import formatico.feature.workspace.generated.resources.workspace_empty_title
import formatico.feature.workspace.generated.resources.workspace_find
import formatico.feature.workspace.generated.resources.workspace_no_value_body
import formatico.feature.workspace.generated.resources.workspace_no_value_title
import formatico.feature.workspace.generated.resources.workspace_plain_body
import formatico.feature.workspace.generated.resources.workspace_plain_title
import formatico.feature.workspace.generated.resources.workspace_view_problems
import formatico.feature.workspace.generated.resources.workspace_view_problems_short
import formatico.feature.workspace.generated.resources.workspace_view_summary
import formatico.feature.workspace.generated.resources.workspace_view_table
import formatico.feature.workspace.generated.resources.workspace_view_text
import formatico.feature.workspace.generated.resources.workspace_view_tree
import org.jetbrains.compose.resources.stringResource

/** The widest a pane's message is set. */
private val MessageWidth = 320.dp

/** A pane's message where it has nothing else to show: an icon, a title and a sentence, in its middle. */
@Composable
internal fun PaneMessage(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    val spacing = PantrixTheme.spacing
    Column(
        modifier = modifier.padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PantrixIcon(imageVector = icon, contentDescription = null, tint = PantrixTheme.colors.textSecondary)
        PantrixText(
            text = title,
            style = PantrixTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center
        )
        PantrixText(
            text = body,
            modifier = Modifier.widthIn(max = MessageWidth),
            style = PantrixTheme.typography.footnote,
            color = PantrixTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/** Where the text editor will stand: what works on the document meanwhile, by what Formatico makes of it. */
@Composable
internal fun EditorComing(state: WorkspaceState, modifier: Modifier = Modifier) {
    val body = when {
        DocumentEdit.Minify in state.edits -> Res.string.workspace_editor_body
        DocumentEdit.Format in state.edits -> Res.string.workspace_editor_body_table
        state.reading is Reading.Read -> Res.string.workspace_editor_body_read
        else -> Res.string.workspace_editor_body_unread
    }
    PaneMessage(
        icon = PantrixIcons.File,
        title = stringResource(Res.string.workspace_editor_title),
        body = stringResource(body),
        modifier = modifier
    )
}

/**
 * What the tree's pane says where there is no tree to show, or null where there is one. An empty document says it is
 * empty, whatever its format: a new one is plain text until something is written in it.
 */
@Composable
internal fun treeMessage(state: WorkspaceState): Pair<String, String>? {
    val document = state.document ?: return null
    val reading = state.reading
    return when {
        reading == Reading.Pending -> {
            null
        }

        document.text.isBlank() -> {
            stringResource(Res.string.workspace_empty_title) to stringResource(Res.string.workspace_empty_body)
        }

        reading == Reading.PlainText -> {
            stringResource(Res.string.workspace_plain_title) to stringResource(Res.string.workspace_plain_body)
        }

        reading is Reading.Read && reading.root != null -> {
            null
        }

        else -> {
            stringResource(Res.string.workspace_no_value_title) to stringResource(Res.string.workspace_no_value_body)
        }
    }
}

/** What stands in the tree's place where there is no tree: why there is none. */
@Composable
internal fun TreePaneMessage(state: WorkspaceState, modifier: Modifier = Modifier) {
    val message = treeMessage(state) ?: return
    PaneMessage(icon = PantrixIcons.Info, title = message.first, body = message.second, modifier = modifier)
}

/**
 * The tree's pane in a wide window or on a tablet: the view's choice, the trail to the chosen value and the copying of
 * its path, the levels the tree is opened to, the search in it, and the tree.
 */
@Composable
internal fun TreePane(
    state: WorkspaceState,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands
) {
    val spacing = PantrixTheme.spacing
    val wide = layout == WorkspaceLayout.Wide
    val message = treeMessage(state)
    val search = rememberTreeSearch(state, onIntent)
    Column(modifier = Modifier.fillMaxSize()) {
        TreeHeader(state = state, layout = layout, read = message == null, search = search, onIntent = onIntent, commands = commands)
        if (wide) PantrixHorizontalDivider()
        if (search.open && message == null) {
            if (!wide) PantrixHorizontalDivider()
            val focus = remember { FocusRequester() }
            TreeSearchBar(
                query = search.query,
                position = search.position(),
                layout = layout,
                onStep = search::step,
                onClose = search::close,
                focus = focus
            )
            // The caret goes to the field as the bar is opened.
            LaunchedEffect(focus) { focus.requestFocus() }
            PantrixHorizontalDivider()
        }
        if (message != null) {
            TreePaneMessage(state = state, modifier = Modifier.fillMaxSize())
            return@Column
        }
        if (wide) {
            Row(
                // As tall as the board's 36 where a pointer is used; a touch target's height where a finger is.
                modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp).padding(start = spacing.md, end = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                PathTrail(state = state, onIntent = onIntent, modifier = Modifier.weight(1f))
                (state.selected ?: state.root)?.let { node ->
                    CopyPathMenu(node = node, layout = layout, onCopy = { commands.copyText(Copied.Path, it) })
                }
            }
        }
        DocumentTree(
            state = state,
            layout = layout,
            onIntent = onIntent,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pantrixVerticalScroll(rememberScrollState())
                .padding(start = spacing.sm, end = spacing.sm, bottom = spacing.sm)
        )
    }
}

/**
 * The row over the tree: the views' choice; on a tablet the trail to the chosen value and the copying of its path; where
 * the document is [read], the levels the tree is opened to; and the search, which [search] holds.
 */
@Composable
private fun TreeHeader(
    state: WorkspaceState,
    layout: WorkspaceLayout,
    read: Boolean,
    search: TreeSearch,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands
) {
    val spacing = PantrixTheme.spacing
    val wide = layout == WorkspaceLayout.Wide
    Row(
        modifier = Modifier.fillMaxWidth().height(if (wide) 48.dp else 52.dp).padding(horizontal = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        ViewChoice(
            selected = DocumentPane.Tree,
            withText = false,
            onSelect = { onIntent(WorkspaceIntent.ShowPane(it)) },
            problems = state.problems.size
        )
        val chosen = state.selected ?: state.root
        if (!wide && read && chosen != null) {
            PathTrail(state = state, onIntent = onIntent, modifier = Modifier.padding(start = spacing.sm))
            CopyPathMenu(node = chosen, layout = layout, onCopy = { commands.copyText(Copied.Path, it) })
        }
        Spacer(modifier = Modifier.weight(1f))
        val root = state.root
        if (read && root != null) TreeLevels(root = root, layout = layout, onIntent = onIntent)
        PantrixIconButton(
            imageVector = PantrixIcons.Search,
            contentDescription = stringResource(Res.string.workspace_find),
            onClick = search::toggle,
            variant = if (search.open) ButtonVariant.Soft else ButtonVariant.Text,
            size = layout.buttons,
            enabled = read,
            tagKey = "find"
        )
    }
}

/**
 * The views of a document: its text where [withText] says so, its tree, its table where [withTable] says so, and its
 * [problems] where it has any. It fills its place's width where [fullWidth] says so, by default where it shows the
 * text: on a row of its own.
 */
@Composable
internal fun ViewChoice(
    selected: DocumentPane,
    withText: Boolean,
    onSelect: (DocumentPane) -> Unit,
    modifier: Modifier = Modifier,
    fullWidth: Boolean = withText,
    problems: Int = 0,
    withTable: Boolean = true
) {
    val textLabel = stringResource(Res.string.workspace_view_text)
    val treeLabel = stringResource(Res.string.workspace_view_tree)
    val text = SegmentOption<DocumentPane?>(value = DocumentPane.Text, label = textLabel, tagKey = "text")
    val tree = SegmentOption<DocumentPane?>(value = DocumentPane.Tree, label = treeLabel, tagKey = "tree")
    val tableLabel = stringResource(Res.string.workspace_view_table)
    val table = SegmentOption<DocumentPane?>(value = DocumentPane.Table, label = tableLabel, tagKey = "table")
    // A phone has no room for the count beside its three views.
    val problemsLabel = if (withText) {
        stringResource(Res.string.workspace_view_problems_short)
    } else {
        stringResource(Res.string.workspace_view_problems, problems)
    }
    val problemList = SegmentOption<DocumentPane?>(value = DocumentPane.Problems, label = problemsLabel, tagKey = "problems")
    // The Summary is a view while it is shown, as the status bar's size opens it; a phone shows it in the table's place.
    val summaryShown = selected == DocumentPane.Summary
    val summary = SegmentOption<DocumentPane?>(
        value = DocumentPane.Summary,
        label = stringResource(Res.string.workspace_view_summary),
        tagKey = "summary"
    )
    PantrixSegmentButton(
        options = listOfNotNull(
            text.takeIf { withText },
            tree,
            table.takeIf { withTable && !(withText && summaryShown) },
            problemList.takeIf { problems > 0 },
            summary.takeIf { summaryShown }
        ),
        selected = selected,
        onSelect = { pane -> pane?.let(onSelect) },
        modifier = modifier,
        fullWidth = fullWidth,
        tagKey = "view"
    )
}

/** The document's tree: a value to a row, its strings previewed. */
@Composable
internal fun DocumentTree(
    state: WorkspaceState,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val root = state.root ?: return
    val words = previewWords()
    val nodes = remember(root, words) { listOf(treeNodeOf(root, words)) }
    PantrixTreeView(
        nodes = nodes,
        expanded = state.expanded,
        onExpandedChange = { onIntent(WorkspaceIntent.Expand(it)) },
        modifier = modifier,
        selection = TreeSelection.Single,
        selected = setOfNotNull(state.selected?.key),
        onSelectionChange = { onIntent(WorkspaceIntent.Select(it.firstOrNull())) },
        style = TreeViewStyle(
            rowHeight = when (layout) {
                WorkspaceLayout.Wide -> 32.dp
                WorkspaceLayout.Tablet -> 36.dp
                WorkspaceLayout.Phone -> 44.dp
            },
            indent = PantrixTheme.spacing.lg
        ),
        tagKey = "document"
    )
}

/**
 * A node as the tree draws it: an object or an array by its name, with how much it holds at the row's end; any other
 * value as `name: value`, with what it looks like it holds at the row's end.
 */
private fun treeNodeOf(node: ValueNode, words: PreviewWords): TreeNode = if (node.kind.isContainer) {
    TreeNode(
        key = node.key,
        label = node.name,
        children = node.children.map { treeNodeOf(it, words) },
        trailing = listOfNotNull(node.text, node.anchor?.let { "&$it" }).joinToString(" · "),
        tagKey = tagOf(node)
    )
} else {
    TreeNode(
        key = node.key,
        label = if (node.path.isEmpty()) node.text else "${node.name}: ${node.text}",
        trailing = listOfNotNull(
            node.anchor?.let { "&$it" },
            node.target?.let { "→ ${jsonPath(it.path)}" },
            node.preview?.let(words::of)
        ).joinToString(" · ").ifEmpty { null },
        tagKey = tagOf(node)
    )
}

/** Whether a value of this kind holds others. */
internal val ValueKind.isContainer: Boolean get() = this == ValueKind.Object || this == ValueKind.Array
