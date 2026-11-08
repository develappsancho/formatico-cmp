package com.developersancho.formatico.core.format.xml

import com.developersancho.formatico.core.format.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class XmlDocumentTest {
    private fun String.at(range: TextRange): String = substring(range.start, range.end)

    private fun valid(text: String): XmlDocument {
        val document = XmlDocument.parse(text)
        assertTrue(document.isValid, "Not valid: ${document.problems}")
        return document
    }

    private fun XmlElement.elements(): List<XmlElement> = children.filterIsInstance<XmlElement>()

    @Test
    fun anElementKeepsItsNamesAttributesAndChildrenWhereTheyAreWritten() {
        val text = """<a:root xmlns:a="urn:a" id='1'><child/><b>t</b></a:root>"""
        val root = requireNotNull(valid(text).root)

        assertEquals("a:root", root.name)
        assertEquals("a:root", text.at(root.nameRange))
        assertEquals(TextRange(0, text.length), root.range)
        assertEquals(listOf("xmlns:a", "id"), root.attributes.map { it.name })
        assertEquals(listOf("xmlns:a=\"urn:a\"", "id='1'"), root.attributes.map { text.at(it.range) })
        assertEquals(listOf("\"urn:a\"", "'1'"), root.attributes.map { it.raw })
        assertEquals(listOf("urn:a", "1"), root.attributes.map { it.value })
        assertEquals("id", text.at(root.attributes[1].nameRange))

        val (child, b) = root.elements()
        assertTrue(child.selfClosing)
        assertEquals("<child/>", text.at(child.range))
        assertEquals("<b>t</b>", text.at(b.range))
        assertEquals("t", (b.children.single() as XmlText).value)
    }

    @Test
    fun theReferencesAreReadInTextAndInAttributes() {
        val text = """<p title="x &amp; y &#65;&#x42;">&lt;a&gt; &quot;q&quot; &apos;s&apos;&#10;&#x1F600;</p>"""
        val root = requireNotNull(valid(text).root)
        val content = root.children.single() as XmlText

        assertEquals("x & y AB", root.attributes.single().value)
        assertEquals("<a> \"q\" 's'\n😀", content.value)
        assertEquals("&lt;a&gt; &quot;q&quot; &apos;s&apos;&#10;&#x1F600;", content.raw)
        assertEquals(content.raw, text.at(content.range))
    }

    @Test
    fun lineBreaksAreReadAsXmlReadsThem() {
        val root = requireNotNull(valid("<a b=\"x\r\ny\tz&#10;\">l1\r\nl2\rl3</a>").root)

        assertEquals("x y z\n", root.attributes.single().value)
        assertEquals("l1\nl2\nl3", (root.children.single() as XmlText).value)
    }

    @Test
    fun aCdataSectionKeepsItsContentAsItIs() {
        val text = "<s><![CDATA[a < b && c &amp;]]></s>"
        val cdata = requireNotNull(valid(text).root).children.single() as XmlText

        assertTrue(cdata.cdata)
        assertEquals("<![CDATA[a < b && c &amp;]]>", cdata.raw)
        assertEquals(cdata.raw, text.at(cdata.range))
        assertEquals("a < b && c &amp;", cdata.value)
    }

    @Test
    fun theTopLevelHoldsTheDeclarationInstructionsCommentsAndTheRoot() {
        val text =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <?xml-stylesheet type="text/xsl" href="style.xsl"?>
            <!-- top -->
            <r><!-- inner --><?php echo 1; ?></r>
            """.trimIndent()
        val document = valid(text)
        val written = document.nodes.filterNot { it is XmlText }

        assertEquals(listOf("xml", "xml-stylesheet"), written.filterIsInstance<XmlProcessingInstruction>().map { it.target })
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", text.at(written[0].range))
        assertTrue(written[2] is XmlComment)
        assertSame<XmlNode?>(document.root, written[3])
        assertEquals(listOf("<!-- top -->", "<!-- inner -->"), document.comments.map { text.at(it) })
        val instruction = requireNotNull(document.root).children[1] as XmlProcessingInstruction
        assertEquals("php", instruction.target)
        assertEquals("<?php echo 1; ?>", text.at(instruction.range))
    }

    @Test
    fun aDoctypeIsKeptAsItIsWrittenAndItsEntitiesAreNoProblem() {
        val doctype =
            """
            <!DOCTYPE note [
              <!ENTITY writer "Donald > Duck.">
              <!-- a > in a comment -->
              <!ELEMENT note (#PCDATA)>
            ]>
            """.trimIndent()
        val text = "<?xml version=\"1.0\"?>\n$doctype\n<note>&writer; &amp;</note>"
        val document = valid(text)

        assertEquals(doctype, text.at(document.nodes.filterIsInstance<XmlDoctype>().single().range))
        assertEquals("&writer; &", (requireNotNull(document.root).children.single() as XmlText).value)
        val html = "<!DOCTYPE html PUBLIC \"a > b\" 'c'><html/>"
        assertEquals("<!DOCTYPE html PUBLIC \"a > b\" 'c'>", html.at(valid(html).nodes.first().range))
    }

    @Test
    fun aByteOrderMarkIsReadPast() {
        val text = Char(0xFEFF) + "<?xml version=\"1.0\"?><a/>"
        val document = valid(text)

        assertEquals(1, document.nodes.first().range.start)
        assertEquals("a", document.root?.name)
        assertEquals(text, document.print())
    }

    @Test
    fun printGivesTheTextBackCharacterForCharacter() {
        val everything =
            "<?xml version=\"1.0\"?>\r\n<!DOCTYPE r [<!ENTITY e 'x'>]>\r\n<!-- c -->\r\n" +
                "<x:r xmlns:x='urn:x' a = \"1 &amp; 2\"><![CDATA[ <raw> ]]>text &e; &#x41;<?pi data?><e/>\r\n</x:r>\r\n"
        val broken = "<a><b x=1 x=\"2\"></c>AT&T <d></a></a> tail <!-- open"

        assertEquals(everything, valid(everything).print())
        assertEquals(broken, XmlDocument.parse(broken).print())
        assertEquals("", XmlDocument.parse("").print())
        // A text cut anywhere, as it is while it is typed, is read without failing and given back.
        (0..everything.length).map { everything.substring(0, it) }.forEach { assertEquals(it, XmlDocument.parse(it).print()) }
    }

    @Test
    fun theNodesOfTheTopLevelFollowEachOther() {
        val text = "<?xml version=\"1.0\"?>\n<!-- c -->\n<r>\n  <e/>\n</r>\n"
        val nodes = valid(text).nodes

        assertEquals(0, nodes.first().range.start)
        assertEquals(text.length, nodes.last().range.end)
        nodes.zipWithNext().forEach { (before, after) -> assertEquals(before.range.end, after.range.start) }
    }

    @Test
    fun asMuchAsCanBeReadIsReadWhereThereAreErrors() {
        val text = "<a><b>text"
        val document = XmlDocument.parse(text)
        val root = requireNotNull(document.root)
        val b = root.elements().single()

        assertEquals(TextRange(0, text.length), root.range)
        assertEquals("<b>text", text.at(b.range))
        assertEquals("text", (b.children.single() as XmlText).value)
        assertNull(XmlDocument.parse("<!-- no element -->").root)
    }
}
