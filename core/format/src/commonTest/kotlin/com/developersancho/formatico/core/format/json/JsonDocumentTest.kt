package com.developersancho.formatico.core.format.json

import com.developersancho.formatico.core.format.Problem
import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JsonDocumentTest {
    private fun JsonDocument.codes(): List<ProblemCode> = problems.map(Problem::code)

    private fun JsonValue?.asObject(): JsonObject = assertIs<JsonObject>(this)

    @Test
    fun theTokensAreTheWholeText() {
        val texts = listOf(
            "",
            "  ",
            "{}",
            "{\n  \"a\": 1.50, // money\n  \"b\": [true, false, null],\n}\n",
            "\uFEFF[1e3, -0.0, \"\\u00e7\"]",
            "{'a': 0x1F, b: .5, c: +Infinity, /* note */}",
            "{\"broken\": [1, 2",
            "{\"a\" 1 \"b\": }",
            "@#$ ]]"
        )
        for (dialect in JsonDialect.entries) {
            for (text in texts) {
                val document = JsonDocument.parse(text, dialect)
                assertEquals(text, document.print(), "Round trip of <$text> as $dialect")
                document.tokens.zipWithNext().forEach { (before, after) ->
                    assertEquals(before.end, after.start, "No gap between tokens of <$text>")
                }
            }
        }
    }

    @Test
    fun aValidDocumentKeepsItsKeysInOrderAndItsNumbersAsWritten() {
        val document = JsonDocument.parse("""{"zebra": 1.50, "apple": 1e3, "mango": -0}""")
        assertTrue(document.isValid)
        val members = document.root.asObject().members
        assertEquals(listOf("zebra", "apple", "mango"), members.map { it.key.name })
        assertEquals(listOf("1.50", "1e3", "-0"), members.map { assertIs<JsonNumber>(it.value).raw })
    }

    @Test
    fun stringsAreReadWithTheirEscapes() {
        val document = JsonDocument.parse("""["tab\there", "\u00e7ay", "quote \" slash \/"]""")
        val values = assertIs<JsonArray>(document.root).items.map { assertIs<JsonString>(it).value }
        assertEquals(listOf("tab\there", "çay", "quote \" slash /"), values)
    }

    @Test
    fun aProblemSaysItsLineAndColumn() {
        val document = JsonDocument.parse("{\n  \"a\": 1,\n  \"b\" 2\n}")
        val problem = document.problems.single()
        assertEquals(ProblemCode.ExpectedColon, problem.code)
        assertEquals(3, problem.start.line)
        assertEquals(7, problem.start.column)
    }

    @Test
    fun commentsAndTrailingCommasAreErrorsInJsonAndAllowedInJsonc() {
        val text = "{\n  // the port\n  \"port\": 8080,\n}"
        val json = JsonDocument.parse(text, JsonDialect.Json)
        assertEquals(listOf(ProblemCode.CommentNotAllowed, ProblemCode.TrailingComma), json.codes())
        assertEquals(setOf(JsonFeature.Comments, JsonFeature.TrailingCommas), json.features)

        val jsonc = JsonDocument.parse(text, JsonDialect.Jsonc)
        assertTrue(jsonc.isValid)
        assertEquals("// the port", jsonc.text(jsonc.comments.single().range))
    }

    @Test
    fun json5SyntaxIsReadAsJson5AndReportedAsJson() {
        val text = "{unquoted: 'single', hex: 0x1F, half: .5, plus: +1, big: Infinity}"
        val json5 = JsonDocument.parse(text, JsonDialect.Json5)
        assertTrue(json5.isValid, json5.problems.toString())
        assertEquals(
            listOf("unquoted", "hex", "half", "plus", "big"),
            json5.root.asObject().members.map {
                it.key.name
            }
        )

        val json = JsonDocument.parse(text, JsonDialect.Json)
        assertEquals(
            listOf(
                ProblemCode.UnquotedPropertyName,
                ProblemCode.SingleQuotedString,
                ProblemCode.UnquotedPropertyName,
                ProblemCode.InvalidNumber,
                ProblemCode.UnquotedPropertyName,
                ProblemCode.InvalidNumber,
                ProblemCode.UnquotedPropertyName,
                ProblemCode.InvalidNumber,
                ProblemCode.UnquotedPropertyName,
                ProblemCode.InvalidNumber
            ),
            json.codes()
        )
    }

    @Test
    fun numbersJsonDoesNotSpell() {
        for (number in listOf("01", "1.", ".5", "+1", "1e", "--1", "0x")) {
            val document = JsonDocument.parse("[$number]")
            assertEquals(listOf(ProblemCode.InvalidNumber), document.codes(), number)
        }
    }

    @Test
    fun aKeyGivenTwiceIsAWarning() {
        val document = JsonDocument.parse("""{"a": 1, "a": 2}""")
        val problem = document.problems.single()
        assertEquals(ProblemCode.DuplicateKey, problem.code)
        assertEquals(Severity.Warning, problem.severity)
        assertTrue(document.isValid)
        assertEquals(2, document.root.asObject().members.size)
    }

    @Test
    fun readingGoesOnPastAMistake() {
        val document = JsonDocument.parse("""{"a": [1 2], "b": , "c": true}""")
        assertEquals(listOf(ProblemCode.ExpectedCommaOrEnd, ProblemCode.ExpectedValue), document.codes())
        val members = document.root.asObject().members
        assertEquals(listOf("a", "b", "c"), members.map { it.key.name })
        assertEquals(2, assertIs<JsonArray>(members[0].value).items.size)
        assertNull(members[1].value)
        assertIs<JsonBoolean>(members[2].value)
    }

    @Test
    fun anUnclosedContainerIsReportedWhereItOpens() {
        val document = JsonDocument.parse("{\"items\": [1, 2")
        assertEquals(listOf(ProblemCode.Unclosed, ProblemCode.Unclosed), document.codes())
        assertEquals(listOf(0, 10), document.problems.map { it.range.start })
    }

    @Test
    fun brokenStringsAndCharacters() {
        assertEquals(listOf(ProblemCode.UnterminatedString), JsonDocument.parse("[\"open\n]").codes())
        assertEquals(listOf(ProblemCode.InvalidEscape), JsonDocument.parse("[\"\\q\"]").codes())
        assertEquals(listOf(ProblemCode.ControlCharacterInString), JsonDocument.parse("[\"a\tb\"]").codes())
        assertEquals(listOf(ProblemCode.UnexpectedCharacter), JsonDocument.parse("[1, @]").codes().take(1))
        assertEquals(
            listOf(ProblemCode.UnterminatedComment),
            JsonDocument.parse("[] /* open", JsonDialect.Jsonc).codes()
        )
    }

    @Test
    fun anEmptyTextAndTextAfterTheValue() {
        assertEquals(listOf(ProblemCode.ExpectedValue), JsonDocument.parse("   ").codes())
        assertEquals(listOf(ProblemCode.TrailingContent), JsonDocument.parse("{} []").codes())
    }

    @Test
    fun aKeyThatIsNoKeyIsReportedOnce() {
        assertEquals(listOf(ProblemCode.ExpectedPropertyName), JsonDocument.parse("{1: 2}").codes())
    }

    @Test
    fun nestingDeeperThanTheLimitIsReportedAndReadPast() {
        val depth = JsonParser.MAX_DEPTH + 10
        val text = "[".repeat(depth) + "]".repeat(depth) + " "
        val document = JsonDocument.parse(text)
        assertEquals(listOf(ProblemCode.TooDeep), document.codes())
        assertEquals(text, document.print())
    }
}
