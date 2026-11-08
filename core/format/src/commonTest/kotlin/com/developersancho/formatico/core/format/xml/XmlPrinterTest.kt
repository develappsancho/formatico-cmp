package com.developersancho.formatico.core.format.xml

import com.developersancho.formatico.core.format.text.Indentation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class XmlPrinterTest {
    private val feed =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0"   xmlns:atom='http://www.w3.org/2005/Atom'><channel>
        <title>Formatico &amp; friends</title>
              <atom:link href="https://formatico.pantrixui.com/feed.xml" rel="self"
                 type="application/rss+xml" />
        <item><title>Release</title><description><![CDATA[<p>New <b>XML</b></p>]]></description>
        <pubDate>Wed, 04 Nov 2026 10:00:00 GMT</pubDate></item>
        <!-- more items soon -->
        </channel></rss>
        """.trimIndent() + "\n"

    private val pom =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <!-- The build -->
        <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
          <!-- coordinates -->
          <groupId>com.developersancho</groupId>   <artifactId>formatico</artifactId>
        <dependencies>
                <dependency><groupId>junit</groupId><artifactId>junit</artifactId><scope>test</scope></dependency>
          </dependencies>
        </project>
        """.trimIndent()

    private val layout =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
            android:orientation="vertical">
            <TextView android:id="@+id/title"
                android:text="Hello" />
            <Button
                android:id="@+id/go" android:text="Go"/>
        </LinearLayout>
        """.trimIndent()

    private val mixed =
        """
        <doc><p>Hello <!-- aside --><b>big</b>   world</p>
        <pre xml:space="preserve">
          a
            <x> y </x>
        </pre><keep xml:space="preserve">  <b/>  </keep><loose xml:space="default">  <b/>  </loose>
        <empty></empty><blank>   </blank><self /></doc>
        """.trimIndent()

    /**
     * What [text] holds, in a form that says nothing of how it is laid out: each element's name, attributes and
     * children, comments left out and the text between two elements read as one, whitespace alone dropped.
     */
    private fun contentOf(text: String): String {
        val document = XmlDocument.parse(text)
        assertTrue(document.isValid, "Not valid: <$text> ${document.problems}")
        return canonical(requireNotNull(document.root))
    }

    private fun canonical(element: XmlElement): String {
        val parts = ArrayList<String>()
        val pending = StringBuilder()

        fun flush() {
            if (pending.isNotBlank()) parts += "text(${pending.trim()})"
            pending.clear()
        }
        element.children.forEach { child ->
            when (child) {
                is XmlText -> pending.append(child.value)
                is XmlElement -> flush().also { parts += canonical(child) }
                is XmlProcessingInstruction -> flush().also { parts += "pi(${child.target})" }
                else -> Unit
            }
        }
        flush()
        val attributes = element.attributes.joinToString("") { " ${it.name}=${it.value}" }
        return "${element.name}$attributes${parts.joinToString(", ", "[", "]")}"
    }

    @Test
    fun aFeedIsWrittenAnElementToALine() {
        val expected =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:atom='http://www.w3.org/2005/Atom'>
              <channel>
                <title>Formatico &amp; friends</title>
                <atom:link href="https://formatico.pantrixui.com/feed.xml" rel="self" type="application/rss+xml"/>
                <item>
                  <title>Release</title>
                  <description><![CDATA[<p>New <b>XML</b></p>]]></description>
                  <pubDate>Wed, 04 Nov 2026 10:00:00 GMT</pubDate>
                </item>
                <!-- more items soon -->
              </channel>
            </rss>
            """.trimIndent() + "\n"
        assertEquals(expected, XmlDocument.parse(feed).formatted())
    }

    @Test
    fun aPomKeepsItsCommentsOnLinesOfTheirOwn() {
        val expected =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <!-- The build -->
            <project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion>
              <!-- coordinates -->
              <groupId>com.developersancho</groupId>
              <artifactId>formatico</artifactId>
              <dependencies>
                <dependency>
                  <groupId>junit</groupId>
                  <artifactId>junit</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
            </project>
            """.trimIndent()
        assertEquals(expected, XmlDocument.parse(pom).formatted())
    }

    @Test
    fun anAndroidLayoutIsIndentedAsAsked() {
        val expected =
            """
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" android:orientation="vertical">
                <TextView android:id="@+id/title" android:text="Hello"/>
                <Button android:id="@+id/go" android:text="Go"/>
            </LinearLayout>
            """.trimIndent()
        assertEquals(expected, XmlDocument.parse(layout).formatted(Indentation.Spaces(4)))
        assertEquals(expected.replace("    ", "\t"), XmlDocument.parse(layout).formatted(Indentation.Tabs))
    }

    @Test
    fun textAndWhatIsPreservedKeepTheirContentAsItIs() {
        val expected =
            """
            <doc>
              <p>Hello <!-- aside --><b>big</b>   world</p>
              <pre xml:space="preserve">
              a
                <x> y </x>
            </pre>
              <keep xml:space="preserve">  <b/>  </keep>
              <loose xml:space="default">
                <b/>
              </loose>
              <empty></empty>
              <blank></blank>
              <self/>
            </doc>
            """.trimIndent()
        assertEquals(expected, XmlDocument.parse(mixed).formatted())
    }

    @Test
    fun theLineEndingsAreTheDocuments() {
        assertEquals("<a>\r\n  <b>x</b>\r\n</a>\r\n", XmlDocument.parse("<a>\r\n<b>x</b></a>\r\n").formatted())
        assertEquals("<a>\n  <b/>\n</a>", XmlDocument.parse("<a><b/></a>").formatted())
    }

    @Test
    fun minifiedTakesOutTheRoomBetweenElementsAndTheComments() {
        assertEquals(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><project xmlns=\"http://maven.apache.org/POM/4.0.0\">" +
                "<modelVersion>4.0.0</modelVersion><groupId>com.developersancho</groupId><artifactId>formatico</artifactId>" +
                "<dependencies><dependency><groupId>junit</groupId><artifactId>junit</artifactId><scope>test</scope>" +
                "</dependency></dependencies></project>",
            XmlDocument.parse(pom).minified()
        )
        assertEquals(
            "<doc><p>Hello <b>big</b>   world</p><pre xml:space=\"preserve\">\n  a\n    <x> y </x>\n</pre>" +
                "<keep xml:space=\"preserve\">  <b/>  </keep><loose xml:space=\"default\"><b/></loose>" +
                "<empty></empty><blank></blank><self/></doc>",
            XmlDocument.parse(mixed).minified()
        )
        assertEquals(
            "<!DOCTYPE note [<!ENTITY t \"Tove\">]><?pi x?><note><to>&t;</to></note>",
            XmlDocument.parse("<!DOCTYPE note [<!ENTITY t \"Tove\">]>\n<?pi x?>\n<note>\n  <to>&t;</to>\n</note>\n").minified()
        )
    }

    @Test
    fun whatIsWrittenOutReadsAsTheSameDocument() {
        listOf(feed, pom, layout, mixed).forEach { text ->
            val document = XmlDocument.parse(text)
            val formatted = document.formatted()
            assertEquals(contentOf(text), contentOf(formatted))
            assertEquals(contentOf(text), contentOf(document.minified()))
            assertEquals(formatted, XmlDocument.parse(formatted).formatted())
        }
    }

    @Test
    fun aDocumentThatIsNotValidIsNotWrittenOut() {
        assertFailsWith<IllegalArgumentException> { XmlDocument.parse("<a><b></a>").formatted() }
        assertFailsWith<IllegalArgumentException> { XmlDocument.parse("<!-- no element -->").minified() }
    }
}
