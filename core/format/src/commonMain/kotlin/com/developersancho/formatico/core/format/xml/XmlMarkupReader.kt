package com.developersancho.formatico.core.format.xml

import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.text.TextRange

/**
 * Reads the markup of a document that is not a tag: comments, CDATA sections, processing instructions and the
 * DOCTYPE. Each ends where its closer is; one with no closer runs to the end of the text, or, for a processing
 * instruction, to its first `>`, and is reported.
 */
internal class XmlMarkupReader(
    private val text: String,
    private val characterData: XmlCharacterData,
    private val report: XmlReport
) {
    /** The comment whose `<!--` stands at [start]. */
    fun comment(start: Int): XmlComment {
        val from = start + COMMENT_OPEN.length
        val close = text.indexOf(COMMENT_CLOSE, from)
        if (close < 0) {
            report(ProblemCode.UnterminatedComment, start, text.length)
            return XmlComment(TextRange(start, text.length))
        }
        // `--` may not stand inside a comment, nor may the comment end in `-`, which makes `--->`.
        val hyphens = text.indexOf("--", from)
        if (hyphens < close) report(ProblemCode.XmlHyphensInComment, hyphens, hyphens + 2)
        return XmlComment(TextRange(start, close + COMMENT_CLOSE.length))
    }

    /** The CDATA section whose `<![CDATA[` stands at [start]. */
    fun cdata(start: Int): XmlText {
        val from = start + CDATA_OPEN.length
        val close = text.indexOf(CDATA_CLOSE, from)
        if (close < 0) report(ProblemCode.XmlUnterminatedCdata, start, text.length)
        val contentEnd = if (close < 0) text.length else close
        val end = if (close < 0) text.length else close + CDATA_CLOSE.length
        return XmlText(TextRange(start, end), text.substring(start, end), characterData.lineBreaksRead(from, contentEnd), cdata = true)
    }

    /** The processing instruction whose `<?` stands at [start], a name after it. */
    fun instruction(start: Int): XmlProcessingInstruction {
        val targetEnd = text.nameEnd(start + INSTRUCTION_OPEN.length)
        val target = text.substring(start + INSTRUCTION_OPEN.length, targetEnd)
        val end = instructionEnd(start, targetEnd, declaration = target.equals(DECLARATION_TARGET, ignoreCase = true))
        // The target is followed by whitespace, or by the instruction's end.
        val after = text.getOrNull(targetEnd)
        if (after != null && !isXmlWhitespace(after) && !text.startsWith(INSTRUCTION_CLOSE, targetEnd)) {
            report(ProblemCode.UnexpectedCharacter, targetEnd, targetEnd + 1)
        }
        return XmlProcessingInstruction(TextRange(start, end), target)
    }

    /**
     * Where the processing instruction at [start] ends: after its `?>`. The declaration holds no `>` of its own, so that
     * one that does not follow a `?` ends it all the same; another instruction may hold one.
     */
    private fun instructionEnd(start: Int, targetEnd: Int, declaration: Boolean): Int {
        val close = text.indexOf(INSTRUCTION_CLOSE, targetEnd)
        val angle = text.indexOf('>', targetEnd)
        if (close >= 0 && (!declaration || close + 1 == angle)) return close + INSTRUCTION_CLOSE.length
        val end = if (angle >= 0) angle + 1 else text.length
        report(ProblemCode.XmlUnterminatedInstruction, start, end)
        return end
    }

    /**
     * The DOCTYPE whose `<!DOCTYPE` stands at [start], to the `>` that closes it: one in a quoted literal, or inside its
     * internal subset, `[ … ]`, does not; nor does one in a comment or a processing instruction in the subset.
     */
    fun doctype(start: Int): XmlDoctype {
        var index = start + DOCTYPE_OPEN.length
        var subset = false
        while (index < text.length) {
            val char = text[index]
            if (char == '>' && !subset) return XmlDoctype(TextRange(start, index + 1))
            index = when {
                char == '"' || char == '\'' -> {
                    past(text.indexOf(char, index + 1), 1)
                }

                subset && text.startsWith(COMMENT_OPEN, index) -> {
                    past(text.indexOf(COMMENT_CLOSE, index + COMMENT_OPEN.length), COMMENT_CLOSE.length)
                }

                subset && text.startsWith(INSTRUCTION_OPEN, index) -> {
                    past(text.indexOf(INSTRUCTION_CLOSE, index + INSTRUCTION_OPEN.length), INSTRUCTION_CLOSE.length)
                }

                else -> {
                    if (char == '[') {
                        subset = true
                    } else if (char == ']') {
                        subset = false
                    }
                    index + 1
                }
            }
        }
        report(ProblemCode.XmlUnterminatedTag, start, text.length)
        return XmlDoctype(TextRange(start, text.length))
    }

    /** Where a closer of [length] found at [close] ends, or the end of the text where none is found. */
    private fun past(close: Int, length: Int): Int = if (close < 0) text.length else close + length
}
