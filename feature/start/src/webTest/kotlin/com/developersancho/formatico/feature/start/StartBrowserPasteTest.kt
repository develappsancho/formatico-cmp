package com.developersancho.formatico.feature.start

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.testing.awaitText
import com.developersancho.formatico.core.testing.awaitUntil
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.testing.PantrixTestTheme
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A browser's own paste, which Cmd+V or Ctrl+V makes on the page: a test cannot press the browser's keys, and so it
 * hands the page the paste the browser would.
 */
@OptIn(ExperimentalTestApi::class)
class StartBrowserPasteTest {
    @Test
    fun theBrowsersPasteIsHeardBeforeAnythingHasBeenFocused() = runComposeUiTest {
        val intents = mutableListOf<StartIntent>()
        setContent {
            PantrixTestTheme(window = PantrixWindow(DpSize(1440.dp, 900.dp))) {
                StartScreen(state = StartState(), onIntent = { intents += it })
            }
        }
        awaitText("Paste from clipboard")

        // The screen's root holds the focus: nothing inside it has been focused.
        val taken = pasteInBrowser("a: 1\n")

        awaitUntil("The paste") { intents.isNotEmpty() }
        assertEquals(listOf<StartIntent>(StartIntent.Paste("a: 1\n")), intents)
        // A paste the screen took is kept from the browser's own.
        assertEquals(true, taken)
    }
}

/** Hands the page a paste of [text], as the browser does, and says whether what heard it took it. */
@OptIn(ExperimentalWasmJsInterop::class)
private fun pasteInBrowser(text: String): Boolean = js(
    "(() => { const data = new DataTransfer(); data.setData('text/plain', text); " +
        "const paste = new ClipboardEvent('paste', { clipboardData: data, bubbles: true, cancelable: true }); " +
        "document.body.dispatchEvent(paste); return paste.defaultPrevented; })()"
)
