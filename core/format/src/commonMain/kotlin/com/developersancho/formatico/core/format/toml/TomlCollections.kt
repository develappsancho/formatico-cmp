package com.developersancho.formatico.core.format.toml

import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.text.TextRange

/** A key's part on a line: bare, or in quotes of either kind. */
private const val KEY_PART = """(?:[A-Za-z0-9_-]+|"[^"\r\n]*"|'[^'\r\n]*')"""

/** A line that, met in an array not yet closed, is the key/value after it, `name = …`: the array was never closed. */
private val KeyValueLine = Regex("""^$KEY_PART(?:[ \t]*\.[ \t]*$KEY_PART)*[ \t]*=""")

/** A line that, met in an array not yet closed, is the header after it, `[name]` or `[[name.part]]`. */
private val HeaderLine = Regex("""^\[\[?[ \t]*([A-Za-z_][A-Za-z0-9_. \t-]*)\]\]?[ \t]*(?:#.*)?$""")

/** The words a line in brackets holds that make it an array of a value, `[true]`, rather than a header. */
private val ValueWords = setOf("true", "false", "inf", "nan")

/**
 * Reads arrays: values between brackets and commas, over as many lines as they like, with comments between them and a
 * comma after the last or none.
 */
internal class TomlArrayReader(private val scanner: TomlScanner, private val values: TomlValueReader) {
    /**
     * Reads the array whose `[` is where the scanner is, [depth] deep. One that is never closed is reported, and ends
     * with what was read of it: at the end of the text, or before the key/value or the header that follows it.
     */
    fun read(depth: Int): TomlArraySyntax {
        val open = scanner.position
        scanner.position++
        val items = ArrayList<TomlValueSyntax>()
        // Where what has been read of the array ends: its bracket, its last value, or the comma after it.
        var end = scanner.position
        var trailingComma = false
        var expectsValue = true
        while (true) {
            scanner.skipTrivia()
            val char = scanner.current
            when {
                char == ']' -> {
                    scanner.position++
                    return TomlArraySyntax(TextRange(open, scanner.position), items, trailingComma)
                }

                char == null || statementFollows(end) -> {
                    scanner.report(ProblemCode.Unclosed, open, open + 1)
                    scanner.position = end
                    return TomlArraySyntax(TextRange(open, end), items, trailingComma)
                }

                !expectsValue && char == ',' -> {
                    scanner.position++
                    end = scanner.position
                    trailingComma = true
                    expectsValue = true
                }

                // The value after it is read as if a comma stood here.
                !expectsValue -> {
                    scanner.report(ProblemCode.ExpectedCommaOrEnd, scanner.position, scanner.position + 1)
                    expectsValue = true
                }

                char == ',' -> {
                    scanner.report(ProblemCode.ExpectedValue, scanner.position, scanner.position + 1)
                    scanner.position++
                    end = scanner.position
                }

                else -> {
                    item(depth)?.let { items += it }
                    end = scanner.position
                    trailingComma = false
                    expectsValue = false
                }
            }
        }
    }

    /** Reads a value of the array; what cannot be read is read past, to the comma, the bracket or the line's end. */
    private fun item(depth: Int): TomlValueSyntax? {
        val item = values.value(depth)
        if (item == null) {
            while (!atItemEnd()) scanner.position++
        }
        return item
    }

    /** Whether what is read is the end of a value of the array: a comma, the bracket, the line's end or the text's. */
    private fun atItemEnd(): Boolean {
        val char = scanner.current
        return char == null || char == ',' || char == ']' || scanner.atLineBreak
    }

    /** Whether the line being read, after a line break since [end], is a key/value or a header rather than a value. */
    private fun statementFollows(end: Int): Boolean {
        if (!scanner.hasLineBreak(end, scanner.position)) return false
        val line = scanner.text.substring(scanner.position, scanner.lineEnd())
        if (KeyValueLine.containsMatchIn(line)) return true
        val header = HeaderLine.find(line) ?: return false
        return header.groupValues[1].trim() !in ValueWords
    }
}

/**
 * What has been read of an inline table.
 *
 * @property end where what has been read of it ends.
 * @property expectsPair whether a key/value comes next, rather than a comma or the closing brace.
 * @property comma where the last comma stands, or -1 for none.
 * @property brokeLine whether a line break in it has been reported.
 * @property done whether it has been read to its end, or to a mistake that ends it.
 * @property failed whether a mistake ended it.
 */
private class InlineTable(var end: Int) {
    val pairs = ArrayList<TomlKeyValue>()
    var expectsPair = true
    var comma = -1
    var brokeLine = false
    var done = false
    var failed = false
}

/**
 * Reads inline tables: key/values between braces and commas, on one line, with no comma after the last.
 */
internal class TomlInlineTableReader(private val scanner: TomlScanner, private val values: TomlValueReader) {
    /**
     * Reads the inline table whose `{` is where the scanner is, [depth] deep; null where a key/value in it cannot be
     * read. One that is never closed is reported, and ends with what was read of it at the end of its line.
     */
    fun read(depth: Int): TomlInlineTableSyntax? {
        val open = scanner.position
        scanner.position++
        val table = InlineTable(end = scanner.position)
        while (!table.done) {
            if (gapEndsLine(table)) {
                scanner.report(ProblemCode.Unclosed, open, open + 1)
                return TomlInlineTableSyntax(TextRange(open, table.end), table.pairs)
            }
            next(table, depth)
        }
        return if (table.failed) null else TomlInlineTableSyntax(TextRange(open, table.end), table.pairs)
    }

    /** Reads what comes next in [table]: a key/value, a comma, or its closing brace. */
    private fun next(table: InlineTable, depth: Int) {
        val char = scanner.current
        when {
            char == '}' -> {
                if (table.expectsPair && table.pairs.isNotEmpty()) {
                    scanner.report(ProblemCode.TrailingComma, table.comma, table.comma + 1)
                }
                scanner.position++
                table.end = scanner.position
                table.done = true
            }

            table.expectsPair -> {
                val pair = values.keyValue(depth)
                if (pair == null) {
                    table.failed = true
                    table.done = true
                } else {
                    table.pairs += pair
                    table.end = scanner.position
                    table.expectsPair = false
                }
            }

            char == ',' -> {
                table.comma = scanner.position
                scanner.position++
                table.end = scanner.position
                table.expectsPair = true
            }

            else -> {
                scanner.report(ProblemCode.ExpectedCommaOrEnd, scanner.position, scanner.position + 1)
                table.failed = true
                table.done = true
            }
        }
    }

    /**
     * Reads past the spaces before what comes next in [table]. A line break there, which TOML 1.0 does not allow in an
     * inline table, is reported once and read past where what follows it goes on with the table; where it does not,
     * the table was never closed and its line ends it: this says so, and leaves the scanner at the line's end.
     */
    private fun gapEndsLine(table: InlineTable): Boolean {
        scanner.skipSpaces()
        if (scanner.current != '#' && !scanner.atLineBreak && !scanner.atEnd) return false
        val before = scanner.position
        val lineEnd = scanner.lineEnd()
        scanner.skipTrivia()
        val char = scanner.current
        val goesOn = char == '}' || if (table.expectsPair) char?.let(::startsKey) == true else char == ','
        if (!goesOn) {
            scanner.position = before
            return true
        }
        if (!table.brokeLine) {
            scanner.report(ProblemCode.TomlInlineTableLineBreak, lineEnd, lineEnd + scanner.lineBreakAt(lineEnd))
            table.brokeLine = true
        }
        return false
    }
}

/** Whether a key starts with [char]: a bare one, or one in quotes. */
private fun startsKey(char: Char): Boolean = isBareKeyCharacter(char) || char == '"' || char == '\''
