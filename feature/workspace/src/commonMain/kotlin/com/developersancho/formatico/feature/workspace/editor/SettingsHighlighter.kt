package com.developersancho.formatico.feature.workspace.editor

import com.developersancho.formatico.core.format.text.wholeRegex
import com.pantrixui.components.data.codeblock.CodeTokenKind
import com.pantrixui.components.data.codeeditor.CodeHighlight
import com.pantrixui.components.data.codeeditor.CodeHighlighter

/** A number as a setting writes it, in TOML's ways among them. */
private val SettingNumber = wholeRegex("""[-+]?(\d[\d_]*(\.\d+)?([eE][-+]?\d+)?|0x[0-9a-fA-F_]+|inf|nan)""")

/**
 * A file of settings whose lines stand alone, a line at a time: INI. A section's name in brackets, a setting's name
 * before one of its [separators] in the key's colour, its value a string, a number or a word, and the comments
 * [comments] start.
 */
internal class SettingsHighlighter(private val comments: String, private val separators: String) : CodeHighlighter<Unit> {
    override val initialState = Unit

    override fun highlight(line: String, state: Unit): CodeHighlight<Unit> {
        val scan = LineScanner(line)
        scan.skipSpace()
        val char = scan.peek() ?: return CodeHighlight(scan.tokens(), state)
        when {
            char in comments -> scan.take(line.length, CodeTokenKind.Comment)
            char == '[' -> scan.take(line.indexOf(']', scan.at).let { if (it < 0) line.length else it + 1 }, CodeTokenKind.Tag)
            else -> setting(scan)
        }
        return CodeHighlight(scan.tokens(), state)
    }

    private fun setting(scan: LineScanner) {
        val separator = scan.line.indexOfFirst { it in separators }.takeIf { it >= scan.at } ?: return
        scan.take(separator, CodeTokenKind.Property)
        scan.take(separator + 1, CodeTokenKind.Punctuation)
        scan.skipSpace()
        val rest = scan.line.substring(scan.at)
        val comment = rest.indexOfFirst { it in comments }.let { if (it < 0) rest.length else it }
        val value = rest.substring(0, comment).trimEnd()
        val kind = when {
            value.startsWith("\"") || value.startsWith("'") -> CodeTokenKind.StringLiteral
            value == "true" || value == "false" -> CodeTokenKind.Keyword
            SettingNumber.containsMatchIn(value) -> CodeTokenKind.Number
            value.startsWith("[") || value.startsWith("{") -> CodeTokenKind.Punctuation
            else -> CodeTokenKind.StringLiteral
        }
        scan.take(scan.at + value.length, kind)
        scan.skipSpace()
        if (!scan.done) scan.take(scan.line.length, CodeTokenKind.Comment)
    }
}
