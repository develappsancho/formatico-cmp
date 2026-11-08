package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pantrixui.components.container.alert.AlertTone
import com.pantrixui.components.container.alert.PantrixAlert
import com.pantrixui.components.data.listitem.ListItemColors
import com.pantrixui.components.data.listitem.ListItemStyle
import com.pantrixui.components.data.listitem.PantrixListItem
import com.pantrixui.components.data.tag.PantrixTag
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme
import com.pantrixui.foundation.token.shape.ShapeSpec
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.compare_alone_body
import formatico.feature.workspace.generated.resources.compare_alone_title
import formatico.feature.workspace.generated.resources.compare_invalid_body
import formatico.feature.workspace.generated.resources.compare_invalid_title
import formatico.feature.workspace.generated.resources.compare_no_differences
import formatico.feature.workspace.generated.resources.compare_not_counted
import formatico.feature.workspace.generated.resources.compare_same_meaning
import formatico.feature.workspace.generated.resources.compare_same_text
import formatico.feature.workspace.generated.resources.compare_unread_body
import formatico.feature.workspace.generated.resources.compare_unread_title
import formatico.feature.workspace.generated.resources.workspace_untitled
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The mark of a difference's change, `~`, `+` or `−`, on a tag of its tone, named for a screen reader. */
@Composable
internal fun ChangeMark(difference: ShownDifference) {
    PantrixTag(
        text = difference.change.mark,
        tone = difference.change.tagTone,
        contentDescription = changeName(difference.change),
        textStyle = PantrixTheme.typography.code
    )
}

/**
 * A difference's row, [chosen] where it is the one shown, showing it with [onChoose]: in a wide window its path over
 * what it is, on a tablet the two side by side.
 */
@Composable
internal fun DifferenceRow(difference: ShownDifference, layout: WorkspaceLayout, chosen: Boolean, onChoose: () -> Unit, tagKey: String) {
    val colors = PantrixTheme.colors
    val typography = PantrixTheme.typography
    val spacing = PantrixTheme.spacing
    val paint = ListItemColors(
        content = colors.textPrimary,
        supportingContent = colors.textSecondary,
        selectedContainer = colors.primary.container,
        selectedContent = colors.primary.onContainer
    )
    val wide = layout == WorkspaceLayout.Wide
    PantrixListItem(
        headline = if (wide) differenceWhere(difference) else differenceWhat(difference),
        supportingText = if (wide) differenceWhat(difference) else null,
        leading = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                ChangeMark(difference)
                if (!wide) {
                    PantrixText(
                        text = differenceWhere(difference),
                        modifier = Modifier.width(TabletPathWidth),
                        style = typography.code,
                        color = if (chosen) colors.primary.onContainer else colors.textPrimary,
                        maxLines = 1
                    )
                }
            }
        },
        onClick = onChoose,
        selected = chosen,
        supportingMaxLines = 1,
        style = ListItemStyle(
            oneLineHeight = 48.dp,
            twoLineHeight = 52.dp,
            horizontalPadding = spacing.md,
            verticalPadding = spacing.sm,
            gap = spacing.md,
            shape = ShapeSpec.Fixed(PantrixTheme.shapes.sm)
        ),
        colors = paint,
        tagKey = tagKey,
        headlineStyle = typography.code,
        supportingTextStyle = typography.code.copy(fontSize = typography.footnote.fontSize, lineHeight = typography.footnote.lineHeight)
    )
}

/** How wide a difference's path stands on a tablet, beside what it is: the board's 170. */
private val TabletPathWidth = 170.dp

/**
 * What a comparison by meaning did not count as a difference, in an alert of its own; or, where the two documents
 * differ in nothing, that they mean the same or are the same text.
 */
@Composable
internal fun NotCountedAlert(compared: CompareOutcome.Compared, modifier: Modifier = Modifier) {
    if (compared.differences.isEmpty()) {
        PantrixAlert(
            message = stringResource(
                if (compared.by ==
                    CompareBy.Meaning
                ) {
                    Res.string.compare_same_meaning
                } else {
                    Res.string.compare_same_text
                }
            ),
            modifier = modifier,
            tone = AlertTone.Success,
            title = stringResource(Res.string.compare_no_differences),
            tagKey = "no_differences"
        )
        return
    }
    val message = notCountedMessage(compared) ?: return
    PantrixAlert(
        message = message,
        modifier = modifier,
        tone = AlertTone.Info,
        title = stringResource(Res.string.compare_not_counted),
        announce = false,
        tagKey = "not_counted"
    )
}

/** Why there is nothing compared: no other document open, or one of the two with no value to compare by meaning. */
@Composable
internal fun CompareMessage(outcome: CompareOutcome?, modifier: Modifier = Modifier) {
    val untitled = stringResource(Res.string.workspace_untitled)
    when (outcome) {
        CompareOutcome.Alone -> PaneMessage(
            icon = PantrixIcons.Info,
            title = stringResource(Res.string.compare_alone_title),
            body = stringResource(Res.string.compare_alone_body),
            modifier = modifier
        )

        is CompareOutcome.Unread -> PaneMessage(
            icon = PantrixIcons.Info,
            title = stringResource(Res.string.compare_unread_title, outcome.name ?: untitled),
            body = stringResource(Res.string.compare_unread_body),
            modifier = modifier
        )

        is CompareOutcome.Invalid -> PaneMessage(
            icon = PantrixIcons.Warning,
            title = pluralStringResource(Res.plurals.compare_invalid_title, outcome.errors, outcome.name ?: untitled, outcome.errors),
            body = stringResource(Res.string.compare_invalid_body),
            modifier = modifier
        )

        else -> Unit
    }
}
