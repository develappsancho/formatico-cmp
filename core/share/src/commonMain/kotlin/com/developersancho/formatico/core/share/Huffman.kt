package com.developersancho.formatico.core.share

private const val BYTE_MASK = 0xFF

/** The byte as a number from 0 to 255. */
internal fun Byte.unsigned(): Int = toInt() and BYTE_MASK

/**
 * The length of each symbol's Huffman code, by its [frequencies], none longer than [maxBits]: 0 for a symbol never
 * used. Two symbols or more make a complete code; one alone is given a code of one bit, as RFC 1951 lets a code of one
 * symbol be.
 */
internal fun huffmanLengths(frequencies: IntArray, maxBits: Int): IntArray {
    val lengths = IntArray(frequencies.size)
    // The symbols used, the rarest first.
    val symbols = frequencies.indices.filter { frequencies[it] > 0 }.sortedWith(compareBy({ frequencies[it] }, { it }))
    if (symbols.size == 1) lengths[symbols.single()] = 1
    if (symbols.size < 2) return lengths
    val codes = codesOfEachLength(treeDepths(symbols.map { frequencies[it].toLong() }), maxBits)
    // The rarest symbols take the longest codes.
    var index = 0
    for (bits in maxBits downTo 1) repeat(codes[bits]) { lengths[symbols[index++]] = bits }
    return lengths
}

/**
 * The depth of each leaf in Huffman's tree of [weights], which are in their order: the two lightest nodes are always
 * at the front of the leaves or of the inner nodes, which are made in the order of their weights too.
 */
private fun treeDepths(weights: List<Long>): IntArray {
    val leaves = weights.size
    val nodes = 2 * leaves - 1
    val weight = LongArray(nodes)
    val parent = IntArray(nodes)
    for (index in 0 until leaves) weight[index] = weights[index]
    var leaf = 0
    var inner = leaves
    var next = leaves
    val lightest = { if (leaf < leaves && (inner >= next || weight[leaf] <= weight[inner])) leaf++ else inner++ }
    while (next < nodes) {
        val first = lightest()
        val second = lightest()
        weight[next] = weight[first] + weight[second]
        parent[first] = next
        parent[second] = next
        next++
    }
    // A node is made after its children: from the root down, each depth is its parent's and one.
    val depth = IntArray(nodes)
    for (node in nodes - 2 downTo 0) depth[node] = depth[parent[node]] + 1
    return depth.copyOf(leaves)
}

/**
 * How many codes each length has, of a tree whose leaves are as deep as [depths]: the longest cut to [maxBits], and
 * then as many lengthened from shorter ones as Kraft's sum asks, as miniz does.
 */
private fun codesOfEachLength(depths: IntArray, maxBits: Int): IntArray {
    val codes = IntArray(maxBits + 1)
    for (depth in depths) codes[minOf(depth, maxBits)]++
    var total = 0L
    for (bits in 1..maxBits) total += codes[bits].toLong() shl (maxBits - bits)
    while (total != 1L shl maxBits) {
        codes[maxBits]--
        val shorter = (maxBits - 1 downTo 1).first { codes[it] != 0 }
        codes[shorter]--
        codes[shorter + 1] += 2
        total--
    }
    return codes
}

/**
 * The canonical code of each symbol of [lengths], as RFC 1951 (3.2.2) gives it, its bits reversed: deflate's stream is
 * written from the lowest bit up, and a code from its highest.
 */
internal fun canonicalCodes(lengths: IntArray): IntArray {
    val count = IntArray(DeflateTables.MAX_BITS + 1)
    for (length in lengths) if (length > 0) count[length]++
    val next = IntArray(DeflateTables.MAX_BITS + 1)
    var code = 0
    for (bits in 1..DeflateTables.MAX_BITS) {
        code = (code + count[bits - 1]) shl 1
        next[bits] = code
    }
    return IntArray(lengths.size) { symbol ->
        val length = lengths[symbol]
        if (length == 0) 0 else reversed(next[length]++, length)
    }
}

/** The lowest [length] bits of [code], the other way round. */
private fun reversed(code: Int, length: Int): Int {
    var from = code
    var to = 0
    repeat(length) {
        to = (to shl 1) or (from and 1)
        from = from ushr 1
    }
    return to
}

/** Bytes written a bit at a time, from each byte's lowest bit up, as deflate's stream is. */
internal class BitWriter {
    private var bytes = ByteArray(INITIAL_SIZE)
    private var size = 0
    private var bits = 0L
    private var count = 0

    /** Writes the lowest [length] bits of [value], the lowest first. */
    fun write(value: Int, length: Int) {
        bits = bits or ((value.toLong() and ((1L shl length) - 1)) shl count)
        count += length
        while (count >= Byte.SIZE_BITS) {
            put(bits.toByte())
            bits = bits ushr Byte.SIZE_BITS
            count -= Byte.SIZE_BITS
        }
    }

    /** The bytes written, the last one filled up with nought bits. */
    fun toByteArray(): ByteArray {
        if (count > 0) put(bits.toByte())
        bits = 0
        count = 0
        return bytes.copyOf(size)
    }

    private fun put(byte: Byte) {
        if (size == bytes.size) bytes = bytes.copyOf(bytes.size * 2)
        bytes[size++] = byte
    }

    private companion object {
        const val INITIAL_SIZE = 1024
    }
}
