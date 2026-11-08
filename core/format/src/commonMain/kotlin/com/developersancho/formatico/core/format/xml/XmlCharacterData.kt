package com.developersancho.formatico.core.format.xml

import com.developersancho.formatico.core.format.ProblemCode

/** The five entities XML has without a DOCTYPE, and what each stands for. */
private val PredefinedEntities = mapOf("amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'")

/** What opens a character reference in decimal digits: `&#10;`. */
private const val DECIMAL_REFERENCE_OPEN = "&#"

/** What opens a character reference in hexadecimal digits: `&#x1F600;`. */
private const val HEX_REFERENCE_OPEN = "&#x"

private const val DECIMAL = 10
private const val HEXADECIMAL = 16

/** The last character Unicode has. */
private const val LAST_CODE_POINT = 0x10FFFF

/** The first character beyond U+FFFF, which UTF-16 writes as two surrogates. */
private const val FIRST_SUPPLEMENTARY = 0x10000
private const val SURROGATE_BITS = 10
private const val LOW_SURROGATE_MASK = 0x3FF

/**
 * Reads the character data of a document, its text and its attributes' values: the references in them, and the line
 * breaks, which XML reads as `\n` in text and as a space in an attribute's value, as it reads a tab there. What cannot
 * stand in them is reported: an `&` that starts no reference, a character XML does not have, a `<` in text, a `]]>`
 * in text.
 *
 * @param hasDoctype whether the document has a DOCTYPE before where it is read, which can declare entities beyond the
 *   five XML has. An entity it may declare is kept as it is written.
 */
internal class XmlCharacterData(
    private val text: String,
    private val hasDoctype: () -> Boolean,
    private val report: XmlReport
) {
    /** The value of the text from [start] to [end]: text, or an attribute's value where [attribute] says so. */
    fun read(start: Int, end: Int, attribute: Boolean): String {
        val value = Value(text, start)
        var index = start
        while (index < end) {
            index = when (text[index]) {
                '&' -> reference(index, end, value)

                '\r' -> lineBreak(index, end, value, attribute)

                '\n', '\t' -> {
                    if (attribute) value.replace(index, index + 1, " ")
                    index + 1
                }

                else -> {
                    check(index, start, attribute)
                    index + 1
                }
            }
        }
        return value.end(end)
    }

    /** A CDATA section's content, from [start] to [end], its line breaks read as `\n`. */
    fun lineBreaksRead(start: Int, end: Int): String {
        val value = Value(text, start)
        var index = start
        while (index < end) {
            index = if (text[index] == '\r') lineBreak(index, end, value, attribute = false) else index + 1
        }
        return value.end(end)
    }

    /** Reads the `\r` at [index], alone or before `\n`, as one line break, and says where it ends. */
    private fun lineBreak(index: Int, end: Int, value: Value, attribute: Boolean): Int {
        val next = if (index + 1 < end && text[index + 1] == '\n') index + 2 else index + 1
        value.replace(index, next, if (attribute) " " else "\n")
        return next
    }

    /** Reports the character at [index], in what starts at [start], where it cannot stand there. */
    private fun check(index: Int, start: Int, attribute: Boolean) {
        val char = text[index]
        val wrong = when (char) {
            '<' -> true

            // `]]>` closes a CDATA section, and may not stand in text that is not one.
            '>' -> !attribute && index - start >= 2 && text[index - 1] == ']' && text[index - 2] == ']'

            else -> !isXmlChar(char)
        }
        if (wrong) report(ProblemCode.UnexpectedCharacter, index, index + 1)
    }

    /** Reads the reference whose `&` stands at [at], before [end], and says where it ends. */
    private fun reference(at: Int, end: Int, value: Value): Int {
        var close = at + 1
        while (close < end && (isNameChar(text[close]) || text[close] == '#')) close++
        if (close >= end || text[close] != ';' || close == at + 1) {
            report(ProblemCode.XmlInvalidReference, at, at + 1)
            return at + 1
        }
        val read = if (text[at + 1] == '#') character(at, close) else entity(at, close)
        if (read != null) value.replace(at, close + 1, read)
        return close + 1
    }

    /** The character the reference from [at] to its `;` at [close] stands for, `&#10;` or `&#x1F600;`, or null. */
    private fun character(at: Int, close: Int): String? {
        val hex = text.startsWith(HEX_REFERENCE_OPEN, at)
        val from = at + if (hex) HEX_REFERENCE_OPEN.length else DECIMAL_REFERENCE_OPEN.length
        var code = 0
        var valid = from < close
        for (index in from until close) {
            val digit = digitOf(text[index], hex)
            if (digit < 0) {
                valid = false
            } else if (code <= LAST_CODE_POINT) {
                // Past the last character the code stays past it, and cannot overflow.
                code = code * (if (hex) HEXADECIMAL else DECIMAL) + digit
            }
        }
        if (!valid || !isXmlCodePoint(code)) {
            report(ProblemCode.XmlInvalidReference, at, close + 1)
            return null
        }
        return codePointText(code)
    }

    /**
     * What the entity from [at] to its `;` at [close] stands for, or null where it is not one of the five XML has. One
     * a DOCTYPE may declare is no problem; without a DOCTYPE, or where it is no name, it is reported.
     */
    private fun entity(at: Int, close: Int): String? {
        val name = text.substring(at + 1, close)
        val predefined = PredefinedEntities[name]
        val isName = isNameStart(name[0]) && name.none { it == '#' }
        if (predefined == null && (!isName || !hasDoctype())) report(ProblemCode.XmlInvalidReference, at, close + 1)
        return predefined
    }
}

/** The value of [char] as a digit, decimal or [hex], or -1 where it is none. */
private fun digitOf(char: Char, hex: Boolean): Int = when (char) {
    in '0'..'9' -> char - '0'
    in 'a'..'f' -> if (hex) char - 'a' + DECIMAL else -1
    in 'A'..'F' -> if (hex) char - 'A' + DECIMAL else -1
    else -> -1
}

/** Whether XML has the character [code]. */
private fun isXmlCodePoint(code: Int): Boolean = if (code < FIRST_SUPPLEMENTARY) {
    val char = code.toChar()
    isXmlChar(char) && !char.isSurrogate()
} else {
    code <= LAST_CODE_POINT
}

/** The character [code] in UTF-16: one unit, or two surrogates beyond U+FFFF. */
private fun codePointText(code: Int): String {
    if (code < FIRST_SUPPLEMENTARY) return code.toChar().toString()
    val offset = code - FIRST_SUPPLEMENTARY
    val high = Char.MIN_HIGH_SURROGATE + (offset shr SURROGATE_BITS)
    val low = Char.MIN_LOW_SURROGATE + (offset and LOW_SURROGATE_MASK)
    return charArrayOf(high, low).concatToString()
}

/**
 * A value read from the text from [start], copied only where it differs from what is written: a value with no
 * reference and no line break to read is the text itself.
 */
private class Value(private val text: String, private val start: Int) {
    private var builder: StringBuilder? = null
    private var copied = start

    /** Puts [replacement] in place of the text from [from] to [to]. */
    fun replace(from: Int, to: Int, replacement: String) {
        val out = builder ?: StringBuilder().also { builder = it }
        out.append(text, copied, from).append(replacement)
        copied = to
    }

    /** The value, the text up to [end] taken in. */
    fun end(end: Int): String = builder?.append(text, copied, end)?.toString() ?: text.substring(start, end)
}
