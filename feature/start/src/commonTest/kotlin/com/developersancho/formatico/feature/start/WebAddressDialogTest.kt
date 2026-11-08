package com.developersancho.formatico.feature.start

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.network.AddressReading
import com.developersancho.formatico.core.network.Online
import com.developersancho.formatico.core.network.SharedOpening
import com.developersancho.formatico.core.storage.MemorySmallStore
import com.developersancho.formatico.core.storage.MemoryTextStore
import com.developersancho.formatico.core.testing.awaitText
import com.developersancho.formatico.core.testing.awaitUntil
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.files.rememberFilePicker
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.foundation.platform.rememberPantrixClipboard
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.onPantrixNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

@OptIn(ExperimentalTestApi::class)
class WebAddressDialogTest {
    private val desktop = PantrixWindow(DpSize(1000.dp, 700.dp))

    /** Shows the dialog of a [DocumentOpening] whose web is [addresses], handing what it opens to [opened]. */
    private fun ComposeUiTest.opening(addresses: FakeAddresses, opened: MutableList<OpenedDocument>): DocumentOpening {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val history = StoredDocumentHistory(small = MemorySmallStore(), texts = MemoryTextStore(), scope = scope)
        val opener =
            DocumentOpener(history, Clock.System, maxFileSize = 10_000_000, online = Online(OpenedShares(SharedOpening.Gone), addresses))
        var opening: DocumentOpening? = null
        setContent {
            PantrixTestTheme(window = desktop) {
                val clipboard = rememberPantrixClipboard()
                val picker = rememberFilePicker()
                val shown = remember {
                    DocumentOpening(
                        scope,
                        opener,
                        clipboard,
                        picker,
                        mutableStateOf(null),
                        mutableStateOf({ it: OpenedDocument ->
                            opened +=
                                it
                        })
                    )
                }
                opening = shown
                Box(Modifier.fillMaxSize()) { WebAddressDialog(shown) }
            }
        }
        waitForIdle()
        return requireNotNull(opening)
    }

    @Test
    fun anAddressTypedIsOpenedAndTheDialogLeaves() = runComposeUiTest {
        val opened = mutableListOf<OpenedDocument>()
        val opening = opening(FakeAddresses(AddressReading.Read("a: 1\n", "deployment.yaml")), opened)

        opening.askAddress()
        awaitText("Formatico downloads it itself. Up to 10 MB.")
        onPantrixNode("Button", "address_open").assertIsNotEnabled()
        onPantrixNode("TextField", "address").performTextInput("https://example.com/deployment.yaml")
        onPantrixNode("Button", "address_open").performClick()

        awaitUntil("The document is opened") { opened.isNotEmpty() }
        assertEquals(OpenedDocument(name = "deployment.yaml", text = "a: 1\n", format = Format.Yaml, historyId = 1), opened.single())
        awaitUntil("The dialog leaves") { opening.address == null }
    }

    @Test
    fun whyADocumentWasNotOpenedIsSaidUnderTheAddress() = runComposeUiTest {
        val addresses = FakeAddresses(AddressReading.Answered(404))
        val opening = opening(addresses, mutableListOf())

        opening.askAddress()
        opening.changeAddress("https://example.com/none.json")
        opening.openAddress()
        awaitText("The address answered 404: there is no document there.")

        addresses.answer = AddressReading.TooLarge(24_000_000)
        opening.openAddress()
        awaitText("The document is 24 MB, and Formatico opens up to 10 MB here.")

        onPantrixNode("Button", "address_cancel").performClick()
        awaitUntil("The dialog leaves") { opening.address == null }
    }

    @Test
    fun onTheWebTheAddressComesThroughFormaticosServer() = runComposeUiTest {
        setContent {
            PantrixTestTheme(platform = PlatformType.Web, window = desktop) {
                Box(Modifier.fillMaxSize()) {
                    WebAddressDialog(ask = AddressAsk(), maxFileSize = 10_000_000, onChange = {}, onOpen = {}, onDismiss = {})
                }
            }
        }

        awaitText("On the web it comes through Formatico’s server, which keeps nothing. Up to 10 MB.")
    }
}
