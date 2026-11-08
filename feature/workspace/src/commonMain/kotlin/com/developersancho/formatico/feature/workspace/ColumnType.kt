package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.table_type_alias
import formatico.feature.workspace.generated.resources.table_type_array
import formatico.feature.workspace.generated.resources.table_type_boolean
import formatico.feature.workspace.generated.resources.table_type_date
import formatico.feature.workspace.generated.resources.table_type_null
import formatico.feature.workspace.generated.resources.table_type_number
import formatico.feature.workspace.generated.resources.table_type_object
import formatico.feature.workspace.generated.resources.table_type_text
import org.jetbrains.compose.resources.stringResource

/** What a table's column holds, as its header says under its name. */
internal enum class ColumnType { Text, Date, Number, Boolean, Object, Array, Alias, Null }

/**
 * The types of the values of [rows] under [column], in the order they are first met and a null last; none where every
 * row is without the key. A column's strings are dates where every one of them reads as a date, and text otherwise.
 */
internal fun typesOf(rows: List<TableRow>, column: String): List<ColumnType> {
    val cells = rows.mapNotNull { it.cell(column) }
    val strings = cells.filter { it.kind == ValueKind.String }
    val dates = strings.isNotEmpty() && strings.all { it.preview is ValuePreview.Moment || it.preview is ValuePreview.Day }
    val types = cells.map { cell ->
        when (cell.kind) {
            ValueKind.String -> if (dates) ColumnType.Date else ColumnType.Text
            ValueKind.Number -> ColumnType.Number
            ValueKind.Boolean -> ColumnType.Boolean
            ValueKind.Object -> ColumnType.Object
            ValueKind.Array -> ColumnType.Array
            ValueKind.Alias -> ColumnType.Alias
            ValueKind.Null -> ColumnType.Null
        }
    }.distinct()
    return types.filter { it != ColumnType.Null } + types.filter { it == ColumnType.Null }
}

/** The word each type of a column is written with under its name: `text`, `date`, `number`. */
@Composable
internal fun columnTypeWords(): Map<ColumnType, String> = mapOf(
    ColumnType.Text to stringResource(Res.string.table_type_text),
    ColumnType.Date to stringResource(Res.string.table_type_date),
    ColumnType.Number to stringResource(Res.string.table_type_number),
    ColumnType.Boolean to stringResource(Res.string.table_type_boolean),
    ColumnType.Object to stringResource(Res.string.table_type_object),
    ColumnType.Array to stringResource(Res.string.table_type_array),
    ColumnType.Alias to stringResource(Res.string.table_type_alias),
    ColumnType.Null to stringResource(Res.string.table_type_null)
)

/** What a column's header says under its name of the [types] it holds, as [words] write them: `text, null`. */
internal fun typesLine(types: List<ColumnType>, words: Map<ColumnType, String>): String? =
    types.takeIf { it.isNotEmpty() }?.joinToString(", ") { words.getValue(it) }
