package com.developersancho.formatico.core.format.ini

import com.developersancho.formatico.core.format.Finding
import com.developersancho.formatico.core.format.Problem
import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.Severity
import com.developersancho.formatico.core.format.linesOf
import com.developersancho.formatico.core.format.problemsOf
import com.developersancho.formatico.core.format.text.TextRange

/**
 * A key of an INI section and its value.
 *
 * @property range the entry's line, from its key's first character to its value's last.
 * @property keyRange the key as it is written.
 * @property valueRange the value as it is written, its quotes included: empty for a key with no value.
 * @property value the value, the quotes round all of it taken off.
 * @property separated whether a `=` or a `:` stands after the key: a key may be written alone, with no value.
 */
class IniEntry(
    val range: TextRange,
    val keyRange: TextRange,
    val key: String,
    val valueRange: TextRange,
    val value: String,
    val separated: Boolean
)

/**
 * A section of an INI text: its header, `[name]`, and its keys; or the keys before the first header, which have no
 * header and no name.
 *
 * @property range from the header, or the first key, to the last key: the header alone for a section with no keys.
 */
class IniSection(val range: TextRange, val headerRange: TextRange?, val name: String?, val entries: List<IniEntry>)

/**
 * An INI text as it was read: its sections in order, its comments, and its problems, each with its line and column.
 *
 * A line is a header, `[name]`; a comment, which starts with `;` or `#`; or a key, then `=` or `:` and its value, or a
 * key alone. A value is the rest of its line: a `;` after it is part of it, as most readers take it. A header with no
 * `]` is an error; a key given twice in a section, a section given twice, and text after a header's `]` are warnings.
 * Nothing of the text is lost: [print] gives it back as it was written.
 *
 * @property text the text read.
 * @property sections its sections in order, the keys before the first header first where there are any.
 * @property comments each comment, from its `;` or `#` to the end of its line.
 * @property problems what is wrong in it, in the order of the text.
 */
class IniDocument private constructor(
    val text: String,
    val sections: List<IniSection>,
    val comments: List<TextRange>,
    val problems: List<Problem>
) {
    /** Whether the document has no error; a warning leaves it valid. */
    val isValid: Boolean get() = problems.none { it.severity == Severity.Error }

    /** The document as it was written. */
    fun print(): String = text

    companion object {
        /** Reads [text] as sections of keys and values. */
        fun parse(text: String): IniDocument {
            val reader = IniReader(text)
            reader.read()
            return IniDocument(text, reader.sections, reader.comments, problemsOf(text, reader.findings))
        }
    }
}

/** Reads an INI text a line at a time. */
private class IniReader(private val text: String) {
    val sections = ArrayList<IniSection>()
    val comments = ArrayList<TextRange>()
    val findings = ArrayList<Finding>()

    private var header: TextRange? = null
    private var name: String? = null
    private var entries = ArrayList<IniEntry>()
    private val names = HashSet<String>()
    private var keys = HashSet<String>()

    fun read() {
        linesOf(text).forEach { line ->
            val start = (line.start until line.end).firstOrNull { !text[it].isWhitespace() } ?: return@forEach
            val end = (line.end - 1 downTo start).first { !text[it].isWhitespace() } + 1
            when (text[start]) {
                ';', '#' -> comments += TextRange(start, line.end)
                '[' -> header(start, end)
                else -> entry(start, end)
            }
        }
        close()
    }

    /** Starts the section whose header is written from [start] to [end]. */
    private fun header(start: Int, end: Int) {
        close()
        val closing = text.indexOf(']', start).takeIf { it in start until end }
        if (closing == null) {
            findings += Finding(ProblemCode.IniUnclosedSection, Severity.Error, start, end)
        } else if (closing + 1 < end) {
            val after = (closing + 1 until end).first { !text[it].isWhitespace() }
            if (text[after] != ';' && text[after] != '#') {
                findings += Finding(ProblemCode.TrailingContent, Severity.Warning, after, end)
            }
        }
        val range = TextRange(start, closing?.plus(1) ?: end)
        val section = text.substring(start + 1, closing ?: end).trim()
        if (!names.add(section)) findings += Finding(ProblemCode.IniDuplicateSection, Severity.Warning, range.start, range.end)
        header = range
        name = section
    }

    /** Reads the entry written from [start] to [end]. */
    private fun entry(start: Int, end: Int) {
        val mark = (start until end).firstOrNull { text[it] == '=' || text[it] == ':' }
        val keyEnd = trimmedEnd(start, mark ?: end)
        val keyRange = TextRange(start, keyEnd)
        val valueStart = mark?.let { trimmedStart(it + 1, end) } ?: end
        val valueRange = TextRange(valueStart, end)
        val key = text.substring(keyRange.start, keyRange.end)
        val raw = text.substring(valueRange.start, valueRange.end)
        if (keyRange.length == 0) {
            findings += Finding(ProblemCode.ExpectedPropertyName, Severity.Error, start, start + 1)
        } else if (!keys.add(key)) {
            findings += Finding(ProblemCode.DuplicateKey, Severity.Warning, keyRange.start, keyRange.end)
        }
        entries += IniEntry(TextRange(start, end), keyRange, key, valueRange, unquoted(raw), separated = mark != null)
    }

    /** Ends the section being read, where it has a header or a key. */
    private fun close() {
        val header = header
        if (header != null || entries.isNotEmpty()) {
            val start = header?.start ?: entries.first().range.start
            val end = entries.lastOrNull()?.range?.end ?: requireNotNull(header).end
            sections += IniSection(TextRange(start, end), header, name, entries)
        }
        this.header = null
        name = null
        entries = ArrayList()
        keys = HashSet()
    }

    private fun trimmedEnd(from: Int, to: Int): Int {
        var end = to
        while (end > from && text[end - 1].isWhitespace()) end--
        return end
    }

    private fun trimmedStart(from: Int, to: Int): Int {
        var start = from
        while (start < to && text[start].isWhitespace()) start++
        return start
    }
}

/** [raw] with the quotes round all of it, double or single, taken off. */
private fun unquoted(raw: String): String {
    val quoted = raw.length >= 2 && (raw.first() == '"' || raw.first() == '\'') && raw.last() == raw.first()
    return if (quoted) raw.substring(1, raw.length - 1) else raw
}
