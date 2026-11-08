package com.developersancho.formatico.core.format.xml

import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.text.TextRange

/** A problem found while reading, by its offsets. */
internal class XmlFoundProblem(val code: ProblemCode, val start: Int, val end: Int)

/**
 * Reads an XML text, a piece of markup or a run of text at a time, into [tree], and goes on past a mistake, so that
 * every problem of the text is found. A `<` that starts no markup is read as text, and reported.
 */
internal class XmlReader(private val text: String) {
    /** What is wrong in the text, in the order it was found. */
    val found = ArrayList<XmlFoundProblem>()

    private val report: XmlReport = { code, start, end -> found += XmlFoundProblem(code, start, end) }

    /** The nodes read. */
    val tree = XmlTree(report)

    private val characterData = XmlCharacterData(text, { tree.hasDoctype }, report)
    private val markup = XmlMarkupReader(text, characterData, report)
    private val tags = XmlTagReader(text, characterData, report)

    /** Where the content starts: after a byte order mark, which is read past. */
    private val begin = if (text.startsWith('\uFEFF')) 1 else 0

    /** Reads the whole text. */
    fun read() {
        var index = begin
        while (index < text.length) {
            index = if (startsMarkup(index)) readMarkup(index) else readText(index)
        }
        tree.finish(text, begin)
    }

    /**
     * Whether the `<` at [at] starts markup: a tag, a comment, a CDATA section, a processing instruction or the
     * DOCTYPE.
     */
    private fun startsMarkup(at: Int): Boolean {
        if (text[at] != '<') return false
        val next = text.getOrNull(at + 1) ?: return false
        return when (next) {
            '!' -> text.startsWith(COMMENT_OPEN, at) || text.startsWith(CDATA_OPEN, at) || text.startsWith(DOCTYPE_OPEN, at)
            '?', '/' -> text.getOrNull(at + 2)?.let(::isNameStart) == true
            else -> isNameStart(next)
        }
    }

    /** Reads the markup at [start] into the tree, and says where it ends. */
    private fun readMarkup(start: Int): Int = when {
        text.startsWith(COMMENT_OPEN, start) -> markup.comment(start).also(tree::comment).range.end
        text.startsWith(CDATA_OPEN, start) -> markup.cdata(start).also(tree::add).range.end
        text.startsWith(DOCTYPE_OPEN, start) -> markup.doctype(start).also(tree::doctype).range.end
        text[start + 1] == '?' -> instruction(start)
        text[start + 1] == '/' -> tags.endTag(start).also(tree::close).range.end
        else -> tags.startTag(start).also(tree::open).range.end
    }

    /**
     * Reads the processing instruction at [start] into the tree, and says where it ends. The declaration stands only at
     * the start, and in small letters: `<?xml`.
     */
    private fun instruction(start: Int): Int {
        val instruction = markup.instruction(start)
        val target = instruction.target
        if (target.equals(DECLARATION_TARGET, ignoreCase = true) && (start != begin || target != DECLARATION_TARGET)) {
            report(ProblemCode.XmlMisplacedDeclaration, start, instruction.range.end)
        }
        tree.add(instruction)
        return instruction.range.end
    }

    /** Reads the text from [start] up to the next markup into the tree, and says where it ends. */
    private fun readText(start: Int): Int {
        var end = text.indexOf('<', start + 1)
        while (end >= 0 && !startsMarkup(end)) end = text.indexOf('<', end + 1)
        if (end < 0) end = text.length
        val value = characterData.read(start, end, attribute = false)
        tree.add(XmlText(TextRange(start, end), text.substring(start, end), value, cdata = false))
        return end
    }
}
