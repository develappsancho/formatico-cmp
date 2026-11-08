package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DocumentSummaryTest {
    /** The Summary board's order. */
    private val order =
        """
        {
          "id": "ord_8842",
          "status": "shipped",
          "createdAt": "2026-10-28T14:32:00Z",
          "total": 129.90,
          "currency": "EUR",
          "paid": true,
          "coupon": null,
          "customer": {
            "name": "Elif Yılmaz",
            "email": "elif@example.com",
            "avatar": "https://images.example.com/u/elif.png",
            "website": "https://elif.dev"
          },
          "theme": {
            "accent": "#0F766E"
          },
          "items": [
            { "sku": "TS-001", "name": "T-shirt", "qty": 2, "price": 39.95 },
            { "sku": "MG-014", "name": "Mug", "qty": 1, "price": 50.00 }
          ],
          "shipping": {
            "method": "express",
            "eta": "2026-11-04",
            "address": {
              "city": "İzmir",
              "country": "TR"
            }
          }
        }
        """.trimIndent()

    private fun summary(text: String, format: Format = Format.Json) =
        summaryOf((readDocument(text, format) as Reading.Read).root!!)

    @Test
    fun theBoardsOrderComesToWhatItsSummarySays() {
        val summary = summary(order)

        assertEquals(32, summary.values)
        assertEquals(7, summary.objects)
        assertEquals(1, summary.arrays)
        assertEquals(29, summary.keys)
        assertEquals(0, summary.repeatedKeys)
        assertEquals(3 to "$.items[0].sku", summary.deepest!!.let { it.amount to it.node.key })
        assertEquals(2 to "$.items", summary.longestArray!!.let { it.amount to it.node.key })
        assertEquals(37 to "$.customer.avatar", summary.longestText!!.let { it.amount to it.node.key })
        assertEquals(5, summary.numbers)
        assertEquals(listOf(Respelled("129.90", "129.9"), Respelled("50.00", "50")), summary.respelled)
        assertEquals(
            listOf(
                ValueKind.String to 17,
                ValueKind.Object to 7,
                ValueKind.Number to 5,
                ValueKind.Array to 1,
                ValueKind.Boolean to 1,
                ValueKind.Null to 1
            ),
            summary.kinds
        )
        assertEquals(
            listOf(
                FoundKind.Dates to listOf("$.createdAt", "$.shipping.eta"),
                FoundKind.Pictures to listOf("$.customer.avatar"),
                FoundKind.Links to listOf("$.customer.website"),
                FoundKind.Colours to listOf("$.theme.accent")
            ),
            summary.found.map { found -> found.kind to found.nodes.map { it.key } }
        )
        assertEquals(listOf(Triple("$.items", 2, 4)), summary.tables.map { Triple(it.node.key, it.rows, it.keys) })
    }

    @Test
    fun aKeyAnObjectHasBeforeIsRepeatedAndArraysOfOtherKeysAreNoTable() {
        val summary = summary("{\"a\": 1, \"a\": 2, \"rows\": [{\"x\": 1}, {\"y\": 2}]}")

        assertEquals(1, summary.repeatedKeys)
        assertEquals(emptyList(), summary.tables)
    }

    @Test
    fun aDocumentOfOneValueHasNoDepthAndNoArray() {
        val summary = summary("\"just text\"")

        assertEquals(1, summary.values)
        assertNull(summary.deepest)
        assertNull(summary.longestArray)
        assertEquals(9, summary.longestText!!.amount)
    }

    @Test
    fun aNumberIsReadAsJavaScriptWritesIt() {
        assertEquals("129.9", readAsNumber("129.90"))
        assertEquals("50", readAsNumber("50.00"))
        assertEquals("1000", readAsNumber("1E3"))
        assertEquals("1e-7", readAsNumber("0.0000001"))
        // An integer past what a double holds exactly loses its last digits.
        assertNotNull(readAsNumber("12345678901234567890"))
        assertNull(readAsNumber("42"))
        assertNull(readAsNumber("3.14"))
        assertNull(readAsNumber("0x1F"))
    }
}
