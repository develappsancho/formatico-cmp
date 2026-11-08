package com.developersancho.formatico.core.share

/** The tables RFC 1951 writes a match's length and distance by, and the order a block's code lengths come in. */
internal object DeflateTables {
    /** The shortest length of each length code, 257 to 285. */
    val LengthBase =
        intArrayOf(3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258)

    /** How many bits follow each length code. */
    val LengthExtra = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0)

    /** The shortest distance of each distance code, 0 to 29. */
    val DistanceBase = intArrayOf(1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513, 769) +
        intArrayOf(1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577)

    /** How many bits follow each distance code. */
    val DistanceExtra = intArrayOf(0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13)

    /** The order a dynamic block writes the code lengths of its code lengths in. */
    val CodeLengthOrder = intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)

    const val END_OF_BLOCK = 256
    const val LITERAL_CODES = 286
    const val DISTANCE_CODES = 30
    const val MAX_BITS = 15
    const val MAX_CODE_LENGTH_BITS = 7
    const val MIN_MATCH = 3
    const val MAX_MATCH = 258
    const val WINDOW = 32_768

    /** The code lengths of the fixed codes: of a literal or a length, and of a distance. */
    val FixedLiteralLengths = IntArray(288) { symbol ->
        when {
            symbol < 144 -> 8
            symbol < 256 -> 9
            symbol < 280 -> 7
            else -> 8
        }
    }
    val FixedDistanceLengths = IntArray(DISTANCE_CODES) { 5 }

    /** The length code, 0 to 28 (257 to 285 less 257), of each length from 3 to 258. */
    val LengthCode = IntArray(MAX_MATCH + 1).also { codes ->
        var code = 0
        for (length in MIN_MATCH..MAX_MATCH) {
            while (code + 1 < LengthBase.size && LengthBase[code + 1] <= length) code++
            codes[length] = code
        }
    }

    /** The distance code of [distance], from 1 to 32768. */
    fun distanceCode(distance: Int): Int {
        var code = DistanceBase.size - 1
        while (DistanceBase[code] > distance) code--
        return code
    }
}
