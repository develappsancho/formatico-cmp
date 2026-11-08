package com.developersancho.formatico.core.format.properties

import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.Severity
import com.developersancho.formatico.core.format.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PropertiesDocumentTest {
    private val application =
        """
        # The service, as Spring reads it.
        server.port=8080
        spring.application.name = formatico
        greeting: Merhaba, d\u00fcnya
        ! a comment of its own kind
        path\ with\ spaces value\tafter a tab
        list = one, \
               two, \
               three
        empty=
        alone
        """.trimIndent() + "\n"

    @Test
    fun keysAndValuesAreReadAsJavaReadsThem() {
        val document = PropertiesDocument.parse(application)

        assertTrue(document.isValid)
        assertEquals(
            listOf("server.port", "spring.application.name", "greeting", "path with spaces", "list", "empty", "alone"),
            document.entries.map { it.key }
        )
        val values = document.entries.associate { it.key to it.value }
        assertEquals("formatico", values["spring.application.name"])
        assertEquals("Merhaba, dünya", values["greeting"])
        assertEquals("value\tafter a tab", values["path with spaces"])
        assertEquals("one, two, three", values["list"])
        assertEquals("", values["empty"])
        assertEquals("", values["alone"])
        assertEquals(2, document.comments.size)
        assertEquals(application, document.print())
    }

    @Test
    fun anEntryKnowsWhereItsKeyAndItsValueAre() {
        val document = PropertiesDocument.parse(application)
        val list = document.entries.first { it.key == "list" }

        assertEquals("list", application.substring(list.keyRange.start, list.keyRange.end))
        assertEquals("one, \\\n       two, \\\n       three", application.substring(list.valueRange.start, list.valueRange.end))
        assertEquals("one, two, three", list.joined)
        val empty = document.entries.first { it.key == "empty" }
        assertEquals(TextRange(empty.range.end, empty.range.end), empty.valueRange)
    }

    @Test
    fun aKeyGivenTwiceIsAWarningAndABrokenEscapeAnError() {
        val document = PropertiesDocument.parse("a=1\nb=\\u00zz\na=2\n")

        val (escape, duplicate) = document.problems
        assertEquals(ProblemCode.InvalidEscape, escape.code)
        assertEquals(Severity.Error, escape.severity)
        assertEquals(2, escape.start.line)
        assertEquals(3, escape.start.column)
        assertEquals(ProblemCode.DuplicateKey, duplicate.code)
        assertEquals(Severity.Warning, duplicate.severity)
        assertEquals(3, duplicate.start.line)
        assertFalse(document.isValid)
    }

    @Test
    fun formatWritesEveryEntryWithTheFirstOnesSeparator() {
        val text = "\n\n  # Ports\n  http = 80\nhttps:443\n\n\n\nname   formatico\nlist = a, \\\n      b\n\n"

        val formatted = PropertiesDocument.parse(text).formatted()

        assertEquals("# Ports\nhttp = 80\nhttps = 443\n\nname = formatico\nlist = a, \\\n    b\n", formatted)
        assertEquals(PropertiesDocument.parse(text).entries.map { it.value }, PropertiesDocument.parse(formatted).entries.map { it.value })
    }

    @Test
    fun sortingTheKeysTakesTheirCommentsWithThem() {
        val text = "# Settings\n\n# the port\nport=8080\nhost=localhost\n# the name\nname=formatico\n"

        assertEquals(
            "# Settings\n\nhost=localhost\n# the name\nname=formatico\n# the port\nport=8080\n",
            PropertiesDocument.parse(text).formatted(sortKeys = true)
        )
    }

    @Test
    fun minifyLeavesOnlyTheEntriesOnALineEach() {
        val minified = PropertiesDocument.parse(application.replace("\n", "\r\n")).minified()

        assertEquals("server.port=8080\r\nspring.application.name=formatico\r\n", minified.substringBefore("greeting"))
        assertTrue("list=one, two, three\r\n" in minified)
        assertEquals(
            PropertiesDocument.parse(application).entries.map { it.key to it.value },
            PropertiesDocument.parse(minified).entries.map { it.key to it.value }
        )
    }
}
