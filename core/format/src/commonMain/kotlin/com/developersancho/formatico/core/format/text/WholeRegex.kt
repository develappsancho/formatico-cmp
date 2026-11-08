package com.developersancho.formatico.core.format.text

/**
 * [pattern] as a regular expression that matches a text only as a whole, alike on every platform: it is tried with
 * [Regex.containsMatchIn], or [Regex.find] for its groups. Kotlin/JS's `matches` and `matchEntire` take the first match
 * its engine finds from the text's start and try no other: there `y|yes` does not match "yes", and `a|h.*` matches
 * "aa". Anchored at the start and where no character follows, the pattern is read alike by every platform's engine.
 */
fun wholeRegex(pattern: String, vararg options: RegexOption): Regex = Regex("^(?:$pattern)(?![\\s\\S])", options.toSet())

/** A decimal number: digits, a point, a fraction and an exponent, each where it may be, signed or not. */
private val DecimalNumber = wholeRegex("[-+]?([0-9]+(\\.[0-9]*)?|\\.[0-9]+)([eE][-+]?[0-9]+)?")

/**
 * This text read as a decimal number, or null where it is not one. Kotlin/JS's `toDoubleOrNull` reads what JavaScript's
 * `Number` reads, `0x1F`, `0b101` and ` 12 ` among them, which the other platforms do not.
 */
fun String.toDecimalOrNull(): Double? = if (DecimalNumber.containsMatchIn(this)) toDoubleOrNull() else null
