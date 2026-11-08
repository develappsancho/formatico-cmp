package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.designsystem.LocalSyntaxColors
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.csv.csvText
import com.pantrixui.components.data.datatable.DataTableColumn
import com.pantrixui.components.data.datatable.DataTableLayout
import com.pantrixui.components.data.datatable.DataTablePin
import com.pantrixui.components.data.datatable.DataTableSort
import com.pantrixui.components.data.datatable.DataTableVariant
import com.pantrixui.components.data.datatable.PantrixDataTable
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.inputs.textfield.PantrixTextField
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.workspace_filter_rows
import formatico.feature.workspace.generated.resources.workspace_index
import formatico.feature.workspace.generated.resources.workspace_missing_key
import formatico.feature.workspace.generated.resources.workspace_no_rows
import formatico.feature.workspace.generated.resources.workspace_no_table_body
import formatico.feature.workspace.generated.resources.workspace_no_table_title
import formatico.feature.workspace.generated.resources.workspace_open_value
import formatico.feature.workspace.generated.resources.workspace_rows
import formatico.feature.workspace.generated.resources.workspace_rows_of
import formatico.feature.workspace.generated.resources.workspace_save_csv
import formatico.feature.workspace.generated.resources.workspace_separated
import formatico.feature.workspace.generated.resources.workspace_table_columns
import formatico.feature.workspace.generated.resources.workspace_value_column
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The widest the filter field stands in a wide window, and on a tablet. */
private val WideFilterWidth = 200.dp
private val TabletFilterWidth = 180.dp

/** The index column's width. */
private val IndexWidth = 48.dp

/** The least width of a value's column. */
private val ColumnMinWidth = 96.dp

/** The line drawn round the chosen cell. */
private val ChosenCellLine = 2.dp

/**
 * The table of the array the tree's choice is in: its toolbar, a row to each item and a column to each key, and under
 * it what the chosen column's values come to. The rows are filtered by what is written in the filter, ordered by a
 * press on a header, and a cell or a row chooses its value in the tree.
 */
@Composable
internal fun TablePane(
    state: WorkspaceState,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands,
    modifier: Modifier = Modifier
) {
    val document = state.document ?: return
    key(document.id) {
        val filter = rememberTextFieldState()
        val table = state.table
        val view = document.view
        val visible = table?.columns?.filterNot { it in view.hiddenColumns }.orEmpty()
        val query = filter.text.toString()
        val shown = remember(table, visible, query, view.tableSort) {
            table?.let { sorted(filtered(it.rows, visible, query), view.tableSort) }.orEmpty()
        }
        val valueName = stringResource(Res.string.workspace_value_column)
        val toolbar = TableToolbarState(
            state = state,
            filter = filter,
            shown = shown.size,
            hidden = view.hiddenColumns,
            onSave = {
                table?.let { commands.saveCsv(document, csvText(csvRows(shown, visible, document.text, valueName))) }
            }
        )
        Column(modifier = modifier.fillMaxSize().background(PantrixTheme.colors.background)) {
            TableToolbar(toolbar, layout, onIntent)
            val message = treeMessage(state)
            val gap = PantrixTheme.spacing.md
            when {
                message != null ->
                    PaneMessage(PantrixIcons.Info, message.first, message.second, Modifier.fillMaxSize())

                table == null -> PaneMessage(
                    icon = PantrixIcons.Info,
                    title = stringResource(Res.string.workspace_no_table_title),
                    body = stringResource(Res.string.workspace_no_table_body),
                    modifier = Modifier.fillMaxSize()
                )

                else -> RowsTable(
                    table = table,
                    visible = visible,
                    shown = shown,
                    state = state,
                    query = query,
                    phone = layout == WorkspaceLayout.Phone,
                    onIntent = onIntent,
                    // On a phone the trail over the table stands in for the gap above it.
                    modifier = Modifier.weight(1f).padding(
                        start = gap,
                        end = gap,
                        bottom = gap,
                        top = if (layout == WorkspaceLayout.Phone) PantrixTheme.spacing.none else gap
                    )
                )
            }
        }
    }
}

/** What the table's toolbar shows and does. */
private class TableToolbarState(
    val state: WorkspaceState,
    val filter: TextFieldState,
    val shown: Int,
    val hidden: Set<String>,
    val onSave: () -> Unit
)

/**
 * The views' choice and the table's trail and count over its filter, its columns' choice and its saving: a row in a
 * wide window and on a tablet, two on a phone.
 */
@Composable
private fun TableToolbar(toolbar: TableToolbarState, layout: WorkspaceLayout, onIntent: (WorkspaceIntent) -> Unit) {
    val spacing = PantrixTheme.spacing
    val table = toolbar.state.table
    val size = if (layout == WorkspaceLayout.Phone) ButtonSize.Large else layout.buttons
    val count = countOf(toolbar, wide = layout == WorkspaceLayout.Wide)
    val tools: @Composable () -> Unit = {
        if (table != null) {
            ColumnsChooser(table, toolbar.hidden, wide = layout == WorkspaceLayout.Wide, size = size, onHide = {
                onIntent(WorkspaceIntent.HideColumns(it))
            })
            PantrixIconButton(
                imageVector = PantrixIcons.Download,
                contentDescription = stringResource(Res.string.workspace_save_csv),
                onClick = toolbar.onSave,
                size = size,
                tagKey = "save_csv"
            )
        }
    }
    if (layout == WorkspaceLayout.Phone) {
        PhoneTableToolbar(toolbar, count, onIntent, tools)
        return
    }
    val wide = layout == WorkspaceLayout.Wide
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (wide) 48.dp else 64.dp)
            .background(PantrixTheme.colors.surface)
            .padding(horizontal = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        ViewChoice(selected = DocumentPane.Table, withText = !wide, onSelect = {
            onIntent(WorkspaceIntent.ShowPane(it))
        }, fullWidth = false)
        // The trail gives way first where the window is narrow, so that the tools at the end keep their room.
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            if (table != null) {
                PathTrail(
                    nodes = trail(toolbar.state.root, table.source.key),
                    onIntent = onIntent,
                    // A trail keeps a short page whole and draws past its room: it is cut there, not over the count.
                    modifier = Modifier.weight(1f, fill = false).clipToBounds()
                )
            }
            CountText(count)
        }
        if (table != null) FilterField(toolbar.filter, Modifier.width(if (wide) WideFilterWidth else TabletFilterWidth))
        tools()
    }
}

/** A phone's table toolbar: the filter, the columns' choice and the saving, then the table's trail and count. */
@Composable
private fun PhoneTableToolbar(
    toolbar: TableToolbarState,
    count: String?,
    onIntent: (WorkspaceIntent) -> Unit,
    tools: @Composable () -> Unit
) {
    val spacing = PantrixTheme.spacing
    val table = toolbar.state.table
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PantrixTheme.colors.surface)
            .padding(start = spacing.lg, end = spacing.lg, bottom = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        FilterField(toolbar.filter, Modifier.weight(1f).padding(end = spacing.xs))
        tools()
    }
    Row(
        modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        if (table != null) {
            PathTrail(trail(toolbar.state.root, table.source.key), onIntent, Modifier.weight(1f))
        } else {
            Spacer(Modifier.weight(1f))
        }
        CountText(count)
    }
}

@Composable
private fun countOf(toolbar: TableToolbarState, wide: Boolean): String? {
    val table = toolbar.state.table ?: return null
    val total = table.rows.size
    val rows = if (toolbar.shown == total) {
        pluralStringResource(Res.plurals.workspace_rows, total, total)
    } else {
        pluralStringResource(Res.plurals.workspace_rows_of, total, toolbar.shown, total)
    }
    if (!wide) return rows
    val columns = table.columns.size
    return stringResource(
        Res.string.workspace_separated,
        rows,
        pluralStringResource(Res.plurals.workspace_table_columns, columns, columns)
    )
}

@Composable
private fun CountText(count: String?) {
    count?.let {
        PantrixText(
            text = it,
            style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium),
            color = PantrixTheme.colors.textSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun FilterField(filter: TextFieldState, modifier: Modifier = Modifier) {
    PantrixTextField(
        state = filter,
        modifier = modifier,
        placeholder = stringResource(Res.string.workspace_filter_rows),
        leadingIcon = { PantrixIcon(imageVector = PantrixIcons.Search, contentDescription = null) },
        showClearButton = true,
        contentDescription = stringResource(Res.string.workspace_filter_rows),
        tagKey = "filter"
    )
}

/**
 * The table itself: an index column pinned at its start, a column to each shown key, the chosen row tinted and the
 * chosen cell outlined, and the chosen column's values summed up under it.
 */
@Composable
private fun RowsTable(
    table: TableModel,
    visible: List<String>,
    shown: List<TableRow>,
    state: WorkspaceState,
    query: String,
    phone: Boolean,
    onIntent: (WorkspaceIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val choice = rememberUpdatedState(state.tableChoice)
    val asWritten = state.document?.format.let { it != Format.Csv && it != Format.Tsv }
    val select: (String) -> Unit = { onIntent(WorkspaceIntent.Select(it)) }
    val valueName = stringResource(Res.string.workspace_value_column)
    val indexName = stringResource(Res.string.workspace_index)
    val columns = remember(table, visible, valueName, indexName) {
        listOf(indexColumn(indexName, choice, select)) + visible.map { name ->
            val numbers = statsOf(table.rows, name) is ColumnStats.Numbers
            DataTableColumn<TableRow>(
                key = name,
                header = if (name == VALUE_COLUMN) valueName else name,
                minWidth = ColumnMinWidth,
                alignment = if (numbers) Alignment.End else Alignment.Start,
                sortable = true,
                resizable = true
            ) { row -> TableCell(row, name, choice, select) }
        }
    }
    val sort = state.document?.view?.tableSort
    PantrixDataTable(
        rows = shown,
        columns = columns,
        modifier = modifier.fillMaxWidth(),
        rowKey = { it.node.key },
        rowLabel = { "${it.index}" },
        selected = setOfNotNull(choice.value?.let { table.rows.getOrNull(it.row)?.node?.key }),
        sort = listOfNotNull(sort?.let { DataTableSort(it.column, it.descending) }),
        onSortChange = { order ->
            onIntent(WorkspaceIntent.SortTable(order.firstOrNull()?.let { TableSort(it.columnKey, it.descending) }))
        },
        // A phone's record is a button that chooses its row; a table's cells choose their values themselves.
        onRowClick = if (phone) { row -> select(row.node.key) } else null,
        empty = { PantrixText(text = stringResource(Res.string.workspace_no_rows, query.trim())) },
        footer = if (phone) {
            null
        } else {
            { StatsBar(table, shown, visible, choice.value, sort) }
        },
        listContent = { row -> TableRecord(row, visible, chosen = choice.value?.row == row.index, asWritten) },
        variant = DataTableVariant.Grid,
        layout = if (phone) DataTableLayout.List else DataTableLayout.Table,
        tagKey = "table"
    )
}

private fun indexColumn(header: String, choice: State<TableChoice?>, select: (String) -> Unit) =
    DataTableColumn<TableRow>(
        key = INDEX_KEY,
        header = "#",
        width = IndexWidth,
        alignment = Alignment.End,
        pin = DataTablePin.Start,
        showInList = false
    ) { row ->
        val chosen = choice.value?.row == row.index
        PantrixText(
            text = "${row.index}",
            modifier = Modifier
                .clickable(role = Role.Button) { select(row.node.key) }
                .semantics { contentDescription = "$header ${row.index}" },
            style = PantrixTheme.typography.code.copy(fontSize = PantrixTheme.typography.footnote.fontSize),
            color = if (chosen) PantrixTheme.colors.primary.color else PantrixTheme.colors.textSecondary
        )
    }

/** The key of the index column, which no array's object has as a key. */
private const val INDEX_KEY = "#index"

/**
 * A cell: a value in the colour of its kind, a null in italics, a missing key as a dash, an object or an array as a
 * button that opens it in the table. A press chooses the cell's value in the tree, or the row where the key is missing.
 */
@Composable
private fun TableCell(row: TableRow, column: String, choice: State<TableChoice?>, select: (String) -> Unit) {
    val cell = row.cell(column)
    val chosen = choice.value.let { it != null && it.row == row.index && it.column == column }
    val syntax = LocalSyntaxColors.current
    val code = PantrixTheme.typography.code.copy(fontSize = PantrixTheme.typography.label.fontSize)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (chosen) {
                    Modifier.border(
                        ChosenCellLine,
                        PantrixTheme.colors.focusRing,
                        PantrixTheme.shapes.xs
                    )
                } else {
                    Modifier
                }
            )
            .clickable(role = Role.Button) { select(cell?.key ?: row.node.key) }
            .padding(horizontal = PantrixTheme.spacing.xs)
    ) {
        when {
            cell == null -> {
                val missing = stringResource(Res.string.workspace_missing_key, column)
                PantrixText(
                    text = "—",
                    modifier = Modifier.semantics { contentDescription = missing },
                    color = PantrixTheme.colors.textSecondary
                )
            }

            cell.kind.isContainer -> PantrixButton(
                text = cell.text,
                onClick = { select(cell.key) },
                variant = ButtonVariant.Outline,
                size = ButtonSize.Small,
                trailingIcon = { PantrixIcon(imageVector = PantrixIcons.ChevronForward, contentDescription = null) },
                contentDescription = stringResource(Res.string.workspace_open_value, cell.text),
                tagKey = "open_${row.index}_$column"
            )

            else -> PantrixText(
                text = cell.shownText,
                style = if (cell.kind == ValueKind.Null) code.copy(fontStyle = FontStyle.Italic) else code,
                color = when (cell.kind) {
                    ValueKind.Number -> syntax.number
                    ValueKind.Boolean, ValueKind.Null -> syntax.literal
                    ValueKind.Alias -> PantrixTheme.colors.textSecondary
                    else -> PantrixTheme.colors.textPrimary
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** A column's values summed up, and the order the rows are in: the bar under the table. */
@Composable
private fun StatsBar(
    table: TableModel,
    shown: List<TableRow>,
    visible: List<String>,
    choice: TableChoice?,
    sort: TableSort?
) {
    val spacing = PantrixTheme.spacing
    val column = choice?.column?.takeIf { it in visible }
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.lg)
    ) {
        if (column != null) ColumnSummary(column, statsOf(shown, column), table)
        Spacer(Modifier.weight(1f))
        sort?.let { SortSummary(table, it) }
    }
}
