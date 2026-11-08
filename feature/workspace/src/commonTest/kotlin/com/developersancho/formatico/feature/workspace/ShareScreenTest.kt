package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.share.ShareKeep
import com.developersancho.formatico.core.share.ShareLinks
import com.developersancho.formatico.core.share.ShareOutcome
import com.developersancho.formatico.core.share.SharedDocument
import com.developersancho.formatico.core.testing.awaitNode
import com.developersancho.formatico.core.testing.awaitText
import com.developersancho.formatico.core.testing.awaitUntil
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.files.LocalFileSaver
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.TestClipboard
import com.pantrixui.testing.TestFileSaver
import com.pantrixui.testing.hasPantrixTag
import com.pantrixui.testing.onPantrixNode
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ShareScreenTest {
    // An overlay is laid out in the window it is told of: these fit in the test's own, which is smaller than a board.
    private val desktop = PantrixWindow(DpSize(1000.dp, 700.dp))
    private val phone = PantrixWindow(DpSize(390.dp, 700.dp))
    private val order = """{"id": "ord_8842", "status": "shipped"}"""
    private val link = (ShareLinks.share(SharedDocument(order, "order.json")) as ShareOutcome.Link).link

    private fun sharing(
        text: String = order,
        name: String = "order.json",
        format: Format? = Format.Json,
        canFormat: Boolean = true,
        outcome: ShareOutcome? = ShareLinks.share(SharedDocument(text, name)),
        way: ShareWay = ShareWay.Link,
        encrypted: EncryptedShare = EncryptedShare.Choosing
    ): WorkspaceState {
        val state = previewState(name, text, format)
        return state.copy(share = DocumentShare(name, format, canFormat = canFormat, outcome = outcome, way = way, encrypted = encrypted))
    }

    /** The share of [state] in [window], what it asks for going to [intents]; a phone sends with [sending]. */
    private fun ComposeUiTest.share(
        state: WorkspaceState,
        intents: MutableList<WorkspaceIntent>,
        window: PantrixWindow = desktop,
        clipboard: TestClipboard? = null,
        saver: TestFileSaver = TestFileSaver(),
        sending: ((String) -> Unit)? = null
    ) = setContent {
        PantrixTestTheme(window = window, clipboard = clipboard) {
            CompositionLocalProvider(LocalFileSaver provides saver) {
                val commands = rememberDocumentCommands(remember { Workbench() })
                // Over a screen, as the workspace has it: a dialog over nothing is laid out at nought in a window told of
                // (PANTRIX_FINDINGS, "Pencere verilen test temasında boş bir içeriğin üstündeki dialog").
                Box(Modifier.fillMaxSize()) {
                    ShareDocument(state, workspaceLayout(), onIntent = { intents += it }, commands = commands, sending = sending)
                }
            }
        }
    }

    @Test
    fun aWideWindowShowsTheLinkHowLongItIsAndCopiesIt() = runComposeUiTest {
        val clipboard = TestClipboard()
        val intents = mutableListOf<WorkspaceIntent>()
        share(sharing(), intents, clipboard = clipboard)

        awaitText("Share order.json")
        awaitText("short enough for every browser and app.", substring = true)
        awaitText(link)
        onPantrixNode("SwitchButton", "formatted").assertIsOff().performClick()
        onPantrixNode("Button", "copy_link").performClick()

        awaitUntil("The link is copied") { clipboard.entries.isNotEmpty() }
        assertEquals(link, clipboard.text())
        assertEquals(listOf(WorkspaceIntent.ShareFormatted(true), WorkspaceIntent.Share(open = false)), intents)
    }

    @Test
    fun plainTextIsSharedAsItIsAndALinkBeingMadeCannotBeCopied() = runComposeUiTest {
        val intents = mutableListOf<WorkspaceIntent>()
        share(sharing(text = "a note\n", name = "notes.txt", format = null, canFormat = false, outcome = null), intents)

        awaitText("Share notes.txt")
        onPantrixNode("SwitchButton", "formatted").assertIsNotEnabled()
        onPantrixNode("Button", "copy_link").assertIsNotEnabled()
    }

    @Test
    fun theEncryptedLinkIsChosenWithHowLongItIsKeptAndMade() = runComposeUiTest {
        val intents = mutableListOf<WorkspaceIntent>()
        var state by mutableStateOf(sharing())
        setContent {
            PantrixTestTheme(window = desktop) {
                val commands = rememberDocumentCommands(remember { Workbench() })
                Box(Modifier.fillMaxSize()) { ShareDocument(state, workspaceLayout(), onIntent = { intents += it }, commands = commands) }
            }
        }

        awaitText("How to share it")
        onPantrixNode("RadioButton", "encrypted", scope = "RadioGroup_way").performClick()
        assertEquals(WorkspaceIntent.ShareBy(ShareWay.Encrypted), intents.last())

        state = sharing(way = ShareWay.Encrypted)
        awaitText("cannot read it: the key is in the link alone", substring = true)
        onPantrixNode("TextField", "link").assertDoesNotExist()
        onPantrixNode("Select", "keep").performClick()
        onPantrixNode("SelectOption", "30", scope = "Select_keep").performClick()
        onPantrixNode("Button", "create_link").performClick()

        assertEquals(listOf(WorkspaceIntent.ShareKept(ShareKeep.Month), WorkspaceIntent.CreateEncryptedLink), intents.takeLast(2))
    }

    @Test
    fun aLinkWhoseServerWasNotReachedIsTriedAgain() = runComposeUiTest {
        val intents = mutableListOf<WorkspaceIntent>()
        share(sharing(way = ShareWay.Encrypted, encrypted = EncryptedShare.Failed()), intents)

        awaitText("Formatico’s server could not be reached")
        awaitText("The document was not sent. Try again, or share it in the link.")
        onPantrixNode("Button", "try_again").performClick()

        assertEquals(listOf<WorkspaceIntent>(WorkspaceIntent.CreateEncryptedLink), intents)
    }

    @Test
    fun aServerTakingNoShareNowIsTriedAgainLater() = runComposeUiTest {
        val intents = mutableListOf<WorkspaceIntent>()
        share(sharing(way = ShareWay.Encrypted, encrypted = EncryptedShare.Failed(ShareFailure.Busy)), intents)

        awaitText("Formatico’s server is not taking shares right now")
        awaitText("The document was not sent. Try again later, or share it in the link.")
        onPantrixNode("Alert", "busy").assertExists()
        onPantrixNode("Button", "try_again").performClick()

        assertEquals(listOf<WorkspaceIntent>(WorkspaceIntent.CreateEncryptedLink), intents)
    }

    @Test
    fun theDaysLinksOfANetworkMadeAreSaidWithNoTryingAgain() = runComposeUiTest {
        val intents = mutableListOf<WorkspaceIntent>()
        share(sharing(way = ShareWay.Encrypted, encrypted = EncryptedShare.Failed(ShareFailure.DailyLimit(50))), intents)

        awaitText("No more encrypted links from this network today")
        awaitText("Formatico’s server keeps up to 50 a day from one network. Try again tomorrow, or share it in the link.")
        onPantrixNode("Button", "try_again").assertDoesNotExist()
        onPantrixNode("Button", "create_link").assertDoesNotExist()
        onPantrixNode("RadioButton", "link", scope = "RadioGroup_way").performClick()

        assertEquals(listOf<WorkspaceIntent>(WorkspaceIntent.ShareBy(ShareWay.Link)), intents)
    }

    @Test
    fun aDocumentOnlyAnEncryptedLinkTakesSaysTheDaysLinksAreMadeWithoutTheLink() = runComposeUiTest {
        share(
            sharing(
                name = "events.ndjson",
                format = Format.Ndjson,
                outcome = ShareOutcome.TooLarge(4_200_000),
                encrypted = EncryptedShare.Failed(ShareFailure.DailyLimit(50))
            ),
            mutableListOf()
        )

        awaitText("Formatico’s server keeps up to 50 a day from one network. Try again tomorrow.")
        onPantrixNode("RadioGroup", "way").assertDoesNotExist()
    }

    @Test
    fun theEncryptedLinkMadeIsCopiedOrDeleted() = runComposeUiTest {
        val clipboard = TestClipboard()
        val intents = mutableListOf<WorkspaceIntent>()
        share(sharing(way = ShareWay.Encrypted, encrypted = EncryptedShare.Created(TestEncryptedLink)), intents, clipboard = clipboard)

        awaitText(TestEncryptedLink.link)
        awaitText("Kept, encrypted, until", substring = true)
        onPantrixNode("Button", "delete_link").performClick()
        onPantrixNode("Button", "copy_link").performClick()

        awaitUntil("The link is copied") { clipboard.entries.isNotEmpty() }
        assertEquals(TestEncryptedLink.link, clipboard.text())
        assertEquals(listOf(WorkspaceIntent.DeleteEncryptedLink, WorkspaceIntent.Share(open = false)), intents)
    }

    @Test
    fun aDocumentTooLargeForALinkGoesAsAnEncryptedLink() = runComposeUiTest {
        val intents = mutableListOf<WorkspaceIntent>()
        share(sharing(name = "events.ndjson", format = Format.Ndjson, outcome = ShareOutcome.TooLarge(4_200_000)), intents)

        awaitText("Share events.ndjson")
        awaitText("At 4.2 MB, events.ndjson is too large to go in a link", substring = true)
        onPantrixNode("RadioGroup", "way").assertDoesNotExist()
        onPantrixNode("Button", "create_link").performClick()

        assertEquals(listOf<WorkspaceIntent>(WorkspaceIntent.CreateEncryptedLink), intents)
    }

    @Test
    fun aDocumentTooLargeForAnEncryptedLinkIsSavedAsAFileInstead() = runComposeUiTest {
        val saver = TestFileSaver()
        val intents = mutableListOf<WorkspaceIntent>()
        share(sharing(name = "access.ndjson", format = Format.Ndjson, outcome = ShareOutcome.TooLarge(24_000_000)), intents, saver = saver)

        awaitText("access.ndjson is too large to share")
        awaitText("A link holds about 32 KB and an encrypted link up to 10 MB, and this document is 24 MB.", substring = true)
        onPantrixNode("TextField", "link").assertDoesNotExist()
        onPantrixNode("Button", "save").performClick()

        awaitUntil("The file is saved") { saver.files.isNotEmpty() }
        assertEquals(listOf<WorkspaceIntent>(WorkspaceIntent.Share(open = false)), intents)
    }

    @Test
    fun aPhoneSharesInASheetAndSendsTheLinkWithAnotherApp() = runComposeUiTest {
        val sent = mutableListOf<String>()
        val intents = mutableListOf<WorkspaceIntent>()
        share(sharing(), intents, window = phone, sending = { sent += it })

        awaitNode(hasPantrixTag("Sheet", "share"), "The share's sheet")
        awaitText("Share order.json")
        onPantrixNode("Button", "send").performClick()

        assertEquals(listOf(link), sent)
        assertEquals(listOf<WorkspaceIntent>(WorkspaceIntent.Share(open = false)), intents)
    }

    @Test
    fun aPhoneSendsTheEncryptedLinkMade() = runComposeUiTest {
        val sent = mutableListOf<String>()
        share(
            sharing(
                way = ShareWay.Encrypted,
                encrypted = EncryptedShare.Created(TestEncryptedLink)
            ),
            mutableListOf(),
            window = phone,
            sending = {
                sent +=
                    it
            }
        )

        awaitNode(hasPantrixTag("Sheet", "share"), "The share's sheet")
        onPantrixNode("Button", "delete_link").assertExists()
        onPantrixNode("Button", "send").performClick()

        assertEquals(listOf(TestEncryptedLink.link), sent)
    }

    @Test
    fun aPhoneWithNoShareSheetOnlyCopies() = runComposeUiTest {
        share(sharing(), mutableListOf(), window = phone, sending = null)

        awaitNode(hasPantrixTag("Sheet", "share"), "The share's sheet")
        onPantrixNode("Button", "copy_link").assertExists()
        onPantrixNode("Button", "send").assertDoesNotExist()
    }
}
