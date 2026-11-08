package com.developersancho.formatico.core.format.json

import com.developersancho.formatico.core.format.ProblemCode

private const val UNICODE_ESCAPE_DIGITS = 4
private const val HEX_ESCAPE_DIGITS = 2

private val JsonNumberPattern = Regex("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?")
private val Json5NumberPattern = Regex(
    "[+-]?(?:Infinity|NaN|0[xX][0-9a-fA-F]+|(?:(?:0|[1-9][0-9]*)(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?)"
)

/**
 * Cuts a JSON text into tokens, its whitespace and comments among them, so that the tokens are the whole text.
 *
 * It reads every dialect's syntax and reports what [dialect] does not allow, so that a document read as JSON says
 * where it has a comment rather than failing on it. What a text uses beyond JSON goes into [features].
 */
@Suppress("TooManyFunctions") // A lexer reads each kind of token in a function of its own.
internal class JsonLexer(
    private val text: String,
    private val dialect: JsonDialect,
    private val features: MutableSet<JsonFeature>,
    private val report: (code: ProblemCode, start: Int, end: Int) -> Unit
) {
    private var position = 0

    fun tokenize(): List<JsonToken> {
        val tokens = ArrayList<JsonToken>()
        while (position < text.length) {
            val start = position
            val kind = nextKind()
            tokens += JsonToken(kind, start, position)
        }
        tokens += JsonToken(JsonTokenKind.EndOfFile, text.length, text.length)
        return tokens
    }

    private fun nextKind(): JsonTokenKind {
        val char = text[position]
        return when {
            isWhitespace(char, position) -> whitespace()
            char == '{' -> single(JsonTokenKind.LeftBrace)
            char == '}' -> single(JsonTokenKind.RightBrace)
            char == '[' -> single(JsonTokenKind.LeftBracket)
            char == ']' -> single(JsonTokenKind.RightBracket)
            char == ':' -> single(JsonTokenKind.Colon)
            char == ',' -> single(JsonTokenKind.Comma)
            char == '"' || char == '\'' -> string(char)
            char == '/' && peek(1) == '/' -> lineComment()
            char == '/' && peek(1) == '*' -> blockComment()
            char == '-' || char == '+' || char == '.' || char.isDigit() -> number()
            isIdentifierStart(char) -> word()
            else -> unexpected()
        }
    }

    private fun peek(ahead: Int): Char? = text.getOrNull(position + ahead)

    private fun single(kind: JsonTokenKind): JsonTokenKind {
        position++
        return kind
    }

    private fun isWhitespace(char: Char, at: Int): Boolean = when (char) {
        ' ', '\t', '\n', '\r' -> true

        // A byte order mark that opens a file is read past in every dialect.
        '\uFEFF' -> at == 0 || dialect.isJson5

        '\u000B', '\u000C', '\u00A0', '\u2028', '\u2029' -> dialect.isJson5

        else -> dialect.isJson5 && char.category == CharCategory.SPACE_SEPARATOR
    }

    private fun whitespace(): JsonTokenKind {
        while (position < text.length && isWhitespace(text[position], position)) position++
        return JsonTokenKind.Whitespace
    }

    private fun lineComment(): JsonTokenKind {
        val start = position
        while (position < text.length && text[position] != '\n' && text[position] != '\r') position++
        comment(start)
        return JsonTokenKind.LineComment
    }

    private fun blockComment(): JsonTokenKind {
        val start = position
        val close = text.indexOf("*/", startIndex = position + 2)
        if (close < 0) {
            position = text.length
            report(ProblemCode.UnterminatedComment, start, position)
        } else {
            position = close + 2
        }
        comment(start)
        return JsonTokenKind.BlockComment
    }

    private fun comment(start: Int) {
        features += JsonFeature.Comments
        if (!dialect.allowsComments) report(ProblemCode.CommentNotAllowed, start, position)
    }

    private fun string(quote: Char): JsonTokenKind {
        val start = position
        position++
        while (true) {
            if (position >= text.length) {
                report(ProblemCode.UnterminatedString, start, position)
                break
            }
            val char = text[position]
            when {
                char == quote -> {
                    position++
                    break
                }

                char == '\n' || char == '\r' -> {
                    report(ProblemCode.UnterminatedString, start, position)
                    break
                }

                char == '\\' -> {
                    escape()
                }

                char < ' ' -> {
                    controlCharacter(char)
                }

                else -> {
                    position++
                }
            }
        }
        if (quote == '\'') {
            features += JsonFeature.Json5Syntax
            if (!dialect.isJson5) report(ProblemCode.SingleQuotedString, start, position)
        }
        return JsonTokenKind.String
    }

    /** A control character in a string, which only JSON5 allows, and only a tab. */
    private fun controlCharacter(char: Char) {
        if (!(dialect.isJson5 && char == '\t')) report(ProblemCode.ControlCharacterInString, position, position + 1)
        position++
    }

    private fun escape() {
        val start = position
        position++
        if (position >= text.length) {
            report(ProblemCode.InvalidEscape, start, position)
            return
        }
        when (text[position]) {
            '"', '\\', '/', 'b', 'f', 'n', 'r', 't' -> position++
            'u' -> hexEscape(start, UNICODE_ESCAPE_DIGITS, json5Only = false)
            else -> json5Escape(start)
        }
    }

    private fun hexEscape(start: Int, digits: Int, json5Only: Boolean) {
        position++
        var read = 0
        while (read < digits && position < text.length && text[position].isHexDigit()) {
            position++
            read++
        }
        if (read < digits) {
            report(ProblemCode.InvalidEscape, start, position)
        } else if (json5Only) {
            json5Spelling(start)
        }
    }

    /** An escape JSON5 has and JSON does not: `\'`, `\v`, `\0`, `\x41`, a line continuation, `\a` for `a`. */
    private fun json5Escape(start: Int) {
        val char = text[position]
        when {
            char == 'x' -> {
                hexEscape(start, HEX_ESCAPE_DIGITS, json5Only = true)
            }

            char in '1'..'9' || (char == '0' && peek(1)?.isDigit() == true) -> {
                position++
                report(ProblemCode.InvalidEscape, start, position)
            }

            char == '\n' || char == '\r' || char == '\u2028' || char == '\u2029' -> {
                if (!dialect.isJson5) {
                    // Not a continuation in JSON: the string ends at the line break.
                    report(ProblemCode.InvalidEscape, start, position)
                    features += JsonFeature.Json5Syntax
                    return
                }
                position += if (char == '\r' && peek(1) == '\n') 2 else 1
                features += JsonFeature.Json5Syntax
            }

            else -> {
                position++
                json5Spelling(start)
            }
        }
    }

    private fun json5Spelling(start: Int) {
        features += JsonFeature.Json5Syntax
        if (!dialect.isJson5) report(ProblemCode.InvalidEscape, start, position)
    }

    private fun number(): JsonTokenKind {
        val start = position
        position++
        while (position < text.length) {
            val char = text[position]
            // A sign belongs to the number after an exponent's `e`, or after another sign: `--1` is one bad number.
            val sign = (char == '+' || char == '-') && text[position - 1] in "eE+-"
            if (char.isLetterOrDigit() || char == '.' || sign) position++ else break
        }
        checkNumber(start)
        return JsonTokenKind.Number
    }

    private fun checkNumber(start: Int) {
        val spelling = text.substring(start, position)
        when {
            JsonNumberPattern.matches(spelling) -> Unit
            Json5NumberPattern.matches(spelling) -> json5Number(start)
            else -> report(ProblemCode.InvalidNumber, start, position)
        }
    }

    private fun json5Number(start: Int) {
        features += JsonFeature.Json5Syntax
        if (!dialect.isJson5) report(ProblemCode.InvalidNumber, start, position)
    }

    private fun isIdentifierStart(char: Char): Boolean = char.isLetter() || char == '$' || char == '_'

    private fun isIdentifierPart(char: Char): Boolean = char.isLetterOrDigit() || char == '$' || char == '_'

    private fun word(): JsonTokenKind {
        val start = position
        while (position < text.length && isIdentifierPart(text[position])) position++
        return when (text.substring(start, position)) {
            "true" -> {
                JsonTokenKind.True
            }

            "false" -> {
                JsonTokenKind.False
            }

            "null" -> {
                JsonTokenKind.Null
            }

            "Infinity", "NaN" -> {
                json5Number(start)
                JsonTokenKind.Number
            }

            else -> {
                JsonTokenKind.Identifier
            }
        }
    }

    private fun unexpected(): JsonTokenKind {
        val start = position
        val high = text[position]
        position++
        if (high.isHighSurrogate() && position < text.length && text[position].isLowSurrogate()) position++
        report(ProblemCode.UnexpectedCharacter, start, position)
        return JsonTokenKind.Error
    }
}
