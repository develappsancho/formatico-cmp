package com.developersancho.formatico.core.format.query

import com.developersancho.formatico.core.format.text.TextRange
import com.developersancho.formatico.core.format.xml.XmlComment
import com.developersancho.formatico.core.format.xml.XmlDocument
import com.developersancho.formatico.core.format.xml.XmlElement
import com.developersancho.formatico.core.format.xml.XmlNode
import com.developersancho.formatico.core.format.xml.XmlProcessingInstruction
import com.developersancho.formatico.core.format.xml.XmlText
import com.developersancho.formatico.core.format.xml.isXmlWhitespace

/** The kinds of node of XPath's model but the namespace node. */
internal enum class XPathKind { Root, Element, Attribute, Text, Comment, Instruction }

/**
 * A node of XPath's model, with what the axes and the string-values need of it.
 *
 * @property name an element's or an attribute's name as it is written, a processing instruction's target, or empty.
 * @property range where it is written: the whole text for the root, a text node's run from its first part to its last.
 */
internal class XPathTreeNode(
    val kind: XPathKind,
    val parent: XPathTreeNode?,
    val node: XPathNode,
    val name: String,
    val range: TextRange
) {
    /** Its place in document order, attributes counted, from 0 for the root. */
    var order = 0

    /** Its place among the nodes that are no attributes, in [XPathIndex.nodes]; an attribute's is its owner's. */
    var flat = 0

    /** The place in [XPathIndex.nodes] after its last descendant. */
    var subtreeEnd = 0

    /** Its children: elements, text nodes, comments and processing instructions. */
    var children: List<XPathTreeNode> = emptyList()

    /** Its attributes, where it is an element. */
    var attributes: List<XPathTreeNode> = emptyList()

    /** Its place among its parent's children, or among its owner's attributes. */
    var siblingIndex = 0

    /** Where its string-value starts in [XPathIndex.text], for the root, an element and a text node. */
    var textStart = 0

    /** Where its string-value ends in [XPathIndex.text]. */
    var textEnd = 0

    /** Its position among the siblings its step in a path could also select, from 1. */
    var stepPosition = 1

    /** How many siblings its step in a path could select, itself among them. */
    var stepCount = 1
}

/**
 * XPath's model of [document], built once for each evaluation, with a stack rather than by recursion, whatever its
 * depth: its nodes in document order, with each one's parent, children and attributes, and the text of all its text
 * nodes in one string, from which every node's string-value is cut. Text outside the document's element, a DOCTYPE,
 * the XML declaration and namespace declarations are none of its nodes; a run of text and CDATA sections next to each
 * other is one text node, and an empty one none.
 */
internal class XPathIndex(val document: XmlDocument) {
    /** The nodes that are no attributes, in document order, the root first. */
    val nodes = ArrayList<XPathTreeNode>()

    /** The text of every text node, in document order. */
    val text = StringBuilder()

    /** The root. */
    val root: XPathTreeNode

    private var count = 0

    /** The elements by their `xml:id`, the first of each, built when `id()` first asks. */
    val ids: Map<String, XPathTreeNode> by lazy {
        buildMap {
            nodes.forEach { node ->
                val id = node.attributes.firstOrNull { it.name == XML_ID }?.attributeValue?.trim(::isXmlWhitespace)
                if (id != null) getOrPut(id) { node }
            }
        }
    }

    init {
        root = XPathTreeNode(XPathKind.Root, null, XPathNode.Root(document), "", TextRange(0, document.text.length))
        enter(root)
        val frames = arrayListOf(Frame(root, document.nodes))
        while (frames.isNotEmpty()) {
            val frame = frames.last()
            if (frame.next == frame.sources.size) {
                finish(frame)
                frames.removeAt(frames.lastIndex)
            } else {
                read(frame)?.let { frames += it }
            }
        }
    }

    /** An element, the root among them, whose children are being read from [sources]. */
    private class Frame(val node: XPathTreeNode, val sources: List<XmlNode>) {
        var next = 0
        val children = ArrayList<XPathTreeNode>()
    }

    /** Reads the source next in [frame], and gives the frame of an element it opens. */
    private fun read(frame: Frame): Frame? {
        val parent = frame.node
        when (val source = frame.sources[frame.next++]) {
            is XmlText -> textRun(frame, source)

            is XmlElement -> {
                val element = XPathTreeNode(XPathKind.Element, parent, XPathNode.Element(source), source.name, source.range)
                frame.children += enter(element)
                element.attributes = source.attributes.filterNot { isNamespaceDeclaration(it.name) }.map { attribute ->
                    enter(
                        XPathTreeNode(XPathKind.Attribute, element, XPathNode.Attribute(source, attribute), attribute.name, attribute.range)
                    )
                }
                return Frame(element, source.children)
            }

            is XmlComment -> frame.children += enter(XPathTreeNode(XPathKind.Comment, parent, XPathNode.Comment(source), "", source.range))

            is XmlProcessingInstruction -> if (!source.target.equals(DECLARATION, ignoreCase = true)) {
                frame.children +=
                    enter(XPathTreeNode(XPathKind.Instruction, parent, XPathNode.Instruction(source), source.target, source.range))
            }

            // A DOCTYPE is no node of XPath's model.
            else -> {}
        }
        return null
    }

    /** Reads the run of text and CDATA sections that starts with [first] in [frame] as one text node. */
    private fun textRun(frame: Frame, first: XmlText) {
        var last = first
        val start = text.length
        text.append(first.value)
        while (frame.next < frame.sources.size) {
            val next = frame.sources[frame.next] as? XmlText ?: break
            text.append(next.value)
            last = next
            frame.next++
        }
        // The text around the document's element is no node; an empty CDATA section, nor any run of them, either.
        if (frame.node.kind == XPathKind.Root || text.length == start) {
            text.setLength(start)
            return
        }
        val node = XPathTreeNode(XPathKind.Text, frame.node, XPathNode.Text(first), "", TextRange(first.range.start, last.range.end))
        frame.children += enter(node)
        node.textStart = start
        node.textEnd = text.length
    }

    /** Gives [node] its place in document order. */
    private fun enter(node: XPathTreeNode): XPathTreeNode {
        node.order = count++
        if (node.kind == XPathKind.Attribute) {
            node.flat = requireNotNull(node.parent).flat
        } else {
            node.flat = nodes.size
            nodes += node
            node.subtreeEnd = nodes.size
            node.textStart = text.length
            node.textEnd = text.length
        }
        return node
    }

    /** Closes the element of [frame], whose children have all been read. */
    private fun finish(frame: Frame) {
        val node = frame.node
        node.children = frame.children
        node.subtreeEnd = nodes.size
        node.textEnd = text.length
        numberSteps(node.children)
        numberSteps(node.attributes)
    }

    private companion object {
        const val XML_ID = "xml:id"
        const val DECLARATION = "xml"
    }
}

/** Gives each of [siblings] its place among them, and its position among those its step in a path could select. */
private fun numberSteps(siblings: List<XPathTreeNode>) {
    siblings.forEachIndexed { index, sibling -> sibling.siblingIndex = index }
    if (siblings.size < 2) return
    val counts = HashMap<String, Int>()
    siblings.forEach { sibling ->
        val key = stepKey(sibling)
        val position = (counts[key] ?: 0) + 1
        counts[key] = position
        sibling.stepPosition = position
    }
    siblings.forEach { it.stepCount = counts.getValue(stepKey(it)) }
}

/** What a step in a path that selects [node] names: its kind, and its name for an element, an attribute and an instruction. */
private fun stepKey(node: XPathTreeNode): String = "${node.kind.ordinal}:${node.name}"

/** Whether an attribute named [name] declares a namespace, which XPath's model has as no attribute. */
private fun isNamespaceDeclaration(name: String): Boolean = name == "xmlns" || name.startsWith("xmlns:")

/** The value of [node], where it is an attribute, as XML reads it: its references read, its line breaks spaces. */
internal val XPathTreeNode.attributeValue: String? get() = (node as? XPathNode.Attribute)?.attribute?.value
