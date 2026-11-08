package com.developersancho.formatico.core.format.query

import com.developersancho.formatico.core.format.json.isHexDigit

/** The largest integer I-JSON keeps exactly, 2^53 - 1: an index, a slice's bound and its step lie within it either way. */
private const val MAX_EXACT_INTEGER = 9_007_199_254_740_991L

/** How many digits [MAX_EXACT_INTEGER] has; an integer of more is out of range whatever they are. */
private const val MAX_EXACT_DIGITS = 16

private const val HEX_RADIX = 16
private const val UNICODE_ESCAPE_DIGITS = 4

/**
 * The string whose quote is next, its escapes read, as RFC 9535 writes one: between single or double quotes, with
 * JSON's escapes and the quote's own, and no control character as it is.
 */
internal fun JsonPathCursor.readString(): String {
    val start = at
    val quote = text[at++]
    val value = StringBuilder()
    while (true) {
        val char = peek() ?: queryError(QueryErrorCode.UnclosedString, start)
        when {
            char == quote -> {
                at++
                return value.toString()
            }

            char == '\\' -> readEscape(start, quote, value)

            char < ' ' || (char.isSurrogate() && characterLength(text, at) == 1) -> queryError(QueryErrorCode.UnexpectedCharacter, at)

            else -> {
                val length = characterLength(text, at)
                value.append(text, at, at + length)
                at += length
            }
        }
    }
}

/** Reads the escape whose backslash is next, in the string whose [quote] is at [start], into [value]. */
private fun JsonPathCursor.readEscape(start: Int, quote: Char, value: StringBuilder) {
    val backslash = at++
    val escaped = peek() ?: queryError(QueryErrorCode.UnclosedString, start)
    at++
    when (escaped) {
        'b' -> value.append('\b')
        'f' -> value.append('\u000C')
        'n' -> value.append('\n')
        'r' -> value.append('\r')
        't' -> value.append('\t')
        '/', '\\', quote -> value.append(escaped)
        'u' -> readUnicodeEscape(backslash, value)
        else -> queryError(QueryErrorCode.InvalidEscape, backslash)
    }
}

/** Reads the four digits after a `\u` whose backslash is at [backslash], and the low surrogate after a high one. */
private fun JsonPathCursor.readUnicodeEscape(backslash: Int, value: StringBuilder) {
    val unit = readHexUnit(backslash)
    if (unit.isLowSurrogate()) queryError(QueryErrorCode.InvalidEscape, backslash)
    value.append(unit)
    if (!unit.isHighSurrogate()) return
    val second = at
    if (peek() != '\\' || peek(1) != 'u') queryError(QueryErrorCode.InvalidEscape, backslash)
    at += 2
    val low = readHexUnit(second)
    if (!low.isLowSurrogate()) queryError(QueryErrorCode.InvalidEscape, second)
    value.append(low)
}

/** The code unit the four hexadecimal digits next write, in the escape whose backslash is at [backslash]. */
private fun JsonPathCursor.readHexUnit(backslash: Int): Char {
    val end = at + UNICODE_ESCAPE_DIGITS
    if (end > text.length || (at until end).any { !text[it].isHexDigit() }) queryError(QueryErrorCode.InvalidEscape, backslash)
    val unit = text.substring(at, end).toInt(HEX_RADIX).toChar()
    at = end
    return unit
}

/**
 * The integer next, as an index or a slice writes it: `0`, or digits that do not start with `0`, after a `-` or not;
 * null where none is next.
 */
internal fun JsonPathCursor.readInteger(): Long? {
    val start = at
    accept('-')
    val digits = skipDigits()
    if (digits == 0) {
        if (at == start) return null
        unexpected()
    }
    if (text[at - digits] == '0' && (digits > 1 || at - digits > start)) queryError(QueryErrorCode.InvalidNumber, start)
    val value = if (digits > MAX_EXACT_DIGITS) null else text.substring(start, at).toLongOrNull()
    if (value == null || value !in -MAX_EXACT_INTEGER..MAX_EXACT_INTEGER) queryError(QueryErrorCode.IntegerOutOfRange, start)
    return value
}

/** The number next, as a filter's literal writes it: an integer, `-0` among them, a fraction and an exponent. */
internal fun JsonPathCursor.readNumber(): Double {
    val start = at
    accept('-')
    val digits = skipDigits()
    if (digits == 0) unexpected()
    if (digits > 1 && text[at - digits] == '0') queryError(QueryErrorCode.InvalidNumber, start)
    if (accept('.') && skipDigits() == 0) queryError(QueryErrorCode.InvalidNumber, start)
    if (accept('e') || accept('E')) {
        if (!accept('-')) accept('+')
        if (skipDigits() == 0) queryError(QueryErrorCode.InvalidNumber, start)
    }
    return text.substring(start, at).toDouble()
}

/** Reads past the digits next, and says how many there were. */
private fun JsonPathCursor.skipDigits(): Int {
    val start = at
    while (peek() in '0'..'9') at++
    return at - start
}

/**
 * The member name next, as a shorthand writes it: `$.name`. It starts with a letter, `_` or a character beyond
 * U+007F, and goes on with them and digits.
 */
internal fun JsonPathCursor.readMemberName(): String {
    val start = at
    while (!atEnd) {
        val char = text[at]
        val fits = char in 'a'..'z' || char in 'A'..'Z' || char == '_' || char >= '\u0080' || (at > start && char in '0'..'9')
        if (!fits || (char.isSurrogate() && characterLength(text, at) == 1)) break
        at += characterLength(text, at)
    }
    if (at == start) unexpected()
    return text.substring(start, at)
}

/** The function name, or the word `true`, `false` or `null`, next: a lowercase letter, then lowercase letters, digits and `_`. */
internal fun JsonPathCursor.readWord(): String {
    val start = at
    while (true) {
        val char = peek()
        val later = at > start && (char in '0'..'9' || char == '_')
        if (char !in 'a'..'z' && !later) break
        at++
    }
    return text.substring(start, at)
}
