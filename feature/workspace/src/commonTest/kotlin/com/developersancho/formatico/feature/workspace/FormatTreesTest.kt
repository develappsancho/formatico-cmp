package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class FormatTreesTest {
    private fun root(text: String, format: Format): ValueNode = assertIs<Reading.Read>(readDocument(text, format)).root!!

    private fun ValueNode.find(key: String): ValueNode = trail(this, key).last()

    private fun ValueNode.written(text: String): String = text.substring(range.start, range.end)

    @Test
    fun newlineDelimitedJsonIsAnArrayOfItsRecords() {
        val text = "{\"event\": \"open\", \"bytes\": 1200}\n\n{\"event\": \"copy\", \"bytes\": 1237}\n"

        val root = root(text, Format.Ndjson)

        assertEquals(ValueKind.Array, root.kind)
        assertEquals("[2]", root.text)
        assertEquals(listOf("$[0]", "$[1]"), root.children.map { it.key })
        // A record's values are where they are written in the whole text.
        val bytes = root.find("$[1].bytes")
        assertEquals("1237", bytes.written(text))
        assertEquals(bytes, nodeAt(root, text.lastIndexOf("1237")))
    }

    @Test
    fun aPropertiesFileIsAnObjectOfItsKeys() {
        val text = "# Ports\nserver.port=8080\nlist = one, \\\n    two\nserver.port = 9090\nalone\n"

        val root = root(text, Format.Properties)

        assertEquals(listOf("server.port", "list", "server.port", "alone"), root.children.map { it.name })
        assertEquals(listOf("$['server.port']", "$.list", "$['server.port']#2", "$.alone"), root.children.map { it.key })
        val port = root.children.first()
        assertEquals(ValueKind.String, port.kind)
        assertEquals("8080", port.text)
        assertEquals("8080", port.written(text))
        // A value over two lines is shown as it is read; a key with no value is found at its key.
        assertEquals("one, two", root.find("$.list").text)
        assertEquals("alone", root.find("$.alone").written(text))
    }

    @Test
    fun anIniFileIsItsLooseKeysAndASectionToAnObject() {
        val text = "version = 3\n\n[editor]\ntheme = \"teal\"\n\n[files]\nrecent = 12\n"

        val root = root(text, Format.Ini)

        assertEquals(listOf("version", "editor", "files"), root.children.map { it.name })
        val editor = root.find("$.editor")
        assertEquals(ValueKind.Object, editor.kind)
        assertEquals("{1}", editor.text)
        val theme = root.find("$.editor.theme")
        assertEquals("\"teal\"", theme.text)
        assertEquals("teal", theme.value)
        assertEquals("[editor]\ntheme = \"teal\"", editor.written(text))
    }

    @Test
    fun anXmlTreeHasTheRootUnderTheDocumentAndGathersElementsOfOneName() {
        val text =
            """
            <?xml version="1.0"?>
            <rss version="2.0">
              <channel>
                <title>Formatico notes</title>
                <item><title>Tabs that remember</title></item>
                <item><title>A tree that previews colours</title></item>
                <empty/>
                <note lang="en">Mixed <b>text</b> here</note>
              </channel>
            </rss>
            """.trimIndent()

        val root = root(text, Format.Xml)

        assertEquals(listOf("rss"), root.children.map { it.name })
        val rss = root.find("$.rss")
        assertEquals(listOf("@version", "channel"), rss.children.map { it.name })
        assertEquals("\"2.0\"", rss.find("$.rss['@version']").text)
        assertEquals("2.0", rss.find("$.rss['@version']").value)
        val items = root.find("$.rss.channel.item")
        assertEquals(ValueKind.Array, items.kind)
        assertEquals("[2]", items.text)
        val second = root.find("$.rss.channel.item[1].title")
        assertEquals(ValueKind.String, second.kind)
        assertEquals("A tree that previews colours", second.value)
        assertEquals("<title>A tree that previews colours</title>", second.written(text))
        assertEquals("", root.find("$.rss.channel.empty").text)
        assertEquals(listOf("@lang", "b", "#text"), root.find("$.rss.channel.note").children.map { it.name })
        assertEquals("Mixed  here", root.find("$.rss.channel.note['#text']").value)
    }

    @Test
    fun aSettingsFileWithNoKeyHasNoTree() {
        assertNull(assertIs<Reading.Read>(readDocument("# nothing yet\n", Format.Properties)).root)
        assertNull(assertIs<Reading.Read>(readDocument("; nothing yet\n", Format.Ini)).root)
    }
}
