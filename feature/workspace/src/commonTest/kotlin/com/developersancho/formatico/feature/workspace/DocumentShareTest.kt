package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.share.ShareLinks
import com.developersancho.formatico.core.share.ShareOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DocumentShareTest {
    private fun share(outcome: ShareOutcome?, way: ShareWay = ShareWay.Link) = DocumentShare(
        "a.json",
        Format.Json,
        outcome = outcome,
        way = way
    )

    @Test
    fun aDocumentThatFitsInALinkIsSharedTheWayChosen() {
        val fits = share(ShareOutcome.Link("https://formatico.pantrixui.com/#d=x", 10))
        assertEquals(ShareWay.Link, fits.shownWay)
        assertEquals(ShareWay.Encrypted, share(fits.outcome, ShareWay.Encrypted).shownWay)
        assertFalse(fits.encryptedOnly)
        assertFalse(fits.tooLarge)
    }

    @Test
    fun aDocumentALinkCannotHoldGoesAsAnEncryptedLinkUpTo10MB() {
        val large = share(ShareOutcome.TooLarge(4_200_000))
        assertTrue(large.encryptedOnly)
        assertFalse(large.tooLarge)
        assertEquals(ShareWay.Encrypted, large.shownWay)

        val largest = share(ShareOutcome.TooLarge(ShareLinks.MAX_TEXT))
        assertTrue(largest.encryptedOnly)

        val tooLarge = share(ShareOutcome.TooLarge(ShareLinks.MAX_TEXT + 1))
        assertFalse(tooLarge.encryptedOnly)
        assertTrue(tooLarge.tooLarge)
    }
}
