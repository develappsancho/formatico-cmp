package com.developersancho.formatico.core.format.convert

/** A number as RFC 8259 writes it. */
private val JsonNumber = Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")

/** A number as JSON5 writes it: JSON's, a `+`, a point at either end, hexadecimal, `Infinity` and `NaN`. */
private val Json5Number = Regex("[+-]?(Infinity|NaN|0[xX][0-9a-fA-F]+|((0|[1-9][0-9]*)(\\.[0-9]*)?|\\.[0-9]+)([eE][+-]?[0-9]+)?)")

/** A number of YAML 1.2's core schema: an integer in tens, eights or sixteens, a float, an infinity or `.nan`. */
private val YamlNumber = Regex(
    "[-+]?[0-9]+|0o[0-7]+|0x[0-9a-fA-F]+|[-+]?(\\.[0-9]+|[0-9]+(\\.[0-9]*)?)([eE][-+]?[0-9]+)?|[-+]?\\.(inf|Inf|INF)|\\.(nan|NaN|NAN)"
)

/** A TOML integer in tens, its digits joined by single `_`. */
private val TomlInteger = Regex("[+-]?(0|[1-9](_?[0-9])*)")

/** A TOML integer in sixteens, eights or twos. */
private val TomlRadix = Regex("0x[0-9A-Fa-f](_?[0-9A-Fa-f])*|0o[0-7](_?[0-7])*|0b[01](_?[01])*")

/** A TOML float: a fraction, an exponent or both, or an infinity or `nan`. */
private val TomlFloat = Regex(
    "[+-]?(0|[1-9](_?[0-9])*)((\\.[0-9](_?[0-9])*)([eE][+-]?[0-9](_?[0-9])*)?|[eE][+-]?[0-9](_?[0-9])*)|[+-]?(inf|nan)"
)

/** A number as a CSV field writes it, which Formatico's table reads as a number. */
private val PlainNumber = Regex("[-+]?([0-9]+(\\.[0-9]*)?|\\.[0-9]+)([eE][-+]?[0-9]+)?")

/** [written] as JSON writes it, or null where JSON has no number for it: an infinity, NaN, or more than 64 bits. */
internal fun jsonSpelling(written: String): String? {
    if (JsonNumber.matches(written)) return written
    return when (val parts = numberParts(written) ?: return null) {
        is NumberParts.Radix -> parts.inTens()
        is NumberParts.Decimal -> parts.asJson()
        else -> null
    }
}

/**
 * The number as JSON writes it and, where JSON has none for it, as JSON5 does: `Infinity`, `-Infinity`, `NaN`. Null for
 * no number either reads. A JSON Schema is asked of it so (Faz 19), whatever format it was read from.
 */
val DataNumber.jsonSpelled: String? get() = jsonSpelling(written) ?: json5Spelling(written)

/** [written] as JSON5 writes it, or null where it is no number JSON5 writes. */
internal fun json5Spelling(written: String): String? {
    if (Json5Number.matches(written)) return written
    return when (val parts = numberParts(written) ?: return null) {
        is NumberParts.Radix -> if (parts.radix == HEXADECIMAL) signed(parts.negative, "0x${parts.digits}") else parts.inTens()
        is NumberParts.Decimal -> parts.asJson()
        is NumberParts.Infinite -> signed(parts.negative, "Infinity")
        NumberParts.NotANumber -> "NaN"
    }
}

/** [written] as YAML's core schema writes it, or null where it is no number YAML reads. */
internal fun yamlSpelling(written: String): String? {
    if (YamlNumber.matches(written)) return written
    return when (val parts = numberParts(written) ?: return null) {
        is NumberParts.Radix -> when {
            parts.negative || parts.radix == BINARY -> parts.inTens()
            parts.radix == HEXADECIMAL -> "0x${parts.digits}"
            else -> "0o${parts.digits}"
        }

        is NumberParts.Decimal -> parts.asJson()

        is NumberParts.Infinite -> signed(parts.negative, ".inf")

        NumberParts.NotANumber -> ".nan"
    }
}

/**
 * [written] as TOML writes it, or null where TOML has no number for it: an integer is 64 bits there, signed, in tens,
 * sixteens, eights or twos.
 */
internal fun tomlSpelling(written: String): String? {
    if (TomlFloat.matches(written)) return written
    val parts = numberParts(written) ?: return null
    if (TomlInteger.matches(written) || TomlRadix.matches(written)) return written.takeIf { parts.fitsTomlInteger() }
    return when (parts) {
        is NumberParts.Radix -> parts.inTens()?.takeIf { it.toLongOrNull() != null }

        is NumberParts.Decimal -> {
            val json = parts.asJson()
            if (parts.isInteger) json.takeIf { it.toLongOrNull() != null } else json
        }

        is NumberParts.Infinite -> signed(parts.negative, "inf")

        NumberParts.NotANumber -> "nan"
    }
}

/** Whether a TOML integer of these parts is one TOML holds: 64 bits, signed. */
private fun NumberParts.fitsTomlInteger(): Boolean = when (this) {
    is NumberParts.Radix -> digits.toLongOrNull(radix) != null
    is NumberParts.Decimal -> asJson().toLongOrNull() != null
    else -> true
}

/** [written] as a CSV field writes a number that is read as one, or null where it is none: an infinity, NaN. */
internal fun plainSpelling(written: String): String? {
    if (PlainNumber.matches(written)) return written
    return when (val parts = numberParts(written) ?: return null) {
        is NumberParts.Radix -> parts.inTens()
        is NumberParts.Decimal -> parts.asJson()
        else -> null
    }
}

private fun signed(negative: Boolean, number: String): String = if (negative) "-$number" else number

private const val HEXADECIMAL = 16
private const val BINARY = 2
