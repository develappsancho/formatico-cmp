package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TreeToolsTest {
    private val order =
        """
        {
          "id": "ord_8842",
          "customer": {
            "name": "Elif Yılmaz",
            "email": "elif@example.com",
            "avatar": "https://images.example.com/u/elif.png",
            "website": "https://elif.dev"
          },
          "items": [{"sku": "TS-001", "tags": ["cotton"]}],
          "city": "Isparta",
          "from": "İzmir"
        }
        """.trimIndent()

    private val root = readDocument(order, Format.Json).let { (it as Reading.Read).root!! }

    @Test
    fun theTreeOpensToALevelOrWhole() {
        assertEquals(setOf("$"), expandedTo(root, 1))
        assertEquals(setOf("$", "$.customer", "$.items"), expandedTo(root, 2))
        assertEquals(setOf("$", "$.customer", "$.items", "$.items[0]"), expandedTo(root, 3))
        assertEquals(setOf("$", "$.customer", "$.items", "$.items[0]", "$.items[0].tags"), expandedTo(root, null))
        assertTrue(expandedTo(null, null).isEmpty())
    }

    @Test
    fun aSearchFindsKeysAndValuesInTheTreesOrder() {
        val found = matchesOf(root, "elif", "en").map { it.key }

        assertEquals(listOf("$.customer.name", "$.customer.email", "$.customer.avatar", "$.customer.website"), found)
        // A branch is found by its name; the root and a blank search find nothing.
        assertEquals(listOf("$.customer"), matchesOf(root, "custom", "en").map { it.key })
        assertTrue(matchesOf(root, "  ", "en").isEmpty())
        assertTrue(matchesOf(root, "$", "en").isEmpty())
    }

    @Test
    fun turkishLettersAreMatchedSmallAsTurkishWritesThem() {
        assertEquals(listOf("$.city"), matchesOf(root, "ısparta", "tr").map { it.key })
        assertEquals(listOf("$.from"), matchesOf(root, "izmir", "tr").map { it.key })
        assertEquals("ısparta izmir", folded("ISPARTA İZMİR", "tr"))
        assertEquals("isparta", folded("ISPARTA", "en"))
    }
}
