package com.developersancho.formatico.core.share

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ShareLinksTest {
    private val order = SharedDocument(
        text = """{"id": "ord_8842", "status": "shipped", "total": 129.90, "customer": {"name": "Elif Yılmaz"}}""",
        name = "order.json"
    )

    private fun linkOf(document: SharedDocument) = assertIs<ShareOutcome.Link>(ShareLinks.share(document))

    @Test
    fun aLinkCarriesTheTextCompressedAndTheName() {
        val shared = linkOf(order)

        assertTrue(shared.link.startsWith("https://formatico.pantrixui.com/#d=eJ"), shared.link)
        assertTrue(shared.link.endsWith("&n=order.json"), shared.link)
        assertEquals(order.text.encodeToByteArray().size, shared.bytes)
        assertTrue(shared.short)
        assertEquals(LinkReading.Read(order), ShareLinks.read(shared.link))
    }

    @Test
    fun aNameIsWrittenForAnAddressAndReadBack() {
        val named = order.copy(name = "Sipariş listesi & notlar #2.json")
        val shared = linkOf(named)

        assertTrue(shared.link.endsWith("&n=Sipari%C5%9F%20listesi%20%26%20notlar%20%232.json"), shared.link)
        assertEquals(LinkReading.Read(named), ShareLinks.read(shared.link))
        assertEquals(LinkReading.Read(order.copy(name = null)), ShareLinks.read(linkOf(order.copy(name = null)).link))
    }

    @Test
    fun theBrowsersFragmentAndAPastedLinkAreRead() {
        val link = linkOf(order).link
        assertEquals(LinkReading.Read(order), ShareLinks.read(link.substringAfter(ShareLinks.SITE)))
        assertEquals(LinkReading.Read(order), ShareLinks.read("  $link\n"))
        assertEquals(
            LinkReading.Read(order),
            ShareLinks.read(link.replace("https://formatico.pantrixui.com/", "http://Formatico.PantrixUI.com/index.html"))
        )
    }

    @Test
    fun aTextThatIsNotFormaticosLinkIsNone() {
        val fragment = linkOf(order).link.substringAfter(ShareLinks.SITE)
        for (text in listOf(
            "{\"a\": 1}",
            "https://example.com/$fragment",
            "#document?id=3",
            "#start",
            "https://formatico.pantrixui.com/#settings",
            ""
        )) {
            assertEquals(LinkReading.NotALink, ShareLinks.read(text), text)
        }
    }

    @Test
    fun aLinkCutShortOrChangedIsBroken() {
        val link = linkOf(order.copy(name = null)).link
        val broken = listOf(
            // The Share board's link: its checksum is not its text's.
            "https://formatico.pantrixui.com/#d=eJyrVspMUbJSUEpKzSmyUiopTUoqUqoFAGG1CBs",
            link.dropLast(6),
            link.replaceRange(link.length - 8, link.length - 7, if (link[link.length - 8] == 'A') "B" else "A"),
            "#d=not*base64",
            "#d="
        )
        for (text in broken) assertEquals(LinkReading.Broken, ShareLinks.read(text), text)
    }

    @Test
    fun anEncryptedLinkNamesTheShareAndCarriesItsKey() {
        val key = ByteArray(ShareLinks.KEY_BYTES) { it.toByte() }
        val link = ShareLinks.encrypted("7Qm2xLp9fK", key)

        assertEquals("https://formatico.pantrixui.com/#s=7Qm2xLp9fK&k=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8", link)
        val read = assertIs<LinkReading.Encrypted>(ShareLinks.read(link))
        assertEquals("7Qm2xLp9fK", read.id)
        assertTrue(key.contentEquals(read.keyBytes()))
        assertEquals(read, ShareLinks.read(link.substringAfter(ShareLinks.SITE)))
        assertEquals(read, ShareLinks.read("  $link\n"))
    }

    @Test
    fun anEncryptedLinkWithoutItsWholeKeyOrWithAnotherIdIsBroken() {
        val key = ShareLinks.encrypted("7Qm2xLp9fK", ByteArray(ShareLinks.KEY_BYTES)).substringAfter("&k=")
        for (text in listOf(
            "#s=7Qm2xLp9fK",
            "#s=7Qm2xLp9fK&k=${key.dropLast(2)}",
            "#s=7Qm2xLp9fK&k=not*base64",
            "#s=7Qm2xLp&k=$key",
            "#s=7Qm2xLp9f!&k=$key",
            "https://formatico.pantrixui.com/#s=&k=$key"
        )) {
            assertEquals(LinkReading.Broken, ShareLinks.read(text), text)
        }
    }

    @Test
    fun aDocumentTooLargeForALinkHasNone() {
        // Bytes that hardly compress: 40 KB of them make a link longer than 32 KB.
        val noise = SharedDocument(pseudoRandom(40_000).joinToString("") { (it.toInt() and 0x3F).plus(48).toChar().toString() })
        assertEquals(ShareOutcome.TooLarge(40_000), ShareLinks.share(noise))
        assertEquals(ShareOutcome.TooLarge(3_000_000), ShareLinks.share(SharedDocument("a".repeat(3_000_000))))

        val long = linkOf(SharedDocument(noise.text.take(10_000)))
        assertFalse(long.short)
        assertTrue(long.characters in ShareLinks.SHORT_LENGTH..ShareLinks.MAX_LENGTH, "${long.characters}")
    }
}
