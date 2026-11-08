package com.developersancho.formatico.core.network

import com.developersancho.formatico.core.share.LinkReading
import com.developersancho.formatico.core.share.ShareEnvelope
import com.developersancho.formatico.core.share.ShareKeep
import com.developersancho.formatico.core.share.ShareLinks
import com.developersancho.formatico.core.share.ShareServer
import com.developersancho.formatico.core.share.SharedDocument
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.SHA256
import kotlinx.coroutines.CancellationException
import kotlin.time.Instant

/**
 * An encrypted link made of a document: the [link], and what deleting the share takes, its [id] and [deleteToken].
 * The server keeps it until [expiresAt].
 */
data class EncryptedLink(val link: String, val id: String, val deleteToken: String, val expiresAt: Instant)

/** What came of sharing a document as an encrypted link. */
sealed interface Sharing {
    /** The document is shared as [link]. */
    data class Shared(val link: EncryptedLink) : Sharing

    /** The document is larger than the server keeps. */
    data object TooLarge : Sharing

    /** Formatico's server could not be reached, or would not keep it. */
    data object Unreachable : Sharing

    /** Formatico's server takes no share now: its room for them is full, or too many came at once. */
    data object Busy : Sharing

    /** The network this comes from has made the [limit] of encrypted links the server keeps from one a day. */
    data class DailyLimit(val limit: Int) : Sharing
}

/** What came of opening an encrypted link. */
sealed interface SharedOpening {
    /** The [document] the link shares. */
    data class Opened(val document: SharedDocument) : SharedOpening

    /** The share is no longer kept: it expired, or whoever shared it deleted it. */
    data object Gone : SharedOpening

    /** Formatico's server could not be reached. */
    data object Unreachable : SharedOpening

    /** The link's key does not open the share: the link was changed, or cut short. */
    data object Broken : SharedOpening
}

/** Shares documents as encrypted links, and opens them. */
interface EncryptedShares {
    /** Shares [document] for [keep]. */
    suspend fun share(document: SharedDocument, keep: ShareKeep): Sharing

    /** The document [link] shares. */
    suspend fun open(link: LinkReading.Encrypted): SharedOpening

    /** Deletes the share of [link] from the server, before it expires. */
    suspend fun delete(link: EncryptedLink): Forgetting
}

/**
 * [EncryptedShares] on Formatico's server: each document is encrypted on this device with a key of its own, AES-GCM
 * of 256 bits, and only that is given to [server]. The key goes in the link, after its `#`, which no browser sends to
 * a server.
 */
internal class ServerShares(
    private val server: FormaticoServer,
    private val provider: CryptographyProvider = ShareCipherProvider
) : EncryptedShares {
    override suspend fun share(document: SharedDocument, keep: ShareKeep): Sharing {
        if (document.text.encodeToByteArray().size > ShareLinks.MAX_TEXT) return Sharing.TooLarge
        val packed = ShareEnvelope.pack(document)
        if (packed.size + GCM_OVERHEAD > ShareServer.MAX_SHARE_BYTES) return Sharing.TooLarge
        val key = provider.get(AES.GCM).keyGenerator(AES.Key.Size.B256).generateKey()
        val encrypted = key.cipher().encrypt(packed)
        return when (val kept = server.keep(encrypted, keep)) {
            is Keeping.Kept -> {
                val (id, token, expiresAt) = kept.share
                val link = ShareLinks.encrypted(id, key.encodeToByteArray(AES.Key.Format.RAW), linkSite())
                Sharing.Shared(EncryptedLink(link, id, token, expiresAt))
            }

            Keeping.Refused -> Sharing.TooLarge

            Keeping.Busy -> Sharing.Busy

            is Keeping.DailyLimit -> Sharing.DailyLimit(kept.limit)

            Keeping.Unreachable -> Sharing.Unreachable
        }
    }

    override suspend fun open(link: LinkReading.Encrypted): SharedOpening = when (val fetched = server.share(link.id)) {
        is Fetching.Found -> decrypted(fetched.bytes, link.keyBytes())?.let(SharedOpening::Opened) ?: SharedOpening.Broken
        Fetching.Gone -> SharedOpening.Gone
        Fetching.Unreachable -> SharedOpening.Unreachable
    }

    override suspend fun delete(link: EncryptedLink): Forgetting = server.forget(link.id, link.deleteToken)

    // A share the key does not open fails on each platform its own way: the JDK's AEADBadTagException, CryptoKit's
    // error, a browser's rejected promise (a JsException, which is no Exception in Wasm).
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun decrypted(bytes: ByteArray, key: ByteArray): SharedDocument? = try {
        val cipher = provider.get(AES.GCM).keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, key).cipher()
        ShareEnvelope.unpack(cipher.decrypt(bytes))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (notDecrypted: Throwable) {
        null
    }

    private companion object {
        /** What AES-GCM adds to what it encrypts: its nonce of 12 bytes before, and its tag of 16 after. */
        const val GCM_OVERHEAD = 12 + 16
    }
}

/**
 * [text]'s fingerprint, SHA-256 in hexadecimal, from the platform's own cryptography: what tells the share a link was
 * made of this very text, and of no other.
 */
suspend fun fingerprintOf(text: String): String =
    ShareCipherProvider.get(SHA256).hasher().hash(text.encodeToByteArray()).toHexString()
