package com.developersancho.formatico.core.format.xml

import com.developersancho.formatico.core.format.text.Indentation
import com.developersancho.formatico.core.format.text.LineEnding
import com.developersancho.formatico.core.format.text.TextLayout

/**
 * The document written out again: each node of the top level on a line of its own, and in an element whose children
 * are elements, comments and processing instructions, each child on a line of its own, a level of [indentation]
 * deeper. The whitespace between them is left out. An element that holds text, alone or among elements, a CDATA
 * section among it, and one that says `xml:space="preserve"`, keeps its content exactly as it is written, on the line
 * of its start tag.
 *
 * A tag is written as its name and its attributes, a space before each and each value as it is written, its quotes
 * kept: `<a href="x"/>`. An element written `<a></a>` stays so, and one that closes itself stays `<a/>`. Comments,
 * processing instructions, CDATA sections and the DOCTYPE stay as they are written.
 *
 * The lines end as the document's first line does, and the text ends with a line break where the document's did.
 *
 * @throws IllegalArgumentException when the document is not valid.
 */
fun XmlDocument.formatted(indentation: Indentation = Indentation.Spaces(2)): String {
    require(isValid) { "Only a valid document is written out again" }
    val newline = (TextLayout.of(text).lineEnding ?: LineEnding.Lf).characters
    val written = XmlWriter(text, indentation.unit, newline, minify = false).document(nodes)
    return if (text.endsWith('\n') || text.endsWith('\r')) written + newline else written
}

/**
 * The document with the room between its elements, and its comments, taken out: on one line, with no indentation.
 * The content of an element that holds text, or that says `xml:space="preserve"`, keeps its whitespace; the
 * declaration, the processing instructions and the DOCTYPE stay, with no line break after them.
 *
 * @throws IllegalArgumentException when the document is not valid.
 */
fun XmlDocument.minified(): String {
    require(isValid) { "Only a valid document is written out again" }
    return XmlWriter(text, indent = "", newline = "", minify = true).document(nodes)
}

/** Whether the element keeps its content as it is written: it holds text, or says `xml:space="preserve"`. */
private fun XmlElement.keepsContent(): Boolean = attributes.any { it.name == "xml:space" && it.value == "preserve" } ||
    children.any { it is XmlText && (it.cdata || !it.raw.isXmlBlank()) }

/**
 * Writes a valid document out again, a node to a line, or on one line where [minify] says so, and its comments left
 * out then.
 *
 * @param indent one level of indentation.
 * @param newline the line ending.
 */
private class XmlWriter(
    private val text: String,
    private val indent: String,
    private val newline: String,
    private val minify: Boolean
) {
    private val out = StringBuilder(text.length)

    /** The top level, [nodes], written out. */
    fun document(nodes: List<XmlNode>): String {
        nodes.filter(::isWritten).forEachIndexed { index, node ->
            if (index > 0) out.append(newline)
            node(node, depth = 0)
        }
        return out.toString()
    }

    /** Whether [node] is written among elements: whitespace is not, nor is a comment where the document is minified. */
    private fun isWritten(node: XmlNode): Boolean = when (node) {
        is XmlText -> node.cdata || !node.raw.isXmlBlank()
        is XmlComment -> !minify
        else -> true
    }

    private fun node(node: XmlNode, depth: Int) {
        if (node is XmlElement) element(node, depth) else asWritten(node)
    }

    /** Writes [element] at [depth]: its tags, and its children on lines of their own or its content as it is. */
    private fun element(element: XmlElement, depth: Int) {
        startTag(element)
        if (element.selfClosing) return
        if (element.keepsContent()) {
            content(element)
        } else {
            val children = element.children.filter(::isWritten)
            children.forEach { child ->
                out.append(newline)
                indent(depth + 1)
                node(child, depth + 1)
            }
            if (children.isNotEmpty()) {
                out.append(newline)
                indent(depth)
            }
        }
        endTag(element)
    }

    /** Writes the content of an element that keeps it: as it is written, or, minified, without its comments. */
    private fun content(element: XmlElement) {
        val children = element.children
        if (minify) {
            children.forEach(::kept)
        } else if (children.isNotEmpty()) {
            out.append(text, children.first().range.start, children.last().range.end)
        }
    }

    /** Writes [node] inside content that is kept, minified: its text as it is, its tags as tags are written, no comment. */
    private fun kept(node: XmlNode) {
        when (node) {
            is XmlComment -> Unit

            is XmlElement -> {
                startTag(node)
                if (!node.selfClosing) {
                    node.children.forEach(::kept)
                    endTag(node)
                }
            }

            else -> asWritten(node)
        }
    }

    private fun startTag(element: XmlElement) {
        out.append('<').append(element.name)
        element.attributes.forEach { out.append(' ').append(it.name).append('=').append(it.raw) }
        out.append(if (element.selfClosing) "/>" else ">")
    }

    private fun endTag(element: XmlElement) {
        out.append("</").append(element.name).append('>')
    }

    private fun asWritten(node: XmlNode) {
        out.append(text, node.range.start, node.range.end)
    }

    private fun indent(depth: Int) {
        repeat(depth) { out.append(indent) }
    }
}
