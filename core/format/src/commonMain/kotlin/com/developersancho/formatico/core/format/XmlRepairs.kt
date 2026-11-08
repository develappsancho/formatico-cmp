package com.developersancho.formatico.core.format

import com.developersancho.formatico.core.format.text.TextRange

/**
 * XML's repairs: the end tag of an element never closed, quotes around an attribute's value, and `&amp;` for an `&` that
 * starts no reference.
 */
internal object XmlRepairs {
    /**
     * The end tag of the element whose start tag stands in [tag], where what was read of it ends: before the end tag of
     * the element around it, or at the text's end, past the room before either, as the reader closes it.
     */
    fun closeElement(text: String, tag: TextRange): Repair? {
        val name = nameAt(text, tag.start + 1) ?: return null
        val at = backOverRoom(text, contentEnd(text, tag.end), tag.end)
        return Repair(RepairKind.Close, listOf(TextEdit(TextRange(at, at), "</$name>")))
    }

    /** The attribute's value that stands in [value] with no quotes, in double ones: a double quote in it written `&quot;`. */
    fun quoteAttribute(text: String, value: TextRange): Repair {
        val raw = text.substring(value.start, value.end).replace("\"", "&quot;")
        return Repair(RepairKind.QuoteValue, listOf(TextEdit(value, "\"$raw\"")))
    }

    /**
     * An `&` that starts no reference, written `&amp;`; a reference to a character XML does not have, or to an entity it
     * does not know, is not repaired: what it was meant to be is not known.
     */
    fun escapeAmpersand(text: String, at: TextRange): Repair? =
        if (at.end - at.start == 1 && text[at.start] == '&') Repair(RepairKind.Escape, listOf(TextEdit(at, "&amp;"))) else null

    /** The element's name that starts at [start], after its `<`. */
    private fun nameAt(text: String, start: Int): String? {
        var end = start
        while (end < text.length && !endsName(text[end])) end++
        return text.substring(start, end).ifEmpty { null }
    }

    private fun endsName(char: Char): Boolean = char.isWhitespace() || char == '>' || char == '/'

    /**
     * Where the content that starts at [from] ends: at the first end tag no start tag after [from] opened, or at the
     * text's end. Comments, CDATA sections, processing instructions and declarations are passed over.
     */
    private fun contentEnd(text: String, from: Int): Int {
        var depth = 0
        var at = from
        var end = -1
        while (end < 0) {
            val open = text.indexOf('<', at)
            val passed = if (open < 0) null else passedOver(text, open)
            when {
                open < 0 -> end = text.length

                passed != null -> at = passed

                text.startsWith("</", open) && depth == 0 -> end = open

                else -> {
                    at = tagEnd(text, open)
                    depth += depthChange(text, open, at)
                }
            }
        }
        return end
    }

    /** Past the `>` of the tag that starts at [open], or the text's end where it has none. */
    private fun tagEnd(text: String, open: Int): Int = text.indexOf('>', open).let { if (it < 0) text.length else it + 1 }

    /** How the tag from [open] to [end] changes how deep the content is: an end tag closes, an empty element does not open. */
    private fun depthChange(text: String, open: Int, end: Int): Int = when {
        text.startsWith("</", open) -> -1
        end - open >= 2 && text[end - 1] == '>' && text[end - 2] == '/' -> 0
        else -> 1
    }

    /** Past a comment, a CDATA section, a processing instruction or a declaration that starts at [open], or null. */
    private fun passedOver(text: String, open: Int): Int? {
        val closer = Passed.entries.firstOrNull { text.startsWith(it.opener, open) }?.closer ?: return null
        return text.indexOf(closer, open).let { if (it < 0) text.length else it + closer.length }
    }

    /** [end], back over the room before it, but not before [floor]. */
    private fun backOverRoom(text: String, end: Int, floor: Int): Int {
        var at = end
        while (at > floor && text[at - 1].isWhitespace()) at--
        return at
    }

    /** What the content of an element passes over, by what opens and what closes it: the order is the order tried. */
    private enum class Passed(val opener: String, val closer: String) {
        Comment("<!--", "-->"),
        CharacterData("<![CDATA[", "]]>"),
        Instruction("<?", "?>"),
        Declaration("<!", ">")
    }
}
