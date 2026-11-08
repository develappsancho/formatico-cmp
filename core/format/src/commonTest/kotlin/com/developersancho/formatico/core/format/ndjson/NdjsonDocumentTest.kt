package com.developersancho.formatico.core.format.ndjson

import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.json.JsonNumber
import com.developersancho.formatico.core.format.json.JsonObject
import com.developersancho.formatico.core.format.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NdjsonDocumentTest {
    private val events =
        """
        {"at": "2026-10-29T09:00:12Z", "event": "open", "bytes": 1200}
        {"at":"2026-10-29T09:01:12Z","event":"search","bytes":1237}

        {"event": "format", "at": "2026-10-29T09:02:12Z", "bytes": 1274}
        """.trimIndent() + "\n"

    @Test
    fun eachLineIsARecordAndABlankLineIsNone() {
        val document = NdjsonDocument.parse(events)

        assertTrue(document.isValid)
        assertEquals(3, document.records.size)
        assertEquals(TextRange(0, events.indexOf('\n')), document.records[0].range)
        // A record's ranges count from its own line.
        val third = document.records[2]
        val bytes = (third.value as JsonObject).members.last().value as JsonNumber
        assertEquals("1274", events.substring(third.range.start + bytes.range.start, third.range.start + bytes.range.end))
        assertEquals(events, document.print())
    }

    @Test
    fun aProblemIsSaidAtItsLineInTheWholeText() {
        val text = "{\"a\": 1}\n{\"b\": 2,}\n{\"c\": \n"

        val document = NdjsonDocument.parse(text)

        assertFalse(document.isValid)
        val (comma, value) = document.problems
        assertEquals(ProblemCode.TrailingComma, comma.code)
        assertEquals(2, comma.start.line)
        assertEquals(8, comma.start.column)
        assertEquals(3, value.start.line)
        assertEquals(text, document.print())
    }

    @Test
    fun formatWritesARecordToALineSpacedAndMinifyWithNoRoom() {
        val document = NdjsonDocument.parse(events)

        assertEquals(
            """
            {"at": "2026-10-29T09:00:12Z", "event": "open", "bytes": 1200}
            {"at": "2026-10-29T09:01:12Z", "event": "search", "bytes": 1237}
            {"event": "format", "at": "2026-10-29T09:02:12Z", "bytes": 1274}
            """.trimIndent() + "\n",
            document.formatted()
        )
        assertEquals(
            """{"at":"2026-10-29T09:00:12Z","event":"open","bytes":1200}""",
            document.minified().lines().first()
        )
        assertEquals(
            """{"at": "2026-10-29T09:02:12Z", "bytes": 1274, "event": "format"}""",
            document.formatted(sortKeys = true).lines()[2]
        )
    }

    @Test
    fun theLineEndingAndTheLastLineBreakAreKept() {
        val document = NdjsonDocument.parse("{\"a\":1}\r\n{\"b\":[1,2]}")

        assertEquals("{\"a\": 1}\r\n{\"b\": [1, 2]}", document.formatted())
        assertTrue(NdjsonDocument.parse(document.minified()).isValid)
    }

    @Test
    fun aDocumentWithErrorsIsNotWrittenOut() {
        assertFailsWith<IllegalArgumentException> { NdjsonDocument.parse("{\"a\": }\n").formatted() }
    }
}
