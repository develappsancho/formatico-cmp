package com.developersancho.formatico.core.format.convert

/**
 * [value] as a table of fields parted by [delimiter], a comma or a tab: an array's items its rows, and any other value
 * a row alone; a row's columns its object's members, a value inside one a column named by its path, and an item that
 * is no object a column named `value`. The header names every column, in the order the rows first give them. A null is
 * an empty field, as a number CSV does not read as one is a field of its text. What it cannot write as it is goes to
 * [log].
 */
internal fun csvDocument(value: DataValue, delimiter: Char, log: ConvertLog): String {
    val records = if (value is DataArray) {
        if (value.items.isEmpty()) log.lose(Lost.Emptied, "")
        value.items.mapIndexed { index, item -> item to itemPath("", index) }
    } else {
        log.wrap("[0]")
        listOf(value to "")
    }
    val rows = records.map { (record, path) -> uniqueLeaves(rowOf(record, path, log), log).associateBy { it.name } }
    if (rows.isEmpty()) return ""
    val columns = LinkedHashSet<String>()
    rows.forEach { columns.addAll(it.keys) }
    return buildString {
        appendRow(columns.map { csvField(it, delimiter) }, delimiter)
        rows.forEach { row ->
            appendRow(columns.map { column -> row[column]?.let { csvField(fieldOf(it, log), delimiter) }.orEmpty() }, delimiter)
        }
    }
}

/** The fields of the row of [record], at [path]. */
private fun rowOf(record: DataValue, path: String, log: ConvertLog): List<FlatLeaf> = when (record) {
    is DataObject -> record.members.flatMap { flatten(it.value, it.name, memberPath(path, it.name), log) }
    is DataArray -> record.items.flatMapIndexed { index, item -> flatten(item, "[$index]", itemPath(path, index), log, nested = true) }
    else -> listOf(FlatLeaf(VALUE_COLUMN, path, record))
}

/** The text of [leaf]'s field: a number as CSV writes one that is read as a number. */
private fun fieldOf(leaf: FlatLeaf, log: ConvertLog): String {
    val number = leaf.value as? DataNumber ?: return textOf(leaf, log)
    return when (val spelled = plainSpelling(number.written)) {
        null -> number.written.also { log.lose(Lost.Unwritable, leaf.path) }
        number.written -> number.written.also { log.kept(it) }
        else -> spelled.also { log.lose(Lost.Respelled, leaf.path, it) }
    }
}

private fun StringBuilder.appendRow(fields: List<String>, delimiter: Char) {
    fields.joinTo(this, delimiter.toString())
    append('\n')
}

/** [text] as a field: in quotes, its quotes doubled, where it holds the delimiter, a quote, a line break or edge spaces. */
private fun csvField(text: String, delimiter: Char): String {
    val quoted = text.any { it == delimiter || it == '"' || it == '\n' || it == '\r' } ||
        text.firstOrNull()?.isWhitespace() == true ||
        text.lastOrNull()?.isWhitespace() == true
    return if (quoted) "\"${text.replace("\"", "\"\"")}\"" else text
}

/** The column a row of a value that is no object has its value in. */
private const val VALUE_COLUMN = "value"
