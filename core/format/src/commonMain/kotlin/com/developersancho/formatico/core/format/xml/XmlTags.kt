package com.developersancho.formatico.core.format.xml

import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.text.TextRange

/**
 * A start tag as it is read.
 *
 * @property range from its `<` to its `>`, or to where it stops where it has none.
 * @property selfClosing whether it ends in `/>`, and is an element of its own.
 */
internal class XmlStartTag(
    val range: TextRange,
    val name: String,
    val nameRange: TextRange,
    val attributes: List<XmlAttribute>,
    val selfClosing: Boolean
)

/** An end tag as it is read, from its `</` to its `>`. */
internal class XmlEndTag(val range: TextRange, val name: String, val nameRange: TextRange)

/**
 * How a tag ends: where, whether it closes itself, and whether it is [terminated] by a `>` or stops before a `<` or the
 * end of the text.
 */
private class TagEnd(val end: Int, val selfClosing: Boolean, val terminated: Boolean)

/**
 * Reads the tags of a document: a start tag with its attributes, in either quote, and an end tag. It goes on past a
 * mistake: an attribute in no quotes, or with no value, is read as far as it goes, and what cannot stand in a tag is
 * reported and read past.
 */
internal class XmlTagReader(
    private val text: String,
    private val characterData: XmlCharacterData,
    private val report: XmlReport
) {
    /** The start tag whose `<` stands at [start], a name after it. */
    fun startTag(start: Int): XmlStartTag {
        val nameRange = TextRange(start + 1, text.nameEnd(start + 1))
        val attributes = ArrayList<XmlAttribute>()
        val end = rest(nameRange.end, attributes)
        if (!end.terminated) report(ProblemCode.XmlUnterminatedTag, start, end.end)
        return XmlStartTag(
            TextRange(start, end.end),
            text.substring(nameRange.start, nameRange.end),
            nameRange,
            attributes,
            end.selfClosing
        )
    }

    /** The end tag whose `</` stands at [start], a name after it. */
    fun endTag(start: Int): XmlEndTag {
        val nameRange = TextRange(start + 2, text.nameEnd(start + 2))
        val after = text.whitespaceEnd(nameRange.end)
        var stop = after
        while (stop < text.length && text[stop] != '>' && text[stop] != '<') stop++
        if (stop > after) report(ProblemCode.UnexpectedCharacter, after, stop)
        val terminated = stop < text.length && text[stop] == '>'
        val end = when {
            terminated -> stop + 1
            stop > after -> stop
            else -> nameRange.end
        }
        if (!terminated) report(ProblemCode.XmlUnterminatedTag, start, end)
        return XmlEndTag(TextRange(start, end), text.substring(nameRange.start, nameRange.end), nameRange)
    }

    /** Reads the attributes after [last], into [attributes], until the tag ends, and says how it ends. */
    private tailrec fun rest(last: Int, attributes: MutableList<XmlAttribute>): TagEnd {
        val at = text.whitespaceEnd(last)
        val end = tagEnd(at, last)
        if (end != null) return end
        val next = if (isNameStart(text[at])) attribute(at, spaced = at > last, attributes) else junk(at)
        return rest(next, attributes)
    }

    /** How the tag ends at [at], where what was read of it ends at [last]; null where it does not end there. */
    private fun tagEnd(at: Int, last: Int): TagEnd? = when {
        at >= text.length || text[at] == '<' -> TagEnd(last, selfClosing = false, terminated = false)
        text[at] == '>' -> TagEnd(at + 1, selfClosing = false, terminated = true)
        text.startsWith("/>", at) -> TagEnd(at + 2, selfClosing = true, terminated = true)
        else -> null
    }

    /**
     * Reads the attribute whose name starts at [start] into [attributes], and says where it ends. One that is not
     * [spaced] from what is before it, or that is given twice in the tag, is reported.
     */
    private fun attribute(start: Int, spaced: Boolean, attributes: MutableList<XmlAttribute>): Int {
        val nameRange = TextRange(start, text.nameEnd(start))
        val name = text.substring(nameRange.start, nameRange.end)
        if (!spaced) report(ProblemCode.UnexpectedToken, nameRange.start, nameRange.end)
        if (attributes.any { it.name == name }) report(ProblemCode.XmlDuplicateAttribute, nameRange.start, nameRange.end)
        val equals = text.whitespaceEnd(nameRange.end)
        val attribute = if (text.getOrNull(equals) == '=') {
            value(nameRange, name, equals)
        } else {
            report(ProblemCode.XmlExpectedAttributeValue, nameRange.start, nameRange.end)
            XmlAttribute(nameRange, name, nameRange, raw = "", value = "")
        }
        attributes += attribute
        return attribute.range.end
    }

    /** The attribute named [name] at [nameRange], with the value after its `=` at [equals]. */
    private fun value(nameRange: TextRange, name: String, equals: Int): XmlAttribute {
        val at = text.whitespaceEnd(equals + 1)
        val quote = text.getOrNull(at)
        if (quote == '"' || quote == '\'') return quoted(nameRange, name, at, quote)
        var end = at
        while (end < text.length && isUnquotedValue(end)) end++
        if (end == at) {
            report(ProblemCode.XmlExpectedAttributeValue, nameRange.start, equals + 1)
            return XmlAttribute(TextRange(nameRange.start, equals + 1), name, nameRange, raw = "", value = "")
        }
        report(ProblemCode.XmlUnquotedAttribute, at, end)
        return XmlAttribute(
            TextRange(nameRange.start, end),
            name,
            nameRange,
            text.substring(at, end),
            characterData.read(at, end, attribute = true)
        )
    }

    /**
     * The attribute named [name] whose value opens with [quote] at [at]. A value whose quote is never closed stops at a
     * `<`, which cannot stand in one, or at the end of the text.
     */
    private fun quoted(nameRange: TextRange, name: String, at: Int, quote: Char): XmlAttribute {
        var close = at + 1
        while (close < text.length && text[close] != quote && text[close] != '<') close++
        val closed = close < text.length && text[close] == quote
        if (!closed) report(ProblemCode.UnterminatedString, at, close)
        val end = if (closed) close + 1 else close
        return XmlAttribute(
            TextRange(nameRange.start, end),
            name,
            nameRange,
            text.substring(at, end),
            characterData.read(at + 1, close, attribute = true)
        )
    }

    /** Whether the character at [at] goes on a value in no quotes: whitespace, `<`, `>` and `/>` end one. */
    private fun isUnquotedValue(at: Int): Boolean = when (text[at]) {
        ' ', '\t', '\n', '\r', '<', '>' -> false
        '/' -> text.getOrNull(at + 1) != '>'
        else -> true
    }

    /**
     * Reads past what cannot stand in a tag from [start], and reports it: a quoted string as one, or the characters up
     * to a name, whitespace or the tag's end. Says where it ends.
     */
    private fun junk(start: Int): Int {
        val char = text[start]
        var end = start + 1
        if (char == '"' || char == '\'') {
            while (end < text.length && text[end] != char && text[end] != '<') end++
            if (end < text.length && text[end] == char) end++
        } else {
            while (end < text.length && isJunk(text[end])) end++
        }
        report(ProblemCode.UnexpectedCharacter, start, end)
        return end
    }

    /** Whether [char] goes on what cannot stand in a tag. */
    private fun isJunk(char: Char): Boolean = when (char) {
        ' ', '\t', '\n', '\r', '<', '>', '/', '"', '\'' -> false
        else -> !isNameStart(char)
    }
}
