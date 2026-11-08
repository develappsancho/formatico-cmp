package com.developersancho.formatico.core.format.text

/**
 * A place in a text: its offset, and the line and the column it is read at.
 *
 * @property offset the number of UTF-16 units before it.
 * @property line its line, counted from 1.
 * @property column its column on that line, counted from 1 in UTF-16 units.
 */
data class TextPosition(val offset: Int, val line: Int, val column: Int)

/**
 * A stretch of a text, from [start] up to, and not including, [end], both offsets in UTF-16 units.
 */
data class TextRange(val start: Int, val end: Int) {
    init {
        require(start in 0..end) { "A range runs forwards: $start..$end" }
    }

    /** How many UTF-16 units it covers. */
    val length: Int get() = end - start

    /** Whether [offset] falls inside it. */
    operator fun contains(offset: Int): Boolean = offset in start until end
}

/**
 * Where each line of a text starts, to turn an offset into a line and a column. A line ends at `\n`, `\r\n` or `\r`.
 */
class LineMap(text: CharSequence) {
    private val lineStarts: IntArray
    private val length: Int = text.length

    init {
        val starts = ArrayList<Int>()
        starts += 0
        var index = 0
        while (index < text.length) {
            val char = text[index]
            if (char == '\n') {
                starts += index + 1
            } else if (char == '\r') {
                if (index + 1 < text.length && text[index + 1] == '\n') index++
                starts += index + 1
            }
            index++
        }
        lineStarts = starts.toIntArray()
    }

    /** How many lines the text has; an empty text has one. */
    val lineCount: Int get() = lineStarts.size

    /** The position of [offset], which may be the text's length, its end. */
    fun position(offset: Int): TextPosition {
        require(offset in 0..length) { "Offset $offset is outside the text of $length" }
        var low = 0
        var high = lineStarts.size - 1
        while (low < high) {
            val middle = (low + high + 1) ushr 1
            if (lineStarts[middle] <= offset) low = middle else high = middle - 1
        }
        return TextPosition(offset = offset, line = low + 1, column = offset - lineStarts[low] + 1)
    }

    /** The offset where [line], counted from 1, starts. */
    fun lineStart(line: Int): Int = lineStarts[line - 1]
}
