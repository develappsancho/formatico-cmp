package com.developersancho.formatico.core.format.xml

import com.developersancho.formatico.core.format.ProblemCode

/** Where a reader says what is wrong: the problem, and the offsets it runs between. */
internal typealias XmlReport = (code: ProblemCode, start: Int, end: Int) -> Unit

/** What opens a comment. */
internal const val COMMENT_OPEN = "<!--"

/** What closes a comment. */
internal const val COMMENT_CLOSE = "-->"

/** What opens a CDATA section. */
internal const val CDATA_OPEN = "<![CDATA["

/** What closes a CDATA section. */
internal const val CDATA_CLOSE = "]]>"

/** What opens a processing instruction. */
internal const val INSTRUCTION_OPEN = "<?"

/** What closes a processing instruction. */
internal const val INSTRUCTION_CLOSE = "?>"

/** What opens a document type declaration. */
internal const val DOCTYPE_OPEN = "<!DOCTYPE"

/** The target of the XML declaration, `<?xml …?>`. */
internal const val DECLARATION_TARGET = "xml"

/** Whether [char] is whitespace as XML has it: a space, a tab or a line break. */
internal fun isXmlWhitespace(char: Char): Boolean = when (char) {
    ' ', '\t', '\n', '\r' -> true
    else -> false
}

/** Whether the text is nothing but XML's whitespace; a non-breaking space is not. */
internal fun CharSequence.isXmlBlank(): Boolean = all(::isXmlWhitespace)

/**
 * Whether a name can start with [char], as XML 1.0 lists the characters: a letter, `_` or `:`. A surrogate is taken for
 * half of a letter beyond U+FFFF, which names allow.
 */
internal fun isNameStart(char: Char): Boolean = when (char) {
    in 'a'..'z', in 'A'..'Z', '_', ':' -> true

    in '\u00C0'..'\u00D6', in '\u00D8'..'\u00F6', in '\u00F8'..'\u02FF', in '\u0370'..'\u037D', in '\u037F'..'\u1FFF',
    in '\u200C'..'\u200D', in '\u2070'..'\u218F', in '\u2C00'..'\u2FEF', in '\u3001'..'\uD7FF', in '\uF900'..'\uFDCF',
    in '\uFDF0'..'\uFFFD' -> true

    in '\uD800'..'\uDFFF' -> true

    else -> false
}

/** Whether [char] can stand in a name after its first character: a digit, `-`, `.` and a combining mark as well. */
internal fun isNameChar(char: Char): Boolean = when (char) {
    in '0'..'9', '-', '.', '\u00B7', in '\u0300'..'\u036F', in '\u203F'..'\u2040' -> true
    else -> isNameStart(char)
}

/** Whether XML has [char] as a character: no control character but a tab and the line breaks, nor U+FFFE or U+FFFF. */
internal fun isXmlChar(char: Char): Boolean = when (char) {
    '\t', '\n', '\r', in ' '..'\uFFFD' -> true
    else -> false
}

/** Where the name that starts at [from] ends: the first character after it that cannot stand in a name. */
internal fun String.nameEnd(from: Int): Int {
    var index = from
    while (index < length && isNameChar(this[index])) index++
    return index
}

/** Where the whitespace that starts at [from] ends, which is [from] itself where there is none. */
internal fun String.whitespaceEnd(from: Int): Int {
    var index = from
    while (index < length && isXmlWhitespace(this[index])) index++
    return index
}
