package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import com.developersancho.formatico.feature.workspace.strings.graphStrings
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.data.codeeditor.CodeEditorState
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.elements.segmentbutton.PantrixSegmentButton
import com.pantrixui.components.elements.segmentbutton.SegmentOption
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme

/** The widest a pane's message is set. */
private val MessageWidth = 320.dp

/**
 * A pane's message where it has nothing else to show: an icon, a title and a sentence, in its middle. Pantrix has no
 * such empty state (PANTRIX_FINDINGS, "Boş bir listenin hâlini çizen bir bileşen yok").
 */
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
            workspaceStrings.emptyTitle to workspaceStrings.emptyBody
        }

        reading == Reading.PlainText -> {
            workspaceStrings.plainTitle to workspaceStrings.plainBody
        }

        reading is Reading.Read && reading.root != null -> {
            null
        }

        else -> {
            workspaceStrings.noValueTitle to workspaceStrings.noValueBody
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
                .padding(start = spacing.sm, end = spacing.sm, bottom = spacing.sm),
            marks = search.marks()
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
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // A wide window only a little wider than a tablet leaves the tree less room than its board: "Open to" goes first.
        val roomy = maxWidth >= RoomyHeader
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
            if (read && root != null) TreeLevels(root = root, layout = layout, onIntent = onIntent, withLabel = roomy)
            PantrixIconButton(
                imageVector = PantrixIcons.Search,
                contentDescription = workspaceStrings.find,
                onClick = search::toggle,
                variant = if (search.open) ButtonVariant.Soft else ButtonVariant.Text,
                size = layout.buttons,
                enabled = read,
                tagKey = "find"
            )
        }
    }
}

/** How wide a pane's header is for "Open to" to stand in it beside the views, the levels and the pane's own buttons. */
internal val RoomyHeader = 520.dp

/**
 * The views of a document: its text where [withText] says so, its tree, its table where [withTable] says so, its graph,
 * its [problems] where it has any, and its query where [withQuery] says so, while it is shown. It fills its place's width
 * where [fullWidth] says so, by default where it shows the text: on a row of its own.
 */
@Composable
internal fun ViewChoice(
    selected: DocumentPane,
    withText: Boolean,
    onSelect: (DocumentPane) -> Unit,
    modifier: Modifier = Modifier,
    fullWidth: Boolean = withText,
    problems: Int = 0,
    withTable: Boolean = true,
    withQuery: Boolean = false
) {
    val textLabel = workspaceStrings.viewText
    val treeLabel = workspaceStrings.viewTree
    val text = SegmentOption<DocumentPane?>(value = DocumentPane.Text, label = textLabel, tagKey = "text")
    val tree = SegmentOption<DocumentPane?>(value = DocumentPane.Tree, label = treeLabel, tagKey = "tree")
    val tableLabel = workspaceStrings.viewTable
    val table = SegmentOption<DocumentPane?>(value = DocumentPane.Table, label = tableLabel, tagKey = "table")
    val graph = SegmentOption<DocumentPane?>(value = DocumentPane.Graph, label = graphStrings.graph, tagKey = "graph")
    // A phone has no room for the count beside its three views.
    val problemsLabel = if (withText) {
        workspaceStrings.viewProblemsShort
    } else {
        workspaceStrings.viewProblems(problems)
    }
    val problemList = SegmentOption<DocumentPane?>(value = DocumentPane.Problems, label = problemsLabel, tagKey = "problems")
    // The Summary is a view while it is shown, as the status bar's size opens it; a phone shows it in the table's place.
    val summaryShown = selected == DocumentPane.Summary
    val summary = SegmentOption<DocumentPane?>(
        value = DocumentPane.Summary,
        label = workspaceStrings.viewSummary,
        tagKey = "summary"
    )
    // The query is a view while it is shown, as the toolbar opens it; a phone shows it in the table's place too.
    val query = SegmentOption<DocumentPane?>(
        value = DocumentPane.Query,
        label = workspaceStrings.query,
        tagKey = "query"
    )
    PantrixSegmentButton(
        options = listOfNotNull(
            text.takeIf { withText },
            tree,
            table.takeIf { withTable && !(withText && (summaryShown || withQuery)) },
            // A phone has no room for the graph beside an invalid document's problems, which take its place.
            graph.takeIf { !(withText && problems > 0) },
            problemList.takeIf { problems > 0 },
            summary.takeIf { summaryShown },
            query.takeIf { withQuery }
        ),
        selected = selected,
        onSelect = { pane -> pane?.let(onSelect) },
        modifier = modifier,
        fullWidth = fullWidth,
        tagKey = "view"
    )
}

/** Whether a value of this kind holds others. */
internal val ValueKind.isContainer: Boolean get() = this == ValueKind.Object || this == ValueKind.Array

/** What stands beside the text in a wide window and under it on a tablet: the pane the document's view shows. */
@Composable
internal fun PaneBesideText(
    state: WorkspaceState,
    editor: CodeEditorState,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands
) {
    val pane = state.document?.view?.pane
    when {
        pane == DocumentPane.Table -> TablePane(state, layout, onIntent, commands)

        pane == DocumentPane.Problems && state.problems.isNotEmpty() ->
            ProblemsPane(state = state, editor = editor, layout = layout, onIntent = onIntent)

        pane == DocumentPane.Summary -> SummaryPane(state = state, layout = layout, onIntent = onIntent, commands = commands)

        pane == DocumentPane.Query -> QueryPane(state = state, layout = layout, onIntent = onIntent, commands = commands)

        pane == DocumentPane.Graph -> GraphPane(state = state, layout = layout, onIntent = onIntent, commands = commands)

        else -> TreePane(state = state, layout = layout, onIntent = onIntent, commands = commands)
    }
}
