package com.developersancho.formatico.core.format

import com.developersancho.formatico.core.format.ini.IniDocument
import com.developersancho.formatico.core.format.toml.TomlDocument
import com.developersancho.formatico.core.format.xml.XmlDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** XML's, TOML's and INI's repairs (Faz 21): each repairs its problem, and the repaired text reads with no error. */
class XmlTomlIniRepairsTest {
    private fun xml(text: String) = XmlDocument.parse(text).problems

    private fun toml(text: String) = TomlDocument.parse(text).problems

    private fun ini(text: String) = IniDocument.parse(text).problems

    private fun repaired(text: String, format: Format, problems: List<Problem>, code: ProblemCode): String {
        val problem = problems.first { it.code == code }
        val repair = Repairs.of(problem, text, format) ?: error("No repair for $code")
        return Repairs.applied(text, repair.edits)
    }

    private fun errorsIn(problems: List<Problem>) = problems.filter { it.severity == Severity.Error }

    @Test
    fun anXmlElementNeverClosedIsClosedWhereWhatWasReadOfItEnds() {
        val text = "<feed>\n  <title>Notes\n  <link>https://formatico.pantrixui.com</link>\n</feed>\n"
        val result = repaired(text, Format.Xml, xml(text), ProblemCode.XmlUnclosedElement)
        assertEquals("<feed>\n  <title>Notes\n  <link>https://formatico.pantrixui.com</link></title>\n</feed>\n", result)
        assertTrue(errorsIn(xml(result)).isEmpty(), "$result reads with no error")
    }

    @Test
    fun anXmlRootNeverClosedIsClosedAtTheTextsEnd() {
        val text = "<feed>\n  <title>Notes</title>\n  <!-- </feed> is missing -->\n"
        val result = repaired(text, Format.Xml, xml(text), ProblemCode.XmlUnclosedElement)
        assertEquals("<feed>\n  <title>Notes</title>\n  <!-- </feed> is missing --></feed>\n", result)
        assertTrue(errorsIn(xml(result)).isEmpty(), "$result reads with no error")
    }

    @Test
    fun anXmlAttributesValueInNoQuotesIsQuoted() {
        val text = "<item id=42 note=a\"b/>"
        val first = repaired(text, Format.Xml, xml(text), ProblemCode.XmlUnquotedAttribute)
        assertEquals("<item id=\"42\" note=a\"b/>", first)
        val second = repaired(first, Format.Xml, xml(first), ProblemCode.XmlUnquotedAttribute)
        assertEquals("<item id=\"42\" note=\"a&quot;b\"/>", second)
        assertTrue(errorsIn(xml(second)).isEmpty(), "$second reads with no error")
    }

    @Test
    fun aBareAmpersandInXmlIsWrittenAsAReference() {
        val text = "<title>Salt & pepper</title>"
        val result = repaired(text, Format.Xml, xml(text), ProblemCode.XmlInvalidReference)
        assertEquals("<title>Salt &amp; pepper</title>", result)
        assertTrue(errorsIn(xml(result)).isEmpty())
    }

    @Test
    fun anEntityXmlDoesNotKnowIsNotRepaired() {
        val text = "<title>&nbsp;</title>"
        assertNull(Repairs.of(xml(text).first { it.code == ProblemCode.XmlInvalidReference }, text, Format.Xml))
    }

    @Test
    fun aTomlStringIsClosedWithItsOwnQuote() {
        val basic = "name = \"formatico\nversion = \"1.0\"\n"
        assertEquals("name = \"formatico\"\nversion = \"1.0\"\n", repaired(basic, Format.Toml, toml(basic), ProblemCode.UnterminatedString))
        val literal = "path = 'C:\\Users\nversion = \"1.0\"\n"
        val closed = repaired(literal, Format.Toml, toml(literal), ProblemCode.UnterminatedString)
        assertEquals("path = 'C:\\Users'\nversion = \"1.0\"\n", closed)
        assertTrue(errorsIn(toml(closed)).isEmpty(), "$closed reads with no error")
    }

    @Test
    fun aMultiLineTomlStringIsClosedAtTheTextsEnd() {
        val text = "notes = \"\"\"\nfirst\nsecond\n"
        val result = repaired(text, Format.Toml, toml(text), ProblemCode.UnterminatedString)
        assertEquals("notes = \"\"\"\nfirst\nsecond\"\"\"\n", result)
        assertTrue(errorsIn(toml(result)).isEmpty(), "$result reads with no error")
    }

    @Test
    fun anIniSectionsHeaderIsClosed() {
        val text = "[server  \nport = 8080\n"
        val result = repaired(text, Format.Ini, ini(text), ProblemCode.IniUnclosedSection)
        assertEquals("[server]  \nport = 8080\n", result)
        assertTrue(errorsIn(ini(result)).isEmpty(), "$result reads with no error")
    }

    @Test
    fun jsonsRepairsAreStillNotXmls() {
        // A string never closed is TOML's and JSON's to close: XML's attribute value is not repaired.
        val text = "<item id=\"42/>"
        val unclosed = xml(text).firstOrNull { it.code == ProblemCode.UnterminatedString } ?: return
        assertNull(Repairs.of(unclosed, text, Format.Xml))
    }
}
