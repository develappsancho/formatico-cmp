package com.developersancho.formatico

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.storage.memoryStorageModule
import com.developersancho.formatico.core.testing.SendsSystemBack
import com.developersancho.formatico.core.testing.TestSystemBack
import com.developersancho.formatico.core.testing.awaitText
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.onPantrixNode
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppBackTest {
    private val desktop = PantrixWindow(DpSize(1440.dp, 900.dp))
    private val back = TestSystemBack()

    @AfterTest
    fun closeTheApp() {
        stopKoin()
    }

    private fun ComposeUiTest.app(platform: PlatformType) = setContent {
        PantrixTestTheme(platform = platform, window = desktop) {
            SendsSystemBack(back)
            App(systemDark = false, storage = memoryStorageModule)
        }
    }

    private suspend fun ComposeUiTest.openTheOrder() {
        awaitText("Paste anything")
        onPantrixNode("ListItem", "sample_order").performClick()
        awaitText("Valid JSON")
    }

    @Test
    fun escapeOnTheDesktopLeavesTheDocumentShown() = runComposeUiTest {
        app(PlatformType.Desktop)
        openTheOrder()

        runOnIdle { back.send() }

        awaitIdle()
        onNodeWithText("Valid JSON").assertExists()
        onNodeWithText("Paste anything").assertDoesNotExist()
    }

    @Test
    fun escapeOnTheWebLeavesSettingsOpen() = runComposeUiTest {
        app(PlatformType.Web)
        openTheOrder()
        onPantrixNode("IconButton", "settings").performClick()
        awaitText("Keep a history of documents")

        runOnIdle { back.send() }

        awaitIdle()
        onNodeWithText("Keep a history of documents").assertExists()
    }

    @Test
    fun androidsBackGoesFromADocumentToANewTab() = runComposeUiTest {
        app(PlatformType.Android)
        openTheOrder()

        runOnIdle { back.send() }

        awaitText("Paste anything")
    }

    @Test
    fun iosBackClosesSettings() = runComposeUiTest {
        app(PlatformType.Ios)
        openTheOrder()
        onPantrixNode("IconButton", "settings").performClick()
        awaitText("Keep a history of documents")

        runOnIdle { back.send() }

        awaitText("Valid JSON")
        onNodeWithText("Keep a history of documents").assertDoesNotExist()
    }
}
