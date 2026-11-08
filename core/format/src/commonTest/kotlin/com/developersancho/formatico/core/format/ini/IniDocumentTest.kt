package com.developersancho.formatico.core.format.ini

import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.Severity
import com.developersancho.formatico.core.format.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IniDocumentTest {
    private val config =
        """
        ; Formatico's settings
        version = 3

        [editor]
        theme = "teal"
        tab size: 2
        	wrap=on

        # The files it opens
        [files]
        recent = 12 ; a note, part of the value
        skip-hidden
        """.trimIndent() + "\n"

    @Test
    fun sectionsAndTheirKeysAreRead() {
        val document = IniDocument.parse(config)

        assertTrue(document.isValid)
        assertEquals(listOf(null, "editor", "files"), document.sections.map { it.name })
        val (global, editor, files) = document.sections
        assertNull(global.headerRange)
        assertEquals(listOf("version" to "3"), global.entries.map { it.key to it.value })
        assertEquals(listOf("theme" to "teal", "tab size" to "2", "wrap" to "on"), editor.entries.map { it.key to it.value })
        assertEquals("12 ; a note, part of the value", files.entries[0].value)
        assertFalse(files.entries[1].separated)
        assertEquals("\"teal\"", config.substring(editor.entries[0].valueRange.start, editor.entries[0].valueRange.end))
        assertEquals(TextRange(config.indexOf("[editor]"), config.indexOf("wrap=on") + "wrap=on".length), editor.range)
        assertEquals(2, document.comments.size)
        assertEquals(config, document.print())
    }

    @Test
    fun anUnclosedHeaderIsAnErrorAndWhatIsGivenTwiceAWarning() {
        val document = IniDocument.parse("[a]\nx=1\nx=2\n[b\n[a]\n")

        assertFalse(document.isValid)
        val codes = document.problems.map { it.code to it.severity }
        assertEquals(
            listOf(
                ProblemCode.DuplicateKey to Severity.Warning,
                ProblemCode.IniUnclosedSection to Severity.Error,
                ProblemCode.IniDuplicateSection to Severity.Warning
            ),
            codes
        )
        assertEquals(4, document.problems[1].start.line)
    }

    @Test
    fun formatSpacesTheKeysAndPutsABlankLineBeforeEachSection() {
        val text = "\n  version=3\n; the editor\n[editor]\n   theme=teal\n\n\n\n  tab size :2\n[files]\nskip-hidden\n\n"

        assertEquals(
            "version = 3\n\n; the editor\n[editor]\ntheme = teal\n\ntab size = 2\n\n[files]\nskip-hidden\n",
            IniDocument.parse(text).formatted()
        )
    }

    @Test
    fun sortingTheKeysKeepsTheSectionsInTheirOrder() {
        val text = "[z]\nb=2\n; first\na=1\n[a]\nd=4\nc=3\n"

        assertEquals("[z]\n; first\na = 1\nb = 2\n\n[a]\nc = 3\nd = 4\n", IniDocument.parse(text).formatted(sortKeys = true))
    }

    @Test
    fun minifyLeavesTheHeadersAndTheKeys() {
        val minified = IniDocument.parse(config).minified()

        assertEquals(
            "version=3\n[editor]\ntheme=\"teal\"\ntab size=2\nwrap=on\n[files]\nrecent=12 ; a note, part of the value\nskip-hidden\n",
            minified
        )
        val again = IniDocument.parse(minified)
        assertEquals(
            IniDocument.parse(config).sections.map { section -> section.entries.map { it.key to it.value } },
            again.sections.map { section -> section.entries.map { it.key to it.value } }
        )
    }
}
