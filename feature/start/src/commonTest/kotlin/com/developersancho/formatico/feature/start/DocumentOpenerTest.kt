package com.developersancho.formatico.feature.start

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.network.AddressReading
import com.developersancho.formatico.core.network.EncryptedShares
import com.developersancho.formatico.core.network.Online
import com.developersancho.formatico.core.network.SharedOpening
import com.developersancho.formatico.core.network.WebAddresses
import com.developersancho.formatico.core.share.ShareLinks
import com.developersancho.formatico.core.share.ShareOutcome
import com.developersancho.formatico.core.share.SharedDocument
import com.developersancho.formatico.core.storage.MemorySmallStore
import com.developersancho.formatico.core.storage.MemoryTextStore
import com.pantrixui.foundation.files.PantrixFile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentOpenerTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val moment = Instant.parse("2026-11-05T21:00:00Z")
    private val clock = object : Clock {
        override fun now(): Instant = moment
    }

    private fun TestScope.opener(
        shares: EncryptedShares? = null,
        addresses: WebAddresses? = null
    ): Pair<DocumentOpener, DocumentHistory> {
        val history = StoredDocumentHistory(small = MemorySmallStore(), texts = MemoryTextStore(), scope = backgroundScope)
        val online = if (shares == null &&
            addresses == null
        ) {
            null
        } else {
            Online(
                shares ?: OpenedShares(SharedOpening.Gone),
                addresses ?: FakeAddresses(AddressReading.Unreachable)
            )
        }
        val opener = DocumentOpener(history, clock, maxFileSize = MAX_SIZE, reader = dispatcher, online = online)
        return opener to history
    }

    @Test
    fun anEncryptedLinkOpensTheDocumentItsShareHolds() = runTest(dispatcher) {
        val shares = OpenedShares(SharedOpening.Opened(SharedDocument("a: 1\n", "deployment.yaml")))
        val (opener, history) = opener(shares = shares)

        val opened = opener.paste(TestSharedLink)

        assertEquals(
            Opening.Opened(OpenedDocument(name = "deployment.yaml", text = "a: 1\n", format = Format.Yaml, historyId = 1)),
            opened
        )
        assertEquals(listOf("7Qm2xLp9fK"), shares.asked)
        assertEquals("deployment.yaml", history.documents.value.single().name)
    }

    @Test
    fun anEncryptedLinkGoneUnreachableOrWithAnotherKeyIsSaidSo() = runTest(dispatcher) {
        val shares = OpenedShares(SharedOpening.Gone)
        val (opener, _) = opener(shares = shares)
        assertEquals(Opening.SharedLinkGone, opener.link(TestSharedLink))

        shares.answer = SharedOpening.Unreachable
        assertEquals(Opening.SharedLinkUnreachable(TestSharedLink), opener.link(TestSharedLink))

        shares.answer = SharedOpening.Broken
        assertEquals(Opening.BrokenLink, opener.link(TestSharedLink))
    }

    @Test
    fun theAppWaitsWhileAShareIsFetchedAndMayStopIt() = runTest(dispatcher) {
        val shares = OpenedShares(SharedOpening.Gone).apply { given = CompletableDeferred() }
        val (opener, _) = opener(shares = shares)

        val fetching = launch { opener.link(TestSharedLink) }
        assertTrue(opener.fetching.value)

        opener.cancelFetching()
        assertTrue(fetching.isCancelled)
        assertFalse(opener.fetching.value)
    }

    @Test
    fun aWebAddressOpensTheDocumentThereUnderTheNameItGivesIt() = runTest(dispatcher) {
        val addresses = FakeAddresses(AddressReading.Read("""{"id": "ord_8842"}""", "8842.json"))
        val (opener, _) = opener(addresses = addresses)

        val opened = opener.address("https://example.com/orders/8842.json")

        assertEquals(
            Opening.Opened(OpenedDocument(name = "8842.json", text = """{"id": "ord_8842"}""", format = Format.Json, historyId = 1)),
            opened
        )
        assertEquals(listOf("https://example.com/orders/8842.json" to MAX_SIZE), addresses.asked)

        addresses.answer = AddressReading.Answered(404)
        assertEquals(Opening.AddressRefused(AddressReading.Answered(404)), opener.address("https://example.com/none.json"))
    }

    @Test
    fun pastedTextIsOpenedInItsFormatAndGoesToTheHistory() = runTest(dispatcher) {
        val (opener, history) = opener()

        val opening = opener.paste("a: 1\n")

        assertEquals(Opening.Opened(OpenedDocument(name = null, text = "a: 1\n", format = Format.Yaml, historyId = 1)), opening)
        assertEquals(moment, history.documents.value.single().at)
    }

    @Test
    fun aLinkOfFormaticosOpensTheDocumentItCarriesUnderItsName() = runTest(dispatcher) {
        val (opener, history) = opener()
        val link = (ShareLinks.share(SharedDocument("""{"a": 1}""", "order.json")) as ShareOutcome.Link).link

        val pasted = opener.paste(link)
        val opened = opener.link(link.substringAfter(ShareLinks.SITE))

        assertEquals(
            Opening.Opened(OpenedDocument(name = "order.json", text = """{"a": 1}""", format = Format.Json, historyId = 1)),
            pasted
        )
        // The same document again is the one the history holds.
        assertEquals(pasted, opened)
        assertEquals(1, history.documents.value.size)
    }

    @Test
    fun aBrokenLinkOpensNothingAndAnAddressOfNoneIsNoLink() = runTest(dispatcher) {
        val (opener, history) = opener()

        assertEquals(Opening.BrokenLink, opener.paste("https://formatico.pantrixui.com/#d=eJyrVspMUbJSUEpKzSmyUiopTUoqUqoFAGG1CBs"))
        assertEquals(Opening.BrokenLink, opener.link("#d=eJ"))
        assertNull(opener.link("#start"))
        assertNull(opener.link("#document?id=3"))
        assertEquals(emptyList(), history.documents.value)
    }

    @Test
    fun nothingOrBlanksAreNothingToPaste() = runTest(dispatcher) {
        val (opener, history) = opener()

        assertEquals(Opening.NothingToPaste, opener.paste(null))
        assertEquals(Opening.NothingToPaste, opener.paste("  \n"))
        assertEquals(emptyList(), history.documents.value)
    }

    @Test
    fun aFileIsToldByItsNameAndOneTooLargeOrUnreadableIsRefused() = runTest(dispatcher) {
        val (opener, _) = opener()

        val opened = opener.open(PantrixFile("settings.json", """{"a": 1}""".encodeToByteArray()))
        assertEquals(Format.Json, (opened as Opening.Opened).document.format)
        assertEquals("settings.json", opened.document.name)
        assertEquals(
            Opening.Refused(FileRefusal.TooLarge("big.json", MAX_SIZE)),
            opener.open(PantrixFile("big.json", ByteArray(MAX_SIZE.toInt() + 1)))
        )
    }

    @Test
    fun anEmptyDocumentIsLeftOutOfTheHistory() = runTest(dispatcher) {
        val (opener, history) = opener()

        assertNull(opener.record(OpenedDocument(name = null, text = "", format = null)).historyId)
        assertEquals(emptyList(), history.documents.value)
    }

    @Test
    fun whereNoHistoryIsKeptNothingGoesToIt() = runTest(dispatcher) {
        val history = StoredDocumentHistory(small = MemorySmallStore(), texts = MemoryTextStore(), scope = backgroundScope)
        val opener = DocumentOpener(history, clock, maxFileSize = MAX_SIZE, reader = dispatcher, keeps = { false })

        val opening = opener.paste("a: 1\n")

        assertNull((opening as Opening.Opened).document.historyId)
        assertEquals(emptyList(), history.documents.value)
    }

    private companion object {
        const val MAX_SIZE = 100L
    }
}
