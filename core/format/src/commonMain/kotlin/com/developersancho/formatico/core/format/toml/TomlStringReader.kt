package com.developersancho.formatico.core.format.toml

import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.text.TextRange

private const val HEX_RADIX = 16
private const val SHORT_UNICODE_DIGITS = 4
private const val LONG_UNICODE_DIGITS = 8
private const val MULTI_LINE_QUOTES = 3
private const val MOST_QUOTES_BEFORE_CLOSING = 2
private const val LAST_CODE_POINT = 0x10FFFF
private const val FIRST_SUPPLEMENTARY = 0x10000
private const val FIRST_HIGH_SURROGATE = 0xD800
private const val FIRST_LOW_SURROGATE = 0xDC00
private const val LAST_SURROGATE = 0xDFFF
private const val SURROGATE_BITS = 10
private const val SURROGATE_MASK = 0x3FF

/** What each escape of a single letter stands for. */
private val SimpleEscapes = mapOf(
    'b' to '\b',
    't' to '\t',
    'n' to '\n',
    'f' to '\u000C',
    'r' to '\r',
    '"' to '"',
    '\\' to '\\'
)

/**
 * Reads TOML's strings: basic `"…"` and literal `'…'` on one line, and their multi-line kinds `"""…"""` and `'''…'''`.
 * A mistake inside a string is reported and the string read on, so that the mistakes after it are found too.
 */
internal class TomlStringReader(private val scanner: TomlScanner) {
    private val text = scanner.text

    /** Reads the string whose opening quote is where the scanner is; null for one that is never closed, reported. */
    fun read(): TomlString? {
        val start = scanner.position
        val quote = text[start]
        val multiLine = text.startsWith(quote.toString().repeat(MULTI_LINE_QUOTES), start)
        val value = (if (multiLine) multiLine(quote) else singleLine(quote)) ?: return null
        return TomlString(TextRange(start, scanner.position), text.substring(start, scanner.position), value)
    }

    /** Reads a string on one line, basic or literal by its [quote], to its closing quote; null where its line ends first. */
    private fun singleLine(quote: Char): String? {
        val start = scanner.position
        scanner.position++
        val value = StringBuilder()
        while (true) {
            val char = scanner.current
            when {
                char == null || scanner.atLineBreak -> {
                    scanner.report(ProblemCode.UnterminatedString, start, scanner.position)
                    return null
                }

                char == quote -> {
                    scanner.position++
                    return value.toString()
                }

                char == '\\' && quote == '"' -> {
                    escape(value)
                }

                else -> {
                    character(char, value)
                }
            }
        }
    }

    /** Reads a multi-line string, basic or literal by its [quote], to its closing quotes; null where the text ends first. */
    private fun multiLine(quote: Char): String? {
        val start = scanner.position
        scanner.position += MULTI_LINE_QUOTES
        // A line break right after the opening quotes is not part of the value.
        scanner.skipLineBreak()
        val value = StringBuilder()
        while (true) {
            val char = scanner.current
            val lineBreak = scanner.lineBreakAt(scanner.position)
            when {
                char == null -> {
                    scanner.report(ProblemCode.UnterminatedString, start, start + MULTI_LINE_QUOTES)
                    return null
                }

                char == quote -> {
                    if (closes(quote, value)) return value.toString()
                }

                lineBreak > 0 -> {
                    value.append(text, scanner.position, scanner.position + lineBreak)
                    scanner.position += lineBreak
                }

                char == '\\' && quote == '"' -> {
                    if (!continues()) escape(value)
                }

                else -> {
                    character(char, value)
                }
            }
        }
    }

    /** A character of a string: a control character, a tab aside, is reported; any other is the value's. */
    private fun character(char: Char, value: StringBuilder) {
        if (isTomlControl(char)) {
            scanner.report(ProblemCode.ControlCharacterInString, scanner.position, scanner.position + 1)
        } else {
            value.append(char)
        }
        scanner.position++
    }

    /**
     * At a quote of a multi-line string: fewer than three in a row are part of the value; three close the string, and
     * up to two more before them are the value's last characters. Says whether the string is closed.
     */
    private fun closes(quote: Char, value: StringBuilder): Boolean {
        var run = 0
        while (scanner.peek(run) == quote) run++
        val closing = run >= MULTI_LINE_QUOTES
        val kept = if (closing) minOf(run - MULTI_LINE_QUOTES, MOST_QUOTES_BEFORE_CLOSING) else run
        repeat(kept) { value.append(quote) }
        scanner.position += kept + if (closing) MULTI_LINE_QUOTES else 0
        return closing
    }

    /**
     * At a backslash of a multi-line basic string: whether it is the last character of its line, spaces and tabs aside.
     * Such a backslash is read past with every space, tab and line break after it, none of which is part of the value.
     */
    private fun continues(): Boolean {
        var at = scanner.position + 1
        while (text.getOrNull(at) == ' ' || text.getOrNull(at) == '\t') at++
        if (scanner.lineBreakAt(at) == 0) return false
        scanner.position = at
        while (true) {
            when {
                scanner.current == ' ' || scanner.current == '\t' -> scanner.position++
                scanner.atLineBreak -> scanner.skipLineBreak()
                else -> return true
            }
        }
    }

    /** Reads the escape whose backslash is where the scanner is, into [value]. */
    private fun escape(value: StringBuilder) {
        val start = scanner.position
        val letter = scanner.peek(1)
        val simple = letter?.let { SimpleEscapes[it] }
        when {
            simple != null -> {
                value.append(simple)
                scanner.position += 2
            }

            letter == 'u' -> {
                unicode(value, SHORT_UNICODE_DIGITS)
            }

            letter == 'U' -> {
                unicode(value, LONG_UNICODE_DIGITS)
            }

            else -> {
                // The letter after the backslash, if any stands before the line's end, is part of the mistake.
                scanner.position += if (letter == null || scanner.lineBreakAt(start + 1) > 0) 1 else 2
                scanner.report(ProblemCode.InvalidEscape, start, scanner.position)
            }
        }
    }

    /** Reads `\uXXXX` or `\UXXXXXXXX`, whose code must be one of Unicode's scalar values. */
    private fun unicode(value: StringBuilder, digits: Int) {
        val start = scanner.position
        val end = start + 2 + digits
        var at = start + 2
        var code = 0L
        while (at < end) {
            val digit = hexDigit(text.getOrNull(at)) ?: break
            code = code * HEX_RADIX + digit
            at++
        }
        scanner.position = at
        val point = if (at == end && code <= LAST_CODE_POINT) code.toInt() else -1
        if (point < 0 || point in FIRST_HIGH_SURROGATE..LAST_SURROGATE) {
            scanner.report(ProblemCode.InvalidEscape, start, at)
        } else {
            value.appendScalar(point)
        }
    }
}

/** The value of [char] as a hexadecimal digit of ASCII, or null where it is none. */
private fun hexDigit(char: Char?): Int? =
    char?.takeIf { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }?.digitToInt(HEX_RADIX)

/** Appends the Unicode scalar value [code], as two surrogates where it is past the first plane. */
private fun StringBuilder.appendScalar(code: Int) {
    if (code < FIRST_SUPPLEMENTARY) {
        append(code.toChar())
    } else {
        val offset = code - FIRST_SUPPLEMENTARY
        append((FIRST_HIGH_SURROGATE + (offset shr SURROGATE_BITS)).toChar())
        append((FIRST_LOW_SURROGATE + (offset and SURROGATE_MASK)).toChar())
    }
}
