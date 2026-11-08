package com.developersancho.formatico.core.format.json

import com.developersancho.formatico.core.format.Problem
import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.Severity
import com.developersancho.formatico.core.format.text.LineMap
import com.developersancho.formatico.core.format.text.TextRange

/**
 * A JSON text as it was read: its tokens, which are the whole text, its comments and whitespace among them; its
 * value, which keeps the order of keys and the spelling of numbers; and its problems, each with its line and column.
 *
 * Nothing of the text is lost: [print] gives it back as it was written.
 *
 * @property text the text read.
 * @property dialect the JSON it was read as.
 * @property tokens every token of [text], in order, ending with [JsonTokenKind.EndOfFile].
 * @property root the document's value, or null when none can be read.
 * @property problems what is wrong in it, in the order of the text.
 * @property features what it uses beyond RFC 8259, allowed by [dialect] or not.
 */
class JsonDocument private constructor(
    val text: String,
    val dialect: JsonDialect,
    val tokens: List<JsonToken>,
    val root: JsonValue?,
    val problems: List<Problem>,
    val features: Set<JsonFeature>
) {
    /** Whether the document has no error; a warning leaves it valid. */
    val isValid: Boolean get() = problems.none { it.severity == Severity.Error }

    /** The comments of the document, in order. */
    val comments: List<JsonToken>
        get() = tokens.filter { it.kind == JsonTokenKind.LineComment || it.kind == JsonTokenKind.BlockComment }

    /** The text of [range]. */
    fun text(range: TextRange): String = text.substring(range.start, range.end)

    /** The document written out from its tokens: the text it was read from, character for character. */
    fun print(): String = buildString(text.length) {
        tokens.forEach { append(text, it.start, it.end) }
    }

    companion object {
        /** Reads [text] as [dialect]. */
        fun parse(text: String, dialect: JsonDialect = JsonDialect.Json): JsonDocument {
            val found = ArrayList<FoundProblem>()
            val features = mutableSetOf<JsonFeature>()
            val tokens = JsonLexer(text, dialect, features) { code, start, end ->
                found += FoundProblem(code, Severity.Error, start, end)
            }.tokenize()
            val root = JsonParser(text, dialect, tokens, features) { code, severity, start, end ->
                found += FoundProblem(code, severity, start, end)
            }.parseDocument()
            val lines = LineMap(text)
            val problems = found.sortedBy { it.start }.map {
                Problem(
                    code = it.code,
                    severity = it.severity,
                    range = TextRange(it.start, it.end),
                    start = lines.position(it.start),
                    end = lines.position(it.end)
                )
            }
            return JsonDocument(text, dialect, tokens, root, problems, features)
        }
    }

    private class FoundProblem(val code: ProblemCode, val severity: Severity, val start: Int, val end: Int)
}
