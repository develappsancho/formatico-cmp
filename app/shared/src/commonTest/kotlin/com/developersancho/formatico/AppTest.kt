package com.developersancho.formatico

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.pantrixui.testing.onPantrixNode
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
@IgnoreInBrowser
class AppTest {
    @Test
    fun theAppOpensOnStartUnderItsBar() = runComposeUiTest {
        setContent { App() }

        // In a browser the app's words are fetched after its first frame.
        waitUntilExactlyOneExists(hasText("Paste anything"), timeoutMillis = 10_000)
        onNodeWithText("Formatico").assertExists()
    }

    @Test
    fun theBarSwitchesTheTheme() = runComposeUiTest {
        setContent { App() }
        waitUntilExactlyOneExists(hasText("Paste anything"), timeoutMillis = 10_000)
        val theme = onPantrixNode("IconButton", "theme")
        val before = theme.fetchSemanticsNode().config.toString()

        theme.performClick()

        val after = onPantrixNode("IconButton", "theme").fetchSemanticsNode().config.toString()
        check(before != after) { "The theme's switch did not change: $after" }
    }
}
