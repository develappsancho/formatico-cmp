package com.developersancho.formatico.core.format.json

private const val HEX_RADIX = 16
private const val UNICODE_ESCAPE_DIGITS = 4
private const val HEX_ESCAPE_DIGITS = 2

/**
 * The value of a string as it is written, [raw] with its opening quote: the quotes taken off and the escapes read,
 * JSON5's among them. An escape that cannot be read is kept as it is written; a string that is never closed ends with
 * its text.
 */
internal fun decodeJsonString(raw: String): String {
    if (raw.isEmpty()) return raw
    val quote = raw[0]
    val value = StringBuilder(raw.length)
    var index = 1
    while (index < raw.length) {
        val char = raw[index]
        if (char == quote) break
        if (char != '\\') {
            value.append(char)
            index++
            continue
        }
        index++
        if (index >= raw.length) break
        index = when (val escaped = raw[index]) {
            'b' -> value.appendAndStep('\b', index)

            'f' -> value.appendAndStep('\u000C', index)

            'n' -> value.appendAndStep('\n', index)

            'r' -> value.appendAndStep('\r', index)

            't' -> value.appendAndStep('\t', index)

            'v' -> value.appendAndStep('\u000B', index)

            '0' -> value.appendAndStep('\u0000', index)

            'u' -> value.appendHex(raw, index, UNICODE_ESCAPE_DIGITS)

            'x' -> value.appendHex(raw, index, HEX_ESCAPE_DIGITS)

            // A line continuation: the backslash and the line break are not part of the value.
            '\r' -> if (index + 1 < raw.length && raw[index + 1] == '\n') index + 2 else index + 1

            '\n', '\u2028', '\u2029' -> index + 1

            else -> value.appendAndStep(escaped, index)
        }
    }
    return value.toString()
}

private fun StringBuilder.appendAndStep(char: Char, index: Int): Int {
    append(char)
    return index + 1
}

/** Reads `\uXXXX` or `\xXX` whose letter stands at [index]; an escape without its digits is kept as written. */
private fun StringBuilder.appendHex(raw: String, index: Int, digits: Int): Int {
    val end = index + 1 + digits
    val hex = if (end <= raw.length) raw.substring(index + 1, end) else ""
    val code = if (hex.length == digits && hex.all { it.isHexDigit() }) hex.toInt(HEX_RADIX) else null
    return if (code != null) {
        append(code.toChar())
        end
    } else {
        append('\\').append(raw[index])
        index + 1
    }
}

internal fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
