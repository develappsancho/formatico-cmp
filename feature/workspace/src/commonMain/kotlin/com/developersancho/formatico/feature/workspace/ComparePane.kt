package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.feature.workspace.editor.ComparedText
import com.developersancho.formatico.feature.workspace.strings.compareStrings
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.divider.PantrixVerticalDivider
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.elements.text.TextRole
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme

/**
 * A document compared with another in a wide window or on a tablet, in the whole room, the toolbar's and the status
 * bar's as well: A and B and what they are compared by over them; the differences, with what was not counted as one;
 * the two texts, each difference's lines on a band; and which difference is shown under them.
 */
@Composable
internal fun CompareScreen(
    state: WorkspaceState,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val document = state.document ?: return
    Column(modifier = modifier.fillMaxSize().background(PantrixTheme.colors.surface)) {
        CompareBar(state, document, layout, onIntent)
        PantrixHorizontalDivider()
        val compared = state.comparison.compared
        if (compared == null) {
            CompareMessage(state.comparison, Modifier.weight(1f).fillMaxWidth())
            return@Column
        }
        val choice = document.view.compare
        val current = choice.current.coerceIn(0, (compared.differences.size - 1).coerceAtLeast(0))
        val choose = { index: Int -> onIntent(WorkspaceIntent.Compare(choice.copy(current = index))) }
        if (layout == WorkspaceLayout.Wide) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Differences(compared, layout, current, choose, Modifier.width(DifferencesWidth).fillMaxHeight())
                PantrixVerticalDivider()
                Sides(compared, layout, current, Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Differences(compared, layout, current, choose, Modifier.fillMaxWidth().heightIn(max = TabletDifferencesHeight))
            PantrixHorizontalDivider()
            Sides(compared, layout, current, Modifier.weight(1f).fillMaxWidth())
        }
        CompareFooter(compared, layout, current)
    }
}

/** How wide a wide window's differences stand beside the texts: the board's 340. */
private val DifferencesWidth = 340.dp

/** How tall a tablet's differences stand over the texts before they scroll. */
private val TabletDifferencesHeight = 360.dp

/**
 * A and B, what they are compared by, how many differences of each kind there are, and the way out: one row in a wide
 * window, two on a tablet.
 */
@Composable
private fun CompareBar(state: WorkspaceState, document: OpenDocument, layout: WorkspaceLayout, onIntent: (WorkspaceIntent) -> Unit) {
    val spacing = PantrixTheme.spacing
    val choice = document.view.compare
    val size = layout.buttons
    val wide = layout == WorkspaceLayout.Wide
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = spacing.md, vertical = if (wide) spacing.none else spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.CenterVertically),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        SideTag(Side.A)
        SideName(document.name ?: workspaceStrings.untitled)
        PantrixIcon(imageVector = PantrixIcons.ArrowForward, contentDescription = null, tint = PantrixTheme.colors.textSecondary)
        SideTag(Side.B)
        PartnerMenu(state.comparison.partner, state.others, choice, size, onIntent)
        if (wide) {
            PantrixVerticalDivider(modifier = Modifier.height(24.dp))
            CompareByLabel()
            CompareByChoice(choice, onIntent)
            Spacer(modifier = Modifier.weight(1f))
            ChangeTags(state.comparison.compared)
            StopComparingButton(document, size, onIntent)
        } else {
            Spacer(modifier = Modifier.weight(1f))
            StopComparingButton(document, size, onIntent)
            // The choice and the counts go on to a line of their own, as Compare-834 draws them.
            Spacer(modifier = Modifier.fillMaxWidth())
            CompareByChoice(choice, onIntent)
            Spacer(modifier = Modifier.weight(1f))
            ChangeTags(state.comparison.compared)
        }
    }
}

/** The differences, the one shown chosen, and what was not counted as one under them. */
@Composable
private fun Differences(
    compared: CompareOutcome.Compared,
    layout: WorkspaceLayout,
    current: Int,
    onChoose: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = PantrixTheme.spacing
    val wide = layout == WorkspaceLayout.Wide
    LazyColumn(
        modifier = modifier.background(PantrixTheme.colors.background),
        contentPadding = PaddingValues(horizontal = spacing.sm, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (wide && compared.differences.isNotEmpty()) {
            item(key = "title") {
                PantrixText(
                    text = compareStrings.differences(compared.differences.size, compared.differences.size),
                    modifier = Modifier.padding(start = spacing.sm, end = spacing.sm, bottom = spacing.xs),
                    style = PantrixTheme.typography.callout.copy(fontWeight = FontWeight.SemiBold),
                    role = TextRole.Heading(level = 2)
                )
            }
        }
        itemsIndexed(compared.differences, key = { index, _ -> index }) { index, difference ->
            DifferenceRow(difference, layout, chosen = index == current, onChoose = { onChoose(index) }, tagKey = "difference$index")
        }
        item(key = "not counted") {
            NotCountedAlert(compared, Modifier.padding(start = spacing.xs, end = spacing.xs, top = spacing.md))
        }
    }
}

/** A's text and B's beside it, each under its letter, its name and its lines. */
@Composable
private fun Sides(compared: CompareOutcome.Compared, layout: WorkspaceLayout, current: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        SideText(compared, Side.A, layout, current, Modifier.weight(1f).fillMaxHeight())
        PantrixVerticalDivider()
        SideText(compared, Side.B, layout, current, Modifier.weight(1f).fillMaxHeight())
    }
}

/** One of the texts compared, under its letter, its name and, in a wide window, its lines. */
@Composable
internal fun SideText(compared: CompareOutcome.Compared, side: Side, layout: WorkspaceLayout, current: Int, modifier: Modifier = Modifier) {
    val document = if (side == Side.A) compared.a else compared.b
    val letter = if (side == Side.A) compareStrings.a else compareStrings.b
    val name = comparedName(document)
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().height(36.dp).padding(horizontal = PantrixTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PantrixText(
                text = if (layout == WorkspaceLayout.Wide) {
                    compareStrings.side(document.lines, letter, name, document.lines)
                } else {
                    compareStrings.sideShort(letter, name)
                },
                style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium),
                color = PantrixTheme.colors.textSecondary,
                maxLines = 1
            )
        }
        PantrixHorizontalDivider()
        ComparedText(
            text = document.text,
            format = document.format,
            name = name,
            marks = sideMarks(compared, side, current),
            values = sideValues(compared, side),
            shown = shownLines(compared, side, current),
            tagKey = if (side == Side.A) "text_a" else "text_b",
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
    }
}

/** Which difference is shown and where it is, and in a wide window what the documents were compared by. */
@Composable
private fun CompareFooter(compared: CompareOutcome.Compared, layout: WorkspaceLayout, current: Int) {
    val style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium)
    val quiet = PantrixTheme.colors.textSecondary
    Column(modifier = Modifier.fillMaxWidth().background(PantrixTheme.colors.background)) {
        PantrixHorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (layout == WorkspaceLayout.Wide) 28.dp else 32.dp)
                .padding(horizontal = PantrixTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.lg)
        ) {
            val difference = compared.differences.getOrNull(current)
            if (difference != null) {
                PantrixText(
                    text = compareStrings.differenceOf(current + 1, compared.differences.size),
                    style = style,
                    color = quiet
                )
                PantrixText(
                    text = differenceWhere(difference),
                    style = PantrixTheme.typography.code.copy(fontSize = style.fontSize, lineHeight = style.lineHeight),
                    color = quiet,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (layout == WorkspaceLayout.Wide) {
                PantrixText(
                    text = if (compared.by ==
                        CompareBy.Meaning
                    ) {
                        compareStrings.byMeaningNote
                    } else {
                        compareStrings.byTextNote
                    },
                    style = style,
                    color = quiet,
                    maxLines = 1
                )
            }
        }
    }
}
