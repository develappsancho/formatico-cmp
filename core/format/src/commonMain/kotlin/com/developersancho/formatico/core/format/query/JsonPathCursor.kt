package com.developersancho.formatico.core.format.query

/**
 * Where a JSONPath is read: [text], and the place [at] reading has come to; and how deep its brackets, parentheses and
 * functions are nested there, which is bounded by [MAX_QUERY_DEPTH].
 */
internal class JsonPathCursor(val text: String) {
    /** The place of the next character to read. */
    var at = 0

    private var depth = 0

    /** Whether every character has been read. */
    val atEnd: Boolean get() = at >= text.length

    /** The character [ahead] of the next one, or null past the end. */
    fun peek(ahead: Int = 0): Char? = text.getOrNull(at + ahead)

    /** Reads [char] where it is next, and says whether it was. */
    fun accept(char: Char): Boolean {
        if (peek() != char) return false
        at++
        return true
    }

    /** Reads [char], which must be next. */
    fun expect(char: Char) {
        if (!accept(char)) unexpected()
    }

    /** Reads past whitespace, as RFC 9535 has it: spaces, tabs and line breaks. */
    fun skipWhitespace() {
        while (peek()?.let(::isPathWhitespace) == true) at++
    }

    /** Stops at the next character, which cannot stand where it does, or at the end where there is none. */
    fun unexpected(): Nothing = queryError(if (atEnd) QueryErrorCode.UnexpectedEnd else QueryErrorCode.UnexpectedCharacter, at)

    /** Goes one level deeper, at a bracket, a parenthesis or a function. */
    fun enter() {
        if (++depth > MAX_QUERY_DEPTH) queryError(QueryErrorCode.TooDeep, at)
    }

    /** Comes back from the level [enter] went into. */
    fun leave() {
        depth--
    }
}

/** Whether [char] is whitespace in a JSONPath: a space, a tab or a line break. */
internal fun isPathWhitespace(char: Char): Boolean = char == ' ' || char == '\t' || char == '\n' || char == '\r'
