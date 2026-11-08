package com.developersancho.formatico.core.format.json

import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.Severity
import com.developersancho.formatico.core.format.text.TextRange

/**
 * Builds a document's values from its tokens, and goes on past a mistake: every problem of the text is reported, and
 * what can be read of it is read.
 */
internal class JsonParser(
    private val text: String,
    private val dialect: JsonDialect,
    tokens: List<JsonToken>,
    private val features: MutableSet<JsonFeature>,
    private val report: (code: ProblemCode, severity: Severity, start: Int, end: Int) -> Unit
) {
    private val tokens: List<JsonToken> = tokens.filterNot { it.kind.isTrivia }
    private var index = 0
    private var lastEnd = 0

    private val current: JsonToken get() = tokens[index]

    private fun advance(): JsonToken {
        val token = tokens[index]
        if (token.kind != JsonTokenKind.EndOfFile) {
            index++
            lastEnd = token.end
        }
        return token
    }

    private fun error(code: ProblemCode, token: JsonToken) = report(code, Severity.Error, token.start, token.end)

    fun parseDocument(): JsonValue? {
        if (current.kind == JsonTokenKind.EndOfFile) {
            error(ProblemCode.ExpectedValue, current)
            return null
        }
        val value = parseValue(depth = 1)
        if (current.kind != JsonTokenKind.EndOfFile) {
            val lastToken = tokens[tokens.size - 2]
            report(ProblemCode.TrailingContent, Severity.Error, current.start, lastToken.end)
        }
        return value
    }

    private fun parseValue(depth: Int): JsonValue? {
        val token = current
        return when (token.kind) {
            JsonTokenKind.LeftBrace -> if (depth > MAX_DEPTH) skipTooDeep() else parseObject(depth)

            JsonTokenKind.LeftBracket -> if (depth > MAX_DEPTH) skipTooDeep() else parseArray(depth)

            JsonTokenKind.String -> JsonString(advance().range, text.substring(token.start, token.end))

            JsonTokenKind.Number -> JsonNumber(advance().range, text.substring(token.start, token.end))

            JsonTokenKind.True -> JsonBoolean(advance().range, value = true)

            JsonTokenKind.False -> JsonBoolean(advance().range, value = false)

            JsonTokenKind.Null -> JsonNull(advance().range)

            // The lexer has said what is wrong with an error token already.
            JsonTokenKind.Error -> {
                advance()
                null
            }

            JsonTokenKind.Identifier, JsonTokenKind.Colon -> {
                error(ProblemCode.UnexpectedToken, advance())
                null
            }

            else -> {
                error(ProblemCode.ExpectedValue, token)
                null
            }
        }
    }

    private fun parseObject(depth: Int): JsonObject {
        val open = advance()
        val members = ArrayList<JsonMember>()
        val names = HashSet<String>()
        while (true) {
            when (current.kind) {
                JsonTokenKind.RightBrace -> return JsonObject(TextRange(open.start, advance().end), members)

                // A bracket of the other kind is the end of something around this object: it is left to that.
                JsonTokenKind.EndOfFile, JsonTokenKind.RightBracket -> {
                    error(ProblemCode.Unclosed, open)
                    return JsonObject(TextRange(open.start, lastEnd), members)
                }

                else -> Unit
            }
            val key = parseKey(depth) ?: continue
            val hasColon = current.kind == JsonTokenKind.Colon
            if (hasColon) advance() else error(ProblemCode.ExpectedColon, current)
            val value = if (hasColon || startsValue(current.kind)) parseValue(depth + 1) else null
            if (!names.add(key.name)) report(ProblemCode.DuplicateKey, Severity.Warning, key.range.start, key.range.end)
            members += JsonMember(TextRange(key.range.start, value?.range?.end ?: key.range.end), key, value)
            separator(closer = JsonTokenKind.RightBrace)
        }
    }

    private fun parseKey(depth: Int): JsonKey? {
        val token = current
        return when (token.kind) {
            JsonTokenKind.String -> JsonKey(advance().range, text.substring(token.start, token.end), quoted = true)

            JsonTokenKind.Identifier, JsonTokenKind.True, JsonTokenKind.False, JsonTokenKind.Null -> {
                advance()
                features += JsonFeature.Json5Syntax
                if (!dialect.isJson5) error(ProblemCode.UnquotedPropertyName, token)
                JsonKey(token.range, text.substring(token.start, token.end), quoted = false)
            }

            JsonTokenKind.Error -> {
                advance()
                null
            }

            else -> {
                error(ProblemCode.ExpectedPropertyName, token)
                if (startsValue(token.kind)) parseValue(depth + 1) else advance()
                // `{1: 2}`: what stands in the key's place is reported once, and its value read past.
                if (current.kind == JsonTokenKind.Colon) {
                    advance()
                    parseValue(depth + 1)
                    separator(closer = JsonTokenKind.RightBrace)
                }
                null
            }
        }
    }

    private fun parseArray(depth: Int): JsonArray {
        val open = advance()
        val items = ArrayList<JsonValue>()
        while (true) {
            when (current.kind) {
                JsonTokenKind.RightBracket -> return JsonArray(TextRange(open.start, advance().end), items)

                JsonTokenKind.EndOfFile, JsonTokenKind.RightBrace -> {
                    error(ProblemCode.Unclosed, open)
                    return JsonArray(TextRange(open.start, lastEnd), items)
                }

                JsonTokenKind.Comma -> {
                    error(ProblemCode.ExpectedValue, advance())
                    continue
                }

                else -> Unit
            }
            parseValue(depth + 1)?.let { items += it }
            separator(closer = JsonTokenKind.RightBracket)
        }
    }

    /** What follows an item: a comma, the end of its container, or a mistake the next round reads past. */
    private fun separator(closer: JsonTokenKind) {
        when (current.kind) {
            JsonTokenKind.Comma -> {
                val comma = advance()
                if (current.kind == closer) {
                    features += JsonFeature.TrailingCommas
                    if (!dialect.allowsTrailingCommas) error(ProblemCode.TrailingComma, comma)
                }
            }

            JsonTokenKind.RightBrace, JsonTokenKind.RightBracket, JsonTokenKind.EndOfFile -> Unit

            else -> error(ProblemCode.ExpectedCommaOrEnd, current)
        }
    }

    private fun startsValue(kind: JsonTokenKind): Boolean = when (kind) {
        JsonTokenKind.LeftBrace, JsonTokenKind.LeftBracket, JsonTokenKind.String, JsonTokenKind.Number,
        JsonTokenKind.True, JsonTokenKind.False, JsonTokenKind.Null -> true

        else -> false
    }

    /** Reads past a container nested deeper than [MAX_DEPTH], to its end. */
    private fun skipTooDeep(): JsonValue? {
        error(ProblemCode.TooDeep, current)
        var nesting = 0
        while (current.kind != JsonTokenKind.EndOfFile) {
            when (advance().kind) {
                JsonTokenKind.LeftBrace, JsonTokenKind.LeftBracket -> nesting++

                JsonTokenKind.RightBrace, JsonTokenKind.RightBracket -> {
                    nesting--
                    if (nesting == 0) break
                }

                else -> Unit
            }
        }
        return null
    }

    companion object {
        /** How deep values are read; a container deeper than this is reported and read past. */
        const val MAX_DEPTH = 512
    }
}
