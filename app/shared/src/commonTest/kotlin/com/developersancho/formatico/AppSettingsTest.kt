package com.developersancho.formatico

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.storage.memoryStorageModule
import com.developersancho.formatico.core.testing.awaitNode
import com.developersancho.formatico.core.testing.awaitText
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.keyboard.PantrixShortcut
import com.pantrixui.foundation.platform.OperatingSystem
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.TestClipboard
import com.pantrixui.testing.hasPantrixTag
import com.pantrixui.testing.onPantrixNode
import com.pantrixui.testing.performShortcut
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppSettingsTest {
    private val desktop = PantrixWindow(DpSize(1440.dp, 900.dp))
    private val phone = PantrixWindow(DpSize(390.dp, 844.dp))
    private val mac = OperatingSystem.MacOs

    @AfterTest
    fun closeTheApp() {
        stopKoin()
    }

    private fun ComposeUiTest.app(window: PantrixWindow = desktop, clipboard: TestClipboard? = null) = setContent {
        PantrixTestTheme(platform = PlatformType.Desktop, system = mac, window = window, clipboard = clipboard) {
            App(systemDark = false, storage = memoryStorageModule)
        }
    }

    @Test
    fun aWideWindowOpensSettingsInATabOfItsOwnThatChoosingAnotherTabLeaves() = runComposeUiTest {
        app()
        awaitText("Paste anything")
        onPantrixNode("ListItem", "sample_order").performClick()
        awaitText("Valid JSON")

        onPantrixNode("IconButton", "settings").performClick()

        awaitText("Keep a history of documents")
        onPantrixNode("Tab", "settings", scope = "Tabs_documents").assertIsSelected()
        // The theme's switch and the settings are in Settings itself.
        onPantrixNode("IconButton", "theme").assertDoesNotExist()
        onPantrixNode("IconButton", "settings").assertDoesNotExist()

        onPantrixNode("Tab", "document_1", scope = "Tabs_documents").performClick()
        awaitText("Valid JSON")
        onPantrixNode("Tab", "settings", scope = "Tabs_documents").assertDoesNotExist()
    }

    @Test
    fun aWideWindowGoesBackFromSettingsToTheTabItWasOpenedOverByItsName() = runComposeUiTest {
        app()
        awaitText("Paste anything")
        // Opened over a new tab, it goes back to it.
        onPantrixNode("IconButton", "settings").performClick()
        awaitText("Keep a history of documents")
        onPantrixNode("Button", "back").assert(hasContentDescription("Back to New tab")).performClick()
        awaitText("Paste anything")

        onPantrixNode("ListItem", "sample_order").performClick()
        awaitText("Valid JSON")
        onPantrixNode("IconButton", "settings").performClick()
        awaitText("Keep a history of documents")
        onPantrixNode("Button", "back").assert(hasContentDescription("Back to order.json")).performClick()
        awaitText("Valid JSON")
        onPantrixNode("Tab", "settings", scope = "Tabs_documents").assertDoesNotExist()
        onPantrixNode("Tab", "document_1", scope = "Tabs_documents").assertIsSelected()
    }

    @Test
    fun commandAndCommaOpensSettingsAndItsThemeIsTheAppsOwn() = runComposeUiTest {
        app()
        awaitText("Paste anything")

        onRoot().performShortcut(PantrixShortcut(Key.Comma, command = true), mac)

        awaitText("Light or dark, or as the system is.")
        onPantrixNode("Segment", "dark", scope = "SegmentButton_theme").performClick()
        onPantrixNode("IconButton", "new_tab").performClick()
        val light = "Use the light theme"
        awaitNode(hasPantrixTag("IconButton", "theme") and hasContentDescription(light), light)
    }

    @Test
    fun settingsListsThePalettesShortcutsAsTheSystemWritesThem() = runComposeUiTest {
        app()
        awaitText("Paste anything")
        onPantrixNode("IconButton", "settings").performClick()
        awaitText("Keep a history of documents")

        onPantrixNode("Item", "shortcuts", scope = "Sidebar_settings").performClick()
        awaitNode(hasPantrixTag("Keycaps", "palette") and hasContentDescription("⌘K"), "The palette's keys")
        onPantrixNode("Keycaps", "new").assert(hasContentDescription("⌘T"))
        onPantrixNode("Keycaps", "settings").assert(hasContentDescription("⌘,"))
        onPantrixNode("Keycaps", "format").assert(hasContentDescription("⌥⇧F"))
        onPantrixNode("Keycaps", "query").assert(hasContentDescription("⇧⌘F"))
        // The theme's command has no keys, and a list of them leaves it out.
        onPantrixNode("Keycaps", "theme").assertDoesNotExist()
    }

    @Test
    fun aBrowsersSettingsListsNoKeysTheBrowserKeeps() = runComposeUiTest {
        setContent {
            PantrixTestTheme(platform = PlatformType.Web, system = mac, window = desktop) {
                App(systemDark = false, storage = memoryStorageModule)
            }
        }
        awaitText("Paste anything")
        onPantrixNode("IconButton", "settings").performClick()
        awaitText("Keep a history of documents")

        awaitNode(hasPantrixTag("Keycaps", "palette"), "The palette's keys")
        onPantrixNode("Keycaps", "new").assertDoesNotExist()
        onPantrixNode("Keycaps", "tree").assertDoesNotExist()
        onPantrixNode("Keycaps", "query").assertExists()
    }

    @Test
    fun aPhonesSettingsIsAScreenWithABarOfItsOwn() = runComposeUiTest {
        app(window = phone)
        awaitText("Paste anything")

        onPantrixNode("IconButton", "settings").performClick()

        awaitText("Keep a history")
        onPantrixNode("Button", "switch_documents").assertDoesNotExist()
        onPantrixNode("IconButton", "back").performClick()
        awaitText("Paste anything")
    }

    @Test
    fun aTextOfNoFormatToldIsAskedAboutAndOpenedAsTheReaderSays() = runComposeUiTest {
        val clipboard = TestClipboard().apply { putText("Things to check.\n") }
        app(clipboard = clipboard)
        awaitText("Paste anything")

        onRoot().performShortcut(PantrixShortcut(Key.V, command = true, shift = true), mac)

        awaitText("What format is this?")
        onPantrixNode("Button", "leave_as_text").performClick()
        // The toolbar's format and the status both say it.
        awaitNode(hasPantrixTag("Tag", "status") and hasText("Plain text"), "Plain text")
        onPantrixNode("Tab", "document_1", scope = "Tabs_documents").assertExists()
    }
}
