package com.developersancho.formatico.core.format.diff

/**
 * The longest run of pairs, a place in A and one in B, both rising, whose items are the same as [same] says: what two
 * sequences of [aSize] and [bSize] items have in common, in order. Past [LCS_LIMIT] pairs of places to weigh, the
 * sequences are taken to have nothing in common, as working it out would take too long.
 */
internal fun commonRun(aSize: Int, bSize: Int, same: (Int, Int) -> Boolean): List<Pair<Int, Int>> {
    if (aSize == 0 || bSize == 0 || aSize.toLong() * bSize > LCS_LIMIT) return emptyList()
    // lengths[i][j]: how long the common run of A from i and B from j is.
    val lengths = Array(aSize + 1) { IntArray(bSize + 1) }
    for (i in aSize - 1 downTo 0) {
        for (j in bSize - 1 downTo 0) {
            lengths[i][j] = if (same(i, j)) lengths[i + 1][j + 1] + 1 else maxOf(lengths[i + 1][j], lengths[i][j + 1])
        }
    }
    val pairs = ArrayList<Pair<Int, Int>>(lengths[0][0])
    var i = 0
    var j = 0
    while (i < aSize && j < bSize) {
        when {
            same(i, j) -> pairs += (i++ to j++)
            lengths[i + 1][j] >= lengths[i][j + 1] -> i++
            else -> j++
        }
    }
    return pairs
}

/** The most pairs of places a common run is worked out for: two sequences of two thousand items each. */
internal const val LCS_LIMIT = 4_000_000L
