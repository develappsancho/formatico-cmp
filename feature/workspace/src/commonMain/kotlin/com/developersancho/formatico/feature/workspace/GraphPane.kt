package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.designsystem.LocalSyntaxColors
import com.developersancho.formatico.feature.workspace.strings.graphStrings
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.data.graph.GraphDirection
import com.pantrixui.components.data.graph.GraphFoldButton
import com.pantrixui.components.data.graph.GraphNodeScope
import com.pantrixui.components.data.graph.GraphState
import com.pantrixui.components.data.graph.GraphStyle
import com.pantrixui.components.data.graph.PantrixGraph
import com.pantrixui.components.data.graph.PantrixGraphZoom
import com.pantrixui.components.data.graph.rememberGraphState
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.divider.PantrixVerticalDivider
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme

/**
 * The document's graph in a wide window, beside its text, or on a tablet, under it, as Graph-1440 and Graph-834 draw it:
 * the views' choice, the levels the cards are opened to and the zoom over it; the trail to the chosen value and the
 * copying of its path; and the cards.
 */
@Composable
internal fun GraphPane(
    state: WorkspaceState,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands,
    modifier: Modifier = Modifier
) {
    val spacing = PantrixTheme.spacing
    val wide = layout == WorkspaceLayout.Wide
    val graph = rememberGraphState(initialZoom = if (wide) WIDE_ZOOM else 1f)
    val root = state.root
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // A wide window only a little wider than a tablet has no room for the zoom beside the levels: it stands in the
        // graph's corner, as a phone's does.
        val roomy = maxWidth >= RoomyGraphHeader
        val labelled = maxWidth >= RoomyHeader
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(if (wide) 48.dp else 52.dp).padding(start = spacing.md, end = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                ViewChoice(
                    selected = DocumentPane.Graph,
                    withText = false,
                    onSelect = { onIntent(WorkspaceIntent.ShowPane(it)) },
                    problems = state.problems.size
                )
                Spacer(modifier = Modifier.weight(1f))
                if (root != null) {
                    TreeLevels(root = root, layout = layout, onIntent = onIntent, withLabel = labelled)
                    if (roomy) {
                        PantrixVerticalDivider(modifier = Modifier.height(24.dp))
                        PantrixGraphZoom(state = graph, tagKey = "graph_zoom")
                    }
                }
            }
            PantrixHorizontalDivider()
            if (root == null) {
                TreePaneMessage(state = state, modifier = Modifier.fillMaxSize())
                return@Column
            }
            Row(
                // As tall as the board's 36 where a pointer is used; a touch target's height where a finger is.
                modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp).padding(start = spacing.md, end = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                PathTrail(state = state, onIntent = onIntent, modifier = Modifier.weight(1f))
                CopyPathMenu(node = state.selected ?: root, layout = layout, onCopy = { commands.copyText(Copied.Path, it) })
            }
            PantrixHorizontalDivider()
            DocumentGraphView(
                state = state,
                layout = layout,
                onIntent = onIntent,
                graph = graph,
                zoomButtons = !roomy,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** How wide the graph's header is for its zoom to stand in it beside the views and the levels, as Graph-1440 has it. */
private val RoomyGraphHeader = 600.dp

/**
 * The document's cards and the links between them: from the start to the end in a wide window and on a tablet, from
 * the top down on a phone. The graph's own zoom stands in its corner where [zoomButtons] says so: a phone's, and a pane
 * whose header has no room for it. The chosen card is the one the chosen value
 * stands in, kept in step with the text and the tree and brought into view as it is chosen; the cards open are the
 * tree's open branches, or all of them, as a document opens, where it is not large.
 */
@Composable
internal fun DocumentGraphView(
    state: WorkspaceState,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    modifier: Modifier = Modifier,
    graph: GraphState = rememberGraphState(initialZoom = PHONE_ZOOM),
    zoomButtons: Boolean = layout == WorkspaceLayout.Phone
) {
    val root = state.root ?: return
    val document = state.document ?: return
    val phone = layout == WorkspaceLayout.Phone
    val chosen = cardKeyOf(root, state.selected)
    val opening = remember(root) { graphOpening(root) }
    val open = if (document.view.expanded == null) opening else state.expanded
    val name = document.name ?: workspaceStrings.untitled
    val strings = graphStrings
    val width = cardWidth(layout)
    val made = remember(root, open, name, width, strings) {
        documentGraph(root, name, open, width) { card ->
            val count = card.node.children.size
            if (card.node.kind ==
                ValueKind.Array
            ) {
                strings.arrayCard(count, card.title, count)
            } else {
                strings.objectCard(count, card.title, count)
            }
        }
    }
    LaunchedEffect(chosen) { chosen?.let { graph.bringIntoView(it) } }
    PantrixGraph(
        nodes = made.nodes,
        links = made.links,
        modifier = modifier.fillMaxSize(),
        state = graph,
        direction = if (phone) GraphDirection.TopToBottom else GraphDirection.StartToEnd,
        selectedKey = chosen,
        onSelect = { onIntent(WorkspaceIntent.Select(it)) },
        expanded = open,
        onExpandedChange = { onIntent(WorkspaceIntent.Expand(it)) },
        zoomButtons = zoomButtons,
        style = GraphStyle(layerGap = if (phone) 40.dp else 64.dp, siblingGap = 16.dp),
        tagKey = "document"
    ) { node ->
        made.cards[node.key]?.let { card -> GraphCardContent(card) }
    }
}

/** How wide a card is, as the Graph boards draw it: 200 in a wide window, 220 on a tablet, 150 on a phone. */
private fun cardWidth(layout: WorkspaceLayout): Dp = when (layout) {
    WorkspaceLayout.Wide -> 200.dp
    WorkspaceLayout.Tablet -> 220.dp
    WorkspaceLayout.Phone -> 150.dp
}

/**
 * What a card shows: its head, and its single values a row each, as the tree writes them, with what a string looks like
 * it holds at the row's end; and how many more there are.
 */
@Composable
private fun GraphNodeScope.GraphCardContent(card: GraphCard) {
    val typography = PantrixTheme.typography
    val code = typography.code.copy(fontSize = typography.footnote.fontSize, lineHeight = typography.footnote.lineHeight)
    Column(modifier = Modifier.fillMaxSize()) {
        GraphCardHead(card, code)
        if (card.shownRows > 0) PantrixHorizontalDivider()
        card.rows.forEach { row -> GraphCardRow(row, code) }
        if (card.more > 0) {
            Row(
                modifier = Modifier.fillMaxWidth().height(GraphRow).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PantrixText(
                    text = graphStrings.more(card.more, card.more),
                    style = code,
                    color = PantrixTheme.colors.textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

/** A card's head: its title in the keys' colour and how much it holds, the chosen card's on the brand's soft fill. */
@Composable
private fun GraphNodeScope.GraphCardHead(card: GraphCard, code: TextStyle) {
    val colors = PantrixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(GraphHead)
            .background(if (selected) colors.primary.container else Color.Transparent)
            .padding(start = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PantrixText(
                text = card.title,
                style = code,
                color = if (selected) colors.primary.onContainer else LocalSyntaxColors.current.key,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            PantrixText(text = card.node.text, style = code, color = if (selected) colors.primary.onContainer else colors.textSecondary)
        }
        GraphFoldButton()
    }
}

/** A card's row: a single value, `key: value` in the syntax's colours, and what its string looks like it holds. */
@Composable
private fun GraphCardRow(row: ValueNode, code: TextStyle) {
    val syntax = LocalSyntaxColors.current
    val quiet = PantrixTheme.colors.textSecondary
    Row(
        modifier = Modifier.fillMaxWidth().height(GraphRow).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PantrixText(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = syntax.key)) { append(row.name) }
                withStyle(SpanStyle(color = syntax.punctuation)) { append(": ") }
                withStyle(SpanStyle(color = syntaxInk(row.kind, syntax, quiet))) { append(row.text) }
            },
            style = code,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        row.preview?.let { GraphRowPreview(it) }
    }
}

/**
 * What a row's string looks like it holds, at its end, as the Graph boards draw it: a date's calendar, a colour's
 * swatch, a link's and a picture's icons. The card is read as one, by its label: none of these is read on its own.
 */
@Composable
private fun GraphRowPreview(preview: ValuePreview) {
    val quiet = PantrixTheme.colors.textSecondary
    when (preview) {
        is ValuePreview.Moment, is ValuePreview.Day -> PantrixIcon(
            PantrixIcons.Calendar,
            contentDescription = null,
            size = 14.dp,
            tint = quiet
        )

        is ValuePreview.Link -> PantrixIcon(PantrixIcons.ExternalLink, contentDescription = null, size = 14.dp, tint = quiet)

        is ValuePreview.Picture -> PantrixIcon(PantrixIcons.Image, contentDescription = null, size = 14.dp, tint = quiet)

        is ValuePreview.Colour -> Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color(preview.argb), PantrixTheme.shapes.sm)
                .border(1.dp, PantrixTheme.colors.border, PantrixTheme.shapes.sm)
        )
    }
}

/** How close a wide window's graph is looked at as it opens: Graph-1440's 85%. */
private const val WIDE_ZOOM = 0.85f

/** How close a phone's graph is looked at as it opens: Graph-390's 70%, the chosen card and its neighbours in view. */
internal const val PHONE_ZOOM = 0.7f
