package com.developersancho.formatico.core.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.developersancho.formatico.core.testing.awaitUntil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@OptIn(ExperimentalTestApi::class)
class BrowserHistoryTest {
    private val window = browserWindow()

    // The test's page is the test runner's: it gets its address back.
    private val page = window.location.origin + window.location.pathname + window.location.hash

    @AfterTest
    fun giveThePageItsAddressBack() {
        window.history.replaceState(null, "", page)
    }

    @Test
    fun theAppsScreensGoIntoTheTabsHistoryAndTheBrowsersBackComesBackToThem() = runComposeUiTest {
        window.history.replaceState(null, "", window.location.origin + window.location.pathname)
        val restored = mutableListOf<List<String>>()
        var entries by mutableStateOf(listOf("#start"))
        setContent { BrowserHistory(entries = entries, onRestore = { restored += it }) }

        // The first entry: an address with no screens of its own, which the app's screens then replace.
        awaitUntil("The first entry") { window.history.state == "#start" }
        assertEquals(listOf(listOf("")), restored)
        assertEquals("#start", window.location.hash)

        // The app's own move adds an entry.
        entries = listOf("#start", "#document?id=3")
        awaitUntil("The app's move") { window.location.hash == "#document?id=3" }
        assertEquals("#start\n#document?id=3", window.history.state)

        // The browser's back hands the app the screens of the entry it came to.
        window.history.go(-1)
        awaitUntil("The browser's back") { restored.size == 2 }
        assertEquals(listOf("#start"), restored.last())
        assertEquals("#start", window.location.hash)
    }

    @Test
    fun theAppOpeningOnAScreenOverAnotherGivesEachItsEntryAndTheBrowsersBackGoesUnderIt() = runComposeUiTest {
        window.history.replaceState(null, "", window.location.origin + window.location.pathname)
        val restored = mutableListOf<List<String>>()
        setContent {
            BrowserHistory(entries = listOf("#start", "#document?id=3"), onRestore = { restored += it })
        }

        // A new tab opening on a document kept from before: Start has an entry of its own under the document's.
        awaitUntil("The first entries") { window.history.state == "#start\n#document?id=3" }
        assertEquals("#document?id=3", window.location.hash)

        window.history.go(-1)
        awaitUntil("The browser's back") { restored.size == 2 }
        assertEquals(listOf("#start"), restored.last())
        assertEquals("#start", window.location.hash)
    }
}
