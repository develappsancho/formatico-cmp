package com.developersancho.formatico

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.storage.MemorySmallStore
import com.developersancho.formatico.core.storage.MemoryTextStore
import com.developersancho.formatico.feature.start.OpenedDocument
import com.developersancho.formatico.feature.start.StoredDocumentHistory
import com.developersancho.formatico.feature.workspace.DocumentEdit
import com.developersancho.formatico.feature.workspace.Workbench
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class FollowEditsTest {
    /** Lets what runs in the background, the keeper and its pause before it writes among it, run its course. */
    private fun TestScope.settle() {
        advanceTimeBy(1.seconds)
        runCurrent()
    }

    @Test
    fun aDocumentChangedInItsWorkspaceIsListedAsEditedAndOneOnlyOpenedIsNot() = runTest {
        val opened = Instant.parse("2026-11-04T09:00:00Z")
        val clock = object : Clock {
            override fun now(): Instant = opened + 5.minutes
        }
        val history = StoredDocumentHistory(MemorySmallStore(), MemoryTextStore(), backgroundScope)
        val workbench = Workbench(worker = StandardTestDispatcher(testScheduler))
        backgroundScope.launch { followEdits(workbench, history, clock) }
        val order = OpenedDocument("order.json", "{\"a\":1}", Format.Json)
        val other = OpenedDocument("other.json", "[]", Format.Json)
        val orderId = workbench.open(order.name, order.text, order.format, history.record(order, opened))
        workbench.open(other.name, other.text, other.format, history.record(other, opened))
        settle()

        workbench.edit(orderId, DocumentEdit.Format)
        settle()

        val (top, below) = history.documents.value
        assertEquals("order.json", top.name)
        assertTrue(top.edited)
        assertEquals(opened + 5.minutes, top.at)
        assertFalse(below.edited)
        assertEquals(workbench.state.value.documents.first().text, history.document(top.id)?.text)
    }

    @Test
    fun aDocumentTheHistoryForgetsWhileItIsOpenLeavesIt() = runTest {
        val history = StoredDocumentHistory(MemorySmallStore(), MemoryTextStore(), backgroundScope)
        val workbench = Workbench(worker = StandardTestDispatcher(testScheduler))
        val id = history.record(OpenedDocument("order.json", "{}", Format.Json), Instant.parse("2026-11-04T09:00:00Z"))
        val open = workbench.open("order.json", "{}", Format.Json, historyId = id)
        backgroundScope.launch { followForgotten(workbench, history) }
        settle()
        assertEquals(id, workbench.state.value.documents.single().historyId)

        history.clear()
        settle()

        assertNull(workbench.state.value.documents.single { it.id == open }.historyId)
    }
}
