package com.developersancho.formatico.core.format.toml

import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.text.TextRange

/** What stands where a key's part should that is TOML's own syntax, not a stray character. */
private const val NOT_A_KEY = "={}[],.#+"

private const val MULTI_LINE_BASIC = "\"\"\""
private const val MULTI_LINE_LITERAL = "'''"

/**
 * Reads values as TOML writes them, and key/values: strings, numbers, booleans, dates and times, arrays and inline
 * tables. What cannot be read is reported and gives null.
 */
internal class TomlValueReader(private val scanner: TomlScanner) {
    private val strings = TomlStringReader(scanner)
    private val scalars = TomlScalarReader(scanner)
    private val arrays = TomlArrayReader(scanner, this)
    private val inlineTables = TomlInlineTableReader(scanner, this)

    /** Reads keys, of key/values and of headers. */
    val keys = TomlKeyReader(scanner, strings)

    /** Reads a key, its `=` and its value, [depth] deep in arrays and inline tables. */
    fun keyValue(depth: Int): TomlKeyValue? {
        val key = keys.read(depth) ?: return null
        scanner.skipSpaces()
        if (scanner.current != '=') {
            val at = scanner.position
            scanner.report(ProblemCode.TomlExpectedEquals, at, if (scanner.atEnd || scanner.atLineBreak) at else at + 1)
            return null
        }
        scanner.position++
        scanner.skipSpaces()
        val value = value(depth) ?: return null
        return TomlKeyValue(key, value)
    }

    /** Reads the value that starts where the scanner is, [depth] deep in arrays and inline tables. */
    fun value(depth: Int): TomlValueSyntax? {
        val start = scanner.position
        val char = scanner.current
        return when {
            char == null || char == '#' || scanner.atLineBreak -> {
                scanner.report(ProblemCode.ExpectedValue, start, start)
                null
            }

            (char == '[' || char == '{') && depth >= TOML_MAX_DEPTH -> {
                tooDeep()
            }

            char == '"' || char == '\'' -> {
                strings.read()?.let(::TomlScalarSyntax)
            }

            char == '[' -> {
                arrays.read(depth + 1)
            }

            char == '{' -> {
                inlineTables.read(depth + 1)
            }

            else -> {
                scalars.read()?.let(::TomlScalarSyntax)
            }
        }
    }

    /** Reports a value nested deeper than [TOML_MAX_DEPTH], and reads past the rest of its line. */
    private fun tooDeep(): TomlValueSyntax? {
        scanner.report(ProblemCode.TooDeep, scanner.position, scanner.position + 1)
        scanner.position = scanner.lineEnd()
        return null
    }
}

/** Reads keys: bare, `"basic"` and `'literal'` parts, and dotted keys of them, with spaces around the dots or none. */
internal class TomlKeyReader(private val scanner: TomlScanner, private val strings: TomlStringReader) {
    /**
     * Reads the key that starts where the scanner is, [depth] deep in inline tables, and stops after its last part; null
     * where a part cannot be read, which is reported.
     */
    fun read(depth: Int = 0): TomlKey? {
        val parts = ArrayList<TomlKeyPart>()
        while (true) {
            if (depth + parts.size >= TOML_MAX_DEPTH) {
                scanner.report(ProblemCode.TooDeep, scanner.position, scanner.position + 1)
                return null
            }
            parts += part() ?: return null
            val end = scanner.position
            scanner.skipSpaces()
            if (scanner.current != '.') {
                scanner.position = end
                return TomlKey(parts)
            }
            scanner.position++
            scanner.skipSpaces()
        }
    }

    /** Reads a part of a key, bare or quoted; null where none stands, which is reported. */
    private fun part(): TomlKeyPart? {
        val start = scanner.position
        val char = scanner.current
        return when {
            char != null && isBareKeyCharacter(char) -> {
                bare(start)
            }

            char == '"' || char == '\'' -> {
                quoted(start)
            }

            else -> {
                missing(start, char)
                null
            }
        }
    }

    /** Reads a bare part, whose name is as it is written. */
    private fun bare(start: Int): TomlKeyPart {
        while (scanner.current?.let(::isBareKeyCharacter) == true) scanner.position++
        val raw = scanner.text.substring(start, scanner.position)
        return TomlKeyPart(TextRange(start, scanner.position), raw, raw)
    }

    /** A quoted part; a multi-line string, which is no key, is read past and reported. */
    private fun quoted(start: Int): TomlKeyPart? {
        val multiLine = scanner.text.startsWith(MULTI_LINE_BASIC, start) || scanner.text.startsWith(MULTI_LINE_LITERAL, start)
        val string = strings.read() ?: return null
        if (multiLine) {
            scanner.report(ProblemCode.ExpectedPropertyName, start, start + MULTI_LINE_BASIC.length)
            return null
        }
        return TomlKeyPart(string.range, string.raw, string.value)
    }

    /** Reports what stands where a part should: TOML's own syntax, or a character it has no use for. */
    private fun missing(start: Int, char: Char?) {
        val atLineEnd = char == null || scanner.atLineBreak
        val code = if (char == null || atLineEnd || char in NOT_A_KEY) {
            ProblemCode.ExpectedPropertyName
        } else {
            ProblemCode.UnexpectedCharacter
        }
        scanner.report(code, start, if (atLineEnd) start else start + 1)
    }
}
