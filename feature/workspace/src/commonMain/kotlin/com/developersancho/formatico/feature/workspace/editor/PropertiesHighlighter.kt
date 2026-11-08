package com.developersancho.formatico.feature.workspace.editor

import com.pantrixui.components.data.codeblock.CodeTokenKind
import com.pantrixui.components.data.codeeditor.CodeHighlight
import com.pantrixui.components.data.codeeditor.CodeHighlighter

/** What a properties file takes for room: a space, a tab, a form feed. */
private val Room = charArrayOf(' ', '\t', '\u000C')

/**
 * Java's properties, a line at a time: a key in the key's colour, to the first `=`, `:` or room no backslash escapes;
 * the sign after it; its value a string, on the lines a backslash at a line's end carries it on to as well; and the
 * comments, which start with `#` or `!`. What a line hands the next is whether it carries its value on.
 */
internal object PropertiesHighlighter : CodeHighlighter<Boolean> {
    override val initialState = false

    override fun highlight(line: String, state: Boolean): CodeHighlight<Boolean> {
        val scan = LineScanner(line)
        scan.takeWhile(CodeTokenKind.Plain) { it in Room }
        val char = scan.peek()
        val carries = when {
            char == null -> {
                false
            }

            state -> {
                scan.take(line.length, CodeTokenKind.StringLiteral)
                goesOn(line)
            }

            char == '#' || char == '!' -> {
                scan.take(line.length, CodeTokenKind.Comment)
                false
            }

            else -> {
                entry(scan)
                goesOn(line)
            }
        }
        return CodeHighlight(scan.tokens(), carries)
    }

    /** A key, the sign after it, and its value. */
    private fun entry(scan: LineScanner) {
        var end = scan.at
        while (end < scan.line.length && !endsKey(scan.line[end])) end += if (scan.line[end] == '\\') 2 else 1
        scan.take(end, CodeTokenKind.Property)
        scan.takeWhile(CodeTokenKind.Plain) { it in Room }
        if (scan.peek() == '=' || scan.peek() == ':') scan.take(scan.at + 1, CodeTokenKind.Punctuation)
        scan.takeWhile(CodeTokenKind.Plain) { it in Room }
        scan.take(scan.line.length, CodeTokenKind.StringLiteral)
    }

    /** Whether [char], where no backslash escapes it, ends a key. */
    private fun endsKey(char: Char): Boolean = char == '=' || char == ':' || char in Room

    /** Whether [line] ends with a backslash no other escapes, which carries its value on to the next. */
    private fun goesOn(line: String): Boolean = line.takeLastWhile { it == '\\' }.length % 2 == 1
}
