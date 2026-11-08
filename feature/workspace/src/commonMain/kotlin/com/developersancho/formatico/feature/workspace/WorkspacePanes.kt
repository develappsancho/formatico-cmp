package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.json.PathStep
import com.developersancho.formatico.core.format.json.jsonPath
import com.developersancho.formatico.core.format.text.Indentation
import com.developersancho.formatico.core.format.text.LineEnding
import com.pantrixui.components.data.tag.PantrixTag
import com.pantrixui.components.data.tag.TagTone
import com.pantrixui.components.data.treeview.PantrixTreeView
import com.pantrixui.components.data.treeview.TreeNode
import com.pantrixui.components.data.treeview.TreeSelection
import com.pantrixui.components.data.treeview.TreeViewStyle
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.elements.scrollbar.pantrixVerticalScroll
import com.pantrixui.components.elements.segmentbutton.PantrixSegmentButton
import com.pantrixui.components.elements.segmentbutton.SegmentOption
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.navigation.breadcrumb.BreadcrumbItem
import com.pantrixui.components.navigation.breadcrumb.PantrixBreadcrumb
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.workspace_editor_body
import formatico.feature.workspace.generated.resources.workspace_editor_body_unread
import formatico.feature.workspace.generated.resources.workspace_editor_title
import formatico.feature.workspace.generated.resources.workspace_empty_body
import formatico.feature.workspace.generated.resources.workspace_empty_title
import formatico.feature.workspace.generated.resources.workspace_encoding
import formatico.feature.workspace.generated.resources.workspace_find
import formatico.feature.workspace.generated.resources.workspace_indent_spaces
import formatico.feature.workspace.generated.resources.workspace_indent_tabs
import formatico.feature.workspace.generated.resources.workspace_lines
import formatico.feature.workspace.generated.resources.workspace_no_value_body
import formatico.feature.workspace.generated.resources.workspace_no_value_title
import formatico.feature.workspace.generated.resources.workspace_plain_body
import formatico.feature.workspace.generated.resources.workspace_plain_text
import formatico.feature.workspace.generated.resources.workspace_plain_title
import formatico.feature.workspace.generated.resources.workspace_separated
import formatico.feature.workspace.generated.resources.workspace_status_not_checked
import formatico.feature.workspace.generated.resources.workspace_status_problems
import formatico.feature.workspace.generated.resources.workspace_status_reading
import formatico.feature.workspace.generated.resources.workspace_status_valid
import formatico.feature.workspace.generated.resources.workspace_unread_body
import formatico.feature.workspace.generated.resources.workspace_unread_title
import formatico.feature.workspace.generated.resources.workspace_view_table
import formatico.feature.workspace.generated.resources.workspace_view_text
import formatico.feature.workspace.generated.resources.workspace_view_tree
import org.jetbrains.compose.resources.pluralStringResource
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

/** Where the text editor will stand: [readable] says whether the tree shows the document meanwhile. */
@Composable
internal fun EditorComing(readable: Boolean, modifier: Modifier = Modifier) {
    PaneMessage(
        icon = PantrixIcons.File,
        title = stringResource(Res.string.workspace_editor_title),
        body = stringResource(
            if (readable) Res.string.workspace_editor_body else Res.string.workspace_editor_body_unread
        ),
        modifier = modifier
    )
}

/** What the tree's pane says where there is no tree to show, or null where there is one. */
@Composable
private fun treeMessage(state: WorkspaceState): Pair<String, String>? {
    val document = state.document ?: return null
    return when (val reading = state.reading) {
        Reading.Pending -> null

        is Reading.Unread -> {
            val format = reading.format
            if (format == null) {
                stringResource(Res.string.workspace_plain_title) to stringResource(Res.string.workspace_plain_body)
            } else {
                stringResource(Res.string.workspace_unread_title, format.displayName) to
                    stringResource(Res.string.workspace_unread_body, format.displayName)
            }
        }

        is Reading.Read -> when {
            reading.root != null -> null

            document.text.isBlank() ->
                stringResource(Res.string.workspace_empty_title) to stringResource(Res.string.workspace_empty_body)

            else ->
                stringResource(Res.string.workspace_no_value_title) to
                    stringResource(Res.string.workspace_no_value_body)
        }
    }
}

/** What stands in the tree's place where there is no tree: why there is none. */
@Composable
internal fun TreePaneMessage(state: WorkspaceState, modifier: Modifier = Modifier) {
    val message = treeMessage(state) ?: return
    PaneMessage(icon = PantrixIcons.Info, title = message.first, body = message.second, modifier = modifier)
}

/** The tree's pane in a wide window or on a tablet: the view's choice, the trail to the chosen value, the tree. */
@Composable
internal fun TreePane(state: WorkspaceState, layout: WorkspaceLayout, onIntent: (WorkspaceIntent) -> Unit) {
    val spacing = PantrixTheme.spacing
    val wide = layout == WorkspaceLayout.Wide
    val message = treeMessage(state)
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(if (wide) 48.dp else 52.dp).padding(horizontal = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            ViewChoice(selected = DocumentPane.Tree, withText = false, onSelect = {})
            if (!wide && message == null) {
                PathTrail(state = state, onIntent = onIntent, modifier = Modifier.padding(start = spacing.sm))
            }
            Spacer(modifier = Modifier.weight(1f))
            PantrixIconButton(
                imageVector = PantrixIcons.Search,
                contentDescription = stringResource(Res.string.workspace_find),
                onClick = {},
                size = layout.buttons,
                enabled = false,
                tagKey = "find"
            )
        }
        if (wide) PantrixHorizontalDivider()
        if (message != null) {
            TreePaneMessage(state = state, modifier = Modifier.fillMaxSize())
            return@Column
        }
        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth().height(36.dp).padding(horizontal = spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PathTrail(state = state, onIntent = onIntent)
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

/** The views of a document: its text on a phone ([withText]), its tree, and its table, which is not open yet. */
@Composable
internal fun ViewChoice(
    selected: DocumentPane,
    withText: Boolean,
    onSelect: (DocumentPane) -> Unit,
    modifier: Modifier = Modifier
) {
    val textLabel = stringResource(Res.string.workspace_view_text)
    val treeLabel = stringResource(Res.string.workspace_view_tree)
    val text = SegmentOption<DocumentPane?>(value = DocumentPane.Text, label = textLabel, tagKey = "text")
    val tree = SegmentOption<DocumentPane?>(value = DocumentPane.Tree, label = treeLabel, tagKey = "tree")
    // The table is a view of its own still to come: a choice that is not open yet.
    val table = SegmentOption<DocumentPane?>(
        value = null,
        label = stringResource(Res.string.workspace_view_table),
        enabled = false,
        tagKey = "table"
    )
    PantrixSegmentButton(
        options = listOfNotNull(text.takeIf { withText }, tree, table),
        selected = selected,
        onSelect = { pane -> pane?.let(onSelect) },
        modifier = modifier,
        fullWidth = withText,
        tagKey = "view"
    )
}

/** The trail from the document's root to the chosen value; a step of it chooses that value. */
@Composable
internal fun PathTrail(state: WorkspaceState, onIntent: (WorkspaceIntent) -> Unit, modifier: Modifier = Modifier) {
    val trail = state.trail.ifEmpty { listOfNotNull(state.root) }
    if (trail.isEmpty()) return
    PantrixBreadcrumb(
        items = trail.map { node ->
            val label = when (val step = node.path.lastOrNull()) {
                null -> ROOT_LABEL
                is PathStep.Index -> "[${step.index}]"
                is PathStep.Key -> step.name
            }
            BreadcrumbItem(key = node.key, label = label, tagKey = tagOf(node))
        },
        onSelect = { onIntent(WorkspaceIntent.Select(it)) },
        modifier = modifier,
        tagKey = "path"
    )
}

private const val ROOT_LABEL = "$"

/** A node's automation key, the same in every language: `root`, or its path without the `$`. */
internal fun tagOf(node: ValueNode): String = if (node.path.isEmpty()) "root" else node.key.drop(1).removePrefix(".")

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
        trailing = node.text,
        tagKey = tagOf(node)
    )
} else {
    TreeNode(
        key = node.key,
        label = if (node.path.isEmpty()) node.text else "${node.name}: ${node.text}",
        trailing = node.preview?.let(words::of),
        tagKey = tagOf(node)
    )
}

/** Whether a value of this kind holds others. */
internal val ValueKind.isContainer: Boolean get() = this == ValueKind.Object || this == ValueKind.Array
