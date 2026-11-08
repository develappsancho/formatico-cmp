package com.developersancho.formatico.core.format

import com.developersancho.formatico.core.format.csv.CsvDocument
import com.developersancho.formatico.core.format.json.JsonDialect
import com.developersancho.formatico.core.format.json.JsonDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepairsTest {
    /** The Invalid boards' document: a comment, a missing comma, a key in no quotes and a trailing comma. */
    private val settings =
        """
        {
          "name": "formatico-web",
          // turn off before the release
          "hosts": [
            "localhost"
            "127.0.0.1"
          ],
          timeout: 30,
          "features": {
            "ai": false,
          }
        }
        """.trimIndent() + "\n"

    private fun problems(text: String, dialect: JsonDialect = JsonDialect.Json) = JsonDocument.parse(text, dialect).problems

    private fun repaired(text: String, code: ProblemCode, dialect: JsonDialect = JsonDialect.Json): String {
        val problem = problems(text, dialect).first { it.code == code }
        return Repairs.applied(text, requireNotNull(Repairs.of(problem, text)).edits)
    }

    @Test
    fun theBoardsFourProblemsEachHaveTheirRepair() {
        assertEquals(
            listOf(
                ProblemCode.CommentNotAllowed to RepairKind.RemoveComment,
                ProblemCode.ExpectedCommaOrEnd to RepairKind.AddComma,
                ProblemCode.UnquotedPropertyName to RepairKind.QuoteKey,
                ProblemCode.TrailingComma to RepairKind.RemoveComma
            ),
            problems(settings).map { it.code to Repairs.of(it, settings)?.kind }
        )
    }

    @Test
    fun eachRepairChangesWhatItSays() {
        assertTrue("// turn off" !in repaired(settings, ProblemCode.CommentNotAllowed))
        assertTrue(
            "  \"name\": \"formatico-web\",\n  \"hosts\"" in repaired(settings, ProblemCode.CommentNotAllowed),
            "The comment's line goes with it"
        )
        assertTrue("\"localhost\",\n" in repaired(settings, ProblemCode.ExpectedCommaOrEnd))
        assertTrue("  \"timeout\": 30," in repaired(settings, ProblemCode.UnquotedPropertyName))
        assertTrue("\"ai\": false\n" in repaired(settings, ProblemCode.TrailingComma))
    }

    @Test
    fun repairingAllLeavesAValidDocumentThatKeepsItsValues() {
        val all = Repairs.all(settings) { problems(it) }

        assertEquals(emptyList(), problems(all))
        assertTrue("\"timeout\": 30" in all && "\"127.0.0.1\"" in all)
    }

    @Test
    fun theSmallerRepairs() {
        assertEquals("{\"a\": \"it's \\\"so\\\"\"}", repaired("{\"a\": 'it\\'s \"so\"'}", ProblemCode.SingleQuotedString))
        assertEquals("{\"a\": 1}", repaired("{\"a\": 1} x", ProblemCode.TrailingContent).trimEnd())
        assertEquals(
            "[1, 0.5, 10, 1.0]",
            repaired("[1, .5, 10, 1.]", ProblemCode.InvalidNumber).let {
                repaired(it, ProblemCode.InvalidNumber)
            }
        )
        assertEquals("{\"a\": [1]}", Repairs.all("{\"a\": [1") { problems(it) }.trimEnd())
    }

    @Test
    fun aCommentAfterAValueGoesWithTheRoomBeforeIt() {
        assertEquals("{\"a\": 1}\n", repaired("{\"a\": 1} // one\n", ProblemCode.CommentNotAllowed))
    }

    @Test
    fun csvsQuotes() {
        val text = "a,b\n1,say \"hi\n"
        val problem = CsvDocument.parse(text).problems.first { it.code == ProblemCode.StrayQuote }
        assertEquals(RepairKind.DoubleTheQuote, Repairs.of(problem, text)?.kind)
    }

    @Test
    fun aKeyGivenTwiceIsNotRepaired() {
        val text = "{\"a\": 1, \"a\": 2}"
        assertNull(Repairs.of(problems(text).single { it.code == ProblemCode.DuplicateKey }, text))
    }
}
