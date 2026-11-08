package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.network.EncryptedLink
import com.developersancho.formatico.core.network.fingerprintOf
import com.developersancho.formatico.core.storage.SmallStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The encrypted links made on this device, kept in [small] until they expire: what deleting one takes is here alone,
 * and the share of a document unchanged since shows its link again rather than make another.
 *
 * A link is kept by its text's fingerprint ([fingerprint], SHA-256 unless a test gives another), not by the text.
 */
internal class SharedLinks(
    private val small: SmallStore,
    private val clock: Clock = Clock.System,
    private val fingerprint: suspend (String) -> String = ::fingerprintOf
) {
    private val changing = Mutex()

    /** The link made of [text] that has not expired, if one was. */
    suspend fun find(text: String): EncryptedLink? {
        val print = fingerprint(text)
        return changing.withLock { live().firstOrNull { it.fingerprint == print }?.link() }
    }

    /** Keeps [link], made of [text]. */
    suspend fun keep(text: String, link: EncryptedLink) {
        val print = fingerprint(text)
        changing.withLock {
            val kept = live().filterNot { it.fingerprint == print } + Kept(print, link)
            small.put(KEY, kept, ListSerializer(Kept.serializer()))
        }
    }

    /** Forgets [link]: it was deleted, or is gone. */
    suspend fun forget(link: EncryptedLink) = changing.withLock {
        small.put(KEY, live().filterNot { it.id == link.id }, ListSerializer(Kept.serializer()))
    }

    /** The links kept that have not expired. */
    private suspend fun live(): List<Kept> {
        val now = clock.now().toEpochMilliseconds()
        return small.get(KEY, ListSerializer(Kept.serializer())).orEmpty().filter { it.expiresAt > now }
    }

    @Serializable
    private data class Kept(val fingerprint: String, val link: String, val id: String, val deleteToken: String, val expiresAt: Long) {
        constructor(fingerprint: String, link: EncryptedLink) :
            this(fingerprint, link.link, link.id, link.deleteToken, link.expiresAt.toEpochMilliseconds())

        fun link() = EncryptedLink(link, id, deleteToken, Instant.fromEpochMilliseconds(expiresAt))
    }

    private companion object {
        const val KEY = "shared-links"
    }
}
