package com.developersancho.formatico.core.format

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
