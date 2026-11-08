package com.developersancho.formatico.core.network

import com.developersancho.formatico.core.share.LinkReading
import com.developersancho.formatico.core.share.ShareKeep
import com.developersancho.formatico.core.share.ShareLinks
import com.developersancho.formatico.core.share.SharedDocument
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EncryptedSharesTest {
    private val order = SharedDocument("""{"id": "ord_8842", "customer": {"name": "Elif Yılmaz"}}""", "order.json")

    @Test
    fun aDocumentSharedIsOpenedFromItsLinkAsItWas() = runTest {
        val fake = FakeServer()
        val shares = ServerShares(fake.server)

        val shared = assertIs<Sharing.Shared>(shares.share(order, ShareKeep.Week)).link

        assertTrue(shared.link.startsWith("https://formatico.pantrixui.com/#s=7Qm2xLp9fK&k="), shared.link)
        assertEquals(91, shared.link.length)
        val link = assertIs<LinkReading.Encrypted>(ShareLinks.read(shared.link))
        assertEquals(SharedOpening.Opened(order), shares.open(link))
    }

    @Test
    fun theServerSeesNeitherTheTextNorTheName() = runTest {
        val fake = FakeServer()
        ServerShares(fake.server).share(order.copy(text = "secret ".repeat(200)), ShareKeep.Day)

        val kept = assertNotNull(fake.kept).decodeToString()
        assertFalse("secret" in kept)
        assertFalse("order" in kept)
    }

    @Test
    fun eachShareHasAKeyOfItsOwn() = runTest {
        val shares = ServerShares(FakeServer().server)
        val first = assertIs<Sharing.Shared>(shares.share(order, ShareKeep.Day)).link.link
        val second = assertIs<Sharing.Shared>(shares.share(order, ShareKeep.Day)).link.link

        assertTrue(first != second)
    }

    @Test
    fun aLinkWithAnotherKeyAShareGoneAndAServerDownAreSaidSo() = runTest {
        val fake = FakeServer()
        val shares = ServerShares(fake.server)
        val shared = assertIs<Sharing.Shared>(shares.share(order, ShareKeep.Day)).link
        val other = ShareLinks.encrypted("7Qm2xLp9fK", ByteArray(ShareLinks.KEY_BYTES) { 7 })

        assertEquals(SharedOpening.Broken, shares.open(assertIs<LinkReading.Encrypted>(ShareLinks.read(other))))
        assertEquals(Forgetting.Forgotten, shares.delete(shared))
        assertEquals(SharedOpening.Gone, shares.open(assertIs<LinkReading.Encrypted>(ShareLinks.read(shared.link))))

        fake.reachable = false
        assertEquals(Sharing.Unreachable, shares.share(order, ShareKeep.Day))
        assertEquals(SharedOpening.Unreachable, shares.open(assertIs<LinkReading.Encrypted>(ShareLinks.read(shared.link))))
    }

    @Test
    fun aServerThatTakesNoShareNowOrNoMoreTodayIsSaidSo() = runTest {
        val full = ServerShares(FormaticoServer(clientOf { respond("", HttpStatusCode.InsufficientStorage) }, SERVER))
        assertEquals(Sharing.Busy, full.share(order, ShareKeep.Day))
        val limited = ServerShares(
            FormaticoServer(clientOf { json("""{"error": "daily-limit", "limit": 50}""", HttpStatusCode.TooManyRequests) }, SERVER)
        )
        assertEquals(Sharing.DailyLimit(50), limited.share(order, ShareKeep.Day))
    }

    @Test
    fun aTextsFingerprintIsItsSha256() = runTest {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", fingerprintOf(""))
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", fingerprintOf("hello"))
    }

    @Test
    fun aDocumentLargerThanTheServerKeepsIsNotSent() = runTest {
        val fake = FakeServer()
        val document = SharedDocument("a".repeat(ShareLinks.MAX_TEXT + 1))

        assertEquals(Sharing.TooLarge, ServerShares(fake.server).share(document, ShareKeep.Day))
        assertEquals(null, fake.kept)
    }
}
