package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.checkbox.PantrixCheckbox
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.overlay.popover.PantrixPopover
import com.pantrixui.components.overlay.popover.PopoverAlignment
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.workspace_a_to_z
import formatico.feature.workspace.generated.resources.workspace_columns
import formatico.feature.workspace.generated.resources.workspace_columns_all
import formatico.feature.workspace.generated.resources.workspace_columns_some
import formatico.feature.workspace.generated.resources.workspace_largest_first
import formatico.feature.workspace.generated.resources.workspace_newest_first
import formatico.feature.workspace.generated.resources.workspace_numbers
import formatico.feature.workspace.generated.resources.workspace_oldest_first
import formatico.feature.workspace.generated.resources.workspace_smallest_first
import formatico.feature.workspace.generated.resources.workspace_sorted_by
import formatico.feature.workspace.generated.resources.workspace_stat_average
import formatico.feature.workspace.generated.resources.workspace_stat_different
import formatico.feature.workspace.generated.resources.workspace_stat_empty
import formatico.feature.workspace.generated.resources.workspace_stat_max
import formatico.feature.workspace.generated.resources.workspace_stat_min
import formatico.feature.workspace.generated.resources.workspace_value_column
import formatico.feature.workspace.generated.resources.workspace_values
import formatico.feature.workspace.generated.resources.workspace_z_to_a
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The most digits after the mark a column's numbers are summed up with. */
private const val MOST_FRACTION_DIGITS = 4

/** The digits after the mark an average is written with, at the least. */
private const val AVERAGE_FRACTION_DIGITS = 2

/**
 * The button that says how many of the table's columns are shown, and opens a box of them, each with a checkbox that
 * shows it or hides it: a button with its word in a wide window, an icon elsewhere.
 */
@Composable
internal fun ColumnsChooser(
    table: TableModel,
    hidden: Set<String>,
    wide: Boolean,
    size: ButtonSize,
    onHide: (Set<String>) -> Unit
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val total = table.columns.size
    val shown = table.columns.count { it !in hidden }
    val description = if (shown == total) {
        stringResource(Res.string.workspace_columns_all, total)
    } else {
        stringResource(Res.string.workspace_columns_some, shown, total)
    }
    val valueName = stringResource(Res.string.workspace_value_column)
    PantrixPopover(
        visible = open,
        onDismissRequest = { open = false },
        anchor = {
            if (wide) {
                PantrixButton(
                    text = stringResource(Res.string.workspace_columns),
                    onClick = { open = true },
                    variant = ButtonVariant.Outline,
                    size = size,
                    leadingIcon = { PantrixIcon(imageVector = PantrixIcons.Eye, contentDescription = null) },
                    contentDescription = description,
                    tagKey = "columns"
                )
            } else {
                PantrixIconButton(
                    imageVector = PantrixIcons.Eye,
                    contentDescription = description,
                    onClick = { open = true },
                    size = size,
                    tagKey = "columns"
                )
            }
        },
        alignment = PopoverAlignment.End,
        title = stringResource(Res.string.workspace_columns),
        tagKey = "columns"
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            table.columns.forEachIndexed { index, name ->
                PantrixCheckbox(
                    checked = name !in hidden,
                    onCheckedChange = { checked -> onHide(if (checked) hidden - name else hidden + name) },
                    label = if (name == VALUE_COLUMN) valueName else name,
                    tagKey = "column_$index"
                )
            }
        }
    }
}

/** What [column]'s values come to: `total · 248 numbers · min 4.50 · max 1,250.00 · average 128.24`. */
@Composable
internal fun ColumnSummary(column: String, stats: ColumnStats, table: TableModel) {
    val strings = PantrixTheme.strings
    val name = if (column == VALUE_COLUMN) stringResource(Res.string.workspace_value_column) else column
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.lg)
    ) {
        PantrixText(
            text = name,
            style = PantrixTheme.typography.code.copy(
                fontSize = PantrixTheme.typography.footnote.fontSize,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1
        )
        when (stats) {
            is ColumnStats.Numbers -> {
                val digits = fractionDigits(table, column)
                Quiet(pluralStringResource(Res.plurals.workspace_numbers, stats.count, strings.decimal(stats.count, 0)))
                Stat(stringResource(Res.string.workspace_stat_min), strings.decimal(stats.min, digits, digits))
                Stat(stringResource(Res.string.workspace_stat_max), strings.decimal(stats.max, digits, digits))
                val average = maxOf(digits, AVERAGE_FRACTION_DIGITS)
                Stat(
                    stringResource(Res.string.workspace_stat_average),
                    strings.decimal(stats.average, average, average)
                )
            }

            is ColumnStats.Values -> {
                Quiet(pluralStringResource(Res.plurals.workspace_values, stats.count, strings.decimal(stats.count, 0)))
                Stat(
                    strings.decimal(stats.different, 0),
                    stringResource(Res.string.workspace_stat_different),
                    valueFirst = true
                )
                Stat(
                    strings.decimal(stats.empty, 0),
                    stringResource(Res.string.workspace_stat_empty),
                    valueFirst = true
                )
            }
        }
    }
}

/** How many digits after the mark the numbers under [column] are written with, the most of them. */
private fun fractionDigits(table: TableModel, column: String): Int = table.rows
    .mapNotNull { row -> row.cell(column)?.text }
    .maxOfOrNull { text -> text.substringAfter('.', "").takeWhile { it.isDigit() }.length }
    ?.coerceAtMost(MOST_FRACTION_DIGITS)
    ?: 0

@Composable
private fun Quiet(text: String) {
    PantrixText(
        text = text,
        style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium),
        color = PantrixTheme.colors.textSecondary,
        maxLines = 1
    )
}

/** A word and its value, `min 4.50`, the value in the code's type; [valueFirst], `5 different`. */
@Composable
private fun Stat(word: String, value: String, valueFirst: Boolean = false) {
    val quiet = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium)
    val strong = PantrixTheme.typography.code.copy(fontSize = quiet.fontSize, fontWeight = FontWeight.Medium)
    Row(horizontalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.xs)) {
        if (valueFirst) {
            PantrixText(text = word, style = strong, maxLines = 1)
            PantrixText(text = value, style = quiet, color = PantrixTheme.colors.textSecondary, maxLines = 1)
        } else {
            PantrixText(text = word, style = quiet, color = PantrixTheme.colors.textSecondary, maxLines = 1)
            PantrixText(text = value, style = strong, maxLines = 1)
        }
    }
}

/** `Sorted by createdAt, newest first`: the order by its column's kind, dates, numbers or words. */
@Composable
internal fun SortSummary(table: TableModel, sort: TableSort) {
    val first = table.rows.firstNotNullOfOrNull { it.cell(sort.column)?.takeIf { cell -> cell.kind != ValueKind.Null } }
    val dates = first?.preview is ValuePreview.Moment || first?.preview is ValuePreview.Day
    val order = when {
        dates -> {
            if (sort.descending) Res.string.workspace_newest_first else Res.string.workspace_oldest_first
        }

        first?.kind == ValueKind.Number -> {
            if (sort.descending) Res.string.workspace_largest_first else Res.string.workspace_smallest_first
        }

        else -> {
            if (sort.descending) Res.string.workspace_z_to_a else Res.string.workspace_a_to_z
        }
    }
    val name = if (sort.column == VALUE_COLUMN) stringResource(Res.string.workspace_value_column) else sort.column
    Quiet(stringResource(Res.string.workspace_sorted_by, name, stringResource(order)))
}
