package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.types.TypeLanguage
import com.developersancho.formatico.core.storage.MemorySmallStore
import com.developersancho.formatico.core.storage.MemoryTextStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class WorkbenchKeeperTest {
    private val small = MemorySmallStore()
    private val texts = MemoryTextStore()

    /** Lets what runs in the background, the keeper and its pause before it writes among it, run its course. */
    private fun TestScope.settle() {
        advanceTimeBy(1.seconds)
        runCurrent()
    }

    /** A workbench as the app opens it: given back what was kept, and kept from then on. */
    private fun TestScope.opened(): Workbench {
        val workbench = Workbench(worker = StandardTestDispatcher(testScheduler))
        val keeper = WorkbenchKeeper(workbench = workbench, small = small, texts = texts, scope = backgroundScope)
        assertFalse(keeper.restored.value)
        keeper.start()
        settle()
        assertTrue(keeper.restored.value, "The keeper says when what was open is back")
        return workbench
    }

    @Test
    fun theOpenDocumentsComeBackInTheirTabsWithTheOneChosen() = runTest {
        // The history keeps the text of the document opened from it.
        texts.write("history-4", "{\"a\": 1}")
        val before = opened()
        val order = before.open("order.json", "{\"a\": 1}", Format.Json, historyId = 4)
        before.open("countries.csv", "code,name\nAF,Afghanistan\n", Format.Csv)
        before.select(order)
        settle()

        val after = opened().state.value

        assertEquals(listOf("order.json", "countries.csv"), after.documents.map { it.name })
        assertEquals(order, after.selectedId)
        val restored = after.documents.first()
        assertEquals("{\"a\": 1}", restored.text)
        assertEquals(4L, restored.historyId)
        assertEquals(DocumentPane.Table, after.documents.last().view.pane)
    }

    @Test
    fun aChangedTextIsKeptAndAClosedDocumentIsForgotten() = runTest {
        val before = opened()
        val order = before.open("order.json", "{\"a\":1}", Format.Json)
        val other = before.open("other.json", "[]", Format.Json)
        settle()

        before.edit(order, DocumentEdit.Format)
        before.close(other)
        settle()

        val after = opened().state.value.documents.single()
        assertEquals("{\n  \"a\": 1\n}", after.text.trimEnd())
        assertTrue(after.edited)
        assertEquals(setOf("open-$order"), texts.names())
    }

    @Test
    fun whatWasOpenedBeforeTheKeptOnesCameBackStaysBesideThem() = runTest {
        opened().open("order.json", "{}", Format.Json)
        settle()

        val workbench = Workbench(worker = StandardTestDispatcher(testScheduler))
        val pasted = workbench.open(null, "a: 1", Format.Yaml)
        WorkbenchKeeper(workbench = workbench, small = small, texts = texts, scope = backgroundScope).start()
        settle()

        val state = workbench.state.value
        assertEquals(listOf("order.json", null), state.documents.map { it.name })
        assertEquals(pasted, state.selectedId)
        assertEquals(state.documents.size, state.documents.map { it.id }.toSet().size)
    }

    @Test
    fun whatADocumentWasLeftShowingComesBackWithIt() = runTest {
        val before = opened()
        val order = before.open("order.json", "{\"items\": [{\"price\": 2}, {\"price\": 1}]}", Format.Json)
        before.changeView(order) {
            it.copy(
                expanded = setOf("$", "$.items"),
                selected = "$.items[1].price",
                pane = DocumentPane.Table,
                tableSort = TableSort("price", descending = true),
                hiddenColumns = setOf("name"),
                tableFilter = "1",
                types = TypesChoice(language = TypeLanguage.Swift, root = "Purchase")
            )
        }
        settle()

        val view = opened().state.value.documents.single().view

        assertEquals(setOf("$", "$.items"), view.expanded)
        assertEquals("$.items[1].price", view.selected)
        assertEquals(DocumentPane.Table, view.pane)
        assertEquals(TableSort("price", descending = true), view.tableSort)
        assertEquals(setOf("name"), view.hiddenColumns)
        assertEquals("1", view.tableFilter)
        assertEquals(TypesChoice(language = TypeLanguage.Swift, root = "Purchase"), view.types)
    }

    @Test
    fun aDocumentFromTheHistoryHasItsTextThereOnce() = runTest {
        texts.write("history-4", "{\"a\": 1}")
        val before = opened()
        before.open("order.json", "{\"a\": 1}", Format.Json, historyId = 4)
        settle()

        assertEquals(setOf("history-4"), texts.names())

        // The history writes the changes it is told of; the tab comes back with them.
        texts.write("history-4", "{\"a\": 2}")
        val after = opened().state.value.documents.single()
        assertEquals("{\"a\": 2}", after.text)
        assertEquals(4L, after.historyId)
    }

    @Test
    fun aDocumentTheHistoryForgetsKeepsATextOfItsOwn() = runTest {
        texts.write("history-4", "{\"a\": 1}")
        val before = opened()
        val order = before.open("order.json", "{\"a\": 1}", Format.Json, historyId = 4)
        settle()

        before.leaveHistory(order)
        settle()
        texts.delete("history-4")

        val after = opened().state.value.documents.single()
        assertEquals("{\"a\": 1}", after.text)
        assertEquals(null, after.historyId)
        assertEquals(setOf("open-$order"), texts.names())
    }
}
