package com.developersancho.formatico.feature.workspace.editor

import com.pantrixui.components.data.codeblock.CodeTokenKind
import com.pantrixui.components.data.codeeditor.CodeHighlight
import com.pantrixui.components.data.codeeditor.CodeHighlighter

/** What a line of XML hands the next: whether it ends inside a comment, or inside a tag between its attributes. */
internal enum class XmlLine { Text, InComment, InTag }

private const val COMMENT_START = "<!--"
private const val COMMENT_END = "-->"

/**
 * XML, a line at a time: an element's name in the tag's colour and its brackets as signs, an attribute's name in the
 * key's colour and its value as a string, comments, even over several lines, and the text between tags as it is.
 */
internal object XmlHighlighter : CodeHighlighter<XmlLine> {
    override val initialState = XmlLine.Text

    override fun highlight(line: String, state: XmlLine): CodeHighlight<XmlLine> {
        val scan = LineScanner(line)
        var at = state
        while (!scan.done) {
            at = when (at) {
                XmlLine.InComment -> comment(scan)
                XmlLine.InTag -> attributes(scan)
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
        if (open < 0) {
            scan.take(scan.line.length, CodeTokenKind.Plain)
            return XmlLine.Text
        }
        scan.take(open, CodeTokenKind.Plain)
        if (scan.startsWith(COMMENT_START)) return comment(scan)
        scan.takeWhile(CodeTokenKind.Punctuation) { it in "</?!" }
        scan.takeWhile(CodeTokenKind.Tag) { !it.isWhitespace() && it != '>' && it != '/' }
        return attributes(scan)
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
            attribute(scan)
        }
        return XmlLine.InTag
    }

    private fun closesTag(scan: LineScanner) = scan.peek() == '>' || scan.startsWith("/>") || scan.startsWith("?>")

    /** An attribute's name, its `=` or its value in quotes. */
    private fun attribute(scan: LineScanner) {
        val char = scan.peek() ?: return
        when (char) {
            '"', '\'' -> scan.quoted()

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
