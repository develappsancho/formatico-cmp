package com.developersancho.formatico.core.format.convert

import com.developersancho.formatico.core.format.text.wholeRegex

/** A number's spelling read into what any format needs to write it again its own way. */
internal sealed class NumberParts {
    /** Whether a `-` stands before it. */
    abstract val negative: Boolean

    /** An integer written in [radix], 16, 8 or 2: its [digits], in lower case and with no `_`. */
    data class Radix(override val negative: Boolean, val radix: Int, val digits: String) : NumberParts()

    /**
     * A number in tens: its [whole] digits, which may be none, as in `.5`; its [fraction] after a point, which may be
     * empty, as in `5.`, or null where there is no point; and its [exponent], signed, or null.
     */
    data class Decimal(override val negative: Boolean, val whole: String, val fraction: String?, val exponent: String?) : NumberParts()

    /** An infinity. */
    data class Infinite(override val negative: Boolean) : NumberParts()

    /** Not a number. */
    data object NotANumber : NumberParts() {
        override val negative: Boolean get() = false
    }
}

private val DecimalParts = wholeRegex("([0-9]*)(\\.([0-9]*))?([eE]([+-]?[0-9]+))?")

/** The parts of [written], a number as JSON, JSON5, YAML, TOML or CSV writes one; null where it is none of them. */
internal fun numberParts(written: String): NumberParts? {
    val plain = written.replace("_", "")
    val negative = plain.startsWith('-')
    val unsigned = plain.removePrefix("-").removePrefix("+")
    val lower = unsigned.lowercase()
    return when {
        lower in Infinities -> NumberParts.Infinite(negative)

        lower in NotNumbers -> NumberParts.NotANumber

        lower.startsWith("0x") -> radix(negative, SIXTEEN, lower.drop(PREFIX))

        lower.startsWith("0o") -> radix(negative, EIGHT, lower.drop(PREFIX))

        lower.startsWith("0b") -> radix(negative, TWO, lower.drop(PREFIX))

        else -> DecimalParts.find(unsigned)?.let { match ->
            val groups = match.groupValues
            val (whole, point, fraction) = Triple(groups[WHOLE], groups[POINT], groups[FRACTION])
            NumberParts.Decimal(negative, whole, fraction.takeIf { point.isNotEmpty() }, groups[EXPONENT].takeIf { it.isNotEmpty() })
                .takeIf { whole.isNotEmpty() || fraction.isNotEmpty() }
        }
    }
}

/** Where the groups of [DecimalParts] are: the whole digits, the point and the fraction after it, and the exponent. */
private const val WHOLE = 1
private const val POINT = 2
private const val FRACTION = 3
private const val EXPONENT = 5

/** The bases a prefix names, and how long a prefix is: `0x`, `0o`, `0b`. */
private const val SIXTEEN = 16
private const val EIGHT = 8
private const val TWO = 2
private const val PREFIX = 2

private val Infinities = setOf("inf", ".inf", "infinity")
private val NotNumbers = setOf("nan", ".nan")

private fun radix(negative: Boolean, radix: Int, digits: String): NumberParts? =
    NumberParts.Radix(negative, radix, digits).takeIf { digits.isNotEmpty() && digits.all { it.digitToIntOrNull(radix) != null } }

/** The integer [parts] stands for in tens, signed; null where it is larger than 64 bits hold. */
internal fun NumberParts.Radix.inTens(): String? {
    val value = digits.toULongOrNull(radix) ?: return null
    return if (negative) "-$value" else "$value"
}

/** [parts] in tens as JSON writes a number: no `+`, no leading zero, digits on both sides of a point. */
internal fun NumberParts.Decimal.asJson(): String = buildString {
    if (negative) append('-')
    append(whole.trimStart('0').ifEmpty { "0" })
    if (fraction != null) append('.').append(fraction.ifEmpty { "0" })
    if (exponent != null) append('e').append(exponent)
}

/** Whether [parts] is an integer, with no point and no exponent. */
internal val NumberParts.Decimal.isInteger: Boolean get() = fraction == null && exponent == null

/**
 * Whether [written] keeps a spelling a converter that read its value and wrote it again would not: a fraction that
 * ends in nought, `129.90`, an exponent, a `+`, a leading nought, a base, an `_`.
 */
internal fun isSpelledOwnWay(written: String): Boolean {
    val parts = numberParts(written) ?: return false
    return when (parts) {
        is NumberParts.Decimal -> parts.fraction?.endsWith('0') == true ||
            parts.exponent != null ||
            written.startsWith('+') ||
            written.contains('_') ||
            (parts.whole.length > 1 && parts.whole.startsWith('0'))

        is NumberParts.Radix -> true

        else -> false
    }
}
