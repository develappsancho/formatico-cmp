package com.developersancho.formatico.feature.workspace.editor

import com.pantrixui.components.data.codeblock.CodeTokenKind
import com.pantrixui.components.data.codeeditor.CodeHighlight
import com.pantrixui.components.data.codeeditor.CodeHighlighter

/** What a line of JSON hands the next: whether it ends inside a block comment. */
internal enum class JsonLine { Code, InComment }

/**
 * JSON and its kin, JSONC, JSON5 and NDJSON, a line at a time: a key, the string before a colon, in the key's colour;
 * strings, numbers, `true`, `false` and `null`, the brackets and the signs, and line and block comments. JSON5's single
 * quotes, unquoted keys, `Infinity` and `NaN` are read as well: the reader does not judge, the document's check does.
 */
internal object JsonHighlighter : CodeHighlighter<JsonLine> {
    override val initialState = JsonLine.Code

    override fun highlight(line: String, state: JsonLine): CodeHighlight<JsonLine> {
        val scan = LineScanner(line)
        var inComment = state == JsonLine.InComment
        while (!scan.done) {
            if (inComment) {
                inComment = blockComment(scan)
            } else {
                scan.skipSpace()
                if (!scan.done) inComment = token(scan)
            }
        }
        return CodeHighlight(scan.tokens(), if (inComment) JsonLine.InComment else JsonLine.Code)
    }

    /** The run the scanner stands at: whether it opens a block comment that goes on past the line. */
    private fun token(scan: LineScanner): Boolean {
        val char = scan.peek() ?: return false
        when {
            scan.startsWith("//") -> scan.take(scan.line.length, CodeTokenKind.Comment)
            scan.startsWith("/*") -> return blockComment(scan)
            char == '"' || char == '\'' -> scan.quoted(if (isKey(scan, scan.quotedEnd(scan.at))) KEY else STRING)
            char.isDigit() || char in "-+." -> number(scan)
            char.isLetter() || char in "_$" -> word(scan)
            else -> scan.take(scan.at + 1, CodeTokenKind.Punctuation)
        }
        return false
    }

    /** A block comment from where the scanner is, to its `*` and `/` or the line's end: whether it goes on after the line. */
    private fun blockComment(scan: LineScanner): Boolean {
        val close = scan.line.indexOf(BLOCK_END, if (scan.startsWith("/*")) scan.at + BLOCK_END.length else scan.at)
        scan.take(if (close < 0) scan.line.length else close + BLOCK_END.length, CodeTokenKind.Comment)
        return close < 0
    }

    /** Whether what ends at [end] is a key: a colon follows it. */
    private fun isKey(scan: LineScanner, end: Int): Boolean = scan.line.getOrNull(scan.nextWord(end)) == ':'

    private fun number(scan: LineScanner) = scan.takeWhile(CodeTokenKind.Number) { it.isLetterOrDigit() || it in ".-+" }

    /** A word: `true`, `false` and `null`, a JSON5 key with no quotes, `Infinity` and `NaN`, or what JSON does not know. */
    private fun word(scan: LineScanner) {
        var end = scan.at
        while (end < scan.line.length && (scan.line[end].isLetterOrDigit() || scan.line[end] in "_$")) end++
        val kind = when (scan.line.substring(scan.at, end)) {
            "true", "false", "null" -> CodeTokenKind.Keyword
            "Infinity", "NaN" -> CodeTokenKind.Number
            else -> if (isKey(scan, end)) KEY else CodeTokenKind.Plain
        }
        scan.take(end, kind)
    }

    private const val BLOCK_END = "*/"
    private val KEY = CodeTokenKind.Property
    private val STRING = CodeTokenKind.StringLiteral
}
