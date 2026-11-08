package com.developersancho.formatico.core.format.toml

import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.text.TextRange

/**
 * Reads a TOML text a line at a time into its statements, key/values and headers, and goes on past a mistake: a
 * statement that cannot be read is reported and the rest of its line read past, so that the lines after it are read.
 */
internal class TomlStatementReader(private val scanner: TomlScanner) {
    private val values = TomlValueReader(scanner)
    private val statements = ArrayList<TomlStatement>()

    /** Reads the whole text: its statements, in order. */
    fun read(): List<TomlStatement> {
        // A byte order mark that opens a file is read past.
        if (scanner.text.startsWith('\ufeff')) scanner.position = 1
        while (!scanner.atEnd) {
            scanner.skipSpaces()
            when {
                scanner.atEnd -> Unit
                scanner.atLineBreak -> scanner.skipLineBreak()
                scanner.current == '#' -> scanner.comment()
                else -> statement()
            }
        }
        return statements
    }

    /** Reads a header or a key/value, and the rest of its line. */
    private fun statement() {
        val problems = scanner.found.size
        if (scanner.current == '[') {
            header()
        } else {
            values.keyValue(depth = 0)?.let { statements += TomlKeyValueStatement(it) }
        }
        endOfLine(problemsBefore = problems)
    }

    /** Reads `[name]` or `[[name]]`, with spaces inside the brackets or none. */
    private fun header() {
        val start = scanner.position
        val ofArray = scanner.peek(1) == '['
        val opening = if (ofArray) 2 else 1
        scanner.position += opening
        scanner.skipSpaces()
        val key = values.keys.read()
        if (key != null) {
            scanner.skipSpaces()
            val closing = if (ofArray) "]]" else "]"
            if (scanner.text.startsWith(closing, scanner.position)) {
                scanner.position += closing.length
            } else {
                scanner.report(ProblemCode.Unclosed, start, start + opening)
            }
        }
        statements += TomlHeaderStatement(TextRange(start, scanner.position), key, ofArray)
    }

    /**
     * Reads what follows a statement on its line: a comment, or nothing. Anything else is reported, unless the
     * statement had a mistake of its own, which ends its line, and is read past.
     */
    private fun endOfLine(problemsBefore: Int) {
        scanner.skipSpaces()
        if (scanner.current == '#') scanner.comment()
        if (scanner.atEnd || scanner.atLineBreak) return
        val end = scanner.lineEnd()
        if (scanner.found.size == problemsBefore) scanner.report(ProblemCode.TrailingContent, scanner.position, end)
        scanner.position = end
    }
}
