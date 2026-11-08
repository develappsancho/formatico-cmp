package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.convert.DataString
import com.developersancho.formatico.core.format.diff.Change
import com.developersancho.formatico.core.format.diff.KeysMoved
import com.developersancho.formatico.core.format.diff.NumbersRespelled
import com.developersancho.formatico.core.format.json.jsonPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DocumentComparisonTest {
    private val orderA = OpenDocument(id = 1, name = "order.json", text = PreviewCompareA, format = Format.Json)
    private val orderB = OpenDocument(id = 2, name = "order-v2.json", text = PreviewCompareB, format = Format.Json)

    private fun compare(a: OpenDocument, b: OpenDocument?, by: CompareBy = CompareBy.Meaning) =
        comparisonOf(a, readDocument(a.text, a.format), b, b?.let { readDocument(it.text, it.format) }, by)

    @Test
    fun theBoardsOrdersDifferInFourValuesEachOnItsLines() {
        val compared = assertIs<CompareOutcome.Compared>(compare(orderA, orderB))
        assertEquals(
            listOf("$.status", "$.deliveredAt", "$.coupon", "$.items[1].qty"),
            compared.differences.map {
                jsonPath(checkNotNull(it.path))
            }
        )
        assertEquals(listOf(Change.Changed, Change.Added, Change.Removed, Change.Changed), compared.differences.map { it.change })
        assertEquals(listOf("\"shipped\"", null, "null", "1"), compared.differences.map { it.before })
        assertEquals(listOf("\"delivered\"", "\"2026-11-01T10:05:00Z\"", null, "2"), compared.differences.map { it.after })
        // The boards mark A's lines 3, 6 and 9 and B's 2, 5 and 9, counted from one.
        assertEquals(listOf(2..2, IntRange.EMPTY, 5..5, 8..8), compared.differences.map { it.aLines })
        assertEquals(listOf(1..1, 4..4, IntRange.EMPTY, 8..8), compared.differences.map { it.bLines })
        assertEquals(KeysMoved("status", "id", 1), compared.moved)
        assertEquals(NumbersRespelled("129.90", "129.9", 1), compared.respelled)
        assertEquals(11, compared.a.lines)
        assertEquals("order-v2.json", compared.b.name)
    }

    @Test
    fun theOrdersTextsDifferInRunsOfLines() {
        val compared = assertIs<CompareOutcome.Compared>(compare(orderA, orderB, CompareBy.Text))
        assertEquals(listOf(Change.Added, Change.Removed, Change.Changed, Change.Changed), compared.differences.map { it.change })
        assertTrue(compared.differences.all { it.path == null })
        assertEquals("\"status\": \"delivered\",", compared.differences.first().after)
        assertNull(compared.moved)
    }

    @Test
    fun aDocumentMeansWhatItsYamlMeans() {
        val converted =
            assertIs<ConvertOutcome.Converted>(conversionOf(orderA, readDocument(orderA.text, orderA.format), ConvertChoice(), ""))
        val yaml = OpenDocument(id = 2, name = "order.yaml", text = converted.text, format = Format.Yaml)
        val compared = assertIs<CompareOutcome.Compared>(compare(orderA, yaml))
        assertEquals(emptyList(), compared.differences)
    }

    @Test
    fun aDocumentAloneHasNothingToCompareWithAndOneUnreadOrInvalidIsSaidSo() {
        assertEquals(CompareOutcome.Alone, compare(orderA, null))
        val plain = OpenDocument(id = 2, name = "notes.txt", text = "Notes\n", format = null)
        assertEquals(CompareOutcome.Unread(Side.B, "notes.txt", OtherDocument(2, "notes.txt")), compare(orderA, plain))
        val broken = OpenDocument(id = 2, name = "settings.json", text = PreviewInvalid, format = Format.Json)
        val invalid = assertIs<CompareOutcome.Invalid>(compare(orderA, broken))
        assertEquals(Side.B, invalid.side)
        // Text compares whatever it is.
        assertIs<CompareOutcome.Compared>(compare(orderA, plain, CompareBy.Text))
    }

    @Test
    fun aDocumentIsComparedWithTheOneItsViewNamesOrTheTabAfterIt() {
        val third = OpenDocument(id = 3, name = "third.json", text = "{}", format = Format.Json)
        val documents = listOf(orderA, orderB, third)
        assertEquals(2L, partnerOf(1, documents, CompareChoice())?.id)
        assertEquals(3L, partnerOf(1, documents, CompareChoice(with = 3))?.id)
        assertEquals(2L, partnerOf(3, documents, CompareChoice())?.id)
        assertEquals(2L, partnerOf(1, documents, CompareChoice(with = 1))?.id)
        assertNull(partnerOf(1, listOf(orderA), CompareChoice()))
    }

    @Test
    fun aValueIsShownShortOnARow() {
        assertEquals("\"a\"", shortText(DataString("a")))
        val long = DataString("x".repeat(200))
        assertEquals(80, shortText(long).length)
        assertTrue(shortText(long).endsWith("…"))
    }
}
