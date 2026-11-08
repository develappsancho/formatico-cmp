package com.developersancho.formatico.feature.start

import com.developersancho.formatico.core.network.AddressReading
import com.developersancho.formatico.core.network.EncryptedLink
import com.developersancho.formatico.core.network.EncryptedShares
import com.developersancho.formatico.core.network.Forgetting
import com.developersancho.formatico.core.network.SharedOpening
import com.developersancho.formatico.core.network.Sharing
import com.developersancho.formatico.core.network.WebAddresses
import com.developersancho.formatico.core.share.LinkReading
import com.developersancho.formatico.core.share.ShareKeep
import com.developersancho.formatico.core.share.ShareLinks
import com.developersancho.formatico.core.share.SharedDocument
import kotlinx.coroutines.CompletableDeferred

/** An encrypted link of the tests, whose share [OpenedShares] gives. */
internal val TestSharedLink = ShareLinks.encrypted("7Qm2xLp9fK", ByteArray(ShareLinks.KEY_BYTES))

/** Formatico's server as Start's tests open shares from it: [answer] is what it gives, once [given] completes. */
internal class OpenedShares(var answer: SharedOpening) : EncryptedShares {
    var given = CompletableDeferred(Unit)
    val asked = mutableListOf<String>()

    override suspend fun share(document: SharedDocument, keep: ShareKeep): Sharing = Sharing.Unreachable

    override suspend fun open(link: LinkReading.Encrypted): SharedOpening {
        asked += link.id
        given.await()
        return answer
    }

    override suspend fun delete(link: EncryptedLink): Forgetting = Forgetting.Unreachable
}

/** The web as Start's tests open addresses on it: [answer] is what every address gives, with the most it was asked for. */
internal class FakeAddresses(var answer: AddressReading) : WebAddresses {
    val asked = mutableListOf<Pair<String, Long>>()

    override suspend fun open(address: String, maxBytes: Long): AddressReading {
        asked += address to maxBytes
        return answer
    }
}
