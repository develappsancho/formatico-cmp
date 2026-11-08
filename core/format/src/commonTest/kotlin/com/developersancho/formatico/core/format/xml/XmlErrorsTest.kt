package com.developersancho.formatico.core.format.xml

import com.developersancho.formatico.core.format.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class XmlErrorsTest {
    /** Each problem of [text] as its code, where it starts and the text it covers: `XmlSecondRoot 2:1 <b/>`. */
    private fun found(text: String): List<String> = XmlDocument.parse(text).problems.map {
        "${it.code} ${it.start.line}:${it.start.column} ${text.substring(it.range.start, it.range.end)}"
    }

    @Test
    fun anElementNeverClosedIsClosedByItsParentsEndTag() {
        assertEquals(listOf("XmlUnclosedElement 2:3 <b>"), found("<a>\n  <b>\n</a>"))
        assertEquals(listOf("XmlUnclosedElement 1:1 <a>", "XmlUnclosedElement 1:4 <b>"), found("<a><b>"))
    }

    @Test
    fun anEndTagOfAnotherNameClosesTheElementOpenLast() {
        assertEquals(listOf("XmlMismatchedEndTag 2:6 c"), found("<a>\n<b></c>\n</a>"))
    }

    @Test
    fun anEndTagNoElementIsOpenForIsReadPast() {
        assertEquals(listOf("XmlUnexpectedEndTag 2:1 </a>"), found("<a></a>\n</a>"))
        assertEquals(listOf("XmlUnexpectedEndTag 1:12 </t>"), found("<r><t>x</t></t></r>"))
    }

    @Test
    fun aTagWithNoClosingAngleStopsBeforeTheNextOne() {
        assertEquals(listOf("XmlUnterminatedTag 1:1 <a"), found("<a\n<b/></a>"))
        assertEquals(listOf("XmlUnterminatedTag 2:1 </a"), found("<a>\n</a"))
    }

    @Test
    fun attributesAreGivenOnceInQuotesWithAValue() {
        assertEquals(listOf("XmlDuplicateAttribute 1:10 x"), found("""<a x="1" x="2"/>"""))
        assertEquals(listOf("XmlUnquotedAttribute 1:6 1"), found("<a x=1/>"))
        assertEquals(listOf("XmlExpectedAttributeValue 1:8 disabled"), found("<input disabled/>"))
        assertEquals(listOf("XmlExpectedAttributeValue 1:4 x="), found("<a x=></a>"))
        assertEquals(listOf("UnexpectedToken 1:9 y"), found("""<a x="1"y="2"/>"""))
        assertEquals(listOf("UnexpectedCharacter 1:4 @"), found("<a @/>"))
    }

    @Test
    fun anAttributesQuoteNeverClosedStopsAtTheNextTag() {
        assertEquals(
            listOf("XmlUnterminatedTag 1:1 <a x=\"1>text", "UnterminatedString 1:6 \"1>text"),
            found("<a x=\"1>text</a>")
        )
    }

    @Test
    fun aReferenceIsOneXmlHas() {
        assertEquals(listOf("XmlInvalidReference 1:6 &"), found("<a>AT&T</a>"))
        assertEquals(
            listOf("XmlInvalidReference 1:4 &nbsp;", "XmlInvalidReference 1:10 &#x1F;", "XmlInvalidReference 1:16 &#xZZ;"),
            found("<a>&nbsp;&#x1F;&#xZZ;</a>")
        )
        assertEquals(listOf("XmlInvalidReference 1:9 &#0;"), found("<a b=\"x &#0;\"/>"))
        assertEquals(emptyList(), found("<!DOCTYPE a><a>&nbsp;</a>"))
    }

    @Test
    fun whatHasNoCloserRunsToTheEnd() {
        assertEquals(listOf("UnterminatedComment 1:5 <!-- open"), found("<a/><!-- open"))
        assertEquals(listOf("XmlUnclosedElement 1:1 <a>", "XmlUnterminatedCdata 1:4 <![CDATA[x</a>"), found("<a><![CDATA[x</a>"))
        assertEquals(listOf("XmlUnterminatedInstruction 1:1 <?xml version=\"1.0\">"), found("<?xml version=\"1.0\">\n<a/>"))
    }

    @Test
    fun theDeclarationStandsAtTheStartInSmallLetters() {
        assertEquals(listOf("XmlMisplacedDeclaration 2:1 <?xml version=\"1.0\"?>"), found("\n<?xml version=\"1.0\"?>\n<a/>"))
        assertEquals(listOf("XmlMisplacedDeclaration 1:1 <?XML version=\"1.0\"?>"), found("<?XML version=\"1.0\"?><a/>"))
        assertEquals(listOf("XmlMisplacedDeclaration 1:5 <?xml version=\"1.0\"?>"), found("<a/><?xml version=\"1.0\"?>"))
    }

    @Test
    fun theTopLevelHoldsOneElementAndNoText() {
        assertEquals(listOf("XmlSecondRoot 2:1 <b/>"), found("<a/>\n<b/>"))
        assertEquals(listOf("XmlSecondRoot 1:5 <b>"), found("<a/><b></b>"))
        assertEquals(listOf("XmlTextOutsideRoot 2:1 trailing"), found("<a/>\ntrailing \n"))
        assertEquals(listOf("XmlTextOutsideRoot 1:1 <![CDATA[x]]>"), found("<![CDATA[x]]><a/>"))
        assertEquals(listOf("UnexpectedToken 1:5 <!DOCTYPE"), found("<a/><!DOCTYPE a>"))
    }

    @Test
    fun aTextWithNoElementHasNoRootAndAnEmptyOneNoValue() {
        assertEquals(listOf("XmlNoRoot 1:1 <!-- only a comment -->"), found("<!-- only a comment -->\n"))
        assertEquals(listOf("XmlTextOutsideRoot 1:1 text", "XmlNoRoot 1:1 text"), found("text"))
        assertEquals(listOf("ExpectedValue 1:1 "), found(""))
        assertEquals(listOf("ExpectedValue 2:2 "), found(" \n "))
    }

    @Test
    fun twoHyphensCannotStandInAComment() {
        assertEquals(listOf("XmlHyphensInComment 1:8 --"), found("<!-- a -- b --><a/>"))
        assertEquals(listOf("XmlHyphensInComment 1:14 --"), found("<a/><!-- end --->"))
        assertEquals(emptyList(), found("<a/><!---->"))
    }

    @Test
    fun aCharacterThatCannotStandInTextIsReported() {
        assertEquals(listOf("UnexpectedCharacter 1:6 <"), found("<a>1 < 2</a>"))
        assertEquals(listOf("UnexpectedCharacter 1:6 >"), found("<a>]]></a>"))
        assertEquals(listOf("UnexpectedCharacter 1:4 " + Char(1)), found("<a>" + Char(1) + "</a>"))
    }

    @Test
    fun elementsNestedTooDeepAreReportedOnce() {
        assertEquals(listOf("TooDeep 1:1537 <a>"), found("<a>".repeat(600) + "</a>".repeat(600)))
    }

    @Test
    fun everyProblemIsFoundInTheOrderOfTheText() {
        val text =
            """
            <root>
              <item id="1" id="2">
              <name>AT&T</name>
            </root>
            """.trimIndent()
        val document = XmlDocument.parse(text)

        assertFalse(document.isValid)
        assertTrue(document.problems.all { it.severity == Severity.Error })
        assertEquals(
            listOf(
                "XmlUnclosedElement 2:3 <item id=\"1\" id=\"2\">",
                "XmlDuplicateAttribute 2:16 id",
                "XmlInvalidReference 3:11 &"
            ),
            found(text)
        )
        // The element never closed holds what follows it, up to its parent's end tag.
        val item = requireNotNull(document.root).children.filterIsInstance<XmlElement>().single()
        assertEquals(listOf("name"), item.children.filterIsInstance<XmlElement>().map { it.name })
    }
}
