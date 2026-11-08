package com.developersancho.formatico.core.format.ndjson

import com.developersancho.formatico.core.format.json.JsonValue
import com.developersancho.formatico.core.format.json.inlineJson
import com.developersancho.formatico.core.format.json.minifiedJson
import com.developersancho.formatico.core.format.text.LineEnding
import com.developersancho.formatico.core.format.text.TextLayout

/**
 * The document written out again, a record to a line and no line between two: each value on its line with a space
 * after each colon and comma, its keys in their order, or sorted by name where [sortKeys] says so. Pretty-printing a
 * record over several lines would make it no longer newline-delimited JSON.
 *
 * The lines end as the document's first line does, and the text ends with a line break where the document's did.
 *
 * @throws IllegalArgumentException when the document is not valid.
 */
fun NdjsonDocument.formatted(sortKeys: Boolean = false): String = written { inlineJson(it, sortKeys) }

/**
 * The document with no space in its records: each value on its line, as JSON's minifier writes it.
 *
 * @throws IllegalArgumentException when the document is not valid.
 */
fun NdjsonDocument.minified(): String = written(::minifiedJson)

/** Each record written by [write], a line each. */
private fun NdjsonDocument.written(write: (JsonValue) -> String): String {
    require(isValid) { "Only a valid document is written out again" }
    val newline = (TextLayout.of(text).lineEnding ?: LineEnding.Lf).characters
    val lines = records.map { write(requireNotNull(it.value) { "A valid record has a value" }) }
    val ending = if (text.endsWith('\n') || text.endsWith('\r')) newline else ""
    return lines.joinToString(newline) + ending
}
