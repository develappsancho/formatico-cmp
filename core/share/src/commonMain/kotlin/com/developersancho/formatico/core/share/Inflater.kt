package com.developersancho.formatico.core.share

import com.developersancho.formatico.core.share.DeflateTables.CodeLengthOrder
import com.developersancho.formatico.core.share.DeflateTables.DISTANCE_CODES
import com.developersancho.formatico.core.share.DeflateTables.DistanceBase
import com.developersancho.formatico.core.share.DeflateTables.DistanceExtra
import com.developersancho.formatico.core.share.DeflateTables.END_OF_BLOCK
import com.developersancho.formatico.core.share.DeflateTables.FixedDistanceLengths
import com.developersancho.formatico.core.share.DeflateTables.FixedLiteralLengths
import com.developersancho.formatico.core.share.DeflateTables.LITERAL_CODES
import com.developersancho.formatico.core.share.DeflateTables.LengthBase
import com.developersancho.formatico.core.share.DeflateTables.LengthExtra
import com.developersancho.formatico.core.share.DeflateTables.MAX_BITS

/** What a stream that is not deflate's, or that is cut short, or that holds more than it may, stops reading with. */
private class NotDeflate : Exception()

/** Stops reading where [condition] does not hold. */
private fun need(condition: Boolean) {
    if (!condition) throw NotDeflate()
}

/**
 * A deflate stream (RFC 1951) of [data] from [start], read back: after [inflate], [end] is where it ended, at the next
 * whole byte. A stream that would unpack to more than [limit] bytes is not read, as a link made to fill a phone is not.
 * The decoding is puff's (zlib's reference), a bit at a time: what it reads is a link's, a few kilobytes.
 */
internal class Inflater(private val data: ByteArray, start: Int, private val limit: Int) {
    private var at = start
    private var bits = 0
    private var count = 0
    private var output = ByteArray(minOf(limit, maxOf(INITIAL_SIZE, data.size * GROWTH)))
    private var size = 0

    /** Where the stream ended: the byte after its last, once [inflate] has read it. */
    val end: Int get() = at

    /** The bytes the stream holds, or null where it is not a whole deflate stream or holds more than the limit. */
    fun inflate(): ByteArray? = try {
        do {
            val last = bits(1) == 1
            when (bits(TYPE_BITS)) {
                STORED -> stored()
                FIXED -> codes(FixedLiterals, FixedDistances)
                DYNAMIC -> dynamic()
                else -> need(false)
            }
        } while (!last)
        // What is left of the last byte is padding; the stream's end is the next whole byte.
        bits = 0
        count = 0
        output.copyOf(size)
    } catch (_: NotDeflate) {
        null
    } catch (_: IndexOutOfBoundsException) {
        null
    }

    private fun bits(needed: Int): Int {
        var value = bits
        while (count < needed) {
            need(at < data.size)
            value = value or (data[at++].unsigned() shl count)
            count += Byte.SIZE_BITS
        }
        bits = value ushr needed
        count -= needed
        return value and ((1 shl needed) - 1)
    }

    private fun put(byte: Byte) {
        if (size == output.size) {
            need(size < limit)
            output = output.copyOf(minOf(limit, output.size * 2))
        }
        output[size++] = byte
    }

    /** A block stored as it is: its length, the length's complement, and its bytes, from the next whole byte. */
    private fun stored() {
        bits = 0
        count = 0
        need(at + STORED_HEAD <= data.size)
        val length = littleEndian(at)
        val check = littleEndian(at + 2)
        need(length == check.inv() and LENGTH_MASK)
        at += STORED_HEAD
        need(at + length <= data.size)
        repeat(length) { put(data[at++]) }
    }

    private fun littleEndian(from: Int): Int = data[from].unsigned() or (data[from + 1].unsigned() shl Byte.SIZE_BITS)

    /** A block in its own codes: how many codes of each kind it has, and their lengths, in a code of their own. */
    private fun dynamic() {
        val literals = bits(LITERAL_COUNT_BITS) + END_OF_BLOCK + 1
        val distances = bits(DISTANCE_COUNT_BITS) + 1
        val codeLengthCount = bits(CODE_LENGTH_COUNT_BITS) + MIN_CODE_LENGTH_COUNT
        need(literals <= LITERAL_CODES && distances <= DISTANCE_CODES)
        val codeLengthLengths = IntArray(CodeLengthOrder.size)
        for (index in 0 until codeLengthCount) codeLengthLengths[CodeLengthOrder[index]] = bits(CODE_LENGTH_BITS)
        val codeLengths = Huffman(codeLengthLengths)
        val lengths = IntArray(literals + distances)
        var index = 0
        while (index < lengths.size) {
            val symbol = decode(codeLengths)
            if (symbol < REPEAT) {
                lengths[index++] = symbol
                continue
            }
            need(symbol != REPEAT || index > 0)
            val (value, times) = when (symbol) {
                REPEAT -> lengths[index - 1] to REPEAT_MIN + bits(REPEAT_BITS)
                SHORT_ZEROS -> 0 to SHORT_ZEROS_MIN + bits(SHORT_ZEROS_BITS)
                else -> 0 to LONG_ZEROS_MIN + bits(LONG_ZEROS_BITS)
            }
            need(index + times <= lengths.size)
            repeat(times) { lengths[index++] = value }
        }
        // A block without its end has no way to stop.
        need(lengths[END_OF_BLOCK] != 0)
        codes(Huffman(lengths.copyOf(literals)), Huffman(lengths.copyOfRange(literals, lengths.size)))
    }

    /** The symbols of a block up to its end: bytes, and matches of what came before. */
    private fun codes(literals: Huffman, distances: Huffman) {
        while (true) {
            val symbol = decode(literals)
            if (symbol == END_OF_BLOCK) return
            if (symbol < END_OF_BLOCK) {
                put(symbol.toByte())
                continue
            }
            val lengthCode = symbol - END_OF_BLOCK - 1
            need(lengthCode < LengthBase.size)
            val length = LengthBase[lengthCode] + bits(LengthExtra[lengthCode])
            val distanceCode = decode(distances)
            need(distanceCode < DISTANCE_CODES)
            val distance = DistanceBase[distanceCode] + bits(DistanceExtra[distanceCode])
            need(distance <= size)
            repeat(length) { put(output[size - distance]) }
        }
    }

    /** The next symbol of [code], read a bit at a time: each code of a length follows those of the length before. */
    private fun decode(code: Huffman): Int {
        var value = 0
        var first = 0
        var index = 0
        for (length in 1..MAX_BITS) {
            value = value or bits(1)
            val count = code.count[length]
            if (value - count < first) return code.symbol[index + (value - first)]
            index += count
            first = (first + count) shl 1
            value = value shl 1
        }
        need(false)
        return -1
    }

    /** A code read from its lengths: how many codes each length has, and the symbols in the order of their codes. */
    private class Huffman(lengths: IntArray) {
        val count = IntArray(MAX_BITS + 1)
        val symbol = IntArray(lengths.size)

        init {
            for (length in lengths) count[length]++
            count[0] = 0
            // More codes than the lengths have room for: no stream writes them.
            var left = 1
            for (length in 1..MAX_BITS) {
                left = (left shl 1) - count[length]
                need(left >= 0)
            }
            val offset = IntArray(MAX_BITS + 2)
            for (length in 1..MAX_BITS) offset[length + 1] = offset[length] + count[length]
            for (index in lengths.indices) if (lengths[index] != 0) symbol[offset[lengths[index]]++] = index
        }
    }

    private companion object {
        const val INITIAL_SIZE = 4096
        const val GROWTH = 4
        const val TYPE_BITS = 2
        const val STORED = 0
        const val FIXED = 1
        const val DYNAMIC = 2
        const val STORED_HEAD = 4
        const val LENGTH_MASK = 0xFFFF

        val FixedLiterals by lazy { Huffman(FixedLiteralLengths) }
        val FixedDistances by lazy { Huffman(FixedDistanceLengths) }
    }
}
