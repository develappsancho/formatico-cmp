package com.developersancho.formatico.core.format.xml

import com.developersancho.formatico.core.format.Problem
import com.developersancho.formatico.core.format.Severity
import com.developersancho.formatico.core.format.text.LineMap
import com.developersancho.formatico.core.format.text.TextRange

/**
 * An XML text as it was read, XML 1.0 as documents are written: its nodes, which keep every name, attribute and text
 * as it is written, where it is written; its comments; and its problems, each with its line and column. It is not a
 * validating reader: a DOCTYPE is kept as it is written, and its internal subset is not read.
 *
 * Reading goes on past a mistake, so that every problem is found: an element that is never closed is closed by its
 * parent's end tag, and an end tag no element is open for is read past. Nothing of the text is lost: [print] gives it
 * back as it was written.
 *
 * @property text the text read.
 * @property nodes the top level, in order: the declaration, processing instructions, comments, the DOCTYPE, the root,
 *   and the text around them.
 * @property root the document's element, or null where there is none; as much of it as could be read where there are
 *   errors.
 * @property comments where each comment is, at any depth, in order.
 * @property problems what is wrong in it, in the order of the text.
 */
class XmlDocument private constructor(
    val text: String,
    val nodes: List<XmlNode>,
    val root: XmlElement?,
    val comments: List<TextRange>,
    val problems: List<Problem>
) {
    /** Whether the document has no error; a warning leaves it valid. */
    val isValid: Boolean get() = problems.none { it.severity == Severity.Error }

    /**
     * The document written out from its nodes, and from what was read past between them, a byte order mark or an end
     * tag no element is open for: the text it was read from, character for character.
     */
    fun print(): String = buildString(text.length) {
        var at = 0
        nodes.forEach { node ->
            append(text, at, node.range.start)
            append(text, node.range.start, node.range.end)
            at = node.range.end
        }
        append(text, at, text.length)
    }

    companion object {
        /** Reads [text] as XML. */
        fun parse(text: String): XmlDocument {
            val reader = XmlReader(text)
            reader.read()
            val lines = LineMap(text)
            val problems = reader.found.sortedBy { it.start }.map {
                Problem(
                    code = it.code,
                    severity = Severity.Error,
                    range = TextRange(it.start, it.end),
                    start = lines.position(it.start),
                    end = lines.position(it.end)
                )
            }
            val tree = reader.tree
            return XmlDocument(text, tree.nodes, tree.root, tree.comments, problems)
        }
    }
}
