package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pantrixui.components.chart.metergroup.PantrixMeterGroup
import com.pantrixui.components.container.card.CardVariant
import com.pantrixui.components.container.card.PantrixCard
import com.pantrixui.components.data.listitem.ListItemDivider
import com.pantrixui.components.data.listitem.PantrixListItem
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.elements.scrollbar.pantrixVerticalScroll
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.elements.text.TextRole
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.summary_copy
import formatico.feature.workspace.generated.resources.summary_show_table
import formatico.feature.workspace.generated.resources.summary_title_found
import formatico.feature.workspace.generated.resources.summary_title_notes
import formatico.feature.workspace.generated.resources.summary_title_size
import formatico.feature.workspace.generated.resources.summary_title_values
import org.jetbrains.compose.resources.stringResource

/** How many tiles stand in a row of the Summary, at every width. */
private const val TILES_IN_A_ROW = 3

/** The room a row of found values leads with, as wide as an icon. */
private val MarkSize = 20.dp

/** The square of a colour found, a little smaller than an icon, as the tree's preview of it will be. */
private val SwatchSize = 14.dp

/**
 * The Summary's pane in a wide window or on a tablet: the views' choice and the copying of the summary as text over
 * what the document comes to. What stands in the tree's place where the document has no tree stands here too.
 */
@Composable
internal fun SummaryPane(
    state: WorkspaceState,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands,
    modifier: Modifier = Modifier
) {
    val wide = layout == WorkspaceLayout.Wide
    val text = summaryText(state)
    Column(modifier = modifier.fillMaxSize().background(PantrixTheme.colors.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (wide) 48.dp else 52.dp)
                .background(PantrixTheme.colors.surface)
                .padding(horizontal = PantrixTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ViewChoice(
                selected = DocumentPane.Summary,
                withText = false,
                onSelect = { onIntent(WorkspaceIntent.ShowPane(it)) },
                problems = state.problems.size
            )
            Spacer(modifier = Modifier.weight(1f))
            PantrixIconButton(
                imageVector = PantrixIcons.Copy,
                contentDescription = stringResource(Res.string.summary_copy),
                onClick = { text?.let { commands.copyText(Copied.Summary, it) } },
                size = layout.buttons,
                enabled = text != null,
                tagKey = "copy_summary"
            )
        }
        PantrixHorizontalDivider()
        SummaryContent(state = state, layout = layout, onIntent = onIntent, modifier = Modifier.weight(1f))
    }
}

/**
 * What the document comes to, in sections: its size, its values and their kinds, what the values were found to hold,
 * and notes on it. Side by side in a wide window and on a tablet, the last two; one under the other on a phone.
 */
@Composable
internal fun SummaryContent(
    state: WorkspaceState,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = state.summary
    val sizes = state.sizes
    val stats = state.stats
    if (summary == null || sizes == null || stats == null) {
        if (treeMessage(state) != null) TreePaneMessage(state = state, modifier = modifier.fillMaxSize())
        return
    }
    val spacing = PantrixTheme.spacing
    val phone = layout == WorkspaceLayout.Phone
    val tablet = layout == WorkspaceLayout.Tablet
    val gap = if (tablet) spacing.lg else spacing.xl
    Column(
        modifier = modifier
            .fillMaxSize()
            .pantrixVerticalScroll(rememberScrollState())
            .padding(if (tablet) spacing.md else spacing.lg),
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        SummarySection(stringResource(Res.string.summary_title_size)) { Tiles(sizeTiles(stats, sizes)) }
        SummarySection(stringResource(Res.string.summary_title_values)) {
            Tiles(valueTiles(summary, phone))
            SummaryCard(modifier = Modifier.padding(top = if (phone) spacing.sm else spacing.md)) {
                PantrixMeterGroup(
                    items = kindItems(summary),
                    max = summary.values,
                    description = kindsDescription(summary),
                    tagKey = "kinds"
                )
            }
        }
        val found: @Composable (Modifier) -> Unit = { FoundSection(summary, layout, onIntent, it) }
        val notes: @Composable (Modifier) -> Unit = { NotesSection(summary, onIntent, it) }
        if (phone) {
            found(Modifier)
            notes(Modifier)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(if (tablet) spacing.md else spacing.lg)) {
                found(Modifier.weight(1f))
                notes(Modifier.weight(1f))
            }
        }
    }
}

/** A section of the Summary under its [title]. */
@Composable
private fun SummarySection(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier) {
        PantrixText(
            text = title,
            modifier = Modifier.padding(bottom = PantrixTheme.spacing.sm),
            style = PantrixTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
            role = TextRole.Heading(level = 2)
        )
        content()
    }
}

/** A card of the Summary: edged by a hairline, on the surface. */
@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(PantrixTheme.spacing.md),
    content: @Composable () -> Unit
) {
    PantrixCard(modifier = modifier.fillMaxWidth(), variant = CardVariant.Outlined, contentPadding = padding) { content() }
}

/** [tiles], three to a row. */
@Composable
private fun Tiles(tiles: List<SummaryTile>) {
    val gap = PantrixTheme.spacing.sm
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        tiles.chunked(TILES_IN_A_ROW).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { tile -> Tile(tile, Modifier.weight(1f)) }
            }
        }
    }
}

/** A tile: what it counts, how much, and a word on it. */
@Composable
private fun Tile(tile: SummaryTile, modifier: Modifier = Modifier) {
    val typography = PantrixTheme.typography
    val quiet = PantrixTheme.colors.textSecondary
    SummaryCard(modifier = modifier) {
        Column {
            PantrixText(text = tile.label, style = typography.footnote.copy(fontWeight = FontWeight.Medium), color = quiet, maxLines = 1)
            PantrixText(
                text = tile.value,
                style = typography.title,
                maxLines = 1
            )
            PantrixText(
                text = tile.detail,
                style = if (tile.mono) {
                    typography.code.copy(fontSize = typography.footnote.fontSize, lineHeight = typography.footnote.lineHeight)
                } else {
                    typography.footnote.copy(fontWeight = FontWeight.Medium)
                },
                color = quiet,
                maxLines = 1
            )
        }
    }
}

/** What the values were found to hold, a row to a kind: a press on one chooses its first value in the tree. */
@Composable
private fun FoundSection(summary: DocumentSummary, layout: WorkspaceLayout, onIntent: (WorkspaceIntent) -> Unit, modifier: Modifier) {
    val rows = foundRows(summary)
    if (rows.isEmpty()) return
    SummarySection(stringResource(Res.string.summary_title_found), modifier) {
        SummaryCard(padding = PaddingValues()) {
            Column {
                rows.forEachIndexed { index, row ->
                    PantrixListItem(
                        headline = row.label,
                        supportingText = row.paths,
                        leading = { FoundMark(row.found) },
                        chevron = layout != WorkspaceLayout.Tablet,
                        onClick = {
                            onIntent(WorkspaceIntent.Select(row.found.nodes.first().key))
                            onIntent(WorkspaceIntent.ShowPane(DocumentPane.Tree))
                        },
                        divider = if (index < rows.lastIndex) ListItemDivider.Full else ListItemDivider.None,
                        supportingMaxLines = 1,
                        supportingTextStyle = PantrixTheme.typography.code.copy(
                            fontSize = PantrixTheme.typography.footnote.fontSize,
                            lineHeight = PantrixTheme.typography.footnote.lineHeight
                        ),
                        tagKey = row.found.kind.name.lowercase()
                    )
                }
            }
        }
    }
}

/** What a row of found values leads with: a calendar for dates, a picture's icon, a link's, and the first colour itself. */
@Composable
private fun FoundMark(found: Found) {
    Box(modifier = Modifier.size(MarkSize), contentAlignment = Alignment.Center) {
        when (found.kind) {
            FoundKind.Dates -> PantrixIcon(
                imageVector = PantrixIcons.Calendar,
                contentDescription = null,
                tint = PantrixTheme.colors.textSecondary
            )

            FoundKind.Colours -> (found.nodes.first().preview as? ValuePreview.Colour)?.let { colour ->
                Box(
                    modifier = Modifier
                        .size(SwatchSize)
                        .background(Color(colour.argb), PantrixTheme.shapes.sm)
                        .border(Dp.Hairline, PantrixTheme.colors.border, PantrixTheme.shapes.sm)
                )
            }

            FoundKind.Pictures -> PantrixIcon(
                imageVector = PantrixIcons.Image,
                contentDescription = null,
                tint = PantrixTheme.colors.textSecondary
            )

            FoundKind.Links -> PantrixIcon(
                imageVector = PantrixIcons.ExternalLink,
                contentDescription = null,
                tint = PantrixTheme.colors.textSecondary
            )
        }
    }
}

/** The notes on the document, each in a card of its own; one that names an array that reads as a table shows it so. */
@Composable
private fun NotesSection(summary: DocumentSummary, onIntent: (WorkspaceIntent) -> Unit, modifier: Modifier) {
    val notes = summaryNotes(summary)
    if (notes.isEmpty()) return
    SummarySection(stringResource(Res.string.summary_title_notes), modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm)) {
            notes.forEach { note -> NoteCard(note, onIntent) }
        }
    }
}

@Composable
private fun NoteCard(note: SummaryNote, onIntent: (WorkspaceIntent) -> Unit) {
    val spacing = PantrixTheme.spacing
    SummaryCard(padding = PaddingValues(spacing.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            PantrixIcon(imageVector = PantrixIcons.Info, contentDescription = null, tint = PantrixTheme.colors.info.color)
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                PantrixText(text = note.text, style = PantrixTheme.typography.label)
                note.table?.let { table ->
                    PantrixButton(
                        text = stringResource(Res.string.summary_show_table),
                        onClick = {
                            onIntent(WorkspaceIntent.Select(table.node.key))
                            onIntent(WorkspaceIntent.ShowPane(DocumentPane.Table))
                        },
                        variant = ButtonVariant.Soft,
                        size = ButtonSize.ExtraSmall,
                        tagKey = "show_table"
                    )
                }
            }
        }
    }
}
