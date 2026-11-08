package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.feature.workspace.strings.tableStrings
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.checkbox.PantrixCheckbox
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.overlay.common.OverlayPresentation
import com.pantrixui.components.overlay.popover.PantrixPopover
import com.pantrixui.components.overlay.popover.PopoverAlignment
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme

/** The most digits after the mark a column's numbers are summed up with. */
private const val MOST_FRACTION_DIGITS = 4

/** The digits after the mark an average is written with, at the least. */
private const val AVERAGE_FRACTION_DIGITS = 2

/** Where a checkbox's label starts after its box: the line under the header's checkbox stands in by it. */
private val CheckboxLabelInset = 28.dp

/**
 * The button that says how many of the table's columns are shown, and opens a box of them, each with a checkbox that
 * shows it or hides it: a button with its word in a wide window, an icon elsewhere. A CSV's or a TSV's box has the
 * checkbox that reads its first row as the header over the columns, which it names, where [header] is not null: its
 * state.
 */
@Composable
internal fun ColumnsChooser(
    table: TableModel,
    hidden: Set<String>,
    layout: WorkspaceLayout,
    size: ButtonSize,
    onHide: (Set<String>) -> Unit,
    header: Boolean? = null,
    onHeader: (Boolean) -> Unit = {}
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val total = table.columns.size
    val shown = table.columns.count { it !in hidden }
    val description = if (shown == total) {
        workspaceStrings.columnsAll(total)
    } else {
        workspaceStrings.columnsSome(shown, total)
    }
    val valueName = workspaceStrings.valueColumn
    val wide = layout == WorkspaceLayout.Wide
    val phone = layout == WorkspaceLayout.Phone
    val spacing = PantrixTheme.spacing
    PantrixPopover(
        visible = open,
        onDismissRequest = { open = false },
        anchor = {
            if (wide) {
                PantrixButton(
                    text = workspaceStrings.columns,
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
        title = workspaceStrings.columns,
        // A phone's choice is a sheet from the bottom, as Table-390 draws it.
        presentation = OverlayPresentation.Adaptive,
        tagKey = "columns"
    ) {
        // The popover scrolls what it holds, under its title, where the columns are more than its height takes: a
        // scroll of their own inside it would be measured without an end, and fail. A phone's sheet spaces its rows
        // wider, as Table-390 draws it.
        Column(verticalArrangement = Arrangement.spacedBy(if (phone) spacing.sm else spacing.xs)) {
            if (header != null) FirstRowHeader(header, onHeader, phone)
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

/**
 * The checkbox that reads a CSV's or a TSV's first row as its header, [header] its state, the line that says what it
 * does under it, and the line that holds it apart from the columns: wider apart on a [phone]'s sheet.
 */
@Composable
private fun FirstRowHeader(header: Boolean, onHeader: (Boolean) -> Unit, phone: Boolean) {
    PantrixCheckbox(
        checked = header,
        onCheckedChange = onHeader,
        label = tableStrings.firstRowHeader,
        tagKey = "first_row_header"
    )
    PantrixText(
        text = tableStrings.firstRowHeaderOff,
        modifier = Modifier.padding(start = CheckboxLabelInset),
        style = PantrixTheme.typography.footnote,
        color = PantrixTheme.colors.textSecondary
    )
    val spacing = PantrixTheme.spacing
    PantrixHorizontalDivider(modifier = Modifier.padding(vertical = if (phone) spacing.xs else spacing.sm))
}

/** What [column]'s values come to: `total · 248 numbers · min 4.50 · max 1,250.00 · average 128.24`. */
@Composable
internal fun ColumnSummary(column: String, stats: ColumnStats, table: TableModel) {
    val strings = PantrixTheme.strings
    val name = if (column == VALUE_COLUMN) workspaceStrings.valueColumn else column
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
                Quiet(workspaceStrings.numbers(stats.count, strings.decimal(stats.count, 0)))
                Stat(workspaceStrings.statMin, strings.decimal(stats.min, digits, digits))
                Stat(workspaceStrings.statMax, strings.decimal(stats.max, digits, digits))
                val average = maxOf(digits, AVERAGE_FRACTION_DIGITS)
                Stat(
                    workspaceStrings.statAverage,
                    strings.decimal(stats.average, average, average)
                )
            }

            is ColumnStats.Values -> {
                Quiet(workspaceStrings.values(stats.count, strings.decimal(stats.count, 0)))
                Stat(
                    strings.decimal(stats.different, 0),
                    workspaceStrings.statDifferent,
                    valueFirst = true
                )
                Stat(
                    strings.decimal(stats.empty, 0),
                    workspaceStrings.statEmpty,
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
            if (sort.descending) workspaceStrings.newestFirst else workspaceStrings.oldestFirst
        }

        first?.kind == ValueKind.Number -> {
            if (sort.descending) workspaceStrings.largestFirst else workspaceStrings.smallestFirst
        }

        else -> {
            if (sort.descending) workspaceStrings.zToA else workspaceStrings.aToZ
        }
    }
    val name = if (sort.column == VALUE_COLUMN) workspaceStrings.valueColumn else sort.column
    Quiet(workspaceStrings.sortedBy(name, order))
}
