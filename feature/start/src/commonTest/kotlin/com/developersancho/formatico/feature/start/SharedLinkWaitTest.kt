package com.developersancho.formatico.feature.start

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.network.AddressReading
import com.developersancho.formatico.core.network.Online
import com.developersancho.formatico.core.network.SharedOpening
import com.developersancho.formatico.core.storage.MemorySmallStore
import com.developersancho.formatico.core.storage.MemoryTextStore
import com.developersancho.formatico.core.testing.awaitText
import com.developersancho.formatico.core.testing.awaitUntil
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.onPantrixNode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.time.Clock

@OptIn(ExperimentalTestApi::class)
class SharedLinkWaitTest {
    @Test
    fun theAppWaitsForAShareAndCancellingItOpensNothing() = runComposeUiTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val shares = OpenedShares(SharedOpening.Gone).apply { given = CompletableDeferred() }
        val history = StoredDocumentHistory(small = MemorySmallStore(), texts = MemoryTextStore(), scope = scope)
        val opener =
            DocumentOpener(
                history,
                Clock.System,
                maxFileSize = 10_000_000,
                online = Online(shares, FakeAddresses(AddressReading.Unreachable))
            )
        setContent {
            PantrixTestTheme(window = PantrixWindow(DpSize(1000.dp, 700.dp))) { Box(Modifier.fillMaxSize()) { SharedLinkWait(opener) } }
        }

        var opening: Opening? = null
        val fetching = scope.launch { opening = opener.link(TestSharedLink) }
        awaitText("Opening a shared document")
        awaitText("It is fetched from Formatico’s server and decrypted on this device.")
        onPantrixNode("Button", "shared_cancel").performClick()

        awaitUntil("The fetch is stopped") { fetching.isCancelled }
        assertNull(opening)
    }
}
