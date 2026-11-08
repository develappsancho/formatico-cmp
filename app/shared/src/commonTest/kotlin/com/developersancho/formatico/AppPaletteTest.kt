package com.developersancho.formatico

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.storage.memoryStorageModule
import com.developersancho.formatico.core.testing.awaitNode
import com.developersancho.formatico.core.testing.awaitStatus
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
class AppPaletteTest {
    private val desktop = PantrixWindow(DpSize(1440.dp, 900.dp))
    private val phone = PantrixWindow(DpSize(390.dp, 844.dp))
    private val mac = OperatingSystem.MacOs

    @AfterTest
    fun closeTheApp() {
        stopKoin()
    }

    private fun ComposeUiTest.command(key: String) = onPantrixNode("Command", key, scope = "CommandPalette_commands")

    /**
     * Types [words] in the palette's field, and waits for [key]'s command where one is named: the list is drawn as far as
     * it is seen, the rest only once found, and in a browser a frame after the words.
     */
    private suspend fun ComposeUiTest.find(words: String, key: String? = null) {
        onPantrixNode("TextField", "query", scope = "CommandPalette_commands").performTextInput(words)
        if (key != null) awaitNode(hasPantrixTag("Command", key, scope = "CommandPalette_commands"), "The command $key")
    }

    /** Waits for the status bar's size to say [lines], as a screen reader hears it. */
    private suspend fun ComposeUiTest.awaitLines(lines: String) =
        awaitNode(
            hasPantrixTag("Item", "size", scope = "StatusBar_document") and hasContentDescription("$lines · ", substring = true),
            lines
        )

    @Test
    fun theLauncherOpensThePaletteAndACommandRunsFromIt() = runComposeUiTest {
        setContent {
            PantrixTestTheme(platform = PlatformType.Desktop, system = mac, window = desktop) {
                App(systemDark = false, storage = memoryStorageModule)
            }
        }
        awaitText("Paste anything")
        onPantrixNode("ListItem", "sample_order").performClick()
        awaitText("Valid JSON")

        onPantrixNode("Button", "launcher").performClick()

        awaitText("Format document")
        command("format").assertIsEnabled()
        find("yaml", key = "convert_yaml")
        command("convert_yaml").performClick()
        awaitText("Nothing is lost")
        command("convert_yaml").assertDoesNotExist()
    }

    @Test
    fun commandAndKOpensThePaletteAnywhereWithTheAppsOwnCommands() = runComposeUiTest {
        setContent {
            PantrixTestTheme(platform = PlatformType.Desktop, system = mac, window = desktop) {
                App(systemDark = false, storage = memoryStorageModule)
            }
        }
        awaitText("Paste anything")

        onRoot().performShortcut(PantrixShortcut(Key.K, command = true), mac)

        awaitText("Open a file")
        // Over a new tab there is no document to run a command on, and no new tab to open.
        command("format").assertDoesNotExist()
        command("new").assertIsNotEnabled()
        command("settings").assertIsEnabled()
        command("theme").performClick()
        val light = "Use the light theme"
        awaitNode(hasPantrixTag("IconButton", "theme") and hasContentDescription(light), light)
    }

    @Test
    fun theSettingsCommandOpensSettingsAndListsNoDocumentsCommandsOverIt() = runComposeUiTest {
        setContent {
            PantrixTestTheme(platform = PlatformType.Desktop, system = mac, window = desktop) {
                App(systemDark = false, storage = memoryStorageModule)
            }
        }
        awaitText("Paste anything")
        onPantrixNode("ListItem", "sample_order").performClick()
        awaitText("Valid JSON")

        onPantrixNode("Button", "launcher").performClick()
        find("settings", key = "settings")
        command("settings").performClick()

        awaitText("Keep a history of documents")
        onPantrixNode("Button", "launcher").performClick()
        // Settings' Keyboard shortcuts names it too: the palette's own row is waited for.
        awaitNode(hasPantrixTag("Command", "open", scope = "CommandPalette_commands"), "The palette's Open a file")
        command("format").assertDoesNotExist()
        command("settings").assertIsNotEnabled()
        command("new").assertIsEnabled()
    }

    @Test
    fun aCommandsOwnKeysRunItWithoutThePalette() = runComposeUiTest {
        setContent {
            PantrixTestTheme(platform = PlatformType.Desktop, system = mac, window = desktop) {
                App(systemDark = false, storage = memoryStorageModule)
            }
        }
        awaitText("Paste anything")
        onPantrixNode("ListItem", "sample_order").performClick()
        awaitLines("30 lines")

        onRoot().performShortcut(PantrixShortcut(Key.M, shift = true, alt = true), mac)

        awaitLines("1 line")
        command("minify").assertDoesNotExist()
    }

    @Test
    fun pastingAsANewDocumentOpensTheClipboardInATabOfItsOwn() = runComposeUiTest {
        val clipboard = TestClipboard().apply { putText("name: Formatico\n") }
        setContent {
            PantrixTestTheme(platform = PlatformType.Desktop, system = mac, window = desktop, clipboard = clipboard) {
                App(systemDark = false, storage = memoryStorageModule)
            }
        }
        awaitText("Paste anything")

        onRoot().performShortcut(PantrixShortcut(Key.V, command = true, shift = true), mac)

        awaitText("Valid YAML")
        onPantrixNode("Tab", "document_1", scope = "Tabs_documents").assertExists()
    }

    @Test
    fun aPhonesPaletteListsFewerCommands() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = phone) {
                App(systemDark = false, storage = memoryStorageModule)
            }
        }
        awaitText("Paste anything")
        onPantrixNode("ListItem", "sample_order").performClick()
        awaitStatus("Valid JSON")

        onPantrixNode("IconButton", "launcher").performClick()

        awaitText("Format document")
        command("sort").assertDoesNotExist()
        command("tree").assertDoesNotExist()
        command("theme").assertDoesNotExist()
        find("new")
        awaitText("New document")
    }
}
