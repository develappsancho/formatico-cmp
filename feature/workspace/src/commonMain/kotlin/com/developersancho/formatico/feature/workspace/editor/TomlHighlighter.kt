package com.developersancho.formatico.feature.workspace.editor

import com.developersancho.formatico.core.format.text.wholeRegex
import com.pantrixui.components.data.codeblock.CodeTokenKind
import com.pantrixui.components.data.codeeditor.CodeHighlight
import com.pantrixui.components.data.codeeditor.CodeHighlighter

/**
 * What a line of TOML hands the next: the quotes of a string of several lines still open, and how many arrays are open
 * where it ends.
 */
internal data class TomlLine(val openString: String? = null, val arrays: Int = 0)

/** A number as TOML writes it: with `_`, in hexadecimal, octal or binary, `inf` and `nan`. */
private val TomlNumber = wholeRegex("""[-+]?(\d[\d_]*(\.[\d_]+)?([eE][-+]?\d+)?|0x[0-9a-fA-F_]+|0o[0-7_]+|0b[01_]+|inf|nan)""")

/** A date, a time, or both, as TOML writes them. */
private val TomlDate =
    wholeRegex(
        """\d{4}-\d{2}-\d{2}([Tt]\d{2}:\d{2}:\d{2}(\.\d+)?)?([Zz]|[-+]\d{2}:\d{2})?|\d{2}:\d{2}:\d{2}(\.\d+)?([Zz]|[-+]\d{2}:\d{2})?"""
    )

private const val BASIC_QUOTES = "\"\"\""
private const val LITERAL_QUOTES = "'''"

/**
 * TOML, a line at a time: a header in brackets in the tag's colour; a key before its `=` in the key's colour, in an
 * inline table as well; and its value, a string, a number, a date, `true` or `false`, the signs of its arrays and
 * tables. A string of several lines is a string on each of its lines, and an array over several lines goes on as
 * values. Comments run to the line's end.
 */
internal object TomlHighlighter : CodeHighlighter<TomlLine> {
    override val initialState = TomlLine()

    override fun highlight(line: String, state: TomlLine): CodeHighlight<TomlLine> {
        val scan = LineScanner(line)
        val open = state.openString
        val next = when {
            open != null -> if (closeString(scan, open)) values(scan, state.copy(openString = null)) else state
            state.arrays > 0 -> values(scan, state)
            else -> statement(scan, state)
        }
        return CodeHighlight(scan.tokens(), next)
    }

    /** A line that starts a statement: a comment, a header, or a key and its value. */
    private fun statement(scan: LineScanner, state: TomlLine): TomlLine {
        scan.skipSpace()
        return when (scan.peek()) {
            null -> state

            '#' -> {
                scan.take(scan.line.length, CodeTokenKind.Comment)
                state
            }

            '[' -> {
                header(scan)
                state
            }

            else -> {
                key(scan)
                values(scan, state)
            }
        }
    }

    /** `[table]` or `[[array.of.tables]]`, and a comment after it. */
    private fun header(scan: LineScanner) {
        val double = scan.startsWith("[[")
        val close = scan.line.indexOf(if (double) "]]" else "]", scan.at)
        scan.take(if (close < 0) scan.line.length else close + if (double) 2 else 1, CodeTokenKind.Tag)
        scan.skipSpace()
        if (!scan.done) scan.take(scan.line.length, CodeTokenKind.Comment)
    }

    /** A key, its quoted parts and its dots among it, up to its `=`, and the `=`. */
    private fun key(scan: LineScanner) {
        var end = scan.at
        while (end < scan.line.length && scan.line[end] != '=') {
            end = if (scan.line[end] == '"' || scan.line[end] == '\'') scan.quotedEnd(end) else end + 1
        }
        scan.take(end, CodeTokenKind.Property)
        if (!scan.done) scan.take(scan.at + 1, CodeTokenKind.Punctuation)
    }

    /** Values and their signs to the line's end, or to a string left open there; what the next line is handed. */
    private fun values(scan: LineScanner, state: TomlLine): TomlLine {
        var arrays = state.arrays
        while (true) {
            scan.skipSpace()
            val char = scan.peek() ?: break
            when (char) {
                '#' -> scan.take(scan.line.length, CodeTokenKind.Comment)

                '[', ']', '{', '}', ',' -> {
                    if (char == '[') arrays++
                    if (char == ']') arrays = (arrays - 1).coerceAtLeast(0)
                    scan.take(scan.at + 1, CodeTokenKind.Punctuation)
                }

                '"', '\'' -> {
                    val open = string(scan, char)
                    if (open != null) return TomlLine(openString = open, arrays = arrays)
                }

                else -> word(scan)
            }
        }
        return TomlLine(arrays = arrays)
    }

    /** A string that opens with [quote]: the quotes of one of several lines left open at the line's end, or null. */
    private fun string(scan: LineScanner, quote: Char): String? {
        val quotes = if (quote == '"') BASIC_QUOTES else LITERAL_QUOTES
        if (!scan.startsWith(quotes)) {
            scan.quoted()
            return null
        }
        return quotes.takeUnless { closeString(scan, quotes, from = scan.at + quotes.length) }
    }

    /** A bare word: a key of an inline table where a `=` follows it, else `true`, `false`, a number or a date. */
    private fun word(scan: LineScanner) {
        var end = scan.at
        while (end < scan.line.length && !scan.line[end].isWhitespace() && scan.line[end] !in ",]}=#") end++
        if (end == scan.at) end++
        val word = scan.line.substring(scan.at, end)
        if (scan.line.getOrNull(scan.nextWord(end)) == '=') {
            scan.take(end, CodeTokenKind.Property)
            scan.skipSpace()
            scan.take(scan.at + 1, CodeTokenKind.Punctuation)
            return
        }
        val kind = when {
            word == "true" || word == "false" -> CodeTokenKind.Keyword
            TomlNumber.containsMatchIn(word) || TomlDate.containsMatchIn(word) -> CodeTokenKind.Number
            else -> CodeTokenKind.Plain
        }
        scan.take(end, kind)
    }

    /**
     * A string of several lines, or the rest of one, to its [quotes], looked for [from] on, and the quotes it ends with:
     * whether it closed.
     */
    private fun closeString(scan: LineScanner, quotes: String, from: Int = scan.at): Boolean {
        val close = scan.line.indexOf(quotes, from)
        if (close < 0) {
            scan.take(scan.line.length, CodeTokenKind.StringLiteral)
            return false
        }
        // A string may end with one or two of its quotes before the three that close it: `"""a""""`.
        var end = close + quotes.length
        while (end < scan.line.length && scan.line[end] == quotes[0] && end - close < quotes.length + 2) end++
        scan.take(end, CodeTokenKind.StringLiteral)
        return true
    }
}
