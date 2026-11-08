package com.developersancho.formatico.feature.settings

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.settings.AppLanguage
import com.developersancho.formatico.core.settings.ThemeChoice
import com.developersancho.formatico.core.testing.awaitText
import com.pantrixui.components.overlay.commandpalette.PantrixCommand
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.diagnostics.PantrixFindings
import com.pantrixui.foundation.platform.OperatingSystem
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.assertNoFindings
import com.pantrixui.testing.onPantrixNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest {
    private val desktop = PantrixWindow(DpSize(1440.dp, 900.dp))
    private val phone = PantrixWindow(DpSize(390.dp, 844.dp))
    private val state = SettingsState(history = PreviewHistory)

    @Test
    fun aWideWindowSetsOutTheSectionsBesideTheirSidebar() = runComposeUiTest {
        val findings = PantrixFindings()
        val intents = mutableListOf<SettingsIntent>()
        setContent {
            PantrixTestTheme(window = desktop, findings = findings) { SettingsScreen(state = state, onIntent = { intents += it }) }
        }

        awaitText("Settings")
        awaitText("On this device only: 23 documents, 6.1 MB.")
        onPantrixNode("Item", "general", scope = "Sidebar_settings").assertExists()
        onPantrixNode("Item", "about", scope = "Sidebar_settings").assertExists()
        awaitText("When the format cannot be told")
        onPantrixNode("SwitchButton", "keep_history").assertIsOn()
        onPantrixNode("SwitchButton", "wrap_lines").assertIsOn()

        onPantrixNode("Segment", "dark", scope = "SegmentButton_theme").performClick()
        onPantrixNode("SwitchButton", "sort_keys").performClick()
        // The test draws in less than the board, and the sections below scroll.
        onPantrixNode("Button", "clear_history").performScrollTo().performClick()
        assertEquals(
            listOf(SettingsIntent.SetTheme(ThemeChoice.Dark), SettingsIntent.SetSortKeys(true), SettingsIntent.ClearHistory),
            intents
        )
        assertNoFindings(findings)
    }

    @Test
    fun aWideWindowGoesBackOverTheTitleToTheTabItWasOpenedOver() = runComposeUiTest {
        val intents = mutableListOf<SettingsIntent>()
        setContent {
            PantrixTestTheme(window = desktop) { SettingsScreen(state = state, onIntent = { intents += it }, backTo = "order.json") }
        }

        onPantrixNode("Button", "back").assert(hasContentDescription("Back to order.json")).performClick()
        assertEquals(listOf<SettingsIntent>(SettingsIntent.Back), intents)
    }

    @Test
    fun aWindowJustWiderThanATabletLeavesRoomBesideTheColumn() = runComposeUiTest {
        val size = DpSize(860.dp, 700.dp)
        setContent {
            PantrixTestTheme(window = PantrixWindow(size)) {
                SettingsScreen(state = state, onIntent = {}, modifier = Modifier.size(size))
            }
        }

        awaitText("Settings")
        val theme = onPantrixNode("SegmentButton", "theme").getBoundsInRoot()
        assertTrue(size.width - theme.right >= 24.dp, "The column ends ${size.width - theme.right} from the window's edge")
    }

    @Test
    fun aWideWindowListsTheShortcutsUnderTheirGroupsOnTheirKeycaps() = runComposeUiTest {
        setContent {
            PantrixTestTheme(platform = PlatformType.Desktop, system = OperatingSystem.MacOs, window = desktop) {
                SettingsScreen(state = state, onIntent = {}, shortcuts = PreviewShortcuts + Unbound)
            }
        }

        // "Settings" is the page's title and a command's name too.
        awaitText("Keep a history of documents")
        onPantrixNode("Item", "shortcuts", scope = "Sidebar_settings").performClick()
        awaitText("Each runs the command of the same name in the command palette", substring = true)
        awaitText("View")
        onPantrixNode("Keycaps", "palette").performScrollTo().assert(hasContentDescription("⌘K"))
        onPantrixNode("Keycaps", "format").assert(hasContentDescription("⌥⇧F"))
        onPantrixNode("Keycaps", "query").assertExists()
        onPantrixNode("Item", "shortcuts", scope = "Sidebar_settings").assertIsSelected()
        // A command with no shortcut is the palette's alone.
        onNodeWithText(Unbound.label).assertDoesNotExist()
    }

    @Test
    fun settingsOpensAtTheSectionItIsAskedFor() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = desktop) {
                SettingsScreen(state = state, onIntent = {}, shortcuts = PreviewShortcuts, at = SettingsSection.Shortcuts)
            }
        }

        awaitText("Each runs the command of the same name in the command palette", substring = true)
        onPantrixNode("Item", "shortcuts", scope = "Sidebar_settings").assertIsSelected()
    }

    @Test
    fun aPhoneListsNoShortcuts() = runComposeUiTest {
        setContent { PantrixTestTheme(window = phone) { SettingsScreen(state = state, onIntent = {}, shortcuts = PreviewShortcuts) } }

        awaitText("Keep a history")
        onNodeWithText("Keyboard shortcuts").assertDoesNotExist()
        onPantrixNode("Keycaps", "palette").assertDoesNotExist()
    }

    @Test
    fun theSectionsAWindowShows() {
        val all = SettingsSection.entries
        assertEquals(all, shownSections(SettingsLayout.Wide, PreviewShortcuts))
        assertEquals(all, shownSections(SettingsLayout.Tablet, PreviewShortcuts))
        assertEquals(all - SettingsSection.Shortcuts, shownSections(SettingsLayout.Wide, listOf(Unbound)))
        assertEquals(
            all - SettingsSection.Formats - SettingsSection.Shortcuts,
            shownSections(SettingsLayout.Phone, PreviewShortcuts)
        )
    }

    @Test
    fun theLanguageIsTheSystemsUntilOneIsChosenAndCanBeTheSystemsAgain() = runComposeUiTest {
        val intents = mutableListOf<SettingsIntent>()
        var shown by mutableStateOf(state)
        setContent { PantrixTestTheme(window = desktop) { SettingsScreen(state = shown, onIntent = { intents += it }) } }

        awaitText("Language")
        onPantrixNode("Select", "language").assert(hasText("As the system"))
        onPantrixNode("Select", "language").performClick()
        onPantrixNode("SelectOption", "tr", scope = "Select_language").performClick()
        assertEquals(listOf<SettingsIntent>(SettingsIntent.SetLanguage(AppLanguage.Turkish)), intents)

        shown = state.copy(preferences = state.preferences.copy(language = AppLanguage.Turkish))
        waitForIdle()
        onPantrixNode("Select", "language").assert(hasText("Türkçe"))
        onPantrixNode("Select", "language").performClick()
        onPantrixNode("SelectOption", "system", scope = "Select_language").performClick()
        assertEquals(SettingsIntent.SetLanguage(null), intents.last())
    }

    @Test
    fun theLicencesAreListedAndOneIsWrittenOut() = runComposeUiTest {
        val intents = mutableListOf<SettingsIntent>()
        var shown by mutableStateOf(state.copy(page = SettingsPage.Licences))
        setContent { PantrixTestTheme(window = desktop) { SettingsScreen(state = shown, onIntent = { intents += it }) } }

        awaitText("Open-source licences")
        awaitText("navigation3-browser")
        onPantrixNode("ListItem", "library_geist").performScrollTo().performClick()
        assertEquals(listOf<SettingsIntent>(SettingsIntent.ShowLicence(OpenSourceLibrary.Geist)), intents)

        shown = state.copy(page = SettingsPage.Licence(OpenSourceLibrary.Geist))
        awaitText("SIL OPEN FONT LICENSE", substring = true)
        onPantrixNode("Button", "back").performClick()
        assertEquals(SettingsIntent.Back, intents.last())
    }

    @Test
    fun aPhoneHasABarOfItsOwnAndFewerSettings() = runComposeUiTest {
        val intents = mutableListOf<SettingsIntent>()
        setContent { PantrixTestTheme(window = phone) { SettingsScreen(state = state, onIntent = { intents += it }) } }

        awaitText("On this phone only: 23 documents, 6.1 MB.")
        awaitText("Keep a history")
        onPantrixNode("Select", "unknown_format").assertDoesNotExist()
        onPantrixNode("Select", "keep_for").assertDoesNotExist()
        onPantrixNode("Sidebar", "settings").assertDoesNotExist()

        onPantrixNode("IconButton", "back").performClick()
        assertEquals(listOf<SettingsIntent>(SettingsIntent.Back), intents)
    }
}

/** A command of the palette with no shortcut. */
private val Unbound = PantrixCommand("compare", "Compare with another document", "Document")
