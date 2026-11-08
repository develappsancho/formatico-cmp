package com.developersancho.formatico.feature.workspace.editor

import com.pantrixui.components.data.codeblock.CodeTokenKind
import com.pantrixui.components.data.codeeditor.CodeHighlight
import com.pantrixui.components.data.codeeditor.CodeHighlighter

/**
 * What a line of YAML hands the next: the indent of the key a block scalar (`|`, `>`) stands under, while its lines go
 * on, or none.
 */
internal data class YamlLine(val blockUnder: Int? = null)

/** A number as YAML writes it, in its other bases and its infinity among them. */
private val YamlNumber = Regex("""[-+]?(\d[\d_]*(\.\d*)?([eE][-+]?\d+)?|\.\d+|0x[0-9a-fA-F]+|0o[0-7]+|\.inf|\.nan)""")

/** A block scalar's head after its `|` or `>`: a chomping sign and an indent, then at most a comment. */
private val BlockHead = Regex("""[|>][+-]?\d?[+-]?\s*(#.*)?""")

private val YamlWords = setOf("true", "false", "null", "~", "True", "False", "Null", "TRUE", "FALSE", "NULL")

private const val DOCUMENT_MARKER = 3

/**
 * YAML, a line at a time: a key before its colon in the key's colour; strings, numbers, `true`, `false`, `null` and `~`;
 * anchors, aliases and tags as what says something of the value; the dashes, the brackets of a flow and the document's
 * markers; comments; and a block scalar's lines as the string they are.
 */
internal object YamlHighlighter : CodeHighlighter<YamlLine> {
    override val initialState = YamlLine()

    override fun highlight(line: String, state: YamlLine): CodeHighlight<YamlLine> {
        val scan = LineScanner(line)
        val indent = scan.nextWord(0)
        val under = state.blockUnder
        if (under != null && (line.isBlank() || indent > under)) {
            scan.take(line.length, CodeTokenKind.StringLiteral)
            return CodeHighlight(scan.tokens(), state)
        }
        scan.skipSpace()
        if (scan.startsWith("---") || scan.startsWith("...")) scan.take(scan.at + DOCUMENT_MARKER, CodeTokenKind.Punctuation)
        while (isDash(scan)) {
            scan.take(scan.at + 1, CodeTokenKind.Punctuation)
            scan.skipSpace()
        }
        key(scan)
        var block = false
        while (!scan.done) block = value(scan) || block
        return CodeHighlight(scan.tokens(), YamlLine(blockUnder = indent.takeIf { block }))
    }

    /** Whether the scanner stands at a list's dash: one followed by room or the line's end. */
    private fun isDash(scan: LineScanner): Boolean = scan.peek() == '-' && scan.peek(1).let { it == null || it == ' ' || it == '\t' }

    /** A key and its colon, where the line has one: `name:`, `"a key":`. */
    private fun key(scan: LineScanner) {
        val start = scan.at
        val char = scan.peek() ?: return
        val end = if (char == '"' || char == '\'') scan.quotedEnd(start) else plainKeyEnd(scan.line, start)
        val colon = scan.nextWord(end)
        if (end > start && scan.line.getOrNull(colon) == ':' && endsWord(scan.line, colon + 1)) {
            scan.take(end, CodeTokenKind.Property)
            scan.skipSpace()
            scan.take(scan.at + 1, CodeTokenKind.Punctuation)
        }
    }

    /** Whether the place [at] ends a word: the line's end, or room. */
    private fun endsWord(line: String, at: Int): Boolean = line.getOrNull(at).let { it == null || it == ' ' || it == '\t' }

    private fun plainKeyEnd(line: String, start: Int): Int {
        var end = start
        while (end < line.length && line[end] !in ":#[]{},") end++
        return line.substring(start, end).trimEnd().length + start
    }

    /** The next value of the line, or its comment: whether it opens a block scalar, whose lines follow. */
    private fun value(scan: LineScanner): Boolean {
        scan.skipSpace()
        val char = scan.peek() ?: return false
        when {
            char == '#' -> scan.take(scan.line.length, CodeTokenKind.Comment)

            char == '"' || char == '\'' -> scan.quoted()

            char in "&*!" -> scan.takeWhile(CodeTokenKind.Annotation) { it != ' ' && it !in ",[]{}" }

            char in "[]{},:?" -> scan.take(scan.at + 1, CodeTokenKind.Punctuation)

            BlockHead.matches(scan.line.substring(scan.at)) -> {
                scan.takeWhile(CodeTokenKind.Punctuation) { it != ' ' && it != '#' }
                return true
            }

            else -> plainScalar(scan)
        }
        return false
    }

    /** A value with no quotes: a number, a word of YAML's, or text, to a comment or the end of what holds it. */
    private fun plainScalar(scan: LineScanner) {
        var end = scan.at
        while (end < scan.line.length && scan.line[end] !in ",[]{}" && !startsComment(scan.line, end)) end++
        val text = scan.line.substring(scan.at, end).trimEnd()
        val kind = when {
            text in YamlWords -> CodeTokenKind.Keyword
            YamlNumber.matches(text) -> CodeTokenKind.Number
            else -> CodeTokenKind.StringLiteral
        }
        scan.take(scan.at + text.length.coerceAtLeast(1), kind)
    }

    /** Whether a comment starts at [at]: a `#` after room. */
    private fun startsComment(line: String, at: Int): Boolean = line[at] == '#' && at > 0 && line[at - 1] == ' '
}
