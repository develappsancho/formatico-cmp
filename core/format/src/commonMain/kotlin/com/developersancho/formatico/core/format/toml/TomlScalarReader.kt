package com.developersancho.formatico.core.format.toml

import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.text.TextRange

private const val DECIMAL_RADIX = 10
private const val HEX_RADIX = 16
private const val OCTAL_RADIX = 8
private const val BINARY_RADIX = 2
private const val PREFIX_LENGTH = 2

/**
 * An integer as TOML spells it: decimal with a sign or none, or hexadecimal, octal or binary with none. The patterns
 * that are matched against a whole value are anchored: a JavaScript regular expression takes the first alternative
 * that matches, `0` of `0x1F`, where the JVM's looks for one that matches the whole.
 */
private val IntegerPattern =
    Regex("^(?:[+-]?(?:0|[1-9](?:_?[0-9])*)|0x[0-9A-Fa-f](?:_?[0-9A-Fa-f])*|0o[0-7](?:_?[0-7])*|0b[01](?:_?[01])*)$")

/** A float as TOML spells it: a fraction, an exponent or both, `inf` or `nan`, with a sign or none. */
private val FloatPattern = Regex(
    "^[+-]?(?:(?:0|[1-9](?:_?[0-9])*)(?:\\.[0-9](?:_?[0-9])*(?:[eE][+-]?[0-9](?:_?[0-9])*)?|[eE][+-]?[0-9](?:_?[0-9])*)|inf|nan)$"
)

/** How what is meant as a date or a time starts: a year and its dash, or an hour and its colon. */
private val DateTimeStart = Regex("^[0-9]{4}-|^[0-9]{2}:")

/** A date alone, which a space and a time may follow as one value. */
private val DatePattern = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")

/**
 * Reads what stands bare as a value: a boolean, an integer, a float, or a date or a time. Each is kept as it is written,
 * and one TOML does not spell that way is reported.
 */
internal class TomlScalarReader(private val scanner: TomlScanner) {
    private val text = scanner.text

    /** Reads the value that starts where the scanner is; null where it is none, which is reported. */
    fun read(): TomlValue? {
        val start = scanner.position
        word()
        if (scanner.position == start) {
            scanner.report(ProblemCode.ExpectedValue, start, start + 1)
            return null
        }
        return valueOf(text.substring(start, scanner.position), TextRange(start, scanner.position))
    }

    /** Reads past the characters a bare value is written in; a date and a time with a space between them are one. */
    private fun word() {
        val start = scanner.position
        skipWordCharacters()
        val timeFollows = scanner.current == ' ' && isTimeAt(scanner.position + 1)
        if (timeFollows && DatePattern.matches(text.substring(start, scanner.position))) {
            scanner.position++
            skipWordCharacters()
        }
    }

    /** Reads past the characters a bare value may hold. */
    private fun skipWordCharacters() {
        while (scanner.current?.let(::isWordCharacter) == true) scanner.position++
    }

    /** Whether a time starts at [offset]: two digits and a colon. */
    private fun isTimeAt(offset: Int): Boolean =
        text.getOrNull(offset)?.isAsciiDigit() == true && text.getOrNull(offset + 1)?.isAsciiDigit() == true &&
            text.getOrNull(offset + 2) == ':'

    /** The value [raw], written at [range], is; null where it is none, which is reported. */
    private fun valueOf(raw: String, range: TextRange): TomlValue? = when {
        raw == "true" -> TomlBoolean(range, value = true)

        raw == "false" -> TomlBoolean(range, value = false)

        IntegerPattern.matches(raw) -> if (fitsInLong(raw)) TomlInteger(range, raw) else invalid(ProblemCode.InvalidNumber, range)

        FloatPattern.matches(raw) -> TomlFloat(range, raw)

        DateTimeStart.containsMatchIn(raw) -> {
            tomlDateTimeKind(raw)?.let { TomlDateTime(range, raw, it) } ?: invalid(ProblemCode.TomlInvalidDateTime, range)
        }

        raw[0].isAsciiDigit() || raw[0] in "+-." -> invalid(ProblemCode.InvalidNumber, range)

        // A word, as an unquoted string is written: TOML has none.
        else -> invalid(ProblemCode.ExpectedValue, range)
    }

    /** Reports [code] for [range], and gives no value. */
    private fun invalid(code: ProblemCode, range: TextRange): TomlValue? {
        scanner.report(code, range.start, range.end)
        return null
    }
}

/** Whether [char] may stand in a bare value: a letter or a digit of ASCII, `_`, a sign, a dot or a colon. */
private fun isWordCharacter(char: Char): Boolean = isBareKeyCharacter(char) || char == '+' || char == '.' || char == ':'

/** Whether the character is a digit of ASCII, `0` to `9`. */
private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

/** Whether [raw], an integer as TOML spells it, fits in 64 bits, as TOML asks of every integer. */
private fun fitsInLong(raw: String): Boolean {
    val digits = raw.replace("_", "")
    val radix = when {
        digits.startsWith("0x") -> HEX_RADIX
        digits.startsWith("0o") -> OCTAL_RADIX
        digits.startsWith("0b") -> BINARY_RADIX
        else -> DECIMAL_RADIX
    }
    val unsigned = if (radix == DECIMAL_RADIX) digits.removePrefix("+") else digits.substring(PREFIX_LENGTH)
    return unsigned.toLongOrNull(radix) != null
}
