package com.developersancho.formatico.core.format.ndjson

import com.developersancho.formatico.core.format.Finding
import com.developersancho.formatico.core.format.Problem
import com.developersancho.formatico.core.format.Severity
import com.developersancho.formatico.core.format.json.JsonDocument
import com.developersancho.formatico.core.format.json.JsonValue
import com.developersancho.formatico.core.format.linesOf
import com.developersancho.formatico.core.format.problemsOf
import com.developersancho.formatico.core.format.text.TextRange

/**
 * A line of a newline-delimited JSON text, and its value.
 *
 * @property range the line, its line break left out.
 * @property json the line read as JSON. Its ranges count from the line's start, [range]'s start, and not from the
 *   start of the text.
 */
class NdjsonRecord(val range: TextRange, val json: JsonDocument) {
    /** The line's value, or null where none can be read. */
    val value: JsonValue? get() = json.root
}

/**
 * A newline-delimited JSON text as it was read, as JSON Lines writes it: a JSON value on each line, a line with nothing
 * but room on it left out. Each value is RFC 8259's JSON, written on its line alone: a value over two lines is two
 * lines that are not valid.
 *
 * Nothing of the text is lost: [print] gives it back as it was written.
 *
 * @property text the text read.
 * @property records the lines with a value, in order.
 * @property problems what is wrong in it, in the order of the text, each with its line and column in the whole text.
 */
class NdjsonDocument private constructor(val text: String, val records: List<NdjsonRecord>, val problems: List<Problem>) {
    /** Whether the document has no error. */
    val isValid: Boolean get() = problems.none { it.severity == Severity.Error }

    /** The document as it was written. */
    fun print(): String = text

    companion object {
        /** Reads each line of [text] as a JSON value of its own. */
        fun parse(text: String): NdjsonDocument {
            val records = linesOf(text)
                .filter { line -> (line.start until line.end).any { !text[it].isWhitespace() } }
                .map { line -> NdjsonRecord(line, JsonDocument.parse(text.substring(line.start, line.end))) }
            val findings = records.flatMap { record ->
                record.json.problems.map {
                    Finding(it.code, it.severity, record.range.start + it.range.start, record.range.start + it.range.end)
                }
            }
            return NdjsonDocument(text, records, problemsOf(text, findings))
        }
    }
}
