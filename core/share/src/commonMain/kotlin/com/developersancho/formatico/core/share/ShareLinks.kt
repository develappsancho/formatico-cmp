package com.developersancho.formatico.core.share

import kotlin.io.encoding.Base64

/** A document a link carries: its [text], and its [name] where the link has one. */
data class SharedDocument(val text: String, val name: String? = null)

/** What a text is, read as a link of Formatico's. */
sealed interface LinkReading {
    /** Not a link of Formatico's that carries a document: a text to open as it is. */
    data object NotALink : LinkReading

    /** A link of Formatico's whose document cannot be read back: cut short, or changed on its way. */
    data object Broken : LinkReading

    /** A link of Formatico's, and the [document] it carries. */
    data class Read(val document: SharedDocument) : LinkReading

    /**
     * An encrypted link of Formatico's: the share [id] the server keeps, and the [key] it is decrypted with, in Base64
     * for addresses as the link writes it. The server has never seen the key.
     */
    data class Encrypted(val id: String, val key: String) : LinkReading {
        /** The key's bytes: an AES key of [ShareLinks.KEY_BYTES]. */
        fun keyBytes(): ByteArray = ShareLinks.keyBytes(key)
    }
}

/** A link made of a document, or why none was made. */
sealed interface ShareOutcome {
    /** The [link], [characters] long, carrying [bytes] of text in UTF-8. */
    data class Link(val link: String, val bytes: Int) : ShareOutcome {
        val characters: Int get() = link.length

        /** Whether every browser and app takes the link whole: some chat and mail apps cut a longer one. */
        val short: Boolean get() = characters <= ShareLinks.SHORT_LENGTH
    }

    /** A document of [bytes] in UTF-8, whose link would be longer than a link may be. */
    data class TooLarge(val bytes: Int) : ShareOutcome
}

/**
 * Formatico's links that carry a document, with no server: `https://formatico.pantrixui.com/#d=…&n=…`, the text
 * compressed in zlib's format and written in Base64 for addresses after `d`, and the name after `n`. A browser sends
 * no part after `#` to the site, so the document reaches no one but whoever opens the link.
 *
 * An encrypted link, `#s=…&k=…`, names a share Formatico's server keeps after `s`, and the key that decrypts it after
 * `k`: the key, after the `#` as well, never reaches the server.
 */
object ShareLinks {
    /** Where a link opens: Formatico on the web. */
    const val SITE = "https://formatico.pantrixui.com/"

    /** The longest link Formatico makes: "a link holds about 32 KB". */
    const val MAX_LENGTH = 32_768

    /** The longest link every browser and app takes whole. */
    const val SHORT_LENGTH = 2_000

    /**
     * The most a link is unpacked to: the largest document a phone or a browser opens, 10 MB, a million bytes to the MB as
     * a file's size is counted.
     */
    const val MAX_TEXT = 10_000_000

    /** An encrypted link's key: 256 bits of AES. */
    const val KEY_BYTES = 32

    /**
     * The most text Formatico tries to compress into a link: no text compresses into 32 KB from more. A larger one is
     * too large at once, without the time compressing it would take.
     */
    private const val MAX_COMPRESSED_TEXT = 2 * 1024 * 1024

    private val Address = Regex("""^https?://formatico\.pantrixui\.com/?(?:index\.html)?(#.*)$""", RegexOption.IGNORE_CASE)
    private val Base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

    /**
     * The encrypted link of the share [id] the server keeps, decrypted with [key], opened at [site]: Formatico on the
     * web, or the page's own address where the web app runs elsewhere, a local stack or a test one.
     */
    fun encrypted(id: String, key: ByteArray, site: String = SITE): String = siteOf(site) + "#s=" + id + "&k=" + Base64Url.encode(key)

    /** The bytes of an encrypted link's [key], as the link writes it. Throws where it is not a key's. */
    internal fun keyBytes(key: String): ByteArray = Base64Url.decode(key).also { require(it.size == KEY_BYTES) }

    /** The link of [document], opened at [site] as [encrypted]'s is, or why there is none: it is too large for a link. */
    fun share(document: SharedDocument, site: String = SITE): ShareOutcome {
        val bytes = document.text.encodeToByteArray()
        if (bytes.size > MAX_COMPRESSED_TEXT) return ShareOutcome.TooLarge(bytes.size)
        val link = siteOf(site) + fragmentOf(Zlib.compress(bytes), document.name)
        return if (link.length > MAX_LENGTH) ShareOutcome.TooLarge(bytes.size) else ShareOutcome.Link(link, bytes.size)
    }

    /**
     * [text] read as a link of Formatico's: a whole link, at Formatico's site or at [site], or the part of one from its
     * `#`, as a browser's address gives it. Spaces around it are let be, as a paste brings them.
     */
    fun read(text: String, site: String = SITE): LinkReading {
        val trimmed = text.trim()
        // A bare fragment is one of Formatico's only where it carries a document: a text may begin with a comment.
        val bare = trimmed.startsWith("#d=") || trimmed.startsWith("#s=")
        val address = Address.matchEntire(trimmed) ?: siteAddress(site)?.matchEntire(trimmed)
        val fragment = if (bare) trimmed else address?.groupValues?.get(1)
        val parts = fragment?.removePrefix("#")?.split('&')?.associate { part ->
            part.substringBefore('=') to part.substringAfter('=', missingDelimiterValue = "")
        }
        parts?.get("s")?.let { id -> return encryptedOf(id, parts["k"]) }
        val packed = parts?.get("d") ?: return LinkReading.NotALink
        val bytes = runCatching { Zlib.decompress(Base64Url.decode(packed), MAX_TEXT) }.getOrNull()
        val unpacked = bytes?.let { runCatching { it.decodeToString(throwOnInvalidSequence = true) }.getOrNull() }
        return unpacked?.let { LinkReading.Read(SharedDocument(it, parts["n"]?.let(::nameOf))) } ?: LinkReading.Broken
    }

    /** The encrypted link of [id] and [key], or a broken one where either is not what Formatico writes. */
    private fun encryptedOf(id: String, key: String?): LinkReading {
        val read = key?.takeIf { ShareServer.Id.matches(id) && runCatching { keyBytes(it) }.isSuccess }
        return read?.let { LinkReading.Encrypted(id, it) } ?: LinkReading.Broken
    }

    /** [site] as a link begins with it: with the one slash before its `#`. */
    private fun siteOf(site: String): String = site.trimEnd('/') + "/"

    /** A link at [site], where it is not Formatico's own, as [Address] reads one at Formatico's. */
    private fun siteAddress(site: String): Regex? =
        siteOf(site).takeIf { it != SITE }?.let {
            Regex("^" + Regex.escape(it.trimEnd('/')) + "/?(?:index\\.html)?(#.*)$", RegexOption.IGNORE_CASE)
        }

    private fun fragmentOf(packed: ByteArray, name: String?): String =
        "#d=" + Base64Url.encode(packed) + (name?.takeIf { it.isNotBlank() }?.let { "&n=" + percentEncoded(it) } ?: "")

    /** The name a link carries, or none where its encoding is not one. */
    private fun nameOf(encoded: String): String? = runCatching { percentDecoded(encoded) }.getOrNull()?.takeIf { it.isNotBlank() }
}

/** The letters an address may hold as they are: RFC 3986's unreserved ones. */
private fun Byte.unreserved(): Boolean {
    val char = toInt().toChar()
    return char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char == '-' || char == '.' || char == '_' || char == '~'
}

private const val HEX = "0123456789ABCDEF"
private const val HEX_DIGIT_BITS = 4
private const val HEX_DIGIT_MASK = 0x0F
private const val HEX_RADIX = 16
private const val ESCAPE_LENGTH = 3
private const val ASCII_LIMIT = 0x80

/** [text] in UTF-8, every byte but the unreserved ones written as `%` and two hexadecimal digits. */
internal fun percentEncoded(text: String): String = buildString {
    for (byte in text.encodeToByteArray()) {
        if (byte.unreserved()) {
            append(byte.toInt().toChar())
        } else {
            val value = byte.unsigned()
            append('%').append(HEX[value ushr HEX_DIGIT_BITS]).append(HEX[value and HEX_DIGIT_MASK])
        }
    }
}

/** What [encoded] says, its `%` and two hexadecimal digits read as bytes of UTF-8. Throws where they are not. */
internal fun percentDecoded(encoded: String): String {
    val bytes = ArrayList<Byte>(encoded.length)
    var at = 0
    while (at < encoded.length) {
        val char = encoded[at]
        if (char == '%') {
            require(at + ESCAPE_LENGTH <= encoded.length)
            bytes += encoded.substring(at + 1, at + ESCAPE_LENGTH).toInt(HEX_RADIX).toByte()
            at += ESCAPE_LENGTH
        } else {
            require(char.code < ASCII_LIMIT)
            bytes += char.code.toByte()
            at++
        }
    }
    return bytes.toByteArray().decodeToString(throwOnInvalidSequence = true)
}
