package com.developersancho.formatico.core.format.csv

import com.developersancho.formatico.core.format.text.LineEnding
import com.developersancho.formatico.core.format.text.TextLayout

/**
 * The document written out again: each field in quotes only where it holds the delimiter, a quote or a line break,
 * the document's own delimiter and line ending, and a line break after the last record. Its values stay as they are,
 * spaces and all.
 *
 * @throws IllegalArgumentException when the document is not valid: a quote is never closed.
 */
fun CsvDocument.formatted(): String {
    require(isValid) { "Only a valid document is written out again" }
    val newline = (TextLayout.of(text).lineEnding ?: LineEnding.Lf).characters
    return csvText(records.map { record -> record.fields.map { it.value } }, delimiter, newline)
}

/**
 * [rows] as CSV: a row to a line, its values between [delimiter]s, each in quotes only where it holds the delimiter, a
 * quote or a line break, and a line break after the last row.
 */
fun csvText(rows: List<List<String>>, delimiter: Char = ',', newline: String = LineEnding.Lf.characters): String =
    buildString {
        rows.forEach { row ->
            row.forEachIndexed { index, value ->
                if (index > 0) append(delimiter)
                append(quoted(value, delimiter))
            }
            append(newline)
        }
    }

/** What a field is quoted for, besides its delimiter: a quote and a line break. */
private const val QUOTED_BY = "\"\n\r"

/** [value] as a field: as it is, or in quotes, its quotes doubled, where it holds the delimiter, a quote or a break. */
private fun quoted(value: String, delimiter: Char): String = if (value.any { it == delimiter || it in QUOTED_BY }) {
    "\"" + value.replace("\"", "\"\"") + "\""
} else {
    value
}
