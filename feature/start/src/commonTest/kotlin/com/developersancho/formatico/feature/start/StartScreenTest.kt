package com.developersancho.formatico.feature.start

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.diagnostics.PantrixFindings
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.foundation.platform.currentPlatformType
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.TestClock
import com.pantrixui.testing.assertNoFindings
import com.pantrixui.testing.onPantrixNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.time.TimeSource

@OptIn(ExperimentalTestApi::class)
class StartScreenTest {
    private val desktop = PantrixWindow(DpSize(1440.dp, 900.dp))
    private val phone = PantrixWindow(DpSize(390.dp, 844.dp))
    private val now = Instant.parse("2026-11-03T09:41:00Z")

    /**
     * Waits for [text] to be shown once, letting the platform run meanwhile: in a browser the screen's words are
     * fetched after its first frame, and the fetch ends only while the page's event loop runs, which `waitUntil` holds.
     *
     * In a browser a row or a button has no name until its words come, and the diagnostics say so a frame later: what
     * they found until then is [forgetting]'s to forget (PANTRIX_FINDINGS: "MissingName, kelimeler yüklenirken").
     * Elsewhere the words are there from the first frame, and every finding is kept.
     */
    private suspend fun ComposeUiTest.awaitText(text: String, forgetting: PantrixFindings? = null) {
        val waited = TimeSource.Monotonic.markNow()
        while (onAllNodes(hasText(text)).fetchSemanticsNodes().size != 1) {
            check(waited.elapsedNow() < 10.seconds) { "\"$text\" was not shown in 10 seconds" }
            withContext(Dispatchers.Default) { delay(20.milliseconds) }
            awaitIdle()
        }
        if (forgetting != null && currentPlatformType() == PlatformType.Web) {
            withContext(Dispatchers.Default) { delay(100.milliseconds) }
            awaitIdle()
            forgetting.clear()
        }
    }

    @Test
    fun aFirstStartSaysNothingWasOpenedAndOffersTheSamples() = runComposeUiTest {
        val findings = PantrixFindings()
        val intents = mutableListOf<StartIntent>()
        setContent {
            PantrixTestTheme(window = desktop, findings = findings) {
                StartScreen(state = StartState(), onIntent = { intents += it }, onPaste = {}, readsClipboard = true)
            }
        }

        awaitText("Paste anything")
        awaitText("Nothing opened yet", forgetting = findings)
        onNodeWithText("Clear history").assertDoesNotExist()
        onPantrixNode("ListItem", "sample_countries").performClick()

        assertEquals(listOf<StartIntent>(StartIntent.OpenSample(Sample.Countries)), intents)
        assertNoFindings(findings)
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
            at =
            now - 5.minutes
        )
        val intents = mutableListOf<StartIntent>()
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                StartScreen(
                    state = StartState(recent = listOf(edited, opened)),
                    onIntent = { intents += it },
                    onPaste = {},
                    readsClipboard = true
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
    fun theButtonsPasteAndStartANewDocument() = runComposeUiTest {
        var pasted = 0
        val intents = mutableListOf<StartIntent>()
        setContent {
            PantrixTestTheme(window = desktop) {
                StartScreen(state = StartState(), onIntent = {
                    intents += it
                }, onPaste = { pasted++ }, readsClipboard = true)
            }
        }

        awaitText("Paste from clipboard")
        onPantrixNode("Button", "paste").performClick()
        onPantrixNode("Button", "new_document").performClick()

        assertEquals(1, pasted)
        assertEquals(listOf<StartIntent>(StartIntent.NewDocument), intents)
    }

    @Test
    fun aPhoneListsTwoSamplesUntilItIsAskedForAll() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = phone) {
                StartScreen(state = StartState(), onIntent = {}, onPaste = {}, readsClipboard = true)
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
                StartScreen(state = state, onIntent = {}, onPaste = {}, readsClipboard = true)
            }
        }
        awaitText("Paste anything")
        onAllNodesWithText("big.json", substring = true).assertCountEquals(0)

        state = StartState(refusal = FileRefusal.TooLarge("big.json", 10_000_000))

        awaitText("big.json is larger than 10 MB")
    }
}
