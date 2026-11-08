package com.developersancho.formatico.core.format.csv

import com.developersancho.formatico.core.format.Problem
import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CsvDocumentTest {
    private fun CsvDocument.values(): List<List<String>> = records.map { record -> record.fields.map { it.value } }

    private fun CsvDocument.codes(): List<ProblemCode> = problems.map(Problem::code)

    @Test
    fun aRecordIsAFieldBetweenEachDelimiter() {
        val document = CsvDocument.parse("code,name,numeric\nAD,Andorra,020\nAL,Albania,008\n")

        assertTrue(document.isValid)
        assertEquals(',', document.delimiter)
        assertEquals(listOf("code", "name", "numeric"), document.header?.fields?.map { it.value })
        assertEquals(
            listOf(listOf("AD", "Andorra", "020"), listOf("AL", "Albania", "008")),
            document.rows.map { row ->
                row.fields.map { it.value }
            }
        )
    }

    @Test
    fun aQuotedFieldHoldsDelimitersQuotesAndLineBreaks() {
        val text = "name,note\n\"Korea, Republic of\",\"say \"\"hi\"\"\nthen go\"\n"
        val document = CsvDocument.parse(text)

        assertEquals(
            listOf("Korea, Republic of", "say \"hi\"\nthen go"),
            document.rows.single().fields.map {
                it.value
            }
        )
        val first = document.rows.single().fields.first()
        assertEquals("\"Korea, Republic of\"", text.substring(first.range.start, first.range.end))
        assertTrue(first.quoted)
    }

    @Test
    fun theDelimiterIsToldByTheLines() {
        assertEquals(';', CsvDocument.parse("a;b;c\n1;2,5;3\n").delimiter)
        assertEquals('\t', CsvDocument.parse("a\tb\n1\t2\n").delimiter)
        assertEquals('|', CsvDocument.parse("a|b\n1|2\n").delimiter)
        assertEquals(',', CsvDocument.parse("\"a;b\",c\n\"1;2\",3\n").delimiter)
        assertEquals('\t', CsvDocument.parse("a,b\tc\n", delimiter = '\t').delimiter)
    }

    @Test
    fun emptyLinesAreNoRecordsAndEveryLineEndingIsRead() {
        val document = CsvDocument.parse("\uFEFFa,b\r\n\r\n1,2\r3,4")

        assertEquals(listOf(listOf("a", "b"), listOf("1", "2"), listOf("3", "4")), document.values())
    }

    @Test
    fun anEmptyFieldIsAnEmptyValue() {
        assertEquals(listOf(listOf("a", "", "c"), listOf("", "", "")), CsvDocument.parse("a,,c\n,,\n").values())
    }

    @Test
    fun aQuoteNeverClosedIsAnError() {
        val document = CsvDocument.parse("a,b\n1,\"open\n")

        assertFalse(document.isValid)
        assertEquals(listOf(ProblemCode.UnclosedQuote), document.codes())
        assertEquals(2, document.problems.single().start.line)
        assertEquals(3, document.problems.single().start.column)
    }

    @Test
    fun whatMostReadersTakeIsAWarning() {
        val document = CsvDocument.parse("a,b\n1,2,3\nx\"y,\"q\"z\n")

        assertTrue(document.isValid)
        assertEquals(
            listOf(ProblemCode.FieldCount, ProblemCode.StrayQuote, ProblemCode.TextAfterQuote),
            document.codes()
        )
        assertTrue(document.problems.all { it.severity == Severity.Warning })
        assertEquals(listOf("x\"y", "qz"), document.records.last().fields.map { it.value })
    }

    @Test
    fun formattingQuotesOnlyWhatNeedsIt() {
        val text = "\"code\",\"name\"\r\n\"AD\",\"Andorra, Principality\"\r\n\"Q\",\"say \"\"hi\"\"\"\r\n"

        assertEquals(
            "code,name\r\nAD,\"Andorra, Principality\"\r\nQ,\"say \"\"hi\"\"\"\r\n",
            CsvDocument.parse(text).formatted()
        )
    }

    @Test
    fun formattingKeepsTheValuesAndTheDelimiter() {
        val text = "a;b\n\" x \";\"1;2\"\n"
        val formatted = CsvDocument.parse(text).formatted()

        assertEquals("a;b\n x ;\"1;2\"\n", formatted)
        assertEquals(CsvDocument.parse(text).values(), CsvDocument.parse(formatted, delimiter = ';').values())
    }

    @Test
    fun aDocumentThatIsNotValidIsNotWrittenOut() {
        assertFailsWith<IllegalArgumentException> { CsvDocument.parse("a\n\"b\n").formatted() }
    }

    @Test
    fun rowsAreWrittenAsCsv() {
        assertEquals(
            "id,note\n1,\"a, b\"\n2,\n",
            csvText(listOf(listOf("id", "note"), listOf("1", "a, b"), listOf("2", "")))
        )
    }
}
