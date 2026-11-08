package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.share.ShareKeep
import com.developersancho.formatico.core.share.SharedDocument
import com.developersancho.formatico.core.storage.MemorySmallStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class EncryptedShareViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val workbench = Workbench(worker = dispatcher)
    private val shares = FakeShares()

    // The text is its own fingerprint here: a browser's SHA-256 is a promise, which a test's dispatcher does not wait for.
    private val links = SharedLinks(MemorySmallStore(), fingerprint = { it })

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sharing(text: String = """{"a":1}""") = WorkspaceViewModel(
        documentId = workbench.open("order.json", text, Format.Json),
        workbench = workbench,
        reader = dispatcher,
        sharing = EncryptedSharing(shares, links)
    ).also { it.dispatch(WorkspaceIntent.Share(open = true)) }

    private val WorkspaceViewModel.share get() = state.value.share

    @Test
    fun theDocumentIsSharedAsAnEncryptedLinkForTheDaysChosenAndKeptHere() = runTest(dispatcher) {
        val viewModel = sharing()
        assertEquals(ShareWay.Link, viewModel.share?.way)
        assertEquals(ShareKeep.Week, viewModel.share?.keep)

        viewModel.dispatch(WorkspaceIntent.ShareBy(ShareWay.Encrypted))
        viewModel.dispatch(WorkspaceIntent.ShareKept(ShareKeep.Month))
        viewModel.dispatch(WorkspaceIntent.CreateEncryptedLink)

        assertEquals(EncryptedShare.Created(TestEncryptedLink), viewModel.share?.encrypted)
        assertEquals(listOf(SharedDocument("""{"a":1}""", "order.json") to ShareKeep.Month), shares.shared)
        assertEquals(TestEncryptedLink, links.find("""{"a":1}"""))
    }

    @Test
    fun theFormattedTextIsWhatIsEncrypted() = runTest(dispatcher) {
        val viewModel = sharing()
        viewModel.dispatch(WorkspaceIntent.ShareBy(ShareWay.Encrypted))
        viewModel.dispatch(WorkspaceIntent.ShareFormatted(formatted = true))
        viewModel.dispatch(WorkspaceIntent.CreateEncryptedLink)

        assertEquals("{\n  \"a\": 1\n}", shares.shared.single().first.text)
    }

    @Test
    fun aServerNotReachedIsSaidAndTriedAgain() = runTest(dispatcher) {
        shares.reachable = false
        val viewModel = sharing()
        viewModel.dispatch(WorkspaceIntent.ShareBy(ShareWay.Encrypted))
        viewModel.dispatch(WorkspaceIntent.CreateEncryptedLink)
        assertEquals(EncryptedShare.Failed, viewModel.share?.encrypted)
        assertNull(links.find("""{"a":1}"""))

        shares.reachable = true
        viewModel.dispatch(WorkspaceIntent.CreateEncryptedLink)
        assertEquals(EncryptedShare.Created(TestEncryptedLink), viewModel.share?.encrypted)
    }

    @Test
    fun aLinkDeletedIsForgottenAndOneTheServerDidNotDeleteStays() = runTest(dispatcher) {
        val viewModel = sharing()
        viewModel.dispatch(WorkspaceIntent.ShareBy(ShareWay.Encrypted))
        viewModel.dispatch(WorkspaceIntent.CreateEncryptedLink)

        shares.reachable = false
        viewModel.dispatch(WorkspaceIntent.DeleteEncryptedLink)
        assertEquals(EncryptedShare.Created(TestEncryptedLink, deleteFailed = true), viewModel.share?.encrypted)

        shares.reachable = true
        viewModel.dispatch(WorkspaceIntent.DeleteEncryptedLink)
        assertEquals(EncryptedShare.Choosing, viewModel.share?.encrypted)
        assertEquals(listOf(TestEncryptedLink), shares.deleted)
        assertNull(links.find("""{"a":1}"""))
    }

    @Test
    fun theLinkMadeOfTheDocumentAsItIsShowsAgainWhenTheShareOpens() = runTest(dispatcher) {
        val viewModel = sharing()
        viewModel.dispatch(WorkspaceIntent.ShareBy(ShareWay.Encrypted))
        viewModel.dispatch(WorkspaceIntent.CreateEncryptedLink)
        viewModel.dispatch(WorkspaceIntent.Share(open = false))

        viewModel.dispatch(WorkspaceIntent.Share(open = true))
        assertEquals(ShareWay.Encrypted, viewModel.share?.way)
        assertEquals(EncryptedShare.Created(TestEncryptedLink), viewModel.share?.encrypted)

        // Formatted, the document is another text, of which no link was made.
        viewModel.dispatch(WorkspaceIntent.ShareFormatted(formatted = true))
        assertEquals(EncryptedShare.Choosing, viewModel.share?.encrypted)
        assertEquals(1, shares.shared.size)
    }
}
