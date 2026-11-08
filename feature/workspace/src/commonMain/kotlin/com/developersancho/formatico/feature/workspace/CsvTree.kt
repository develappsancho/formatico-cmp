package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.Severity
import com.developersancho.formatico.core.format.csv.CsvDocument
import com.developersancho.formatico.core.format.csv.CsvField
import com.developersancho.formatico.core.format.json.PathStep
import com.developersancho.formatico.core.format.text.TextRange

/** A number as a CSV field writes it: what the table lines up at its end and orders as a number. */
private val CsvNumber = Regex("""[-+]?(\d+(\.\d*)?|\.\d+)([eE][-+]?\d+)?""")

/**
 * Reads [text] as CSV, or as TSV, for its tree: an array of its records after the header, each an object whose keys
 * are the header's names. A field past the header's last is named by its place, counted from 1. A field that is a
 * number is one; any other is a string.
 */
internal fun readCsv(text: String, format: Format): Reading {
    val document = CsvDocument.parse(text, delimiter = if (format == Format.Tsv) '\t' else null)
    val header = document.header?.fields?.map { it.value }.orEmpty()
    val rows = document.rows
    val root = if (document.records.isEmpty()) {
        null
    } else {
        val children = rows.mapIndexed { index, record ->
            val step = PathStep.Index(index)
            val key = childKey(ROOT, step)
            val seen = HashSet<String>()
            val fields = record.fields.mapIndexed { column, field ->
                val name = header.getOrNull(column) ?: "${column + 1}"
                val fieldStep = PathStep.Key(name)
                field(
                    field,
                    text,
                    listOf(step, fieldStep),
                    name,
                    childKey(
                        key,
                        fieldStep,
                        column.takeUnless {
                            seen.add(name)
                        }
                    )
                )
            }
            ValueNode(
                key,
                listOf(
                    step
                ),
                "$index", ValueKind.Object, "{${fields.size}}", null, null, fields, record.range
            )
        }
        ValueNode(
            ROOT, emptyList(), ROOT, ValueKind.Array, "[${rows.size}]", null, null, children,
            TextRange(
                0,
                text.length
            )
        )
    }
    return Reading.Read(
        format = format,
        root = root,
        errors = document.problems.count {
            it.severity == Severity.Error
        }
    )
}

private fun field(field: CsvField, text: String, path: List<PathStep>, name: String, key: String): ValueNode {
    val number = CsvNumber.matches(field.value)
    return ValueNode(
        key = key,
        path = path,
        name = name,
        kind = if (number) ValueKind.Number else ValueKind.String,
        text = if (number) field.value else text.substring(field.range.start, field.range.end),
        value = field.value.takeUnless { number },
        preview = if (number) null else previewOf(field.value),
        children = emptyList(),
        range = field.range
    )
}
