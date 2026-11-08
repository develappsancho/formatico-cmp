package com.developersancho.formatico

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.storage.memoryStorageModule
import com.developersancho.formatico.core.testing.awaitNode
import com.developersancho.formatico.core.testing.awaitStatus
import com.developersancho.formatico.core.testing.awaitText
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.hasPantrixTag
import com.pantrixui.testing.onPantrixNode
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppTest {
    // KoinApplication keeps its context once the app is gone: without this, a test's documents stay open in the next.
    @AfterTest
    fun closeTheApp() {
        stopKoin()
    }

    @Test
    fun theAppOpensOnStartUnderItsBar() = runComposeUiTest {
        setContent { App(systemDark = false, storage = memoryStorageModule, network = boardNetworkModule) }

        awaitText("Paste anything")
        awaitText("Formatico")
    }

    @Test
    fun theBarSwitchesTheTheme() = runComposeUiTest {
        setContent { App(systemDark = false, storage = memoryStorageModule, network = boardNetworkModule) }
        awaitText("Paste anything")
        val theme = onPantrixNode("IconButton", "theme")
        val before = theme.fetchSemanticsNode().config.toString()

        theme.performClick()

        val after = onPantrixNode("IconButton", "theme").fetchSemanticsNode().config.toString()
        check(before != after) { "The theme's switch did not change: $after" }
    }

    @Test
    fun aSampleOpensInATabOfItsOwnAndANewTabGoesBackToStart() = runComposeUiTest {
        setContent { App(systemDark = false, storage = memoryStorageModule, network = boardNetworkModule) }
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
                App(systemDark = false, storage = memoryStorageModule, network = boardNetworkModule)
            }
        }
        awaitText("Paste anything")
        onPantrixNode("Button", "switch_documents").assertDoesNotExist()
        onPantrixNode("ListItem", "sample_order").performClick()
        awaitStatus("Valid JSON")

        onPantrixNode("Button", "switch_documents").performClick()
        onPantrixNode("MenuItem", "new", scope = "Menu_documents").performClick()
        awaitText("Paste anything")
        val switcher = "New tab, beside 1 open document. Switch documents"
        awaitNode(hasPantrixTag("Button", "switch_documents") and hasContentDescription(switcher), switcher)

        onPantrixNode("Button", "switch_documents").performClick()
        onPantrixNode("MenuItem", "document_1", scope = "Menu_documents").performClick()
        awaitStatus("Valid JSON")
    }

    @Test
    fun aPhonesConversionHasABarOfItsOwnInTheAppBarsPlace() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = PantrixWindow(DpSize(390.dp, 844.dp))) {
                App(systemDark = false, storage = memoryStorageModule, network = boardNetworkModule)
            }
        }
        awaitText("Paste anything")
        onPantrixNode("ListItem", "sample_order").performClick()
        awaitStatus("Valid JSON")

        onPantrixNode("IconButton", "more").performClick()
        // The test draws in less than the phone's window, and the menu's row stands past its foot: it is pressed as a
        // screen reader presses it (Pantrix's testing guide, 3.3).
        onPantrixNode("MenuItem", "convert", scope = "Menu_document").performSemanticsAction(SemanticsActions.OnClick)
        awaitText("Convert order.json")
        onPantrixNode("Button", "switch_documents").assertDoesNotExist()

        onPantrixNode("IconButton", "back").performClick()
        awaitStatus("Valid JSON")
        onPantrixNode("Button", "switch_documents").assertExists()
    }

    @Test
    fun aPhonesComparisonHasABarOfItsOwnInTheAppBarsPlace() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = PantrixWindow(DpSize(390.dp, 844.dp))) {
                App(systemDark = false, storage = memoryStorageModule, network = boardNetworkModule)
            }
        }
        awaitText("Paste anything")
        onPantrixNode("ListItem", "sample_order").performClick()
        awaitStatus("Valid JSON")

        onPantrixNode("IconButton", "more").performClick()
        // The menu's row stands past the foot of what the test draws: it is pressed as a screen reader presses it.
        onPantrixNode("MenuItem", "compare", scope = "Menu_document").performSemanticsAction(SemanticsActions.OnClick)
        // The one document open has nothing to compare with.
        awaitText("Nothing to compare with")
        onPantrixNode("Button", "switch_documents").assertDoesNotExist()

        onPantrixNode("IconButton", "back").performClick()
        awaitStatus("Valid JSON")
        onPantrixNode("Button", "switch_documents").assertExists()
    }
}
