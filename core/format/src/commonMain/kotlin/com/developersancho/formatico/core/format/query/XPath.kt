package com.developersancho.formatico.core.format.query

import com.developersancho.formatico.core.format.text.TextRange
import com.developersancho.formatico.core.format.xml.XmlAttribute
import com.developersancho.formatico.core.format.xml.XmlComment
import com.developersancho.formatico.core.format.xml.XmlDocument
import com.developersancho.formatico.core.format.xml.XmlElement
import com.developersancho.formatico.core.format.xml.XmlProcessingInstruction
import com.developersancho.formatico.core.format.xml.XmlText

/** A node of XPath's model of an XML document. */
sealed interface XPathNode {
    /** The root, above the document's element: the document as a whole. */
    data class Root(val document: XmlDocument) : XPathNode

    /** An element. */
    data class Element(val element: XmlElement) : XPathNode

    /** An attribute of [owner]; namespace declarations, `xmlns` and `xmlns:…`, are none in XPath's model. */
    data class Attribute(val owner: XmlElement, val attribute: XmlAttribute) : XPathNode

    /**
     * A text node: a run of text and CDATA sections next to each other, which XPath reads as one. [text] is the first of
     * the run; its [XPathMatch] gives the run's range and value as a whole.
     */
    data class Text(val text: XmlText) : XPathNode

    /** A comment. */
    data class Comment(val comment: XmlComment) : XPathNode

    /** A processing instruction; the XML declaration, `<?xml …?>`, is none in XPath's model. */
    data class Instruction(val instruction: XmlProcessingInstruction) : XPathNode
}

/**
 * A node an XPath selected: [path], the absolute path of it, `/rss/channel/item[1]/title`, a step's position given
 * only where its parent has more than one sibling it could be (1-based), `/rss/@version`, `.../title/text()`,
 * `comment()[2]`; [value], its string-value; [range], where it is written in the document's text.
 */
data class XPathMatch(val node: XPathNode, val path: String, val value: String, val range: TextRange)

/** What an XPath evaluates to. */
sealed interface XPathResult {
    /** A node-set, in document order. */
    data class Nodes(val matches: List<XPathMatch>) : XPathResult

    /** A string. */
    data class Text(val value: String) : XPathResult

    /** [text] is [value] as XPath's string() writes it: `3`, `0.5`, `NaN`, `Infinity`, never an exponent. */
    data class Number(val value: Double, val text: String) : XPathResult

    /** A boolean. */
    data class Truth(val value: Boolean) : XPathResult
}

/**
 * An XPath 1.0 expression: every axis but the namespace axis, the abbreviated syntax and the full one, every node
 * test, predicates, unions, every operator, and the core function library.
 *
 * There is no resolution of namespaces: a name matches as it is written, its prefix and all, so that `x:item` selects
 * the elements written `<x:item>`, and `item` those written `<item>`, whatever namespace a declaration gives either;
 * `namespace-uri()` gives an empty string. Variables are not read. `id()` finds the elements whose `xml:id` is one of
 * its names, since no DTD is read to say which attributes are IDs. Whitespace-only text nodes are kept, as XPath keeps
 * them where nothing strips them; a string's length, `substring()` and `translate()` count characters as Unicode
 * does. An invalid document is evaluated on what was read of it.
 */
class XPath private constructor(private val expression: XPathExpression) {
    /** What the expression evaluates to in [document], with the document's root as its context node. */
    fun evaluate(document: XmlDocument): XPathResult {
        val index = XPathIndex(document)
        return when (val value = XPathEvaluator(index).evaluate(expression, XPathContext(index.root, position = 1, size = 1))) {
            is XPathValue.Nodes -> XPathResult.Nodes(value.nodes.map(index::match))
            is XPathValue.Text -> XPathResult.Text(value.value)
            is XPathValue.Number -> XPathResult.Number(value.value, xpathNumberText(value.value))
            is XPathValue.Truth -> XPathResult.Truth(value.value)
        }
    }

    companion object {
        /** Reads [expression] as an XPath 1.0 expression. */
        fun parse(expression: String): QueryParse<XPath> = readQuery {
            XPath(XPathParser(XPathTokenCursor(XPathLexer(expression).tokens(), expression.length)).parse())
        }
    }
}
