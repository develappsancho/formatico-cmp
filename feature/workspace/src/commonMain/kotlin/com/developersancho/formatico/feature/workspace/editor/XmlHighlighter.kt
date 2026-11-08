package com.developersancho.formatico.feature.workspace.editor

import com.pantrixui.components.data.codeblock.CodeTokenKind
import com.pantrixui.components.data.codeeditor.CodeHighlight
import com.pantrixui.components.data.codeeditor.CodeHighlighter

/**
 * What a line of XML hands the next: whether it ends inside a comment, a CDATA section, a tag between its attributes,
 * or an attribute's value in double or single quotes.
 */
internal enum class XmlLine { Text, InComment, InCdata, InTag, InDoubleQuoted, InSingleQuoted }

private const val COMMENT_START = "<!--"
private const val COMMENT_END = "-->"
private const val CDATA_START = "<![CDATA["
private const val CDATA_END = "]]>"

/**
 * XML, a line at a time: an element's name in the tag's colour and its brackets as signs, an attribute's name in the
 * key's colour and its value as a string, comments, CDATA sections and values, even over several lines, and the text
 * between tags as it is.
 */
internal object XmlHighlighter : CodeHighlighter<XmlLine> {
    override val initialState = XmlLine.Text

    override fun highlight(line: String, state: XmlLine): CodeHighlight<XmlLine> {
        val scan = LineScanner(line)
        var at = state
        while (!scan.done) {
            at = when (at) {
                XmlLine.InComment -> comment(scan)
                XmlLine.InCdata -> cdata(scan)
                XmlLine.InTag -> attributes(scan)
                XmlLine.InDoubleQuoted -> value(scan, '"')
                XmlLine.InSingleQuoted -> value(scan, '\'')
                XmlLine.Text -> text(scan)
            }
        }
        return CodeHighlight(scan.tokens(), at)
    }

    private fun comment(scan: LineScanner): XmlLine {
        val close = scan.line.indexOf(COMMENT_END, scan.at)
        scan.take(if (close < 0) scan.line.length else close + COMMENT_END.length, CodeTokenKind.Comment)
        return if (close < 0) XmlLine.InComment else XmlLine.Text
    }

    private fun text(scan: LineScanner): XmlLine {
        val open = scan.line.indexOf('<', scan.at)
        scan.take(if (open < 0) scan.line.length else open, CodeTokenKind.Plain)
        return when {
            open < 0 -> {
                XmlLine.Text
            }

            scan.startsWith(COMMENT_START) -> {
                comment(scan)
            }

            scan.startsWith(CDATA_START) -> {
                scan.take(scan.at + CDATA_START.length, CodeTokenKind.Punctuation)
                cdata(scan)
            }

            else -> {
                scan.takeWhile(CodeTokenKind.Punctuation) { it in "</?!" }
                scan.takeWhile(CodeTokenKind.Tag) { !it.isWhitespace() && it != '>' && it != '/' }
                attributes(scan)
            }
        }
    }

    /** A CDATA section's text as it is, to the `]]>` that ends it, a sign: the text after it, or the section still. */
    private fun cdata(scan: LineScanner): XmlLine {
        val close = scan.line.indexOf(CDATA_END, scan.at)
        if (close < 0) {
            scan.take(scan.line.length, CodeTokenKind.Plain)
            return XmlLine.InCdata
        }
        scan.take(close, CodeTokenKind.Plain)
        scan.take(close + CDATA_END.length, CodeTokenKind.Punctuation)
        return XmlLine.Text
    }

    /** A tag's attributes, to the bracket that ends the tag: the text after it, or the tag still where the line ends. */
    private fun attributes(scan: LineScanner): XmlLine {
        while (!scan.done) {
            scan.skipSpace()
            if (scan.done) break
            if (closesTag(scan)) {
                scan.takeWhile(CodeTokenKind.Punctuation) { it in "/?>" }
                return XmlLine.Text
            }
            val quote = scan.peek()
            if (quote != null && (quote == '"' || quote == '\'')) {
                val after = value(scan, quote, from = scan.at + 1)
                if (after != XmlLine.InTag) return after
            } else {
                attribute(scan)
            }
        }
        return XmlLine.InTag
    }

    /**
     * A value in [quote]s, or the rest of one, to the quote that closes it, looked for [from] on: the tag goes on, or the
     * value still where the line ends.
     */
    private fun value(scan: LineScanner, quote: Char, from: Int = scan.at): XmlLine {
        val close = scan.line.indexOf(quote, from)
        if (close < 0) {
            scan.take(scan.line.length, CodeTokenKind.StringLiteral)
            return if (quote == '"') XmlLine.InDoubleQuoted else XmlLine.InSingleQuoted
        }
        scan.take(close + 1, CodeTokenKind.StringLiteral)
        return XmlLine.InTag
    }

    private fun closesTag(scan: LineScanner) = scan.peek() == '>' || scan.startsWith("/>") || scan.startsWith("?>")

    /** An attribute's name or its `=`. */
    private fun attribute(scan: LineScanner) {
        val char = scan.peek() ?: return
        when (char) {
            '=' -> scan.take(scan.at + 1, CodeTokenKind.Punctuation)

            else -> {
                val from = scan.at
                scan.takeWhile(CodeTokenKind.Property) { !it.isWhitespace() && it !in "=>/" }
                // A sign no attribute has, as a `/` that closes nothing: it is passed, so that the line is read on.
                if (scan.at == from) scan.take(from + 1, CodeTokenKind.Punctuation)
            }
        }
    }
}
