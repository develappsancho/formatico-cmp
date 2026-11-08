package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.network.EncryptedLink
import com.developersancho.formatico.core.network.EncryptedShares
import com.developersancho.formatico.core.network.Forgetting
import com.developersancho.formatico.core.network.SharedOpening
import com.developersancho.formatico.core.network.Sharing
import com.developersancho.formatico.core.share.LinkReading
import com.developersancho.formatico.core.share.ShareKeep
import com.developersancho.formatico.core.share.SharedDocument
import kotlin.time.Instant

/** The encrypted link the tests' server makes, kept until long after the tests. */
internal val TestEncryptedLink = EncryptedLink(
    link = "https://formatico.pantrixui.com/#s=7Qm2xLp9fK&k=Hq3vN8cTz1Wb5yRk0LmPa2XsD7eFgJ4uV9nQ6tYhC8w",
    id = "7Qm2xLp9fK",
    deleteToken = "token",
    expiresAt = Instant.parse("2099-01-01T00:00:00Z")
)

/**
 * Formatico's server as a workspace's tests need it: what it was asked to share and delete, whether it answers, and
 * what it [answer]s a share with where it does.
 */
internal class FakeShares(var reachable: Boolean = true) : EncryptedShares {
    val shared = mutableListOf<Pair<SharedDocument, ShareKeep>>()
    val deleted = mutableListOf<EncryptedLink>()
    var answer: Sharing = Sharing.Shared(TestEncryptedLink)

    override suspend fun share(document: SharedDocument, keep: ShareKeep): Sharing {
        if (!reachable) return Sharing.Unreachable
        shared += document to keep
        return answer
    }

    override suspend fun open(link: LinkReading.Encrypted): SharedOpening = SharedOpening.Unreachable

    override suspend fun delete(link: EncryptedLink): Forgetting {
        if (!reachable) return Forgetting.Unreachable
        deleted += link
        return Forgetting.Forgotten
    }
}
