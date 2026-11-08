package com.developersancho.formatico.core.format.properties

import com.developersancho.formatico.core.format.Finding
import com.developersancho.formatico.core.format.Problem
import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.Severity
import com.developersancho.formatico.core.format.linesOf
import com.developersancho.formatico.core.format.problemsOf
import com.developersancho.formatico.core.format.text.TextRange

/**
 * A key of a properties text and its value.
 *
 * @property range the entry as it is written, from its key's first character to its value's last, its continued lines
 *   included.
 * @property keyRange the key as it is written, its escapes included.
 * @property key the key, its escapes read.
 * @property valueRange the value as it is written, its continued lines and escapes included: empty where it has none.
 * @property value the value, its escapes read and its continued lines joined.
 * @property joined the value as it is written, its continued lines joined: its escapes kept, the backslash that ends a
 *   line and the room that starts the next taken out.
 */
class PropertiesEntry(
    val range: TextRange,
    val keyRange: TextRange,
    val key: String,
    val valueRange: TextRange,
    val value: String,
    val joined: String
)

/**
 * A Java properties text as `java.util.Properties.load` reads it: a key and a value on each line, between them `=`,
 * `:` or room; a line that starts with `#` or `!` is a comment, and one that ends with a backslash goes on on the next.
 * `\t`, `\n`, `\r`, `\f` and `\uXXXX` are escapes, and a backslash before any other character stands for that
 * character.
 *
 * A key given twice is a warning, as the later value is the one read; a `\u` not followed by four hexadecimal digits is
 * an error. Nothing of the text is lost: [print] gives it back as it was written.
 *
 * @property text the text read.
 * @property entries its keys and values, in order.
 * @property comments each comment, from its `#` or `!` to the end of its line.
 * @property problems what is wrong in it, in the order of the text.
 */
class PropertiesDocument private constructor(
    val text: String,
    val entries: List<PropertiesEntry>,
    val comments: List<TextRange>,
    val problems: List<Problem>
) {
    /** Whether the document has no error; a warning leaves it valid. */
    val isValid: Boolean get() = problems.none { it.severity == Severity.Error }

    /** The document as it was written. */
    fun print(): String = text

    companion object {
        /** Reads [text] as a properties file. */
        fun parse(text: String): PropertiesDocument {
            val reader = PropertiesReader(text)
            reader.read()
            return PropertiesDocument(text, reader.entries, reader.comments, problemsOf(text, reader.findings))
        }
    }
}

/** Reads a properties text a logical line at a time: a line, and those a backslash at its end carries it on to. */
private class PropertiesReader(private val text: String) {
    val entries = ArrayList<PropertiesEntry>()
    val comments = ArrayList<TextRange>()
    val findings = ArrayList<Finding>()

    private val lines = linesOf(text)
    private val seen = HashSet<String>()

    fun read() {
        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            val start = skipRoom(line.start, line.end)
            when {
                start == line.end -> index++

                text[start] == '#' || text[start] == '!' -> {
                    comments += TextRange(start, line.end)
                    index++
                }

                else -> index = entry(index, start)
            }
        }
    }

    /** Reads the entry whose first line is the one at [first], from [start] on, and gives the index of the line after it. */
    private fun entry(first: Int, start: Int): Int {
        val logical = LogicalLine()
        var index = first
        var from = start
        while (true) {
            val line = lines[index]
            val continued = endsWithContinuation(line)
            logical.add(from, if (continued) line.end - 1 else line.end)
            index++
            if (!continued || index == lines.size) break
            from = skipRoom(lines[index].start, lines[index].end)
        }
        add(logical, end = lines[index - 1].end)
        return index
    }

    /** Takes the key and the value out of [logical], whose last line ends at [end]. */
    private fun add(logical: LogicalLine, end: Int) {
        val chars = logical.chars
        var keyEnd = 0
        while (keyEnd < chars.length && !isKeyEnd(chars, keyEnd)) keyEnd += if (chars[keyEnd] == '\\') 2 else 1
        keyEnd = keyEnd.coerceAtMost(chars.length)
        var valueStart = keyEnd
        while (valueStart < chars.length && chars[valueStart] in ROOM) valueStart++
        if (valueStart < chars.length && (chars[valueStart] == '=' || chars[valueStart] == ':')) valueStart++
        while (valueStart < chars.length && chars[valueStart] in ROOM) valueStart++

        val keyRange = TextRange(logical.offset(0), logical.offset(keyEnd - 1) + 1)
        val valueRange = if (valueStart < chars.length) TextRange(logical.offset(valueStart), end) else TextRange(end, end)
        val key = unescaped(chars, 0, keyEnd, logical)
        val joined = chars.substring(valueStart)
        val entry = PropertiesEntry(
            range = TextRange(keyRange.start, end),
            keyRange = keyRange,
            key = key,
            valueRange = valueRange,
            value = unescaped(chars, valueStart, chars.length, logical),
            joined = joined
        )
        if (!seen.add(key)) findings += Finding(ProblemCode.DuplicateKey, Severity.Warning, keyRange.start, keyRange.end)
        entries += entry
    }

    /** Whether the key of [chars] ends at [at]: at `=`, `:` or room that no backslash escapes. */
    private fun isKeyEnd(chars: String, at: Int): Boolean = chars[at] == '=' || chars[at] == ':' || chars[at] in ROOM

    /** [chars] from [from] to [to] with its escapes read; a `\u` with no four hexadecimal digits is an error. */
    private fun unescaped(chars: String, from: Int, to: Int, logical: LogicalLine): String = buildString {
        var at = from
        while (at < to) {
            val char = chars[at]
            if (char != '\\' || at + 1 >= to) {
                if (char != '\\') append(char)
                at++
                continue
            }
            val next = chars[at + 1]
            if (next == 'u') {
                val digits = chars.substring(at + 2, minOf(at + 2 + UNICODE_DIGITS, to))
                val hex = digits.takeWhile { it.isHexDigit() }.length
                if (hex == UNICODE_DIGITS) {
                    append(digits.toInt(HEX).toChar())
                } else {
                    findings += Finding(ProblemCode.InvalidEscape, Severity.Error, logical.offset(at), logical.offset(at + 1 + hex) + 1)
                }
                at += 2 + hex
            } else {
                append(Escapes[next] ?: next)
                at += 2
            }
        }
    }

    /** Whether [line] ends with a backslash no other escapes: an odd number of them. */
    private fun endsWithContinuation(line: TextRange): Boolean {
        var count = 0
        var at = line.end - 1
        while (at >= line.start && text[at] == '\\') {
            count++
            at--
        }
        return count % 2 == 1
    }

    private fun skipRoom(from: Int, to: Int): Int {
        var at = from
        while (at < to && text[at] in ROOM) at++
        return at
    }

    /** The characters of an entry's lines, as one line, each with the offset in the text it is written at. */
    private inner class LogicalLine {
        private val builder = StringBuilder()
        private val offsets = ArrayList<Int>()

        val chars: String get() = builder.toString()

        fun add(from: Int, to: Int) {
            for (at in from until to) {
                builder.append(text[at])
                offsets += at
            }
        }

        /** The offset in the text of the character at [index], or where the last one ends for an index past it. */
        fun offset(index: Int): Int = offsets.getOrNull(index) ?: (offsets.lastOrNull()?.plus(1) ?: 0)
    }

    private companion object {
        /** What a properties file takes for room: a space, a tab, a form feed. */
        val ROOM = charArrayOf(' ', '\t', '\u000C')

        val Escapes = mapOf('t' to '\t', 'n' to '\n', 'r' to '\r', 'f' to '\u000C')

        const val HEX = 16

        /** How many hexadecimal digits follow `\u`. */
        const val UNICODE_DIGITS = 4
    }
}

private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
