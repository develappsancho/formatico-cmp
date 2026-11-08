package com.developersancho.formatico.core.storage

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MemoryStoresTest {
    @Test
    fun aSmallValueIsReadBackUntilItIsDeleted() = runTest {
        val store = MemorySmallStore()
        val names = ListSerializer(String.serializer())

        assertNull(store.get("recent", names))
        store.put("recent", listOf("order.json", "deployment.yaml"), names)
        assertEquals(listOf("order.json", "deployment.yaml"), store.get("recent", names))
        store.delete("recent")
        assertNull(store.get("recent", names))
    }

    @Test
    fun aTextIsKeptUnderANameOfSmallLettersDigitsAndDashes() = runTest {
        val store = MemoryTextStore()

        store.write("history-7", "{\"a\": 1}")
        assertEquals("{\"a\": 1}", store.read("history-7"))
        assertEquals(setOf("history-7"), store.names())
        store.delete("history-7")
        assertNull(store.read("history-7"))

        assertFailsWith<IllegalArgumentException> { store.write("../order.json", "") }
        assertFailsWith<IllegalArgumentException> { store.read("Order") }
        assertFailsWith<IllegalArgumentException> { store.read("") }
    }
}
