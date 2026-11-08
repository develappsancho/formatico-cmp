package com.developersancho.formatico.feature.workspace.editor

import com.pantrixui.components.data.codeblock.CodeToken
import com.pantrixui.components.data.codeblock.CodeTokenKind

/** Reads [line] from its start, a run at a time, and keeps the runs that have a colour: what a highlighter is built on. */
internal class LineScanner(val line: String) {
    /** Where the next run starts. */
    var at = 0

    private val tokens = mutableListOf<CodeToken>()

    /** Whether the line is read to its end. */
    val done: Boolean get() = at >= line.length

    /** The character at the place [ahead] of the next run's start, or null past the line's end. */
    fun peek(ahead: Int = 0): Char? = line.getOrNull(at + ahead)

    /** Whether the line goes on with [text] from the next run's start. */
    fun startsWith(text: String): Boolean = line.startsWith(text, at)

    /** Marks the characters from the next run's start up to [end] as [kind], and goes on from there. */
    fun take(end: Int, kind: CodeTokenKind) {
        val to = end.coerceIn(at, line.length)
        if (to > at && kind != CodeTokenKind.Plain) tokens += CodeToken(at, to, kind)
        at = to
    }

    /** Goes on past the characters that [keep] takes, and marks them as [kind]. */
    fun takeWhile(kind: CodeTokenKind, keep: (Char) -> Boolean) {
        var end = at
        while (end < line.length && keep(line[end])) end++
        take(end, kind)
    }

    /** Goes on past the room before the next word. */
    fun skipSpace() = takeWhile(CodeTokenKind.Plain) { it == ' ' || it == '\t' }

    /** A string from its quote, which is the next character, to the quote that closes it, or the line's end. */
    fun quoted(kind: CodeTokenKind = CodeTokenKind.StringLiteral) {
        take(quotedEnd(at), kind)
    }

    /** Where a string that opens at [start] ends: after the quote that closes it, past escaped ones, or at the line's end. */
    fun quotedEnd(start: Int): Int {
        val quote = line[start]
        var end = start + 1
        while (end < line.length && line[end] != quote) end += if (line[end] == '\\') 2 else 1
        return minOf(end + 1, line.length)
    }

    /** The place of the first character after [from] that is no space, or the line's end. */
    fun nextWord(from: Int): Int {
        var end = from
        while (end < line.length && (line[end] == ' ' || line[end] == '\t')) end++
        return end
    }

    /** The runs read so far. */
    fun tokens(): List<CodeToken> = tokens
}
