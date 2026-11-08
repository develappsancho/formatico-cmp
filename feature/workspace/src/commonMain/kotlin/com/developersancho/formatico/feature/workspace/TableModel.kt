package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.json.PathStep
import com.developersancho.formatico.core.format.text.toDecimalOrNull

/** The column of an array's values that are no objects: the values themselves. */
internal const val VALUE_COLUMN = "#value"

/**
 * A row of a table: an item of the array, at [index], and what it holds under each column, read from the item as a
 * cell is asked for: a large array's rows hold nothing of their own.
 *
 * @property node the item.
 */
internal class TableRow(val index: Int, val node: ValueNode) {
    /**
     * What the row holds under [column], or null where its object has no such key. A key given twice: its first value
     * is the one shown, as the tree shows the first first.
     */
    fun cell(column: String): ValueNode? = when {
        node.kind == ValueKind.Object -> node.children.firstOrNull { it.name == column }
        column == VALUE_COLUMN -> node
        else -> null
    }
}

/**
 * An array of a document as a table: a row to an item, a column to each key its objects have, in the order they are
 * first met, and one for the items that are no objects.
 *
 * @property source the array.
 */
internal class TableModel(val source: ValueNode, val columns: List<String>, val rows: List<TableRow>)

/**
 * Where a table stands in the tree's choice: the row the chosen value is in, or is, and its column where the chosen
 * value is a value of the row.
 */
internal data class TableChoice(val row: Int, val column: String?)

/**
 * The table of [root] for the value whose key is [selected]: the array it stands in, or is, the innermost of them, or
 * where it stands in none, the first array of the document. Null for a document with no array.
 */
internal fun tableOf(root: ValueNode?, selected: String?): TableModel? = tableSource(root, selected)?.let(::tableFrom)

/** The array [tableOf] makes a table of. */
internal fun tableSource(root: ValueNode?, selected: String?): ValueNode? {
    fun firstArray(node: ValueNode?): ValueNode? = when {
        node == null -> null
        node.kind == ValueKind.Array -> node
        else -> node.children.firstNotNullOfOrNull(::firstArray)
    }
    return trail(root, selected).lastOrNull { it.kind == ValueKind.Array } ?: firstArray(root)
}

/** The table of [source], an array. */
internal fun tableFrom(source: ValueNode): TableModel {
    val rows = source.children.mapIndexed { index, item -> TableRow(index, item) }
    val columns = LinkedHashSet<String>()
    source.children.forEach { item ->
        if (item.kind == ValueKind.Object) item.children.mapTo(columns) { it.name } else columns += VALUE_COLUMN
    }
    return TableModel(source, columns.toList(), rows)
}

/** Where [table] stands in the trail to the chosen value, or null where the choice is not in it. */
internal fun tableChoice(table: TableModel?, trail: List<ValueNode>): TableChoice? {
    if (table == null) return null
    val at = trail.indexOfFirst { it.key == table.source.key }
    if (at < 0 || at + 1 >= trail.size) return null
    val row = (trail[at + 1].path.last() as? PathStep.Index)?.index ?: return null
    val item = trail[at + 1]
    val column = when {
        at + 2 < trail.size -> trail[at + 2].name
        item.kind != ValueKind.Object -> VALUE_COLUMN
        else -> null
    }
    return TableChoice(row, column)
}

/** [rows] whose cells under [columns] hold [query], as written, without regard to case. */
internal fun filtered(rows: List<TableRow>, columns: List<String>, query: String): List<TableRow> {
    val wanted = query.trim()
    if (wanted.isEmpty()) return rows
    return rows.filter { row ->
        "${row.index}" == wanted ||
            columns.any { column -> row.cell(column)?.shownText?.contains(wanted, ignoreCase = true) == true }
    }
}

/** A cell's value as a table shows it: a string without its quotes, anything else as it is written. */
internal val ValueNode.shownText: String get() = value ?: text

/**
 * [rows] in the order of [sort]: by the column's values, numbers as numbers, the largest first where it is
 * descending; a null, and then a row without the key, after the others either way.
 */
internal fun sorted(rows: List<TableRow>, sort: TableSort?): List<TableRow> {
    if (sort == null) return rows
    val (present, absent) = rows.partition {
        it.cell(sort.column).let { cell ->
            cell != null &&
                cell.kind != ValueKind.Null
        }
    }
    val (nulls, missing) = absent.partition { it.cell(sort.column) != null }
    val order = compareBy<TableRow> { it.cell(sort.column)!!.kind.order }.thenComparator { first, second ->
        compareValues(first.cell(sort.column)!!, second.cell(sort.column)!!)
    }
    return present.sortedWith(if (sort.descending) order.reversed() else order) + nulls + missing
}

/** Numbers first, then booleans, then strings, then the rest: how values of mixed kinds are ordered. */
private val ValueKind.order: Int
    get() = when (this) {
        ValueKind.Number -> 0
        ValueKind.Boolean -> 1
        ValueKind.String -> 2
        else -> 3
    }

private fun compareValues(first: ValueNode, second: ValueNode): Int {
    if (first.kind == ValueKind.Number && second.kind == ValueKind.Number) {
        val a = first.text.toDecimalOrNull()
        val b = second.text.toDecimalOrNull()
        if (a != null && b != null) return a.compareTo(b)
    }
    return first.shownText.compareTo(second.shownText)
}

/** What a column's values come to, for the bar under the table. */
internal sealed interface ColumnStats {
    /** A column of numbers alone: how many, the least, the most and their average. */
    data class Numbers(val count: Int, val min: Double, val max: Double, val average: Double) : ColumnStats

    /** Any other column: how many values, how many different ones, and how many empty, null or missing. */
    data class Values(val count: Int, val different: Int, val empty: Int) : ColumnStats
}

/** What the values of [rows] under [column] come to. */
internal fun statsOf(rows: List<TableRow>, column: String): ColumnStats {
    val cells = rows.map { it.cell(column) }
    val present = cells.filterNotNull().filter { it.kind != ValueKind.Null && it.shownText.isNotEmpty() }
    // Only a column of numbers alone is read as numbers: reading a cell that is no number fails, which is slow on iOS,
    // where a column of 200 000 names took most of a minute to line up.
    val numbers = if (present.all { it.kind == ValueKind.Number }) present.mapNotNull { it.text.toDecimalOrNull() } else emptyList()
    return if (present.isNotEmpty() && numbers.size == present.size) {
        ColumnStats.Numbers(numbers.size, numbers.min(), numbers.max(), numbers.average())
    } else {
        ColumnStats.Values(rows.size, present.map { it.shownText }.distinct().size, rows.size - present.size)
    }
}

/**
 * [rows] as a CSV file's rows under [columns]: their names first, then each row's values, a string without its
 * quotes, a null or a missing key as nothing, an object or an array as it is written in [text].
 */
internal fun csvRows(rows: List<TableRow>, columns: List<String>, text: String, valueName: String): List<List<String>> =
    listOf(columns.map { if (it == VALUE_COLUMN) valueName else it }) + rows.map { row ->
        columns.map { column ->
            val cell = row.cell(column)
            when {
                cell == null || cell.kind == ValueKind.Null -> ""

                cell.kind.isContainer || cell.kind == ValueKind.Alias -> text.substring(
                    cell.range.start,
                    cell.range.end
                )

                else -> cell.shownText
            }
        }
    }

/**
 * [rows], the first of them the header, as a Markdown table: a backslash in a cell written `\\`, a `|` `\|` and a line
 * break `<br>`, so that each cell stays whole and each row one line.
 */
internal fun markdownTable(rows: List<List<String>>): String {
    if (rows.isEmpty()) return ""

    fun line(cells: List<String>) = cells.joinToString(separator = " | ", prefix = "| ", postfix = " |") { cell ->
        cell.replace("\\", "\\\\").replace("|", "\\|").replace("\r\n", "<br>").replace("\n", "<br>")
    }
    val header = rows.first()
    return buildString {
        appendLine(line(header))
        appendLine(header.joinToString(separator = " | ", prefix = "| ", postfix = " |") { "---" })
        rows.drop(1).forEach { appendLine(line(it)) }
    }
}
