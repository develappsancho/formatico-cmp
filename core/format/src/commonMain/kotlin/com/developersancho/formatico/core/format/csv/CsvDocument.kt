package com.developersancho.formatico.core.format.csv

import com.developersancho.formatico.core.format.Problem
import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.Severity
import com.developersancho.formatico.core.format.text.LineMap
import com.developersancho.formatico.core.format.text.TextRange

/**
 * A field of a CSV record.
 *
 * @property range the field as it is written, its quotes included.
 * @property value its value: the quotes taken off and a doubled quote read as one.
 * @property quoted whether it is written in quotes.
 */
class CsvField(val range: TextRange, val value: String, val quoted: Boolean)

/**
 * A record of a CSV text: a line, or more where a quoted field holds a line break.
 *
 * @property range the record as it is written, its line break left out.
 */
class CsvRecord(val range: TextRange, val fields: List<CsvField>)

/**
 * A CSV or TSV text as it was read, as RFC 4180 writes it: its records, the first of them the header, and its problems,
 * each with its line and column. Nothing of a field is lost: [CsvField.range] gives it back as it was written.
 *
 * A line with nothing on it is no record. A quote that is never closed is an error; a quote inside a field that is not
 * quoted, text after a closing quote, and a record with another number of fields than the header are only warnings,
 * as most readers take them.
 *
 * @property text the text read.
 * @property delimiter what stands between two fields: `,`, `;`, a tab or `|`.
 * @property records the records in order, the header first.
 * @property problems what is wrong in it, in the order of the text.
 */
class CsvDocument private constructor(
    val text: String,
    val delimiter: Char,
    val records: List<CsvRecord>,
    val problems: List<Problem>
) {
    /** Whether the document has no error; a warning leaves it valid. */
    val isValid: Boolean get() = problems.none { it.severity == Severity.Error }

    /** The first record, which names the columns, or null for a text with none. */
    val header: CsvRecord? get() = records.firstOrNull()

    /** The records after the header. */
    val rows: List<CsvRecord> get() = records.drop(1)

    companion object {
        /** The delimiters a CSV text is told by, the most usual first. */
        private val Delimiters = listOf(',', ';', '\t', '|')

        /** How many lines are looked at to tell the delimiter. */
        private const val LINES_LOOKED_AT = 50

        /** Reads [text] as records of fields between [delimiter]s, or between the delimiter its lines use. */
        fun parse(text: String, delimiter: Char? = null): CsvDocument {
            val used = delimiter ?: delimiterOf(text)
            val reader = CsvReader(text, used)
            reader.read()
            val lines = LineMap(text)
            val problems = reader.found.sortedBy { it.start }.map {
                Problem(
                    code = it.code,
                    severity = it.severity,
                    range = TextRange(it.start, it.end),
                    start = lines.position(it.start),
                    end = lines.position(it.end)
                )
            }
            return CsvDocument(text, used, reader.records, problems)
        }

        /**
         * The delimiter [text]'s first lines use the same number of times on each line, and the most of: a comma where
         * none does.
         */
        fun delimiterOf(text: String): Char {
            val lines = text.lineSequence().filter { it.isNotBlank() }.take(LINES_LOOKED_AT).toList()
            if (lines.isEmpty()) return Delimiters.first()
            return Delimiters
                .map { delimiter -> delimiter to lines.map { line -> countOutsideQuotes(line, delimiter) } }
                .filter { (_, counts) -> counts.first() > 0 }
                .maxWithOrNull(
                    compareBy({ (_, counts) ->
                        counts.distinct().size == 1
                    }, { (_, counts) -> counts.first() })
                )
                ?.first
                ?: Delimiters.first()
        }

        private fun countOutsideQuotes(line: String, delimiter: Char): Int {
            var quoted = false
            var count = 0
            line.forEach { char ->
                when {
                    char == '"' -> quoted = !quoted
                    char == delimiter && !quoted -> count++
                }
            }
            return count
        }
    }
}

/** A problem found while reading, by its offsets. */
private class FoundProblem(val code: ProblemCode, val severity: Severity, val start: Int, val end: Int)

/** Reads the records of [text], a field at a time. */
private class CsvReader(private val text: String, private val delimiter: Char) {
    val records = ArrayList<CsvRecord>()
    val found = ArrayList<FoundProblem>()

    private var index = if (text.startsWith('\uFEFF')) 1 else 0

    fun read() {
        while (index < text.length) {
            if (isLineBreak(text[index])) {
                // A line with nothing on it is no record.
                skipLineBreak()
                continue
            }
            val record = record()
            records += record
            val expected = records.first().fields.size
            if (record.fields.size != expected) {
                found += FoundProblem(ProblemCode.FieldCount, Severity.Warning, record.range.start, record.range.end)
            }
        }
    }

    /** Reads a record, to its line break, which it reads past. */
    private fun record(): CsvRecord {
        val start = index
        val fields = ArrayList<CsvField>()
        while (true) {
            fields += field()
            if (index < text.length && text[index] == delimiter) {
                index++
                continue
            }
            val end = index
            skipLineBreak()
            return CsvRecord(TextRange(start, end), fields)
        }
    }

    private fun field(): CsvField = if (index < text.length && text[index] == '"') quotedField() else plainField()

    private fun plainField(): CsvField {
        val start = index
        while (index < text.length && text[index] != delimiter && !isLineBreak(text[index])) {
            if (text[index] == '"') found += FoundProblem(ProblemCode.StrayQuote, Severity.Warning, index, index + 1)
            index++
        }
        return CsvField(TextRange(start, index), text.substring(start, index), quoted = false)
    }

    private fun quotedField(): CsvField {
        val start = index
        val value = StringBuilder()
        index++
        while (true) {
            if (index >= text.length) {
                found += FoundProblem(ProblemCode.UnclosedQuote, Severity.Error, start, start + 1)
                return CsvField(TextRange(start, index), value.toString(), quoted = true)
            }
            val char = text[index]
            if (char != '"') {
                value.append(char)
                index++
            } else if (index + 1 < text.length && text[index + 1] == '"') {
                value.append('"')
                index += 2
            } else {
                index++
                break
            }
        }
        // `"a"b`: what follows the closing quote is kept, as most readers keep it.
        val after = index
        while (index < text.length && text[index] != delimiter && !isLineBreak(text[index])) {
            value.append(text[index])
            index++
        }
        if (index > after) found += FoundProblem(ProblemCode.TextAfterQuote, Severity.Warning, after, index)
        return CsvField(TextRange(start, index), value.toString(), quoted = true)
    }

    private fun skipLineBreak() {
        if (index < text.length && text[index] == '\r') index++
        if (index < text.length && text[index] == '\n') index++
    }

    private fun isLineBreak(char: Char): Boolean = char == '\n' || char == '\r'
}
