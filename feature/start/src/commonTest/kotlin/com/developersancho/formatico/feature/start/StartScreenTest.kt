package com.developersancho.formatico.feature.start

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.testing.awaitText
import com.developersancho.formatico.core.testing.awaitUntil
import com.pantrixui.components.overlay.snackbar.LocalPantrixSnackbars
import com.pantrixui.components.overlay.snackbar.PantrixSnackbarState
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.diagnostics.PantrixFindings
import com.pantrixui.foundation.platform.ClipboardTraits
import com.pantrixui.foundation.platform.OperatingSystem
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.TestClipboard
import com.pantrixui.testing.TestClock
import com.pantrixui.testing.assertNoFindings
import com.pantrixui.testing.onPantrixNode
import com.pantrixui.testing.performPaste
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class StartScreenTest {
    private val desktop = PantrixWindow(DpSize(1440.dp, 900.dp))
    private val phone = PantrixWindow(DpSize(390.dp, 844.dp))
    private val now = Instant.parse("2026-11-03T09:41:00Z")

    @Test
    fun aFirstStartSaysNothingWasOpenedAndOffersTheSamples() = runComposeUiTest {
        val findings = PantrixFindings()
        val intents = mutableListOf<StartIntent>()
        setContent {
            PantrixTestTheme(window = desktop, findings = findings) {
                StartScreen(state = StartState(), onIntent = { intents += it })
            }
        }

        awaitText("Paste anything")
        awaitText("Nothing opened yet")
        onNodeWithText("Clear history").assertDoesNotExist()
        onPantrixNode("ListItem", "sample_countries").performClick()

        assertEquals(listOf<StartIntent>(StartIntent.OpenSample(Sample.Countries)), intents)
        assertNoFindings(findings)
    }

    @Test
    fun theAppIsOfferedOnTheDesktopAndAPhonesBrowserNotInTheAndroidApp() = runComposeUiTest {
        var platform by mutableStateOf(PlatformType.Desktop)
        var window by mutableStateOf(desktop)
        setContent {
            PantrixTestTheme(platform = platform, window = window) { StartScreen(state = StartState(), onIntent = {}) }
        }

        awaitText("Get the app")
        onPantrixNode("Button", "get_the_app").assertExists()
        platform = PlatformType.Android
        awaitUntil("No download in the Android app") { onAllNodesWithText("Get the app").fetchSemanticsNodes().isEmpty() }
        platform = PlatformType.Web
        window = phone
        awaitText("Get the app")
    }

    @Test
    fun recentSaysTheFormatTheSizeAndWhenEachWasEditedOrOpened() = runComposeUiTest {
        val clock = TestClock(now)
        val edited = RecentDocument(
            id = 7,
            name = "order.json",
            format = Format.Json,
            size = 665,
            at = now - 2.minutes,
            edited = true
        )
        val opened = RecentDocument(
            id = 8,
            name = "deployment.yaml",
            format = Format.Yaml,
            size = 1946,
            at = now - 5.minutes
        )
        val intents = mutableListOf<StartIntent>()
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                StartScreen(
                    state = StartState(recent = listOf(edited, opened)),
                    onIntent = { intents += it }
                )
            }
        }

        awaitText("JSON · 665 B · edited 2 minutes ago")
        awaitText("YAML · 1.9 KB · opened 5 minutes ago")
        onPantrixNode("ListItem", "recent_7").performClick()
        onPantrixNode("Link", "clear_history").performClick()

        assertEquals(listOf(StartIntent.OpenRecent(7), StartIntent.ClearHistory), intents)
    }

    @Test
    fun aWebAddressIsAskedForFromEveryWidth() = runComposeUiTest {
        val intents = mutableListOf<StartIntent>()
        var window by mutableStateOf(desktop)
        setContent { PantrixTestTheme(window = window) { StartScreen(state = StartState(), onIntent = { intents += it }) } }

        onPantrixNode("Button", "open_address").performClick()
        window = PantrixWindow(DpSize(390.dp, 844.dp))
        waitForIdle()
        onPantrixNode("Button", "open_address").performClick()

        assertEquals(listOf<StartIntent>(StartIntent.AskAddress, StartIntent.AskAddress), intents)
    }

    @Test
    fun thePasteButtonOpensTheClipboardsTextAndANewDocumentStartsEmpty() = runComposeUiTest {
        val clipboard = TestClipboard().apply { putText("{\"a\": 1}") }
        val intents = mutableListOf<StartIntent>()
        setContent {
            PantrixTestTheme(window = desktop, clipboard = clipboard) {
                StartScreen(state = StartState(), onIntent = { intents += it })
            }
        }

        awaitText("Paste from clipboard")
        onPantrixNode("Button", "paste").performClick()
        awaitUntil("The paste") { intents.isNotEmpty() }
        onPantrixNode("Button", "new_document").performClick()

        assertEquals(listOf(StartIntent.Paste("{\"a\": 1}"), StartIntent.NewDocument), intents)
    }

    @Test
    fun anEmptyClipboardPastesNothing() = runComposeUiTest {
        val intents = mutableListOf<StartIntent>()
        setContent {
            PantrixTestTheme(window = phone, clipboard = TestClipboard()) {
                StartScreen(state = StartState(), onIntent = { intents += it })
            }
        }

        awaitText("Paste from clipboard")
        onPantrixNode("Button", "paste").performClick()

        awaitUntil("The paste") { intents.isNotEmpty() }
        assertEquals(listOf<StartIntent>(StartIntent.Paste(null)), intents)
    }

    @Test
    fun aClipboardThatCannotBeReadIsSaidSoWhenPasteIsPressed() = runComposeUiTest {
        val snackbars = PantrixSnackbarState()
        val intents = mutableListOf<StartIntent>()
        setContent {
            // A browser that gives the page no clipboard to read, as one over plain http does.
            PantrixTestTheme(window = desktop, clipboard = TestClipboard(ClipboardTraits(readable = false))) {
                CompositionLocalProvider(LocalPantrixSnackbars provides snackbars) {
                    StartScreen(state = StartState(), onIntent = { intents += it })
                }
            }
        }

        awaitText("Paste from clipboard")
        onPantrixNode("Button", "paste").performClick()

        awaitUntil("The snackbar") { snackbars.current?.tagKey == "clipboard_unreadable" }
        assertEquals(
            "This browser does not let Formatico read the clipboard. Paste with the keyboard, or open a file.",
            snackbars.current?.message
        )
        assertEquals(emptyList(), intents)
    }

    @Test
    fun theKeyboardsPasteIsHeardAnywhereOnTheScreen() = runComposeUiTest {
        val clipboard = TestClipboard()
        val intents = mutableListOf<StartIntent>()
        setContent {
            PantrixTestTheme(window = desktop, clipboard = clipboard) {
                StartScreen(state = StartState(), onIntent = { intents += it })
            }
        }

        awaitText("Paste from clipboard")
        onAllNodes(hasText("anywhere on this screen", substring = true)).assertCountEquals(1)
        // The keys stand on keycaps; a screen reader hears the hint as one sentence, as the system writes the keys.
        onAllNodes(hasText("or press Ctrl+V anywhere on this screen")).assertCountEquals(1)
        // On the web the browser's own paste follows the key, as a reader's does.
        onRoot().performPaste("a: 1\n", clipboard, OperatingSystem.Android)

        awaitUntil("The paste") { intents.isNotEmpty() }
        assertEquals(listOf<StartIntent>(StartIntent.Paste("a: 1\n")), intents)
    }

    @Test
    fun aPhoneListsTwoSamplesUntilItIsAskedForAll() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = phone) {
                StartScreen(state = StartState(), onIntent = {})
            }
        }

        awaitText("All samples")
        onPantrixNode("ListItem", "sample_manifest").assertDoesNotExist()
        onPantrixNode("Button", "open_file").assertExists()
        onPantrixNode("FileDrop", "file").assertDoesNotExist()

        onPantrixNode("Link", "all_samples").performClick()

        onPantrixNode("ListItem", "sample_manifest").assertExists()
        onPantrixNode("Link", "all_samples").assertDoesNotExist()
    }

    @Test
    fun aRefusedFileIsSaidUnderTheDropZone() = runComposeUiTest {
        var state by mutableStateOf(StartState())
        setContent {
            PantrixTestTheme(window = desktop) {
                StartScreen(state = state, onIntent = {})
            }
        }
        awaitText("Paste anything")
        onAllNodesWithText("big.json", substring = true).assertCountEquals(0)

        state = StartState(refusal = FileRefusal.TooLarge("big.json", 10_000_000))

        awaitText("big.json is larger than 10 MB")

        state = StartState(refusal = FileRefusal.WrongType("photo.png"))
        awaitText("photo.png is not a kind of text Formatico reads")

        state = StartState(refusal = FileRefusal.Unreadable("locked.json"))
        awaitText("locked.json could not be read")
    }

    @Test
    fun aPhoneListsThreeOfRecentAndSaysOnlyWhenOfEach() = runComposeUiTest {
        val clock = TestClock(now)
        val recent = listOf(
            RecentDocument(id = 1, name = "order.json", format = Format.Json, size = 665, at = now - 2.minutes, edited = true),
            RecentDocument(id = 2, name = null, format = Format.Json, size = 118, at = now - 3.minutes),
            RecentDocument(id = 3, name = "notes.txt", format = null, size = 2048, at = now - 4.minutes),
            RecentDocument(id = 4, name = "countries.csv", format = Format.Csv, size = 12_311, at = now - 5.minutes)
        )
        val intents = mutableListOf<StartIntent>()
        setContent {
            PantrixTestTheme(window = phone, clock = clock) {
                StartScreen(state = StartState(recent = recent), onIntent = { intents += it })
            }
        }

        awaitText("JSON · 665 B · 2 minutes ago")
        // A pasted text has no name, and a document in no format is text.
        awaitText("Untitled")
        awaitText("Text · 2 KB · 4 minutes ago")
        onPantrixNode("ListItem", "recent_4").assertDoesNotExist()

        onPantrixNode("ListItem", "recent_2").performClick()
        assertEquals(listOf<StartIntent>(StartIntent.OpenRecent(2)), intents)
    }

    @Test
    fun aWideWindowStartsRecentAndSamplesOnOneLine() = runComposeUiTest {
        val recent = listOf(RecentDocument(id = 1, name = "order.json", format = Format.Json, size = 665, at = now))
        setContent {
            PantrixTestTheme(platform = PlatformType.Desktop, window = desktop, clock = TestClock(now)) {
                StartScreen(state = StartState(recent = recent), onIntent = {})
            }
        }
        awaitText("Clear history")

        assertEquals(onNodeWithText("Recent").getBoundsInRoot().top, onNodeWithText("Samples").getBoundsInRoot().top)
        assertEquals(
            onPantrixNode("ListItem", "recent_1").getBoundsInRoot().top,
            onPantrixNode("ListItem", "sample_order").getBoundsInRoot().top
        )
    }
}
