package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.designsystem.LocalSyntaxColors
import com.developersancho.formatico.core.designsystem.SyntaxColors
import com.developersancho.formatico.core.format.query.QueryErrorCode
import com.developersancho.formatico.feature.workspace.strings.queryStrings
import com.pantrixui.components.data.listitem.ListItemColors
import com.pantrixui.components.data.listitem.ListItemStyle
import com.pantrixui.components.data.listitem.PantrixListItem
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.foundation.theme.PantrixTheme
import com.pantrixui.foundation.token.shape.ShapeSpec

/**
 * A row of what a query found: a value, at its [path], written as [value] in the colour of its [kind] where it has one,
 * choosing the tree's value keyed [key] where it is one; or the value of its own an XPath came to, beside the XPath.
 */
@Immutable
internal data class QueryRow(val path: String, val value: String, val kind: ValueKind?, val key: String?)

/** The rows of what the query of [state] found. */
internal fun matchRows(state: WorkspaceState): List<QueryRow> = when (val outcome = state.query) {
    is QueryOutcome.Found -> outcome.matches.map { QueryRow(path = it.path, value = it.value, kind = it.kind, key = it.key) }

    is QueryOutcome.Computed -> listOf(
        QueryRow(path = state.document?.view?.query?.trim().orEmpty(), value = outcome.value, kind = null, key = null)
    )

    else -> emptyList()
}

/**
 * A match's row, [chosen] where its value is the one chosen, choosing it with [onChoose]: in a wide window and on a
 * tablet its path beside its value, on a phone its value over its path, a chevron saying it leads to the text.
 */
@Composable
internal fun MatchRow(row: QueryRow, layout: WorkspaceLayout, chosen: Boolean, onChoose: (() -> Unit)?, tagKey: String) {
    val colors = PantrixTheme.colors
    val typography = PantrixTheme.typography
    val spacing = PantrixTheme.spacing
    val ink = row.kind?.let { syntaxInk(it, LocalSyntaxColors.current, colors.textSecondary) } ?: colors.textPrimary
    val paint = ListItemColors(
        content = ink,
        supportingContent = colors.textSecondary,
        selectedContainer = colors.primary.container,
        selectedContent = colors.primary.onContainer
    )
    if (layout == WorkspaceLayout.Phone) {
        PantrixListItem(
            headline = row.value,
            supportingText = row.path,
            chevron = onChoose != null,
            onClick = onChoose,
            selected = chosen,
            supportingMaxLines = 1,
            style = ListItemStyle(twoLineHeight = 56.dp, horizontalPadding = spacing.lg, shape = ShapeSpec.Fixed(PantrixTheme.shapes.md)),
            colors = paint,
            tagKey = tagKey,
            headlineStyle = typography.code.copy(fontSize = typography.body.fontSize, lineHeight = typography.body.lineHeight),
            supportingTextStyle = typography.code.copy(fontSize = typography.footnote.fontSize, lineHeight = typography.footnote.lineHeight)
        )
        return
    }
    PantrixListItem(
        headline = row.value,
        leading = {
            PantrixText(
                text = row.path,
                modifier = Modifier.width(pathWidth(layout)),
                style = typography.code,
                color = if (chosen) colors.primary.onContainer else colors.textSecondary,
                maxLines = 1
            )
        },
        onClick = onChoose,
        selected = chosen,
        style = ListItemStyle(
            oneLineHeight = if (layout == WorkspaceLayout.Wide) 40.dp else 48.dp,
            horizontalPadding = spacing.md,
            verticalPadding = 6.dp,
            gap = spacing.md,
            shape = ShapeSpec.Fixed(PantrixTheme.shapes.sm)
        ),
        colors = paint,
        tagKey = tagKey,
        headlineStyle = typography.code
    )
}

/** The colour a value of [kind] is written in: its syntax's, and the size of an object or an array in [quiet]. */
internal fun syntaxInk(kind: ValueKind, syntax: SyntaxColors, quiet: Color): Color = when (kind) {
    ValueKind.Object, ValueKind.Array -> quiet
    ValueKind.String -> syntax.string
    ValueKind.Number -> syntax.number
    ValueKind.Boolean, ValueKind.Null, ValueKind.Alias -> syntax.literal
}

/**
 * What the query's field says under it: how many matches there are, that there are none, how to ask, or where the query
 * stops being one; and where [withOtherLanguage] says so, which documents the other language is for.
 */
@Composable
internal fun queryStatus(outcome: QueryOutcome?, language: QueryLanguage, withOtherLanguage: Boolean): String? {
    val status = when (outcome) {
        null, QueryOutcome.Unasked -> if (language == QueryLanguage.XPath) queryStrings.askXpath else queryStrings.askJsonpath
        QueryOutcome.Unread -> return queryStrings.unread
        is QueryOutcome.Failed -> return queryError(outcome.error.code, outcome.error.offset)
        is QueryOutcome.Found -> resultsTitle(outcome)
        is QueryOutcome.Computed -> queryStrings.computed
    }
    val other = if (language == QueryLanguage.XPath) queryStrings.jsonpathElsewhere else queryStrings.xpathElsewhere
    return if (withOtherLanguage) queryStrings.statusAndNote(status, other) else status
}

/** The title over what [outcome] found: how many matches, none, or the value an XPath came to. */
@Composable
internal fun resultsTitle(outcome: QueryOutcome?): String = when (outcome) {
    is QueryOutcome.Found -> if (outcome.matches.isEmpty()) {
        queryStrings.noMatches
    } else {
        queryStrings.matches(outcome.matches.size, outcome.matches.size)
    }

    is QueryOutcome.Computed -> queryStrings.value

    else -> ""
}

/** Where a query stops being one, at the character [offset] counts from nought, and why: [code]. */
@Composable
internal fun queryError(code: QueryErrorCode, offset: Int): String =
    queryStrings.errorAt(queryStrings.errorOf(code), offset + 1)
