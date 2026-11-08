package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FormatEditsTest {
    private fun edit(text: String, format: Format?, edit: DocumentEdit): Edited =
        edited(OpenDocument(id = 1, name = null, text = text, format = format), edit)

    private fun written(text: String, format: Format, edit: DocumentEdit): String = assertIs<Edited.Text>(edit(text, format, edit)).text

    @Test
    fun eachFormatHasTheEditsItsTextCanTake() {
        assertEquals(setOf(DocumentEdit.Format), Format.Csv.edits)
        assertEquals(setOf(DocumentEdit.Format, DocumentEdit.Minify), Format.Xml.edits)
        listOf(Format.Ndjson, Format.Toml, Format.Properties, Format.Ini, Format.Yaml, Format.Json5).forEach {
            assertEquals(DocumentEdit.entries.toSet(), it.edits, "$it")
        }
    }

    @Test
    fun newlineDelimitedJsonKeepsARecordToALine() {
        val text = "{\"b\":1,\"a\":2}\n{\"c\": [1,2]}\n"

        assertEquals("{\"b\": 1, \"a\": 2}\n{\"c\": [1, 2]}\n", written(text, Format.Ndjson, DocumentEdit.Format))
        assertEquals("{\"b\":1,\"a\":2}\n{\"c\":[1,2]}\n", written(text, Format.Ndjson, DocumentEdit.Minify))
        assertEquals("{\"a\": 2, \"b\": 1}\n{\"c\": [1, 2]}\n", written(text, Format.Ndjson, DocumentEdit.SortKeys))
    }

    @Test
    fun aPropertiesFileAndAnIniFileAreFormattedMinifiedAndSorted() {
        val properties = "# Ports\nhttps = 443\nhttp = 80\n"
        // The comment right above a key goes with it.
        assertEquals("http = 80\n# Ports\nhttps = 443\n", written(properties, Format.Properties, DocumentEdit.SortKeys))
        val minified = assertIs<Edited.Text>(edit(properties, Format.Properties, DocumentEdit.Minify))
        assertEquals("https=443\nhttp=80\n", minified.text)
        assertEquals(1, minified.commentsRemoved)

        val ini = "[editor]\n  theme=teal\n  font=Geist\n"
        assertEquals("[editor]\ntheme = teal\nfont = Geist\n", written(ini, Format.Ini, DocumentEdit.Format))
        assertEquals("[editor]\nfont = Geist\ntheme = teal\n", written(ini, Format.Ini, DocumentEdit.SortKeys))
    }

    @Test
    fun xmlIsFormattedAndMinifiedButItsElementsAreNotSorted() {
        val text = "<a><!-- one --><c/>  <b>text</b></a>\n"

        assertEquals("<a>\n  <!-- one -->\n  <c/>\n  <b>text</b>\n</a>\n", written(text, Format.Xml, DocumentEdit.Format))
        val minified = assertIs<Edited.Text>(edit(text, Format.Xml, DocumentEdit.Minify))
        assertEquals("<a><c/><b>text</b></a>", minified.text)
        assertEquals(1, minified.commentsRemoved)
        assertEquals(Edited.NotPossible, edit(text, Format.Xml, DocumentEdit.SortKeys))
    }

    @Test
    fun plainTextAndADocumentWithErrorsAreNotEdited() {
        assertEquals(Edited.NotPossible, edit("hello", format = null, DocumentEdit.Format))
        assertEquals(Edited.NotPossible, edit("<a><b></a>", Format.Xml, DocumentEdit.Format))
        assertEquals(Edited.NotPossible, edit("[editor\nx=1\n", Format.Ini, DocumentEdit.Format))
    }
}
