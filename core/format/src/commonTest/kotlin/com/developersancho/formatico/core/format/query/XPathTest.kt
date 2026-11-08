package com.developersancho.formatico.core.format.query

import com.developersancho.formatico.core.format.text.TextRange
import com.developersancho.formatico.core.format.xml.XmlDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class XPathTest {
    private val feed =
        """
        <?xml version="1.0"?>
        <rss version="2.0">
          <!-- feed -->
          <channel>
            <title>News</title>
            <item id="1"><title>First</title><price>3</price></item>
            <item id="2"><title>Second</title><price>4.5</price></item>
            <?render fast?>
          </channel>
        </rss>
        """.trimIndent()

    @Test
    fun aPathGivesAStepsPositionOnlyWhereItsParentHasMoreThanOneItCouldBe() {
        val titles = matches("//title", feed)

        assertEquals(listOf("/rss/channel/title", "/rss/channel/item[1]/title", "/rss/channel/item[2]/title"), titles.map { it.path })
        assertEquals(listOf("News", "First", "Second"), titles.map { it.value })
        assertEquals("<title>First</title>", feed.substring(titles[1].range.start, titles[1].range.end))
    }

    @Test
    fun anAttributesPathEndsInItsName() {
        val version = matches("/rss/@version", feed).single()

        assertEquals("/rss/@version", version.path)
        assertEquals("2.0", version.value)
        assertEquals("version=\"2.0\"", feed.substring(version.range.start, version.range.end))
        assertIs<XPathNode.Attribute>(version.node)
        assertEquals(listOf("/rss/channel/item[1]/@id", "/rss/channel/item[2]/@id"), paths("//item/@id", feed))
    }

    @Test
    fun textCommentsAndInstructionsHaveStepsOfTheirOwn() {
        assertEquals(listOf("/rss/channel/title/text()"), paths("/rss/channel/title/text()", feed))
        val comment = matches("//comment()", feed).single()
        assertEquals("/rss/comment()", comment.path)
        assertEquals(" feed ", comment.value)
        val instruction = matches("//processing-instruction()", feed).single()
        assertEquals("/rss/channel/processing-instruction('render')", instruction.path)
        assertEquals("fast", instruction.value)
        assertEquals(listOf("/rss/text()[1]", "/rss/text()[2]", "/rss/text()[3]"), paths("/rss/text()", feed))
        assertEquals(listOf("\n  ", "\n  ", "\n"), matches("/rss/text()", feed).map { it.value })
        assertEquals(listOf("/a/comment()[1]", "/a/comment()[2]"), paths("//comment()", "<a><!--1--><b/><!--2--></a>"))
        assertEquals(
            listOf("/a/processing-instruction('x')[1]", "/a/processing-instruction('y')", "/a/processing-instruction('x')[2]"),
            paths("/a/processing-instruction()", "<a><?x?><?y?><?x?></a>")
        )
    }

    @Test
    fun theRootIsTheWholeDocument() {
        val document = XmlDocument.parse(feed)
        val root = (xpath("/").evaluate(document) as XPathResult.Nodes).matches.single()

        assertEquals("/", root.path)
        assertEquals(XPathNode.Root(document), root.node)
        assertEquals(TextRange(0, feed.length), root.range)
        assertEquals("NewsFirst3Second4.5", root.value.filterNot { it.isWhitespace() })
    }

    @Test
    fun theDeclarationTheDoctypeAndTheTextAroundTheElementAreNoNodes() {
        val xml = "<?xml version=\"1.0\"?>\n<!DOCTYPE r>\n<!-- c -->\n<r/>\n<?pi?>\n"

        assertEquals(listOf("/comment()", "/r", "/processing-instruction('pi')"), paths("/node()", xml))
        assertEquals("", written("string(/)", xml))
    }

    @Test
    fun aRunOfTextAndCdataSectionsIsOneTextNode() {
        val xml = "<a>x<![CDATA[y<]]>z<b/>w<!--c-->v<![CDATA[]]></a>"
        val texts = matches("/a/text()", xml)

        assertEquals(listOf("xy<z", "w", "v"), texts.map { it.value })
        assertEquals(listOf("/a/text()[1]", "/a/text()[2]", "/a/text()[3]"), texts.map { it.path })
        assertEquals("x<![CDATA[y<]]>z", xml.substring(texts[0].range.start, texts[0].range.end))
        assertEquals("x", (texts[0].node as XPathNode.Text).text.raw)
        // The run's range takes in the empty CDATA section after `v`, which gives the node no text.
        assertEquals("v<![CDATA[]]>", xml.substring(texts[2].range.start, texts[2].range.end))
        assertEquals("", written("string(/r/text())", "<r><![CDATA[]]></r>"))
    }

    @Test
    fun eachKindOfNodeHasItsStringValue() {
        val xml = "<a t=\"x &amp;\ty\">1<b>2<![CDATA[3]]></b><!-- note -->4<?pi   data here ?></a>"

        assertEquals("1234", written("string(/)", xml))
        assertEquals("1234", written("string(/a)", xml))
        assertEquals("23", written("string(//b)", xml))
        assertEquals("x & y", written("string(/a/@t)", xml))
        assertEquals("1", written("string(/a/text())", xml))
        assertEquals(" note ", written("string(//comment())", xml))
        assertEquals("data here ", written("string(//processing-instruction('pi'))", xml))
        assertEquals("", written("string(//nothing)", xml))
    }

    @Test
    fun eachTypeOfResultIsGivenAsWhatItIs() {
        assertEquals(XPathResult.Number(2.0, "2"), evaluate("count(//item)", feed))
        assertEquals(XPathResult.Number(7.5, "7.5"), evaluate("sum(//price)", feed))
        assertEquals(XPathResult.Text("News"), evaluate("string(//title)", feed))
        assertEquals(XPathResult.Truth(false), evaluate("boolean(//nothing)", feed))
        assertEquals(listOf("Second"), matches("//item[price > 4]/title", feed).map { it.value })
        assertEquals(XPathResult.Nodes(emptyList()), evaluate("//nothing", feed))
    }

    @Test
    fun namesMatchAsTheyAreWrittenPrefixAndAll() {
        val xml = "<x:root xmlns:x=\"urn:x\" xmlns=\"urn:d\" a=\"1\"><x:item/><item/><y:item/></x:root>"

        assertEquals(listOf("/x:root/x:item"), paths("/x:root/x:item", xml))
        assertEquals(listOf("/x:root/item"), paths("/x:root/item", xml))
        assertEquals(listOf("/x:root/x:item"), paths("/x:root/x:*", xml))
        assertEquals(listOf("/x:root/x:item", "/x:root/item", "/x:root/y:item"), paths("/*/*", xml))
        assertEquals(listOf("/x:root/@a"), paths("/*/@*", xml))
        assertEquals("x:root", written("name(/*)", xml))
        assertEquals("root", written("local-name(/*)", xml))
        assertEquals("", written("namespace-uri(/*)", xml))
        assertEquals(listOf("/x:root/x:item", "/x:root/item", "/x:root/y:item"), paths("//*[local-name() = 'item']", xml))
    }

    @Test
    fun anInvalidDocumentIsEvaluatedOnWhatWasRead() {
        val xml = "<a><b>text<c/>"

        assertEquals(listOf("/a/b"), paths("//b", xml))
        assertEquals("text", matches("//b", xml).single().value)
        assertEquals(listOf("/a[1]", "/a[2]"), paths("/a", "<a/><a/>"))
    }

    @Test
    fun aWideDocumentKeepsEachSiblingsPosition() {
        val xml = "<r>" + "<i/>".repeat(WIDTH) + "</r>"

        assertEquals(listOf("/r/i[$WIDTH]"), paths("/r/i[last()]", xml))
        assertEquals(listOf("/r/i[2]"), paths("/r/i[2]", xml))
        assertEquals(WIDTH.toString(), written("count(//i)", xml))
    }

    @Test
    fun aDeepDocumentIsEvaluatedWithoutRecursion() {
        val xml = "<a>".repeat(DEPTH) + "x" + "</a>".repeat(DEPTH)

        assertEquals(DEPTH.toString(), written("count(//a)", xml))
        assertEquals("x", written("string(/)", xml))
        assertEquals(DEPTH.toString(), written("count(//text()/ancestor::a)", xml))
        val deepest = matches("//a[not(a)]", xml).single()
        assertEquals("/a".repeat(DEPTH), deepest.path)
        assertEquals("x", deepest.value)
        assertIs<XPathNode.Element>(deepest.node)
    }

    private companion object {
        const val WIDTH = 10_000
        const val DEPTH = 2_000
    }
}
