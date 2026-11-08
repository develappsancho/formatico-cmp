package com.developersancho.formatico.core.format.query

/** What `.` matches in an I-Regexp: any character but a line feed and a carriage return. */
private const val ANY_CHARACTER = "[^\\u000A\\u000D]"

/** The characters a backslash escapes in an I-Regexp, each of which then stands for itself. */
private const val ESCAPABLE = "()*+-.?[\\]^{|}"

private const val HEX_RADIX = 16

/** How long what opens a category is: `\p{`. */
private const val CATEGORY_OPEN_LENGTH = 3
private const val UNICODE_ESCAPE_DIGITS = 4

/** The categories `\p{…}` and `\P{…}` name in an I-Regexp: Unicode's general categories, one letter or two. */
private val CATEGORIES = setOf(
    "L",
    "Ll",
    "Lm",
    "Lo",
    "Lt",
    "Lu",
    "M",
    "Mc",
    "Me",
    "Mn",
    "N",
    "Nd",
    "Nl",
    "No",
    "P",
    "Pc",
    "Pd",
    "Pe",
    "Pf",
    "Pi",
    "Po",
    "Ps",
    "Z",
    "Zl",
    "Zp",
    "Zs",
    "S",
    "Sc",
    "Sk",
    "Sm",
    "So",
    "C",
    "Cc",
    "Cf",
    "Cn",
    "Co"
)

/**
 * The [Regex] that matches what [pattern], an I-Regexp (RFC 9485), matches, or null where [pattern] is no I-Regexp,
 * or one this platform's regular expressions cannot compile. It is written in the syntax every platform's [Regex]
 * reads alike: `.` as a class that leaves out the line breaks, a group as one that does not capture, and every
 * character that is neither a letter nor a digit as its `\u` escape, so that `^`, `$`, `&` and the rest stand for
 * themselves, as they do in an I-Regexp.
 */
internal fun iRegexp(pattern: String): Regex? {
    val translated = IRegexpReader(pattern).translate() ?: return null
    return runCatching { Regex(translated) }.getOrNull()
}

/** Reads an I-Regexp, [pattern], and writes it out in Kotlin's syntax, into [out]. */
private class IRegexpReader(val pattern: String) {
    /** The place of the next character to read. */
    var at = 0

    /** What has been written. */
    val out = StringBuilder()

    private var depth = 0
    private var quantifiable = false

    /** The pattern in Kotlin's syntax, or null where it is no I-Regexp. */
    fun translate(): String? {
        var valid = true
        while (valid && at < pattern.length) valid = piece()
        return if (valid && depth == 0) out.toString() else null
    }

    /** Writes out the atom, the quantifier, the parenthesis or the `|` next; false where it is none an I-Regexp has. */
    private fun piece(): Boolean {
        val char = pattern[at]
        val afterAtom = quantifiable
        quantifiable = char !in "(|*+?{"
        return when (char) {
            '(', ')', '|' -> group(char)
            '*', '+', '?' -> afterAtom && write(char.toString())
            '{' -> afterAtom && range()
            '.' -> write(ANY_CHARACTER)
            '\\' -> escape()
            '[' -> characterClass()
            ']', '}' -> false
            else -> literalCharacter()?.let { writeLiteral(it) } ?: false
        }
    }

    /** Writes out a group's parenthesis, as one that does not capture, or `|`. */
    private fun group(char: Char): Boolean {
        if (char == '(') depth++
        if (char == ')') depth--
        return depth >= 0 && write(if (char == '(') "(?:" else char.toString())
    }

    /** Writes out the quantifier `{n}`, `{n,}` or `{n,m}` next, where `n` is no more than `m`. */
    private fun range(): Boolean {
        val close = pattern.indexOf('}', at)
        val bounds = if (close < 0) emptyList() else pattern.substring(at + 1, close).split(',')
        val count = { written: String -> written.takeIf { it.isNotEmpty() && it.all { char -> char in '0'..'9' } }?.toIntOrNull() }
        val least = bounds.firstOrNull()?.let(count)
        val most = bounds.getOrNull(1)?.let { if (it.isEmpty()) Int.MAX_VALUE else count(it) }
        val valid = least != null && bounds.size <= 2 && (bounds.size == 1 || (most != null && least <= most))
        if (valid) {
            out.append(pattern, at, close + 1)
            at = close + 1
        }
        return valid
    }

    /** Writes out the escape next: a character's, or a category's. */
    private fun escape(): Boolean {
        if (isCategoryAt(at)) return category()
        return escapedCharacter()?.let { writeLiteral(it) } ?: false
    }

    /** Writes [text] out as it is, for the one character it stands for. */
    private fun write(text: String): Boolean {
        out.append(text)
        at++
        return true
    }
}

/** Writes out the class, `[…]` or `[^…]`, whose `[` is next. */
private fun IRegexpReader.characterClass(): Boolean {
    at++
    out.append('[')
    if (pattern.getOrNull(at) == '^') {
        out.append('^')
        at++
    }
    var valid = true
    var first = true
    while (valid) {
        val char = pattern.getOrNull(at) ?: return false
        if (char == ']' && !first) break
        // A `-` stands for itself first in a class and last; anywhere else it makes a range.
        valid = if (char == '-' && (first || pattern.getOrNull(at + 1) == ']')) {
            at++
            writeLiteral("-")
        } else {
            classItem()
        }
        first = false
    }
    at++
    out.append(']')
    return valid
}

/** Writes out the item of a class next: a character, a range of them, `a-z`, or a category. */
private fun IRegexpReader.classItem(): Boolean {
    if (isCategoryAt(at)) return category()
    val low = classCharacter() ?: return false
    val range = pattern.getOrNull(at) == '-' && pattern.getOrNull(at + 1).let { it != null && it != ']' }
    if (range) at++
    val high = if (!range) {
        low
    } else if (isCategoryAt(at)) {
        null
    } else {
        classCharacter()
    }
    if (high == null || compareCodePoints(low, high) > 0) return false
    writeLiteral(low)
    if (range) {
        out.append('-')
        writeLiteral(high)
    }
    return true
}

/** Whether a category, `\p{…}` or `\P{…}`, starts at [index]. */
private fun IRegexpReader.isCategoryAt(index: Int): Boolean =
    pattern.getOrNull(index) == '\\' && pattern.getOrNull(index + 1).let { it == 'p' || it == 'P' }

/** The character of a class next: any but `[`, `]`, `-` and `\`, or one escaped. */
private fun IRegexpReader.classCharacter(): String? = when (pattern.getOrNull(at)) {
    null, '[', ']', '-' -> null
    '\\' -> escapedCharacter()
    else -> literalCharacter()
}

/** Writes out the category, `\p{Lu}`, or its complement, `\P{Lu}`, next. */
private fun IRegexpReader.category(): Boolean {
    val close = pattern.indexOf('}', at)
    val name = if (close < 0 || pattern.getOrNull(at + 2) != '{') null else pattern.substring(at + CATEGORY_OPEN_LENGTH, close)
    if (name !in CATEGORIES) return false
    out.append(pattern, at, close + 1)
    at = close + 1
    return true
}

/** The character the escape next stands for: `\n`, `\r`, `\t`, or a character that has a meaning, escaped. */
private fun IRegexpReader.escapedCharacter(): String? {
    val escaped = when (val code = pattern.getOrNull(at + 1)) {
        'n' -> "\n"
        'r' -> "\r"
        't' -> "\t"
        else -> code?.takeIf { it in ESCAPABLE }?.toString()
    }
    if (escaped != null) at += 2
    return escaped
}

/** The character next as it is written, a surrogate pair as one; null where it is half of a pair. */
private fun IRegexpReader.literalCharacter(): String? {
    val length = characterLength(pattern, at)
    val character = pattern.substring(at, at + length)
    if (length == 1 && character[0].isSurrogate()) return null
    at += length
    return character
}

/** Writes out [character] so that it stands for itself: a letter or a digit as it is, any other as its escape. */
private fun IRegexpReader.writeLiteral(character: String): Boolean {
    val char = character[0]
    when {
        character.length > 1 -> out.append(character)
        char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9' -> out.append(char)
        else -> out.append("\\u").append(char.code.toString(HEX_RADIX).padStart(UNICODE_ESCAPE_DIGITS, '0'))
    }
    return true
}
