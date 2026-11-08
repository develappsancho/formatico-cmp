package com.developersancho.formatico.core.format.json

import com.developersancho.formatico.core.format.text.Indentation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JsonPrinterTest {
    private fun format(text: String, dialect: JsonDialect = JsonDialect.Json, sortKeys: Boolean = false): String =
        JsonDocument.parse(text, dialect).formatted(sortKeys = sortKeys)

    @Test
    fun aValueGoesOnALineOfItsOwnAndKeepsItsSpelling() {
        val formatted = format("""{"total":129.90,"big":1E3,"items":[1,{"a":null}],"empty":{},"none":[],"s":"ç"}""")
        val expected = """
            {
              "total": 129.90,
              "big": 1E3,
              "items": [
                1,
                {
                  "a": null
                }
              ],
              "empty": {},
              "none": [],
              "s": "ç"
            }
        """.trimIndent()
        assertEquals(expected, formatted)
    }

    @Test
    fun formattingWhatIsFormattedChangesNothing() {
        val once = format("""[{"b": 2, "a": [true, false]}, "x"]""")
        assertEquals(once, format(once))
    }

    @Test
    fun theIndentationAndTheLineEndingsAreTheDocuments() {
        val document = JsonDocument.parse("{\r\n\"a\": [1]\r\n}\r\n")
        assertEquals("{\r\n\t\"a\": [\r\n\t\t1\r\n\t]\r\n}\r\n", document.formatted(Indentation.Tabs))
        assertEquals("{\n    \"a\": 1\n}", JsonDocument.parse("""{"a":1}""").formatted(Indentation.Spaces(4)))
    }

    @Test
    fun commentsStayWithTheValuesTheyAreWrittenAgainstAndATrailingCommaStays() {
        val text = """
            // The settings
            {
              // Leading
              "a": 1, // after a
              "b": /* inner */ 2,
              "c": [ /* first */ 3, 4 /* after 4 */ ],
              // left over
            } // the end
        """.trimIndent()
        val expected = """
            // The settings
            {
              // Leading
              "a": 1, // after a
              "b": /* inner */ 2,
              "c": [
                /* first */
                3,
                4 /* after 4 */
              ],
              // left over
            } // the end
        """.trimIndent()
        assertEquals(expected, format(text, JsonDialect.Jsonc))
    }

    @Test
    fun aLineCommentBetweenAKeyAndItsValueGoesBeforeTheMember() {
        assertEquals("{\n  // why\n  \"a\": 1\n}", format("{\"a\": // why\n 1}", JsonDialect.Jsonc))
    }

    @Test
    fun aTrailingCommaStaysOnTheLastEntryWhenTheKeysAreSorted() {
        assertEquals(
            "[\n  {\n    b: 1,\n    c: 2,\n  },\n]",
            format("[{c: 2, b: 1,},]", JsonDialect.Json5, sortKeys = true)
        )
    }

    @Test
    fun anEmptyContainerKeepsItsComment() {
        assertEquals("{\n  // nothing yet\n}", format("{ // nothing yet\n}", JsonDialect.Jsonc))
    }

    @Test
    fun sortingTheKeysTakesEachMembersCommentsAlong() {
        val text = """
            {
              "zebra": {"y": 1, "x": 2}, // stripes
              // fruit
              "apple": [{"d": 1, "c": 2}]
            }
        """.trimIndent()
        val expected = """
            {
              // fruit
              "apple": [
                {
                  "c": 2,
                  "d": 1
                }
              ],
              "zebra": {
                "x": 2,
                "y": 1
              } // stripes
            }
        """.trimIndent()
        assertEquals(expected, format(text, JsonDialect.Jsonc, sortKeys = true))
    }

    @Test
    fun json5KeepsItsOwnSpelling() {
        val formatted = format("{unquoted: 'single', hex: 0x1F, half: .5,}", JsonDialect.Json5)
        assertEquals("{\n  unquoted: 'single',\n  hex: 0x1F,\n  half: .5,\n}", formatted)
    }

    @Test
    fun minifyingLeavesNoSpaceAndNoComment() {
        val document = JsonDocument.parse(
            "{\n  \"a\": [1, 2.50], // note\n  \"b\": {\"c\": \"d e\"},\n}\n",
            JsonDialect.Jsonc
        )
        assertEquals("""{"a":[1,2.50],"b":{"c":"d e"}}""", document.minified())
    }

    @Test
    fun aDocumentThatIsNotValidIsNotWrittenOut() {
        val document = JsonDocument.parse("""{"a": }""")
        assertFailsWith<IllegalArgumentException> { document.formatted() }
        assertFailsWith<IllegalArgumentException> { document.minified() }
    }

    @Test
    fun whatIsWrittenOutReadsAsTheSameValues() {
        val text = """{"name": "Elif", "tags": ["a", "b"], "nested": {"deep": [[1], [2, 3]]}, "flag": false}"""
        for (output in listOf(format(text), JsonDocument.parse(text).minified(), format(text, sortKeys = true))) {
            val again = JsonDocument.parse(output)
            assertTrue(again.isValid, output)
            assertEquals(JsonDocument.parse(text).minified().length, again.minified().length)
        }
    }
}
