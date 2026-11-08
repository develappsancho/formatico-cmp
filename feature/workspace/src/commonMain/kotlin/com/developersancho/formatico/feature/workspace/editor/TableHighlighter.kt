package com.developersancho.formatico.feature.workspace.editor

import com.pantrixui.components.data.codeblock.CodeTokenKind
import com.pantrixui.components.data.codeeditor.CodeHighlight
import com.pantrixui.components.data.codeeditor.CodeHighlighter

/** What a line of CSV or TSV hands the next: whether it is the header, and whether a quoted field goes on past it. */
internal data class TableLine(val header: Boolean = true, val inQuotes: Boolean = false)

/** A number as a field writes it. */
private val TableNumber = Regex("""[-+]?(\d+(\.\d*)?|\.\d+)([eE][-+]?\d+)?""")

/**
 * CSV or TSV, a line at a time, its fields split by [delimiter]: the header's names in the key's colour, numbers as
 * numbers, quoted fields as strings, even over several lines, and the delimiters as the signs between them.
 */
internal class TableHighlighter(private val delimiter: Char) : CodeHighlighter<TableLine> {
    override val initialState = TableLine()

    override fun highlight(line: String, state: TableLine): CodeHighlight<TableLine> {
        val scan = LineScanner(line)
        var inQuotes = state.inQuotes
        while (!scan.done) {
            when {
                inQuotes -> inQuotes = closeQuote(scan, from = scan.at)
                scan.peek() == delimiter -> scan.take(scan.at + 1, CodeTokenKind.Punctuation)
                scan.peek() == '"' -> inQuotes = closeQuote(scan, from = scan.at + 1)
                else -> field(scan, state.header)
            }
        }
        return CodeHighlight(scan.tokens(), TableLine(header = state.header && inQuotes, inQuotes = inQuotes))
    }

    /**
     * A quoted field from where the scanner is to the quote that closes it, looked for [from] on, where `""` is a quote
     * inside it: whether it goes on past the line.
     */
    private fun closeQuote(scan: LineScanner, from: Int): Boolean {
        var end = from
        while (end < scan.line.length) {
            if (scan.line[end] != '"') {
                end++
            } else if (scan.line.getOrNull(end + 1) == '"') {
                end += 2
            } else {
                break
            }
        }
        scan.take(minOf(end + 1, scan.line.length), CodeTokenKind.StringLiteral)
        return end >= scan.line.length
    }

    private fun field(scan: LineScanner, header: Boolean) {
        val end = scan.line.indexOf(delimiter, scan.at).let { if (it < 0) scan.line.length else it }
        val text = scan.line.substring(scan.at, end).trim()
        val kind = when {
            header -> CodeTokenKind.Property
            TableNumber.matches(text) -> CodeTokenKind.Number
            else -> CodeTokenKind.Plain
        }
        scan.take(end, kind)
    }
}
