package com.developersancho.formatico.core.share

/**
 * What an encrypted share holds before it is encrypted: a document's name and text together, compressed in zlib's
 * format. The server keeps only what this is encrypted into, so the name is no more its to read than the text.
 *
 * Unpacked, it is a line that names the envelope's version, the name written as an address writes it (empty for a
 * document with none), and the text.
 */
object ShareEnvelope {
    private const val VERSION = "formatico:share:1\n"

    /** The most a name takes once written for an address: three characters for each of its bytes. */
    private const val MAX_NAME = 3 * 1024

    /** [document], packed to be encrypted. */
    fun pack(document: SharedDocument): ByteArray {
        val name = document.name?.takeIf { it.isNotBlank() }?.let(::percentEncoded).orEmpty()
        return Zlib.compress((VERSION + name + "\n" + document.text).encodeToByteArray())
    }

    /** The document [packed] holds, or null where it is not an envelope of Formatico's: changed, or cut short. */
    fun unpack(packed: ByteArray): SharedDocument? {
        val bytes = Zlib.decompress(packed, VERSION.length + MAX_NAME + ShareLinks.MAX_TEXT)
        val unpacked = bytes?.let { runCatching { it.decodeToString(throwOnInvalidSequence = true) }.getOrNull() }
        val rest = unpacked?.takeIf { it.startsWith(VERSION) }?.substring(VERSION.length)
        val end = rest?.indexOf('\n')?.takeIf { it >= 0 } ?: return null
        val name = runCatching { percentDecoded(rest.substring(0, end)) }.getOrNull()
        return name?.let { SharedDocument(text = rest.substring(end + 1), name = it.takeIf(String::isNotBlank)) }
    }
}
