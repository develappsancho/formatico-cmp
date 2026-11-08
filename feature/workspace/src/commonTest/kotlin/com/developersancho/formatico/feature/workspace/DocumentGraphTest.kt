package com.developersancho.formatico.feature.workspace

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DocumentGraphTest {
    private val order = assertNotNull((readDocument(PreviewBoardOrder, Format.Json) as Reading.Read).root)

    private fun graphOf(root: ValueNode, open: Set<String> = graphOpening(root)) =
        documentGraph(root, "order.json", open, 200.dp) { it.title }

    @Test
    fun theOrderIsACardForEachObjectAndArrayLinkedByItsKeyOrItsPlace() {
        val graph = graphOf(order)
        assertEquals(
            listOf("$", "$.customer", "$.theme", "$.items", "$.shipping", "$.items[0]", "$.items[1]", "$.shipping.address"),
            graph.nodes.map { it.key }
        )
        assertEquals(
            listOf("order.json", "customer", "theme", "items", "shipping", "items[0]", "items[1]", "address"),
            graph.nodes.map { graph.cards.getValue(it.key).title }
        )
        assertEquals(
            listOf("customer", "theme", "items", "shipping", "[0]", "[1]", "address"),
            graph.links.map { it.label }
        )
        // A card's tag is its place in the tree, short and its own.
        assertEquals(listOf("root", "7", "8", "9", "10", "9_0", "9_1", "10_2"), graph.nodes.map { it.tagKey })
        // As tall as its head and its rows, and the room under them: the order's seven values, an empty array's head.
        assertEquals(DpSize(200.dp, 32.dp + 24.dp * 7 + 6.dp), graph.nodes.first().size)
        assertEquals(DpSize(200.dp, 32.dp), graph.nodes.first { it.key == "$.items" }.size)
        assertEquals(
            listOf("id", "status", "createdAt", "total", "currency", "paid", "coupon"),
            graph.cards.getValue("$").rows.map {
                it.name
            }
        )
    }

    @Test
    fun aClosedCardHasItsChildrenAndNoMore() {
        // The root closed: its children, which its chevron opens, and none of theirs.
        val graph = graphOf(order, open = emptySet())
        assertEquals(listOf("$", "$.customer", "$.theme", "$.items", "$.shipping"), graph.nodes.map { it.key })
        assertEquals(listOf("customer", "theme", "items", "shipping"), graph.links.map { it.label })
    }

    @Test
    fun aCardShowsItsFirstFiftyValuesAndHowManyMore() {
        val totals = assertNotNull((readDocument((1..60).joinToString(", ", "{ \"totals\": [", "] }\n"), Format.Json) as Reading.Read).root)
        val card = graphOf(totals).cards.getValue("$.totals")
        assertEquals(MOST_CARD_ROWS, card.rows.size)
        assertEquals(10, card.more)
        assertEquals(MOST_CARD_ROWS + 1, card.shownRows)
    }

    @Test
    fun aLargeDocumentOpensToItsSecondLevelAndASmallOneAll() {
        assertEquals(
            setOf("$", "$.customer", "$.theme", "$.items", "$.items[0]", "$.items[1]", "$.shipping", "$.shipping.address"),
            graphOpening(order)
        )
        val people = assertNotNull((readDocument(peopleOf(150), Format.Json) as Reading.Read).root)
        val open = graphOpening(people)
        assertTrue("$" in open && "$[0]" in open && "$[0].address" !in open, "$open")
    }

    @Test
    fun aValueChosenIsInTheCardThatHoldsIt() {
        val customer = trail(order, "$.customer.email").last()
        assertEquals("$.customer", cardKeyOf(order, customer))
        assertEquals("$.items[0]", cardKeyOf(order, trail(order, "$.items[0].qty").last()))
        assertEquals("$", cardKeyOf(order, trail(order, "$.coupon").last()))
        assertEquals("$.shipping", cardKeyOf(order, trail(order, "$.shipping").last()))
        assertNull(cardKeyOf(order, null))
    }
}
