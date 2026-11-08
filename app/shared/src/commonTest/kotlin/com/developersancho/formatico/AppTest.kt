package com.developersancho.formatico

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.storage.memoryStorageModule
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.hasPantrixTag
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
    private suspend fun ComposeUiTest.awaitText(text: String) = awaitNode(hasText(text), "\"$text\"")

    /** Waits, as [awaitText] does, for one node that [matcher] finds, called [what] where it is not found. */
    private suspend fun ComposeUiTest.awaitNode(matcher: SemanticsMatcher, what: String) {
        val waited = TimeSource.Monotonic.markNow()
        while (onAllNodes(matcher).fetchSemanticsNodes().size != 1) {
            check(waited.elapsedNow() < 10.seconds) { "$what was not shown in 10 seconds" }
            withContext(Dispatchers.Default) { delay(20.milliseconds) }
            awaitIdle()
        }
    }

    @Test
    fun theAppOpensOnStartUnderItsBar() = runComposeUiTest {
        setContent { App(systemDark = false, storage = memoryStorageModule) }

        awaitText("Paste anything")
        awaitText("Formatico")
    }

    @Test
    fun theBarSwitchesTheTheme() = runComposeUiTest {
        setContent { App(systemDark = false, storage = memoryStorageModule) }
        awaitText("Paste anything")
        val theme = onPantrixNode("IconButton", "theme")
        val before = theme.fetchSemanticsNode().config.toString()

        theme.performClick()

        val after = onPantrixNode("IconButton", "theme").fetchSemanticsNode().config.toString()
        check(before != after) { "The theme's switch did not change: $after" }
    }

    @Test
    fun aSampleOpensInATabOfItsOwnAndANewTabGoesBackToStart() = runComposeUiTest {
        setContent { App(systemDark = false, storage = memoryStorageModule) }
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

    @Test
    fun aPhonesStartLeadsBackToTheDocumentsLeftOpen() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = PantrixWindow(DpSize(390.dp, 844.dp))) {
                App(systemDark = false, storage = memoryStorageModule)
            }
        }
        awaitText("Paste anything")
        onPantrixNode("Button", "switch_documents").assertDoesNotExist()
        onPantrixNode("ListItem", "sample_order").performClick()
        awaitText("Valid JSON")

        onPantrixNode("Button", "switch_documents").performClick()
        onPantrixNode("MenuItem", "new", scope = "Menu_documents").performClick()
        awaitText("Paste anything")
        val switcher = "New tab, beside 1 open document. Switch documents"
        awaitNode(hasPantrixTag("Button", "switch_documents") and hasContentDescription(switcher), switcher)

        onPantrixNode("Button", "switch_documents").performClick()
        onPantrixNode("MenuItem", "document_1", scope = "Menu_documents").performClick()
        awaitText("Valid JSON")
    }
}
