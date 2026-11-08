package com.developersancho.formatico

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppTest {
    @Test
    fun theAppDrawsItsNameInItsTheme() = runComposeUiTest {
        setContent { App() }

        onNodeWithText("Formatico").assertExists()
        onNodeWithText("""{"format": "json", "valid": true}""").assertExists()
    }
}
