package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.csv.CsvDocument
import com.developersancho.formatico.core.format.csv.CsvField
import com.developersancho.formatico.core.format.csv.CsvRecord
import com.developersancho.formatico.core.format.json.PathStep
import com.developersancho.formatico.core.format.text.TextRange
import com.developersancho.formatico.core.format.text.wholeRegex

/** A number as a CSV field writes it: what the table lines up at its end and orders as a number. */
private val CsvNumber = wholeRegex("""[-+]?(\d+(\.\d*)?|\.\d+)([eE][-+]?\d+)?""")

/**
 * A field that starts with a zero before another digit (`004`, `020`): a code, as a country's number or a postcode
 * is. A column that has one is a column of codes, and every field of it stays text, `248` beside `004`.
 */
private val LeadingZero = wholeRegex("""[-+]?0\d.*""")

/**
 * Reads [text] as CSV, or as TSV, for its tree: an array of its records after the header, each an object whose keys
 * are the header's names. A field past the header's last is named by its place, counted from 1. A field that is a
 * number is one, but in a column of codes, which a leading zero shows; any other is a string.
 */
internal fun readCsv(text: String, format: Format): Reading {
    val document = CsvDocument.parse(text, delimiter = if (format == Format.Tsv) '\t' else null)
    val header = document.header?.fields?.map { it.value }.orEmpty()
    val rows = document.rows
    val codes = rows.flatMap { record ->
        record.fields.mapIndexedNotNull { column, field -> column.takeIf { LeadingZero.containsMatchIn(field.value) } }
    }.toSet()
    val root = if (document.records.isEmpty()) {
        null
    } else {
        val columns = CsvColumns(header, codes)
        val children = rows.mapIndexed { index, record -> columns.record(record, index, text) }
        ValueNode(ROOT, NodePath.Root, ROOT, ValueKind.Array, "[${rows.size}]", null, null, children, TextRange(0, text.length))
    }
    return Reading.Read(
        format = format,
        root = root,
        problems = document.problems
    )
}

/**
 * The columns of a table, each named in [header] or by its place, and the places of those that are [codes]: what every
 * record's fields share, their names and their steps.
 */
private class CsvColumns(private val header: List<String>, private val codes: Set<Int>) {
    private val parts = SharedParts()

    /** The node of the record at [index]: an object of its fields, each named by its column's name. */
    fun record(record: CsvRecord, index: Int, text: String): ValueNode {
        val step = PathStep.Index(index)
        val key = childKey(ROOT, step)
        val path = NodePath.Root + step
        val seen = HashSet<String>()
        val fields = record.fields.mapIndexed { column, field ->
            val fieldStep = parts.key(header.getOrNull(column) ?: "${column + 1}")
            val name = fieldStep.name
            val place = FieldPlace(
                path = path + fieldStep,
                name = name,
                key = childKey(key, fieldStep, column.takeUnless { seen.add(name) }),
                inCodes = column in codes
            )
            field(field, text, place)
        }
        return ValueNode(
            key = key,
            path = path,
            name = parts.place(index),
            kind = ValueKind.Object,
            text = "{${fields.size}}",
            value = null,
            preview = null,
            children = fields,
            range = record.range
        )
    }

    private fun field(field: CsvField, text: String, place: FieldPlace): ValueNode {
        val number = !place.inCodes && CsvNumber.containsMatchIn(field.value)
        // A field in no quotes is written as it is read: its text and its value are one.
        val asRead = !field.quoted && field.range.length == field.value.length &&
            text.regionMatches(field.range.start, field.value, 0, field.value.length)
        val written = if (number || asRead) field.value else text.substring(field.range.start, field.range.end)
        return ValueNode(
            key = place.key,
            path = place.path,
            name = place.name,
            kind = if (number) ValueKind.Number else ValueKind.String,
            text = written,
            value = field.value.takeUnless { number },
            preview = if (number) null else previewOf(field.value),
            children = emptyList(),
            range = field.range
        )
    }
}

/** Where a field stands: its path, its column's name, its node's key, and whether its column is one of codes. */
private class FieldPlace(val path: NodePath, val name: String, val key: String, val inCodes: Boolean)
