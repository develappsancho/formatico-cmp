package com.developersancho.formatico.core.format.xml

import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.text.TextRange

/**
 * How deep elements are nested at most. Deeper ones are read all the same, and reported, so that what walks the tree,
 * the printers among them, is not given a document deeper than its stack.
 */
private const val MAX_DEPTH = 512

/**
 * Where each node read goes: into the element open last, or at the top level, with what XML asks of the top level
 * checked there. An element that is never closed is closed by its parent's end tag, or by the end of the text; an end
 * tag of another name closes the element open last, unless it closes again the element closed just before, and is
 * read past then.
 */
internal class XmlTree(private val report: XmlReport) {
    /** The top level, in order. */
    val nodes = ArrayList<XmlNode>()

    /** Every comment, at any depth, in order. */
    val comments = ArrayList<TextRange>()

    /** The document's element: the first at the top level. */
    var root: XmlElement? = null
        private set

    /** Whether a DOCTYPE has been read. */
    var hasDoctype = false
        private set

    private val open = ArrayList<OpenElement>()
    private var tooDeep = false

    /** An element whose start tag is read, and not yet its end tag. */
    private class OpenElement(val tag: XmlStartTag) {
        val children = ArrayList<XmlNode>()
    }

    /** Adds [node] to the element open last, or to the top level. */
    fun add(node: XmlNode) {
        val parent = open.lastOrNull()
        if (parent != null) {
            parent.children += node
            return
        }
        nodes += node
        if (node is XmlElement && root == null) root = node
        if (node is XmlText && (node.cdata || !node.raw.isXmlBlank())) {
            val written = node.raw.indexOfFirst { !isXmlWhitespace(it) }
            val end = node.raw.indexOfLast { !isXmlWhitespace(it) } + 1
            report(ProblemCode.XmlTextOutsideRoot, node.range.start + written, node.range.start + end)
        }
    }

    /** Adds [comment] where it is. */
    fun comment(comment: XmlComment) {
        comments += comment.range
        add(comment)
    }

    /** Adds [doctype], which stands only once, at the top level and before the root. */
    fun doctype(doctype: XmlDoctype) {
        if (open.isNotEmpty() || root != null || hasDoctype) {
            report(ProblemCode.UnexpectedToken, doctype.range.start, doctype.range.start + DOCTYPE_OPEN.length)
        }
        hasDoctype = true
        add(doctype)
    }

    /** Opens the element of [tag], or adds it where it closes itself. */
    fun open(tag: XmlStartTag) {
        if (open.isEmpty() && root != null) report(ProblemCode.XmlSecondRoot, tag.range.start, tag.range.end)
        if (tag.selfClosing) {
            add(XmlElement(tag.range, tag.name, tag.nameRange, tag.attributes, emptyList(), selfClosing = true))
            return
        }
        if (open.size == MAX_DEPTH && !tooDeep) {
            tooDeep = true
            report(ProblemCode.TooDeep, tag.range.start, tag.range.end)
        }
        open += OpenElement(tag)
    }

    /** Closes the element [tag] is the end tag of, with those open inside it, or reads past it. */
    fun close(tag: XmlEndTag) {
        val index = open.indexOfLast { it.tag.name == tag.name }
        when {
            index >= 0 -> {
                while (open.lastIndex > index) closeUnclosed()
                closeLast(tag.range.end)
            }

            open.isEmpty() || lastElementName() == tag.name -> {
                report(ProblemCode.XmlUnexpectedEndTag, tag.range.start, tag.range.end)
            }

            else -> {
                report(ProblemCode.XmlMismatchedEndTag, tag.nameRange.start, tag.nameRange.end)
                closeLast(tag.range.end)
            }
        }
    }

    /**
     * Closes every element still open at the end of the text, and reports a text with no element: an empty one, or
     * nothing but whitespace, as one with no value, where [begin] is where its content starts.
     */
    fun finish(text: String, begin: Int) {
        while (open.isNotEmpty()) closeUnclosed()
        if (root != null) return
        val first = text.whitespaceEnd(begin)
        if (first == text.length) {
            report(ProblemCode.ExpectedValue, text.length, text.length)
        } else {
            report(ProblemCode.XmlNoRoot, first, text.indexOfLast { !isXmlWhitespace(it) } + 1)
        }
    }

    /** The name of the last element in the element open last, which an end tag may close again by mistake. */
    private fun lastElementName(): String? = (open.last().children.lastOrNull { it is XmlElement } as? XmlElement)?.name

    /** Closes the element open last, which is never closed, where what was read of it ends. */
    private fun closeUnclosed() {
        val element = open.last()
        report(ProblemCode.XmlUnclosedElement, element.tag.range.start, element.tag.range.end)
        closeLast(element.children.lastOrNull()?.range?.end ?: element.tag.range.end)
    }

    /** Closes the element open last at [end], and adds it where it stands. */
    private fun closeLast(end: Int) {
        val element = open.removeAt(open.lastIndex)
        val tag = element.tag
        add(XmlElement(TextRange(tag.range.start, end), tag.name, tag.nameRange, tag.attributes, element.children, selfClosing = false))
    }
}
