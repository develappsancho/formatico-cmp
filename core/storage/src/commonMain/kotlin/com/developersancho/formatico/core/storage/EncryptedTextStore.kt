package com.developersancho.formatico.core.storage

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.operations.AuthenticatedCipher
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.getOrCreateSecret
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.io.encoding.Base64

/** What an encrypted text starts with, before its nonce, its ciphertext and its tag, in Base64. */
internal const val ENCRYPTED_TEXT = "formatico:aes-gcm:1\n"

/** The name of the key the texts are encrypted with, among what KSafe keeps. */
private const val TEXT_KEY = "texts-key"

/** The key's length: 256 bits. */
private const val TEXT_KEY_BYTES = 32

/** The platform's own cryptography, which encrypts the texts: the JDK's, CryptoKit, or the browser's WebCrypto. */
internal expect val TextCipherProvider: CryptographyProvider

/**
 * A [TextStore] that keeps each text in [stored] encrypted (the owner's decision, 2026-11-04): AES-GCM with a 256-bit
 * [key], a nonce of its own each time it is written, and a tag that tells a text changed, or read with another key.
 *
 * A text kept before the texts were encrypted is read as it is, and encrypted when it is written again. One that
 * cannot be decrypted, as one written with a key since lost, is read as none: the document is gone, not the app.
 */
internal class EncryptedTextStore(private val stored: TextStore, private val key: suspend () -> ByteArray) : TextStore {
    private val made = Mutex()
    private var cipher: AuthenticatedCipher? = null

    // A text that is not what was written fails on each platform its own way: the JDK's AEADBadTagException,
    // CryptoKit's error, a browser's rejected promise (a JsException, which is no Exception in Wasm), bad Base64.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun read(name: String): String? {
        val text = stored.read(name) ?: return null
        if (!text.startsWith(ENCRYPTED_TEXT)) return text
        return try {
            cipher().decrypt(Base64.decode(text, startIndex = ENCRYPTED_TEXT.length)).decodeToString()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (notDecrypted: Throwable) {
            null
        }
    }

    override suspend fun write(name: String, text: String) =
        stored.write(name, ENCRYPTED_TEXT + Base64.encode(cipher().encrypt(text.encodeToByteArray())))

    override suspend fun delete(name: String) = stored.delete(name)

    private suspend fun cipher(): AuthenticatedCipher = made.withLock {
        cipher ?: TextCipherProvider.get(AES.GCM)
            .keyDecoder()
            .decodeFromByteArray(AES.Key.Format.RAW, key())
            .cipher()
            .also { cipher = it }
    }
}

/** [texts], encrypted with a key [ksafe] makes the first time and keeps in the device's vault. */
internal fun encryptedTexts(ksafe: KSafe, texts: TextStore): TextStore = EncryptedTextStore(texts) {
    ksafe.awaitReady()
    ksafe.getOrCreateSecret(TEXT_KEY, size = TEXT_KEY_BYTES)
}
