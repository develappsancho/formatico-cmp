package com.developersancho.formatico

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.pantrixui.testing.onPantrixNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@OptIn(ExperimentalTestApi::class)
class AppTest {
    // KoinApplication keeps its context once the app is gone: without this, a test's documents stay open in the next.
    @AfterTest
    fun closeTheApp() {
        stopKoin()
    }

    /**
     * Waits for [text] to be shown once, letting the platform run meanwhile: in a browser the app's words are fetched
     * after its first frame, and the fetch ends only while the page's event loop runs, which `waitUntil` holds.
     */
    private suspend fun ComposeUiTest.awaitText(text: String) {
        val waited = TimeSource.Monotonic.markNow()
        while (onAllNodes(hasText(text)).fetchSemanticsNodes().size != 1) {
            check(waited.elapsedNow() < 10.seconds) { "\"$text\" was not shown in 10 seconds" }
            withContext(Dispatchers.Default) { delay(20.milliseconds) }
            awaitIdle()
        }
    }

    @Test
    fun theAppOpensOnStartUnderItsBar() = runComposeUiTest {
        setContent { App() }

        awaitText("Paste anything")
        awaitText("Formatico")
    }

    @Test
    fun theBarSwitchesTheTheme() = runComposeUiTest {
        setContent { App() }
        awaitText("Paste anything")
        val theme = onPantrixNode("IconButton", "theme")
        val before = theme.fetchSemanticsNode().config.toString()

        theme.performClick()

        val after = onPantrixNode("IconButton", "theme").fetchSemanticsNode().config.toString()
        check(before != after) { "The theme's switch did not change: $after" }
    }

    @Test
    fun aSampleOpensInATabOfItsOwnAndANewTabGoesBackToStart() = runComposeUiTest {
        setContent { App() }
        awaitText("Paste anything")

        onPantrixNode("ListItem", "sample_order").performClick()

        awaitText("Valid JSON")
        onPantrixNode("Tab", "document_1", scope = "Tabs_documents").assertExists()
        onPantrixNode("Tab", "new", scope = "Tabs_documents").assertDoesNotExist()

        onPantrixNode("IconButton", "new_tab").performClick()

        awaitText("Paste anything")
        onPantrixNode("Tab", "document_1", scope = "Tabs_documents").assertExists()
        onPantrixNode("Tab", "new", scope = "Tabs_documents").assertExists()
    }
}
