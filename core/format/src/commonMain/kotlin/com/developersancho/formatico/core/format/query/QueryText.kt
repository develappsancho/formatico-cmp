package com.developersancho.formatico.core.format.query

/** How far the code units from U+E000 up are moved down, so that they sort below the surrogates, as their code points do. */
private const val PRIVATE_USE_SHIFT = 0x800

/** How far the surrogates are moved up, above U+FFFF, where the code points they write are. */
private const val SURROGATE_SHIFT = 0x2000

/** How many characters [text] holds, as Unicode counts them: a surrogate pair is one. */
internal fun codePointCount(text: String): Int {
    var count = 0
    var index = 0
    while (index < text.length) {
        index += characterLength(text, index)
        count++
    }
    return count
}

/** The characters of [text], as Unicode counts them, each as a string: a surrogate pair is one of them. */
internal fun characters(text: String): List<String> {
    val characters = ArrayList<String>(text.length)
    var index = 0
    while (index < text.length) {
        val length = characterLength(text, index)
        characters += text.substring(index, index + length)
        index += length
    }
    return characters
}

/** How many code units the character at [index] of [text] takes: two for a surrogate pair, one for any other. */
internal fun characterLength(text: String, index: Int): Int =
    if (text[index].isHighSurrogate() && text.getOrNull(index + 1)?.isLowSurrogate() == true) 2 else 1

/**
 * [first] against [second] in the order of their code points, below zero where [first] comes first: UTF-16 orders
 * U+E000 to U+FFFF after the surrogates that write the characters beyond them, and Unicode orders them before.
 */
internal fun compareCodePoints(first: String, second: String): Int {
    val shared = minOf(first.length, second.length)
    for (index in 0 until shared) {
        if (first[index] != second[index]) return codePointOrder(first[index]) - codePointOrder(second[index])
    }
    return first.length - second.length
}

/** Where [unit] stands in the order of code points, among the code units that may differ first in two strings. */
private fun codePointOrder(unit: Char): Int = when {
    unit >= '' -> unit.code - PRIVATE_USE_SHIFT
    unit >= '\uD800' -> unit.code + SURROGATE_SHIFT
    else -> unit.code
}
