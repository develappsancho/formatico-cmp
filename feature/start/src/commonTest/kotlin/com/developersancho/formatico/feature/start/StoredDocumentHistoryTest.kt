package com.developersancho.formatico.feature.start

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.storage.MemorySmallStore
import com.developersancho.formatico.core.storage.MemoryTextStore
import com.developersancho.formatico.core.storage.historyTextName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

// The history writes in the background: runCurrent runs what it has waiting, advanceUntilIdle would not.
@OptIn(ExperimentalCoroutinesApi::class)
class StoredDocumentHistoryTest {
    private val small = MemorySmallStore()
    private val texts = MemoryTextStore()
    private val moment = Instant.parse("2026-11-04T09:41:00Z")
    private val order = OpenedDocument(name = "order.json", text = "{\"id\": \"ord_8842\"}", format = Format.Json)

    private fun TestScope.history(limit: Int = 20) =
        StoredDocumentHistory(small = small, texts = texts, scope = backgroundScope, limit = limit)

    @Test
    fun whatWasOpenedIsListedAgainWhenTheAppOpensAgain() = runTest {
        val id = history().record(order, moment)
        runCurrent()

        val again = history()
        runCurrent()

        val recent = again.documents.value.single()
        assertEquals(RecentDocument(id, "order.json", Format.Json, 18, moment), recent)
        assertEquals(order.copy(historyId = id), again.document(id))
    }

    @Test
    fun theSameDocumentOpenedTwiceIsListedOnceAtTheTop() = runTest {
        val history = history()
        val first = history.record(order, moment)
        history.record(order.copy(name = "orders.json"), moment + 1.minutes)
        val again = history.record(order, moment + 2.minutes)

        assertEquals(first, again)
        assertEquals(listOf("order.json", "orders.json"), history.documents.value.map { it.name })
    }

    @Test
    fun anEditMovesTheDocumentToTheTopAsEditedWithItsNewText() = runTest {
        val history = history()
        val id = history.record(order, moment)
        history.record(order.copy(name = "other.json"), moment + 1.minutes)

        history.edit(id, "{\n  \"id\": \"ord_8842\"\n}\n", moment + 2.minutes)
        runCurrent()

        val top = history.documents.value.first()
        assertEquals(id, top.id)
        assertTrue(top.edited)
        assertEquals(23, top.size)
        assertEquals(moment + 2.minutes, top.at)
        assertEquals("{\n  \"id\": \"ord_8842\"\n}\n", history().also { runCurrent() }.document(id)?.text)
    }

    @Test
    fun theOldestGoesWithItsTextPastTheLimit() = runTest {
        val history = history(limit = 2)
        val oldest = history.record(order.copy(name = "a.json"), moment)
        history.record(order.copy(name = "b.json"), moment + 1.minutes)
        history.record(order.copy(name = "c.json"), moment + 2.minutes)
        runCurrent()

        assertEquals(listOf("c.json", "b.json"), history.documents.value.map { it.name })
        assertNull(history.document(oldest))
        assertEquals(2, texts.names().size)
        assertFalse("history-$oldest" in texts.names())
    }

    @Test
    fun clearingForgetsTheListAndEveryText() = runTest {
        val history = history()
        history.record(order, moment)
        history.record(order.copy(name = "b.json"), moment)
        history.clear()
        runCurrent()

        assertTrue(history.documents.value.isEmpty())
        assertTrue(texts.names().isEmpty())
        assertTrue(history().also { runCurrent() }.documents.value.isEmpty())
    }

    @Test
    fun documentsOpenedLongerAgoThanTheyAreKeptForAreForgottenWithTheirTexts() = runTest {
        val history = history()
        val old = history.record(order, moment - 31.days)
        val new = history.record(order.copy(name = "new.json"), moment)
        runCurrent()

        history.forgetBefore(moment - 30.days)
        runCurrent()

        assertEquals(listOf(new), history.documents.value.map { it.id })
        assertEquals(null, texts.read(historyTextName(old)))
        assertEquals(listOf(new), history().also { runCurrent() }.documents.value.map { it.id })
    }
}
