package com.developersancho.formatico.core.format.toml

import com.developersancho.formatico.core.format.Problem
import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TomlErrorsTest {
    /** The problems of [text], each as its code, its line and its column. */
    private fun problemsOf(text: String): List<Triple<ProblemCode, Int, Int>> =
        TomlDocument.parse(text).problems.map { Triple(it.code, it.start.line, it.start.column) }

    private fun only(text: String): Problem = TomlDocument.parse(text).problems.single()

    @Test
    fun eachMistakeIsReportedWithItsLineAndColumn() {
        val cases = listOf(
            "\$a = 1" to Triple(ProblemCode.UnexpectedCharacter, 1, 1),
            "a = 1 # bell \u0007" to Triple(ProblemCode.UnexpectedCharacter, 1, 14),
            "a = \"abc" to Triple(ProblemCode.UnterminatedString, 1, 5),
            "a = 1\nb = '''never closed\nc = 2" to Triple(ProblemCode.UnterminatedString, 2, 5),
            "a = \"\\x41\"" to Triple(ProblemCode.InvalidEscape, 1, 6),
            "a = \"\\uD800\"" to Triple(ProblemCode.InvalidEscape, 1, 6),
            "a = \"\\U00110000\"" to Triple(ProblemCode.InvalidEscape, 1, 6),
            "a = \"\\u12\"" to Triple(ProblemCode.InvalidEscape, 1, 6),
            "a = \"x\u0001y\"" to Triple(ProblemCode.ControlCharacterInString, 1, 7),
            "a = 01" to Triple(ProblemCode.InvalidNumber, 1, 5),
            "a = 1__0" to Triple(ProblemCode.InvalidNumber, 1, 5),
            "a = 3." to Triple(ProblemCode.InvalidNumber, 1, 5),
            "a = 0x8000000000000000" to Triple(ProblemCode.InvalidNumber, 1, 5),
            "a =" to Triple(ProblemCode.ExpectedValue, 1, 4),
            "a = hello" to Triple(ProblemCode.ExpectedValue, 1, 5),
            "= 1" to Triple(ProblemCode.ExpectedPropertyName, 1, 1),
            "a. = 1" to Triple(ProblemCode.ExpectedPropertyName, 1, 4),
            "a = [1 2]" to Triple(ProblemCode.ExpectedCommaOrEnd, 1, 8),
            "a = { b = 1 c = 2 }" to Triple(ProblemCode.ExpectedCommaOrEnd, 1, 13),
            "a = [1, 2" to Triple(ProblemCode.Unclosed, 1, 5),
            "[table" to Triple(ProblemCode.Unclosed, 1, 1),
            "[[items]" to Triple(ProblemCode.Unclosed, 1, 1),
            "a = 1 2" to Triple(ProblemCode.TrailingContent, 1, 7),
            "[a] b" to Triple(ProblemCode.TrailingContent, 1, 5),
            "a = { b = 1, }" to Triple(ProblemCode.TrailingComma, 1, 12),
            "a = 1\na = 2" to Triple(ProblemCode.DuplicateKey, 2, 1),
            "a 1" to Triple(ProblemCode.TomlExpectedEquals, 1, 3),
            "[a]\nb" to Triple(ProblemCode.TomlExpectedEquals, 2, 2),
            "[a]\n[a]" to Triple(ProblemCode.TomlTableRedefined, 2, 2),
            "d = 2026-02-30" to Triple(ProblemCode.TomlInvalidDateTime, 1, 5),
            "t = 24:00:00" to Triple(ProblemCode.TomlInvalidDateTime, 1, 5),
            "t = 07:32" to Triple(ProblemCode.TomlInvalidDateTime, 1, 5),
            "d = 1979-05-27T07:32:00+25:00" to Triple(ProblemCode.TomlInvalidDateTime, 1, 5),
            "a = { b = 1,\n  c = 2 }" to Triple(ProblemCode.TomlInlineTableLineBreak, 1, 13)
        )
        cases.forEach { (text, problem) ->
            assertEquals(listOf(problem), problemsOf(text), "<$text>")
            assertEquals(Severity.Error, only(text).severity)
            assertFalse(TomlDocument.parse(text).isValid)
        }
    }

    @Test
    fun aProblemCoversWhatIsWrong() {
        val number = only("a = 0x")
        assertEquals(4, number.range.start)
        assertEquals(6, number.range.end)
        val string = only("a = \"abc\nb = 1")
        assertEquals(4, string.range.start)
        assertEquals(8, string.range.end)
        val equals = only("key")
        assertEquals(3, equals.range.start)
        assertEquals(3, equals.range.end)
    }

    @Test
    fun readingGoesOnAfterAMistakeSoThatEveryProblemIsFound() {
        val text = toml(
            "a = 01",
            "b = \"never closed",
            "c = 3",
            "d = [1, 2",
            "e = 5",
            "[f",
            "g = 7"
        )
        val document = TomlDocument.parse(text)

        assertEquals(
            listOf(
                Triple(ProblemCode.InvalidNumber, 1, 5),
                Triple(ProblemCode.UnterminatedString, 2, 5),
                Triple(ProblemCode.Unclosed, 4, 5),
                Triple(ProblemCode.Unclosed, 6, 1)
            ),
            problemsOf(text)
        )
        assertEquals(listOf("c", "d", "e", "f"), document.root.entries.map { it.key })
        assertEquals(listOf("1", "2"), (document.root.at("d") as TomlArray).items.map { (it as TomlInteger).raw })
        assertEquals("7", (document.root.at("f", "g") as TomlInteger).raw)
    }

    @Test
    fun anArrayNeverClosedEndsBeforeTheHeaderAfterIt() {
        val text = toml("a = [", "  1,", "  2,", "[server]", "port = 80")
        val document = TomlDocument.parse(text)

        assertEquals(listOf(Triple(ProblemCode.Unclosed, 1, 5)), problemsOf(text))
        assertEquals("80", (document.root.at("server", "port") as TomlInteger).raw)
    }

    @Test
    fun anInlineTableOverLinesIsReadAndReported() {
        val text = toml("a = {", "  b = 1,", "  c = 2", "}", "d = 4")
        val document = TomlDocument.parse(text)

        assertEquals(listOf(Triple(ProblemCode.TomlInlineTableLineBreak, 1, 6)), problemsOf(text))
        assertEquals(listOf("b", "c"), (document.root.at("a") as TomlTable).entries.map { it.key })
        assertEquals("4", (document.root.at("d") as TomlInteger).raw)
    }

    @Test
    fun anInlineTableNeverClosedEndsWithItsLine() {
        val text = toml("a = { b = 1", "c = 2")
        val document = TomlDocument.parse(text)

        assertEquals(listOf(Triple(ProblemCode.Unclosed, 1, 5)), problemsOf(text))
        assertEquals(listOf("a", "c"), document.root.entries.map { it.key })
    }

    @Test
    fun aKeyGivenTwiceAndATableDefinedTwiceAreErrors() {
        val cases = listOf(
            toml("name = \"a\"", "name = \"b\"") to Triple(ProblemCode.DuplicateKey, 2, 1),
            toml("a = 1", "a.b = 2") to Triple(ProblemCode.DuplicateKey, 2, 1),
            toml("a = {}", "a = 1") to Triple(ProblemCode.DuplicateKey, 2, 1),
            toml("a = { b = 1, b = 2 }") to Triple(ProblemCode.DuplicateKey, 1, 14),
            toml("a = [1]", "[[a]]") to Triple(ProblemCode.DuplicateKey, 2, 3),
            toml("[fruit]", "apple = 1", "[fruit]", "pear = 2") to Triple(ProblemCode.TomlTableRedefined, 3, 2),
            toml("a.b = 1", "[a]") to Triple(ProblemCode.TomlTableRedefined, 2, 2),
            toml("a.b = 1", "a = 2") to Triple(ProblemCode.TomlTableRedefined, 2, 1),
            toml("a = { b = 1 }", "a.c = 2") to Triple(ProblemCode.TomlTableRedefined, 2, 1),
            toml("a = { b = 1 }", "[a]") to Triple(ProblemCode.TomlTableRedefined, 2, 2),
            toml("[[a]]", "[a]") to Triple(ProblemCode.TomlTableRedefined, 2, 2),
            toml("[a]", "[[a]]") to Triple(ProblemCode.TomlTableRedefined, 2, 3),
            toml("[a.b]", "[a]", "b = 1") to Triple(ProblemCode.TomlTableRedefined, 3, 1),
            toml("[fruit]", "apple.color = \"red\"", "[fruit.apple]") to Triple(ProblemCode.TomlTableRedefined, 3, 8),
            toml("[a.b.c]", "z = 9", "[a]", "b.c.t = 1") to Triple(ProblemCode.TomlTableRedefined, 4, 1),
            toml("[fruit.physical]", "color = \"red\"", "[[fruit]]") to Triple(ProblemCode.TomlTableRedefined, 3, 3),
            toml("[[fruits]]", "[[fruits.varieties]]", "[fruits.varieties]") to Triple(ProblemCode.TomlTableRedefined, 3, 9),
            toml("[fruits.physical]", "[[fruits.physical]]") to Triple(ProblemCode.TomlTableRedefined, 2, 10)
        )
        cases.forEach { (text, problem) ->
            assertEquals(listOf(problem), problemsOf(text), "<$text>")
        }
    }

    @Test
    fun theKeyValuesUnderAHeaderThatCannotBeAreNotReportedAgain() {
        val text = toml("[a]", "x = 1", "[a]", "x = 2", "[b]", "x = 3")
        val document = TomlDocument.parse(text)

        assertEquals(listOf(Triple(ProblemCode.TomlTableRedefined, 3, 2)), problemsOf(text))
        assertEquals("1", (document.root.at("a", "x") as TomlInteger).raw)
        assertEquals("3", (document.root.at("b", "x") as TomlInteger).raw)
    }

    @Test
    fun aMistakeEndsItsLine() {
        val text = toml("a = = 1 2 3", "b = 2")

        assertEquals(listOf(Triple(ProblemCode.ExpectedValue, 1, 5)), problemsOf(text))
        assertTrue("b" in TomlDocument.parse(text).root.entries.map { it.key })
    }
}
