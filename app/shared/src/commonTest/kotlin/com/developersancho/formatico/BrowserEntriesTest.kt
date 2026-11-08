package com.developersancho.formatico

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.feature.workspace.Workbench
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserEntriesTest {
    private val workbench = Workbench(worker = UnconfinedTestDispatcher())

    @Test
    fun startAndTheDocumentShownOverItAreTheAddresses() {
        assertEquals(listOf("#start"), browserEntries(selectedId = null))
        assertEquals(listOf("#start", "#document?id=3"), browserEntries(selectedId = 3))
    }

    @Test
    fun theBrowsersEntriesShowTheDocumentTheyNameOrStart() {
        val order = workbench.open("order.json", "{}", Format.Json)
        val other = workbench.open("other.json", "[]", Format.Json)

        workbench.showBrowserEntries(listOf("#start", "#document?id=$order"))
        assertEquals(order, workbench.state.value.selectedId)

        workbench.showBrowserEntries(listOf("#start"))
        assertNull(workbench.state.value.selectedId)

        workbench.showBrowserEntries(listOf("#document?id=$other"))
        assertEquals(other, workbench.state.value.selectedId)
    }

    @Test
    fun aDocumentNoLongerOpenGoesToStartAndAnAddressFormaticoDidNotWriteChangesNothing() {
        val order = workbench.open("order.json", "{}", Format.Json)
        workbench.showBrowserEntries(listOf("#start", "#document?id=99"))
        assertNull(workbench.state.value.selectedId)

        workbench.select(order)
        listOf(listOf(""), listOf("#share=abc"), listOf("#document?id=x"), emptyList()).forEach { entries ->
            workbench.showBrowserEntries(entries)
            assertEquals(order, workbench.state.value.selectedId, "$entries")
        }
    }

    @Test
    fun settingsIsAnAddressOverTheScreenItWasOpenedFrom() {
        assertEquals(listOf("#start", "#document?id=3", "#settings"), browserEntries(selectedId = 3, settings = true))
        val order = workbench.open("order.json", "{}", Format.Json)
        var settings: Boolean? = null

        workbench.showBrowserEntries(listOf("#start", "#document?id=$order", "#settings")) { settings = it }
        assertEquals(true, settings)
        assertEquals(order, workbench.state.value.selectedId)

        workbench.showBrowserEntries(listOf("#start")) { settings = it }
        assertEquals(false, settings)
        assertNull(workbench.state.value.selectedId)

        settings = null
        workbench.showBrowserEntries(listOf("#elsewhere")) { settings = it }
        assertNull(settings)
    }
}
