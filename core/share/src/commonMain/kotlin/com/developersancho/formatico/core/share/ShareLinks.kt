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
 */
object ShareLinks {
    /** Where a link opens: Formatico on the web. */
    const val SITE = "https://formatico.pantrixui.com/"

    /** The longest link Formatico makes: "a link holds about 32 KB". */
    const val MAX_LENGTH = 32_768

    /** The longest link every browser and app takes whole. */
    const val SHORT_LENGTH = 2_000

    /** The most a link is unpacked to: the largest document a phone or a browser opens, 10 MB. */
    const val MAX_TEXT = 10 * 1024 * 1024

    /**
     * The most text Formatico tries to compress into a link: no text compresses into 32 KB from more. A larger one is
     * too large at once, without the time compressing it would take.
     */
    private const val MAX_COMPRESSED_TEXT = 2 * 1024 * 1024

    private val Address = Regex("""^https?://formatico\.pantrixui\.com/?(?:index\.html)?(#.*)$""", RegexOption.IGNORE_CASE)
    private val Base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

    /** The link of [document], or why there is none: it is too large for a link. */
    fun share(document: SharedDocument): ShareOutcome {
        val bytes = document.text.encodeToByteArray()
        if (bytes.size > MAX_COMPRESSED_TEXT) return ShareOutcome.TooLarge(bytes.size)
        val link = SITE + fragmentOf(Zlib.compress(bytes), document.name)
        return if (link.length > MAX_LENGTH) ShareOutcome.TooLarge(bytes.size) else ShareOutcome.Link(link, bytes.size)
    }

    /**
     * [text] read as a link of Formatico's: a whole link, or the part of one from its `#`, as a browser's address
     * gives it. Spaces around it are let be, as a paste brings them.
     */
    fun read(text: String): LinkReading {
        val trimmed = text.trim()
        // A bare fragment is one of Formatico's only where it carries a document: a text may begin with a comment.
        val fragment = if (trimmed.startsWith("#d=")) trimmed else Address.matchEntire(trimmed)?.groupValues?.get(1)
        val parts = fragment?.removePrefix("#")?.split('&')?.associate { part ->
            part.substringBefore('=') to part.substringAfter('=', missingDelimiterValue = "")
        }
        val packed = parts?.get("d") ?: return LinkReading.NotALink
        val bytes = runCatching { Zlib.decompress(Base64Url.decode(packed), MAX_TEXT) }.getOrNull()
        val unpacked = bytes?.let { runCatching { it.decodeToString(throwOnInvalidSequence = true) }.getOrNull() }
        return unpacked?.let { LinkReading.Read(SharedDocument(it, parts["n"]?.let(::nameOf))) } ?: LinkReading.Broken
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
