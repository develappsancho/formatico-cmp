package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.navigation.SystemBack
import com.developersancho.formatico.feature.workspace.strings.compareStrings
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.container.card.CardColors
import com.pantrixui.components.container.card.CardVariant
import com.pantrixui.components.container.card.PantrixCard
import com.pantrixui.components.container.topbar.PantrixBackButton
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.elements.text.TextRole
import com.pantrixui.components.overlay.sheet.PantrixSheet
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme

/**
 * A phone's comparison, a screen of its own in the app bar's place: the way back, "Compare" and the document's "more"
 * menu; A and B, what they are compared by and how many differences of each kind there are; and the differences, a
 * card each with what A has and what B has, and the button that shows it in the two texts. The system's back leaves it
 * as its own button does.
 */
@Composable
internal fun PhoneCompare(
    state: WorkspaceState,
    onIntent: (WorkspaceIntent) -> Unit,
    more: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val document = state.document ?: return
    val back = { onIntent(WorkspaceIntent.ShowPane(openingPane(document.format))) }
    SystemBack(onBack = back)
    val choice = document.view.compare
    Column(modifier = modifier.fillMaxSize().background(PantrixTheme.colors.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = PantrixTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.xs)
        ) {
            PantrixBackButton(onClick = back, tagKey = "back")
            PantrixText(
                text = compareStrings.title,
                modifier = Modifier.weight(1f),
                style = PantrixTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                role = TextRole.Heading(level = 1)
            )
            more()
        }
        PantrixHorizontalDivider()
        PhoneCompareChoices(state, document, choice, onIntent)
        PantrixHorizontalDivider()
        val compared = state.comparison.compared
        if (compared == null) {
            CompareMessage(state.comparison, Modifier.weight(1f).fillMaxWidth().background(PantrixTheme.colors.background))
            return@Column
        }
        DifferenceCards(compared, choice, onIntent, Modifier.weight(1f).fillMaxWidth())
    }
}

/** A and B, what they are compared by, and how many differences of each kind there are, each on a line of its own. */
@Composable
private fun PhoneCompareChoices(state: WorkspaceState, document: OpenDocument, choice: CompareChoice, onIntent: (WorkspaceIntent) -> Unit) {
    val spacing = PantrixTheme.spacing
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            SideTag(Side.A)
            SideName(document.name ?: workspaceStrings.untitled)
            PantrixIcon(imageVector = PantrixIcons.ArrowForward, contentDescription = null, tint = PantrixTheme.colors.textSecondary)
            SideTag(Side.B)
            PartnerMenu(state.comparison.partner, state.others, choice, ButtonSize.Medium, onIntent)
        }
        CompareByChoice(choice, onIntent)
        ChangeTags(state.comparison.compared)
    }
}

/** The differences, a card each, the one shown marked, and what was not counted as one under them. */
@Composable
private fun DifferenceCards(
    compared: CompareOutcome.Compared,
    choice: CompareChoice,
    onIntent: (WorkspaceIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = PantrixTheme.spacing
    var showing by rememberSaveable { mutableStateOf(false) }
    val current = choice.current.coerceIn(0, (compared.differences.size - 1).coerceAtLeast(0))
    LazyColumn(
        modifier = modifier.background(PantrixTheme.colors.background),
        contentPadding = PaddingValues(start = spacing.md, end = spacing.md, top = spacing.md, bottom = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        itemsIndexed(compared.differences, key = { index, _ -> index }) { index, difference ->
            DifferenceCard(difference, chosen = index == current, tagKey = "difference$index") {
                onIntent(WorkspaceIntent.Compare(choice.copy(current = index)))
                showing = true
            }
        }
        item(key = "not counted") {
            if (compared.differences.isEmpty()) {
                NotCountedAlert(compared)
            } else {
                notCountedMessage(compared)?.let { message ->
                    PantrixText(
                        text = "${compareStrings.notCounted}: $message",
                        modifier = Modifier.padding(horizontal = spacing.xs),
                        style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium),
                        color = PantrixTheme.colors.textSecondary
                    )
                }
            }
        }
    }
    ShownInBoth(compared, current, visible = showing, onDismiss = { showing = false })
}

/** A difference's card: its mark, its path and the button that shows it in the texts, and what A and B have there. */
@Composable
private fun DifferenceCard(difference: ShownDifference, chosen: Boolean, tagKey: String, onShow: () -> Unit) {
    val spacing = PantrixTheme.spacing
    val primary = PantrixTheme.colors.primary.color
    val where = differenceWhere(difference)
    PantrixCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.Outlined,
        colors = if (chosen) CardColors(border = primary) else null,
        contentPadding = PaddingValues(horizontal = spacing.md, vertical = 10.dp),
        tagKey = tagKey
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            ChangeMark(difference)
            PantrixText(
                text = where,
                modifier = Modifier.weight(1f),
                style = PantrixTheme.typography.code.copy(fontWeight = FontWeight.Medium),
                maxLines = 1
            )
            PantrixButton(
                text = compareStrings.show,
                onClick = onShow,
                variant = ButtonVariant.Text,
                size = ButtonSize.Small,
                contentDescription = compareStrings.showIn(where),
                tagKey = "show_$tagKey"
            )
        }
        SideValue(compareStrings.inA, difference.before)
        SideValue(compareStrings.inB, difference.after)
    }
}

/** What one side has, after its label: "not there" where it has nothing. */
@Composable
private fun SideValue(label: String, value: String?) {
    val typography = PantrixTheme.typography
    Row(horizontalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm)) {
        PantrixText(
            text = label,
            modifier = Modifier.width(56.dp),
            style = typography.footnote.copy(fontWeight = FontWeight.Medium),
            color = PantrixTheme.colors.textSecondary
        )
        PantrixText(
            text = value ?: compareStrings.notThere,
            modifier = Modifier.weight(1f),
            style = typography.code.copy(fontSize = typography.footnote.fontSize, lineHeight = typography.footnote.lineHeight),
            color = if (value == null) PantrixTheme.colors.textSecondary else PantrixTheme.colors.textPrimary,
            maxLines = 1
        )
    }
}

/** The difference at [current] in A's text and in B's, one over the other, in a sheet over the cards. */
@Composable
private fun ShownInBoth(compared: CompareOutcome.Compared, current: Int, visible: Boolean, onDismiss: () -> Unit) {
    val difference = compared.differences.getOrNull(current)
    PantrixSheet(
        visible = visible && difference != null,
        onDismissRequest = onDismiss,
        title = difference?.let { differenceWhere(it) },
        fullScreen = true,
        scrollContent = false,
        tagKey = "shown"
    ) {
        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
            SideText(compared, Side.A, WorkspaceLayout.Phone, current, Modifier.weight(1f).fillMaxWidth())
            PantrixHorizontalDivider()
            SideText(compared, Side.B, WorkspaceLayout.Phone, current, Modifier.weight(1f).fillMaxWidth())
        }
    }
}
