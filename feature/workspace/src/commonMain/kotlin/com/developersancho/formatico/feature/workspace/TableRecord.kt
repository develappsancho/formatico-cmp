package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.designsystem.LocalSyntaxColors
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.foundation.theme.PantrixTheme

/** The width of a record's index, which the line under its first stands in by. */
private val RecordIndexWidth = 20.dp

/** The most a record's second value takes of its first line, so that its first keeps the rest. */
private val RecordSecondWidth = 144.dp

/** What a record shows for a key its object does not have. */
private const val MISSING = "—"

/** The names of the columns that name a record, in any case: the one a phone's record is headed by. */
private val TitleColumns = setOf("name", "title", "label")

/** [columns] in the order a phone's record shows them: the one that names the record first, where there is one. */
internal fun recordOrder(columns: List<String>): List<String> {
    val title = columns.firstOrNull { it.lowercase() in TitleColumns } ?: return columns
    return listOf(title) + columns.filter { it != title }
}

/**
 * A row as a phone's list shows it: its index, the value under the column that names it (or else its first column)
 * and the one under the next, and under them the other columns as `key: value`, on a line cut where the screen ends.
 * On that line a string is written as the document writes it, with its quotes where it is [asWritten], and a CSV
 * field as its value.
 */
@Composable
internal fun TableRecord(row: TableRow, shown: List<String>, chosen: Boolean, asWritten: Boolean) {
    val columns = recordOrder(shown)
    val spacing = PantrixTheme.spacing
    val colors = PantrixTheme.colors
    val code = PantrixTheme.typography.code
    val small = code.copy(fontSize = PantrixTheme.typography.footnote.fontSize)
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            PantrixText(
                text = "${row.index}",
                modifier = Modifier.width(RecordIndexWidth),
                style = small,
                color = if (chosen) colors.primary.color else colors.textSecondary,
                textAlign = TextAlign.End,
                maxLines = 1
            )
            PantrixText(
                text = columns.firstOrNull()?.let { recordText(row.cell(it), asWritten = false) }.orEmpty(),
                modifier = Modifier.weight(1f),
                style = code.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1
            )
            columns.getOrNull(1)?.let { column ->
                PantrixText(
                    text = recordText(row.cell(column), asWritten = false),
                    modifier = Modifier.widthIn(max = RecordSecondWidth),
                    style = small,
                    color = colors.textSecondary,
                    maxLines = 1
                )
            }
        }
        val rest = columns.drop(2)
        if (rest.isNotEmpty()) {
            PantrixText(
                text = fieldsText(row, rest, asWritten),
                modifier = Modifier.padding(start = RecordIndexWidth + spacing.sm),
                style = small,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/** A cell's value on a record's first line: a missing key as a dash, an object or an array by its size. */
private fun recordText(cell: ValueNode?, asWritten: Boolean): String = when {
    cell == null -> MISSING
    cell.kind == ValueKind.String && !asWritten -> cell.shownText
    else -> cell.text
}

/** `paid: true  items: [2]  coupon: null`, each in the colour of its kind. */
@Composable
private fun fieldsText(row: TableRow, columns: List<String>, asWritten: Boolean): AnnotatedString {
    val syntax = LocalSyntaxColors.current
    val missing = PantrixTheme.colors.textSecondary
    val valueName = workspaceStrings.valueColumn
    return buildAnnotatedString {
        columns.forEachIndexed { index, column ->
            if (index > 0) append("  ")
            withStyle(SpanStyle(color = syntax.key)) { append(if (column == VALUE_COLUMN) valueName else column) }
            withStyle(SpanStyle(color = syntax.punctuation)) { append(": ") }
            val cell = row.cell(column)
            val style = when {
                cell == null -> SpanStyle(color = missing)
                cell.kind == ValueKind.Null -> SpanStyle(color = syntax.literal, fontStyle = FontStyle.Italic)
                cell.kind == ValueKind.Boolean -> SpanStyle(color = syntax.literal)
                cell.kind == ValueKind.Number -> SpanStyle(color = syntax.number)
                cell.kind == ValueKind.String -> SpanStyle(color = syntax.string)
                else -> SpanStyle(color = syntax.punctuation)
            }
            withStyle(style) { append(recordText(cell, asWritten)) }
        }
    }
}
