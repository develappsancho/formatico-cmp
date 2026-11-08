package com.developersancho.formatico

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.share.ShareLinks
import com.developersancho.formatico.core.share.ShareOutcome
import com.developersancho.formatico.core.share.SharedDocument
import com.developersancho.formatico.core.storage.memoryStorageModule
import com.developersancho.formatico.core.testing.awaitNode
import com.developersancho.formatico.core.testing.awaitText
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.platform.OperatingSystem
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.foundation.platform.rememberPantrixClipboard
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.TestClipboard
import com.pantrixui.testing.hasPantrixTag
import com.pantrixui.testing.onPantrixNode
import com.pantrixui.testing.performPaste
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppLinkTest {
    private val desktop = PantrixWindow(DpSize(1440.dp, 900.dp))
    private val mac = OperatingSystem.MacOs
    private val order = SharedDocument(text = """{"id": "ord_8842", "status": "shipped"}""", name = "shared order.json")
    private val link = (ShareLinks.share(order) as ShareOutcome.Link).link
    private val broken = "https://formatico.pantrixui.com/#d=eJyrVspMUbJSUEpKzSmyUiopTUoqUqoFAGG1CBs"

    @AfterTest
    fun closeTheApp() {
        stopKoin()
    }

    /** The app on a Mac, its clipboard holding [copied] where something is. */
    private fun ComposeUiTest.app(clipboard: TestClipboard, copied: String? = null) = setContent {
        PantrixTestTheme(platform = PlatformType.Desktop, system = mac, window = desktop, clipboard = clipboard) {
            val pantrixClipboard = rememberPantrixClipboard()
            if (copied != null) LaunchedEffect(Unit) { pantrixClipboard.copyText(copied) }
            App(systemDark = false, storage = memoryStorageModule)
        }
    }

    private fun ComposeUiTest.pasteFromThePalette() {
        onPantrixNode("Button", "launcher").performClick()
        onPantrixNode("TextField", "query", scope = "CommandPalette_commands").performTextInput("paste")
        onPantrixNode("Command", "paste", scope = "CommandPalette_commands").performClick()
    }

    @Test
    fun aLinkPastedOnStartOpensTheDocumentItCarriesUnderItsName() = runComposeUiTest {
        val clipboard = TestClipboard()
        app(clipboard)
        awaitText("Paste anything")

        onRoot().performPaste(link, clipboard, mac)

        awaitText("Valid JSON")
        awaitNode(hasPantrixTag("Tab", scope = "Tabs_documents") and hasText("shared order.json"), "The document's tab")
    }

    @Test
    fun aBrokenLinkPastedOnStartIsSaidAndOpensNothing() = runComposeUiTest {
        val clipboard = TestClipboard()
        app(clipboard)
        awaitText("Paste anything")

        onRoot().performPaste(broken, clipboard, mac)

        awaitNode(hasPantrixTag("Snackbar", "link_broken"), "The broken link's snackbar")
        awaitText("This link holds no document Formatico can read. It may have been cut short.")
        awaitText("Paste anything")
    }

    @Test
    fun aLinkPastedFromThePaletteOpensItsDocumentToo() = runComposeUiTest {
        app(TestClipboard(), copied = link)
        awaitText("Paste anything")

        pasteFromThePalette()

        awaitText("Valid JSON")
    }

    @Test
    fun whatThePalettesPasteCannotOpenIsSaidInASnackbar() = runComposeUiTest {
        app(TestClipboard(), copied = broken)
        awaitText("Paste anything")

        pasteFromThePalette()

        awaitNode(hasPantrixTag("Snackbar", "link_broken"), "The broken link's snackbar")
    }
}
