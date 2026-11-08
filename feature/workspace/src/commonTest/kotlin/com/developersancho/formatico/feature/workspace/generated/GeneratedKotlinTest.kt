package com.developersancho.formatico.feature.workspace.generated

import com.developersancho.formatico.feature.workspace.PreviewBoardOrder
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The Kotlin Types writes of the boards' order, compiled in [GeneratedOrder]'s file, reading that order. */
class GeneratedKotlinTest {
    @Test
    fun theKotlinWrittenOfTheOrderReadsTheOrder() {
        val order = Json.decodeFromString<Order>(PreviewBoardOrder)

        assertEquals("ord_8842", order.id)
        assertEquals(129.9, order.total)
        assertNull(order.coupon)
        assertEquals(listOf("TS-001", "MG-014"), order.items.map { it.sku })
        assertEquals(2, order.items.first().qty)
        assertEquals("İzmir", order.shipping.address.city)
        // And writes it back as it read it, but for the number's spelling.
        assertEquals(order, Json.decodeFromString<Order>(Json.encodeToString(order)))
    }
}
