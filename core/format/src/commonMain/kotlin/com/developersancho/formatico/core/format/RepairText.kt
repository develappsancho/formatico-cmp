package com.developersancho.formatico.core.format

import com.developersancho.formatico.core.format.text.TextRange

/** A hexadecimal number's base. */
private const val HEX = 16

/** How many digits a unicode escape has. */
private const val UNICODE_DIGITS = 4

internal fun quoted(key: String) = "\"" + key.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

/** A string in single quotes in double ones: its `\'` a plain quote, its `"` escaped. */
internal fun doubleQuoted(string: String): String {
    val inner = string.removePrefix("'").removeSuffix("'")
    return "\"" + inner.replace("\\'", "'").replace("\"", "\\\"") + "\""
}

internal fun closer(opener: String): String? = when (opener.firstOrNull()) {
    '{' -> "}"
    '[' -> "]"
    else -> null
}

/** A control character written as JSON writes it. */
internal fun escaped(control: String): String = control.map { char ->
    when (char) {
        '\t' -> "\\t"
        '\n' -> "\\n"
        '\r' -> "\\r"
        else -> "\\u" + char.code.toString(HEX).padStart(UNICODE_DIGITS, '0')
    }
}.joinToString("")

/** A number as JSON spells it: no `+`, no leading zeros, digits on both sides of the point; a hex one in decimal. */
internal fun fixedNumber(number: String): String? {
    val sign = if (number.startsWith("-")) "-" else ""
    val digits = number.removePrefix("-").removePrefix("+")
    if (digits.startsWith("0x") || digits.startsWith("0X")) return digits.drop(2).toLongOrNull(HEX)?.let { sign + it }
    val (whole, fraction) = digits.split('.', limit = 2).let { it[0] to it.getOrNull(1) }
    if (whole.any { !it.isDigit() } || fraction?.any { !it.isDigit() && it !in "eE+-" } == true) return null
    val integer = whole.trimStart('0').ifEmpty { "0" }
    return sign + integer + when {
        fraction == null -> ""
        fraction.isEmpty() || !fraction.first().isDigit() -> ".0$fraction"
        else -> ".$fraction"
    }
}

/**
 * The value of the YAML line [offset] is on, after its key's colon and before a comment: what a colon inside it would
 * start a mapping in. Null where the line has no key before [offset], or its value is quoted already.
 */
internal fun yamlValue(text: String, offset: Int): TextRange? {
    val lineStart = text.lastIndexOf('\n', (offset - 1).coerceAtLeast(0)) + 1
    val lineEnd = text.indexOf('\n', offset).let { if (it < 0) text.length else it }
    val line = text.substring(lineStart, lineEnd)
    val colon = line.indexOf(": ").takeIf { it >= 0 && lineStart + it < offset } ?: return null
    val comment = line.indexOf(" #", colon).let { if (it < 0) line.length else it }
    val value = line.substring(colon + 2, comment)
    val start = lineStart + colon + 2 + (value.length - value.trimStart().length)
    val end = lineStart + colon + 2 + value.trimEnd().length
    return TextRange(start, end).takeIf { end > start && text[start] != '"' && text[start] != '\'' }
}
