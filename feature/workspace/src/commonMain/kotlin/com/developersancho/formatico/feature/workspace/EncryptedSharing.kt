package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.network.EncryptedLink
import com.developersancho.formatico.core.network.EncryptedShares
import com.developersancho.formatico.core.network.Forgetting
import com.developersancho.formatico.core.network.SharedOpening
import com.developersancho.formatico.core.network.Sharing
import com.developersancho.formatico.core.share.LinkReading
import com.developersancho.formatico.core.share.ShareKeep
import com.developersancho.formatico.core.share.SharedDocument
import com.developersancho.formatico.core.storage.MemorySmallStore

/**
 * A workspace's encrypted links: made through [shares], and kept on this device in [links] with what deleting them
 * takes, to be found again for the text they were made of.
 */
internal class EncryptedSharing(
    private val shares: EncryptedShares = NoEncryptedShares,
    private val links: SharedLinks = SharedLinks(MemorySmallStore())
) {
    /** The link made of [text] that has not expired, if one was. */
    suspend fun find(text: String): EncryptedLink? = links.find(text)

    /** Makes the link of [document], kept for [keep]: made and kept here, or failed where the server was not reached. */
    suspend fun create(document: SharedDocument, keep: ShareKeep): EncryptedShare = when (val sharing = shares.share(document, keep)) {
        is Sharing.Shared -> EncryptedShare.Created(sharing.link).also { links.keep(document.text, sharing.link) }
        Sharing.TooLarge, Sharing.Unreachable -> EncryptedShare.Failed
    }

    /** Deletes [created]'s link from the server, and forgets it here; it stays where the server is not reached. */
    suspend fun delete(created: EncryptedShare.Created): EncryptedShare = when (shares.delete(created.link)) {
        // A token the server refuses deletes nothing there: the share expires by itself.
        Forgetting.Forgotten, Forgetting.Gone, Forgetting.Refused -> {
            links.forget(created.link)
            EncryptedShare.Choosing
        }

        Forgetting.Unreachable -> created.copy(deleteFailed = true)
    }
}

/** No server: what a workspace shares through where it is given none, in a preview or a test. */
internal object NoEncryptedShares : EncryptedShares {
    override suspend fun share(document: SharedDocument, keep: ShareKeep): Sharing = Sharing.Unreachable

    override suspend fun open(link: LinkReading.Encrypted): SharedOpening = SharedOpening.Unreachable

    override suspend fun delete(link: EncryptedLink): Forgetting = Forgetting.Unreachable
}
