package com.developersancho.formatico.core.format.toml

import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.text.TextRange

/** How deep arrays, inline tables and the parts of keys are read; deeper is reported and read past. */
internal const val TOML_MAX_DEPTH = 512

/** A problem found while reading, by its offsets. */
internal class TomlFoundProblem(val code: ProblemCode, val start: Int, val end: Int)

/** Whether [char] is a control character TOML allows in no string and no comment: every one but the tab. */
internal fun isTomlControl(char: Char): Boolean = (char < ' ' && char != '\t') || char == '\u007F'

/** Whether [char] may stand in a bare key: a letter or a digit of ASCII, `_` or `-`. */
internal fun isBareKeyCharacter(char: Char): Boolean =
    char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char == '_' || char == '-'

/**
 * Where a TOML text is being read, and what its readers share: the problems they find and the comments they read
 * past. A reader that goes back over what it has read finds a comment or a problem again, so that [comments] and
 * [found] can hold one twice.
 */
internal class TomlScanner(val text: String) {
    /** The offset being read. */
    var position = 0

    /** The problems found, in the order they were found. */
    val found = ArrayList<TomlFoundProblem>()

    /** The comments read past, each from its `#` to the end of its line. */
    val comments = ArrayList<TextRange>()

    /** Whether the whole text has been read. */
    val atEnd: Boolean get() = position >= text.length

    /** The character being read, or null at the end of the text. */
    val current: Char? get() = text.getOrNull(position)

    /** Whether a line break stands where the text is being read. */
    val atLineBreak: Boolean get() = lineBreakAt(position) > 0

    /** The character [ahead] of the one being read, or null past the end of the text. */
    fun peek(ahead: Int): Char? = text.getOrNull(position + ahead)

    /** Reports [code] for the text from [start] to [end]. */
    fun report(code: ProblemCode, start: Int, end: Int) {
        found += TomlFoundProblem(code, start, end)
    }

    /** How long the line break at [offset] is: one for `\n`, two for `\r\n`, none where no line break stands. */
    fun lineBreakAt(offset: Int): Int = when {
        text.getOrNull(offset) == '\n' -> 1
        text.getOrNull(offset) == '\r' && text.getOrNull(offset + 1) == '\n' -> 2
        else -> 0
    }

    /** Where the line of [offset] ends, before its line break; a `\r` alone ends no line in TOML. */
    fun lineEnd(offset: Int = position): Int {
        var at = offset
        while (at < text.length && lineBreakAt(at) == 0) at++
        return at
    }

    /** Reads past spaces and tabs. */
    fun skipSpaces() {
        while (current == ' ' || current == '\t') position++
    }

    /** Reads past the line break where the text is being read, if one stands there. */
    fun skipLineBreak() {
        position += lineBreakAt(position)
    }

    /** Reads a comment, from its `#` to the end of its line; a control character in it, a tab aside, is reported. */
    fun comment() {
        val start = position
        val end = lineEnd()
        for (at in start + 1 until end) {
            if (isTomlControl(text[at])) report(ProblemCode.UnexpectedCharacter, at, at + 1)
        }
        comments += TextRange(start, end)
        position = end
    }

    /** Reads past spaces, tabs, line breaks and comments, as an array may hold between its values. */
    fun skipTrivia() {
        while (true) {
            skipSpaces()
            when {
                atLineBreak -> skipLineBreak()
                current == '#' -> comment()
                else -> return
            }
        }
    }

    /** Whether a line break stands from [from] up to [until]. */
    fun hasLineBreak(from: Int, until: Int): Boolean {
        for (at in from until until) {
            if (text[at] == '\n') return true
        }
        return false
    }
}
