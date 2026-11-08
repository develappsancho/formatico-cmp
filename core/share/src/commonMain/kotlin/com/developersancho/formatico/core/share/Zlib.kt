package com.developersancho.formatico.core.share

/**
 * zlib's format (RFC 1950) around deflate's: two bytes that say it is deflate, the stream, and the Adler-32 of what it
 * holds. It is what a browser's `DecompressionStream("deflate")`, Java's `Inflater` and zlib itself read.
 */
internal object Zlib {
    /** The head Formatico writes: deflate in a window of 32 KB, at zlib's default level (`78 9C`, "eJ" in Base64). */
    private const val METHOD = 0x78
    private const val FLAGS = 0x9C

    private const val HEAD_BYTES = 2
    private const val CHECKSUM_BYTES = 4
    private const val DEFLATE = 8
    private const val METHOD_MASK = 0x0F
    private const val WINDOW_SHIFT = 4
    private const val LARGEST_WINDOW = 7
    private const val HEAD_CHECK = 31
    private const val PRESET_DICTIONARY = 0x20

    /** [data] compressed in zlib's format. */
    fun compress(data: ByteArray): ByteArray {
        val checksum = adler32(data)
        val tail = ByteArray(CHECKSUM_BYTES) { (checksum ushr (Byte.SIZE_BITS * (CHECKSUM_BYTES - 1 - it))).toByte() }
        return byteArrayOf(METHOD.toByte(), FLAGS.toByte()) + deflate(data) + tail
    }

    /**
     * What [data] holds, or null where it is not zlib's format with deflate in it, its checksum does not hold, or it
     * holds more than [limit] bytes.
     */
    fun decompress(data: ByteArray, limit: Int): ByteArray? {
        if (data.size < HEAD_BYTES + CHECKSUM_BYTES || !readable(data[0].unsigned(), data[1].unsigned())) return null
        val inflater = Inflater(data, HEAD_BYTES, limit)
        val unpacked = inflater.inflate() ?: return null
        val end = inflater.end
        return unpacked.takeIf { end + CHECKSUM_BYTES <= data.size && adler32(it) == bigEndian(data, end) }
    }

    /** Whether a head of [method] and [flags] is deflate's, in a window zlib has, with no preset dictionary. */
    private fun readable(method: Int, flags: Int): Boolean =
        method and METHOD_MASK == DEFLATE &&
            method ushr WINDOW_SHIFT <= LARGEST_WINDOW &&
            (method shl Byte.SIZE_BITS or flags) % HEAD_CHECK == 0 &&
            // A preset dictionary is the writer's alone: a link cannot carry it.
            flags and PRESET_DICTIONARY == 0

    private fun bigEndian(data: ByteArray, from: Int): Int =
        (0 until CHECKSUM_BYTES).fold(0) { value, index -> (value shl Byte.SIZE_BITS) or data[from + index].unsigned() }
}

private const val ADLER_MODULO = 65_521
private const val ADLER_HIGH_SHIFT = 16

/** The most bytes added before the sums are taken modulo, as zlib does: their sum cannot yet pass an Int. */
private const val ADLER_RUN = 5_552

/** The Adler-32 checksum of [data] (RFC 1950, 8.2). */
internal fun adler32(data: ByteArray): Int {
    var low = 1L
    var high = 0L
    var at = 0
    while (at < data.size) {
        val end = minOf(at + ADLER_RUN, data.size)
        while (at < end) {
            low += data[at++].unsigned()
            high += low
        }
        low %= ADLER_MODULO
        high %= ADLER_MODULO
    }
    return ((high shl ADLER_HIGH_SHIFT) or low).toInt()
}
