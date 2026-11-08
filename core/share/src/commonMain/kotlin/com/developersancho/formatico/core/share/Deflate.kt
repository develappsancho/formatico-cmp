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
import com.developersancho.formatico.core.share.DeflateTables.LengthCode
import com.developersancho.formatico.core.share.DeflateTables.LengthExtra
import com.developersancho.formatico.core.share.DeflateTables.MAX_BITS
import com.developersancho.formatico.core.share.DeflateTables.MAX_CODE_LENGTH_BITS
import com.developersancho.formatico.core.share.DeflateTables.distanceCode

/** How many symbols a block holds at the most: each block has codes of its own, fitted to what it holds. */
private const val BLOCK_SYMBOLS = 1 shl 15

/** The bits that say a block is the last, and of which kind it is: in the fixed codes, or in its own. */
private const val LAST_BITS = 1
private const val TYPE_BITS = 2
private const val FIXED = 1
private const val DYNAMIC = 2

/**
 * [data] compressed as RFC 1951's deflate: matches found in the last 32 KB (LZ77, with a look one place ahead), and
 * each block in fixed or in its own codes, whichever is shorter. The same data gives the same bytes on every platform.
 */
internal fun deflate(data: ByteArray): ByteArray {
    val out = BitWriter()
    val symbols = Lz77(data).symbols()
    if (symbols.size == 0) {
        // Nothing but the end of a last block, in the fixed codes.
        out.write(1, LAST_BITS)
        out.write(FIXED, TYPE_BITS)
        out.write(0, FixedLiteralLengths[END_OF_BLOCK])
        return out.toByteArray()
    }
    var start = 0
    while (start < symbols.size) {
        val end = minOf(start + BLOCK_SYMBOLS, symbols.size)
        writeBlock(out, symbols, start until end, last = end == symbols.size)
        start = end
    }
    return out.toByteArray()
}

/** Ints in a row that grow as they are added: the symbols of a text, without boxing each. */
internal class IntRow {
    private var values = IntArray(INITIAL_SIZE)
    var size = 0
        private set

    fun add(value: Int) {
        if (size == values.size) values = values.copyOf(values.size * 2)
        values[size++] = value
    }

    operator fun get(index: Int): Int = values[index]

    private companion object {
        const val INITIAL_SIZE = 256
    }
}

/** A match among LZ77's symbols: its length shifted up this far, and its distance in the bits under them. */
internal const val MATCH_SHIFT = 16
internal const val DISTANCE_MASK = (1 shl MATCH_SHIFT) - 1

/**
 * [data] as LZ77's symbols: a byte under 256, or a match as its length shifted up [MATCH_SHIFT] bits and its distance.
 * The earlier places of each three bytes are chained by a hash of them; a match is taken where the next place has none
 * longer.
 */
private class Lz77(private val data: ByteArray) {
    private val head = IntArray(1 shl HASH_BITS) { -1 }
    private val previous = IntArray(DeflateTables.WINDOW)

    fun symbols(): IntRow {
        val symbols = IntRow()
        var at = 0
        while (at < data.size) {
            val match = longest(at)
            insert(at)
            val length = match ushr MATCH_SHIFT
            val better = length in DeflateTables.MIN_MATCH until NICE_MATCH && (longest(at + 1) ushr MATCH_SHIFT) > length
            if (match == 0 || better) {
                symbols.add(data[at].unsigned())
                at++
            } else {
                symbols.add(match)
                for (covered in at + 1 until at + length) insert(covered)
                at += length
            }
        }
        return symbols
    }

    private fun hash(at: Int): Int =
        ((data[at].unsigned() shl FIRST_SHIFT) xor (data[at + 1].unsigned() shl SECOND_SHIFT) xor data[at + 2].unsigned()) and
            HASH_MASK

    private fun insert(at: Int) {
        if (at + DeflateTables.MIN_MATCH > data.size) return
        val key = hash(at)
        previous[at and WINDOW_MASK] = head[key]
        head[key] = at
    }

    /** The longest match for the place [at], as a symbol; nought for none. */
    private fun longest(at: Int): Int {
        if (at + DeflateTables.MIN_MATCH > data.size) return 0
        val most = minOf(DeflateTables.MAX_MATCH, data.size - at)
        var best = DeflateTables.MIN_MATCH - 1
        var distance = 0
        var candidate = head[hash(at)]
        var chain = MAX_CHAIN
        // A distance under the window's size: a place further back may be one the window has written over.
        while (candidate > at - DeflateTables.WINDOW && candidate in 0 until at && chain-- > 0) {
            val length = if (data[candidate + best] == data[at + best]) matched(candidate, at, most) else 0
            if (length > best) {
                best = length
                distance = at - candidate
                if (length >= most || length >= NICE_MATCH) break
            }
            candidate = previous[candidate and WINDOW_MASK]
        }
        return if (best >= DeflateTables.MIN_MATCH) (best shl MATCH_SHIFT) or distance else 0
    }

    /** How many bytes from [earlier] are the bytes from [at], [most] at the most. */
    private fun matched(earlier: Int, at: Int, most: Int): Int {
        var length = 0
        while (length < most && data[earlier + length] == data[at + length]) length++
        return length
    }

    private companion object {
        const val HASH_BITS = 15
        const val HASH_MASK = (1 shl HASH_BITS) - 1
        const val FIRST_SHIFT = 10
        const val SECOND_SHIFT = 5
        const val WINDOW_MASK = DeflateTables.WINDOW - 1

        /** How many earlier places a match is looked for at, and the length that is good enough to stop looking. */
        const val MAX_CHAIN = 256
        const val NICE_MATCH = 128
    }
}

/** The codes a block's symbols are written in: of the literals and the lengths, and of the distances. */
private class BlockCodes(val literalLengths: IntArray, val distanceLengths: IntArray) {
    val literalCodes = canonicalCodes(literalLengths)
    val distanceCodes = canonicalCodes(distanceLengths)

    fun literal(out: BitWriter, symbol: Int) = out.write(literalCodes[symbol], literalLengths[symbol])

    fun distance(out: BitWriter, code: Int) = out.write(distanceCodes[code], distanceLengths[code])
}

/** Writes the [symbols] in [range] as one block, in the fixed codes or in its own, whichever is shorter. */
private fun writeBlock(out: BitWriter, symbols: IntRow, range: IntRange, last: Boolean) {
    val literalFrequencies = IntArray(LITERAL_CODES)
    val distanceFrequencies = IntArray(DISTANCE_CODES)
    for (index in range) {
        val symbol = symbols[index]
        if (symbol < END_OF_BLOCK) {
            literalFrequencies[symbol]++
        } else {
            literalFrequencies[END_OF_BLOCK + 1 + LengthCode[symbol ushr MATCH_SHIFT]]++
            distanceFrequencies[distanceCode(symbol and DISTANCE_MASK)]++
        }
    }
    literalFrequencies[END_OF_BLOCK] = 1
    // A block of no match still names a distance code, as zlib's own blocks do.
    if (distanceFrequencies.all { it == 0 }) distanceFrequencies[0] = 1
    val own = BlockCodes(huffmanLengths(literalFrequencies, MAX_BITS), huffmanLengths(distanceFrequencies, MAX_BITS))
    val header = DynamicHeader(own.literalLengths, own.distanceLengths)
    // The extra bits of the lengths and the distances are the same in either.
    val fixedBits = cost(literalFrequencies, FixedLiteralLengths) + cost(distanceFrequencies, FixedDistanceLengths)
    val ownBits = header.bits + cost(literalFrequencies, own.literalLengths) + cost(distanceFrequencies, own.distanceLengths)
    out.write(if (last) 1 else 0, LAST_BITS)
    if (fixedBits <= ownBits) {
        out.write(FIXED, TYPE_BITS)
        writeSymbols(out, symbols, range, FixedCodes)
    } else {
        out.write(DYNAMIC, TYPE_BITS)
        header.write(out)
        writeSymbols(out, symbols, range, own)
    }
}

private val FixedCodes by lazy { BlockCodes(FixedLiteralLengths, FixedDistanceLengths) }

/** How many bits [frequencies]' symbols take in codes of [lengths]. */
private fun cost(frequencies: IntArray, lengths: IntArray): Long =
    frequencies.indices.sumOf { frequencies[it].toLong() * lengths[it] }

/** Writes the [symbols] in [range] and the end of the block, in [codes]. */
private fun writeSymbols(out: BitWriter, symbols: IntRow, range: IntRange, codes: BlockCodes) {
    for (index in range) {
        val symbol = symbols[index]
        if (symbol < END_OF_BLOCK) {
            codes.literal(out, symbol)
            continue
        }
        val length = symbol ushr MATCH_SHIFT
        val distance = symbol and DISTANCE_MASK
        val lengthCode = LengthCode[length]
        codes.literal(out, END_OF_BLOCK + 1 + lengthCode)
        out.write(length - LengthBase[lengthCode], LengthExtra[lengthCode])
        val code = distanceCode(distance)
        codes.distance(out, code)
        out.write(distance - DistanceBase[code], DistanceExtra[code])
    }
    codes.literal(out, END_OF_BLOCK)
}

/**
 * The head of a block in its own codes: how many codes of each kind it has, and their lengths, written in a code of
 * their own with runs (RFC 1951, 3.2.7).
 */
private class DynamicHeader(literalLengths: IntArray, distanceLengths: IntArray) {
    private val literals = maxOf(END_OF_BLOCK + 1, literalLengths.indexOfLast { it > 0 } + 1)
    private val distances = maxOf(1, distanceLengths.indexOfLast { it > 0 } + 1)

    /** The code lengths in runs: each a symbol from 0 to 18, the value of its extra bits, and how many they are. */
    private val runs = IntRow()
    private val frequencies = IntArray(CODE_LENGTH_SYMBOLS)
    private val codeLengths: IntArray
    private val codeLengthCount: Int

    /** How many bits the head takes. */
    val bits: Long

    init {
        val lengths = literalLengths.copyOf(literals) + distanceLengths.copyOf(distances)
        var at = 0
        while (at < lengths.size) {
            var run = 1
            while (at + run < lengths.size && lengths[at + run] == lengths[at]) run++
            if (lengths[at] == 0) zeros(run) else repeats(lengths[at], run)
            at += run
        }
        // The code of the code lengths has to be complete: two symbols at the least.
        if (frequencies.count { it > 0 } < 2) frequencies[if (frequencies[0] == 0) 0 else 1]++
        codeLengths = huffmanLengths(frequencies, MAX_CODE_LENGTH_BITS)
        codeLengthCount = maxOf(MIN_CODE_LENGTH_COUNT, CodeLengthOrder.indexOfLast { codeLengths[it] > 0 } + 1)
        var total = LITERAL_COUNT_BITS.toLong() + DISTANCE_COUNT_BITS + CODE_LENGTH_COUNT_BITS + CODE_LENGTH_BITS * codeLengthCount
        for (index in 0 until runs.size step RUN_FIELDS) total += codeLengths[runs[index]] + runs[index + 2]
        bits = total
    }

    /** A run of [run] noughts: in runs of 11 to 138, of 3 to 10, and the one or two left. */
    private fun zeros(run: Int) {
        var left = run
        while (left >= LONG_ZEROS_MIN) {
            val taken = minOf(left, LONG_ZEROS_MAX)
            add(LONG_ZEROS, taken - LONG_ZEROS_MIN, LONG_ZEROS_BITS)
            left -= taken
        }
        if (left >= SHORT_ZEROS_MIN) {
            add(SHORT_ZEROS, left - SHORT_ZEROS_MIN, SHORT_ZEROS_BITS)
            left = 0
        }
        repeat(left) { add(0, 0, 0) }
    }

    /** A run of [run] lengths of [length]: the first, then the others repeated 3 to 6 at a time, and what is left. */
    private fun repeats(length: Int, run: Int) {
        add(length, 0, 0)
        var left = run - 1
        while (left >= REPEAT_MIN) {
            val taken = minOf(left, REPEAT_MAX)
            add(REPEAT, taken - REPEAT_MIN, REPEAT_BITS)
            left -= taken
        }
        repeat(left) { add(length, 0, 0) }
    }

    private fun add(symbol: Int, extra: Int, extraBits: Int) {
        frequencies[symbol]++
        runs.add(symbol)
        runs.add(extra)
        runs.add(extraBits)
    }

    fun write(out: BitWriter) {
        out.write(literals - (END_OF_BLOCK + 1), LITERAL_COUNT_BITS)
        out.write(distances - 1, DISTANCE_COUNT_BITS)
        out.write(codeLengthCount - MIN_CODE_LENGTH_COUNT, CODE_LENGTH_COUNT_BITS)
        for (index in 0 until codeLengthCount) out.write(codeLengths[CodeLengthOrder[index]], CODE_LENGTH_BITS)
        val codes = canonicalCodes(codeLengths)
        for (index in 0 until runs.size step RUN_FIELDS) {
            val symbol = runs[index]
            out.write(codes[symbol], codeLengths[symbol])
            if (runs[index + 2] > 0) out.write(runs[index + 1], runs[index + 2])
        }
    }
}

internal const val CODE_LENGTH_SYMBOLS = 19
internal const val MIN_CODE_LENGTH_COUNT = 4
internal const val LITERAL_COUNT_BITS = 5
internal const val DISTANCE_COUNT_BITS = 5
internal const val CODE_LENGTH_COUNT_BITS = 4
internal const val CODE_LENGTH_BITS = 3
internal const val REPEAT = 16
internal const val REPEAT_MIN = 3
internal const val REPEAT_MAX = 6
internal const val REPEAT_BITS = 2
internal const val SHORT_ZEROS = 17
internal const val SHORT_ZEROS_MIN = 3
internal const val SHORT_ZEROS_BITS = 3
internal const val LONG_ZEROS = 18
internal const val LONG_ZEROS_MIN = 11
internal const val LONG_ZEROS_MAX = 138
internal const val LONG_ZEROS_BITS = 7
private const val RUN_FIELDS = 3
