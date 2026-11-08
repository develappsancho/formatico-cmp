package com.developersancho.formatico.core.format.query

import com.developersancho.formatico.core.format.xml.isXmlWhitespace

private const val COMMENT_OPEN = "<!--"
private const val COMMENT_CLOSE = "-->"
private const val INSTRUCTION_OPEN = "<?"
private const val INSTRUCTION_CLOSE = "?>"
private const val XML_LANG = "xml:lang"

/**
 * The string-value of [node], as XPath 1.0 gives it: the text of every text node inside the root or an element, a
 * text node's own, an attribute's value, a comment's content, and a processing instruction's after its target.
 */
internal fun XPathIndex.stringValue(node: XPathTreeNode): String = when (node.kind) {
    XPathKind.Root, XPathKind.Element, XPathKind.Text -> text.substring(node.textStart, node.textEnd)
    XPathKind.Attribute -> node.attributeValue.orEmpty()
    XPathKind.Comment -> markupContent(node, COMMENT_OPEN.length, COMMENT_CLOSE)
    XPathKind.Instruction -> markupContent(node, INSTRUCTION_OPEN.length + node.name.length, INSTRUCTION_CLOSE).trimStart(::isXmlWhitespace)
}

/**
 * What [node], a comment or a processing instruction, holds: after its first [skipped] characters, and before its
 * [close] where it has one.
 */
private fun XPathIndex.markupContent(node: XPathTreeNode, skipped: Int, close: String): String {
    val written = document.text.substring(node.range.start, node.range.end)
    val end = if (written.endsWith(close) && written.length >= skipped + close.length) written.length - close.length else written.length
    return if (skipped >= end) "" else written.substring(skipped, end)
}

/** The local part of [node]'s name: what follows its prefix's colon, or the whole of it. */
internal val XPathTreeNode.localName: String get() = name.substringAfter(':')

/** What an XPath gives of [node]: the node, its absolute path, its string-value and where it is written. */
internal fun XPathIndex.match(node: XPathTreeNode): XPathMatch = XPathMatch(node.node, pathOf(node), stringValue(node), node.range)

/**
 * The absolute path of [node]: `/` for the root, and each step from it, with its position where its parent has more
 * than one node it could select: `/rss/channel/item[2]/@id`, `/a/text()[1]`, `/processing-instruction('pi')`.
 */
internal fun pathOf(node: XPathTreeNode): String {
    if (node.kind == XPathKind.Root) return "/"
    val steps = ArrayList<XPathTreeNode>()
    var step: XPathTreeNode? = node
    while (step != null && step.kind != XPathKind.Root) {
        steps += step
        step = step.parent
    }
    return buildString {
        steps.asReversed().forEach { each ->
            append('/')
            when (each.kind) {
                XPathKind.Attribute -> append('@').append(each.name)
                XPathKind.Text -> append("text()")
                XPathKind.Comment -> append("comment()")
                XPathKind.Instruction -> append("processing-instruction('").append(each.name).append("')")
                XPathKind.Element, XPathKind.Root -> append(each.name)
            }
            if (each.stepCount > 1) append('[').append(each.stepPosition).append(']')
        }
    }
}

/**
 * Whether the language of [node], given by the `xml:lang` of the nearest element at or above it, is [language] or one
 * of its kinds, its case aside: `en` for `en`, `EN` and `en-GB`.
 */
internal fun lang(node: XPathTreeNode, language: String): Boolean {
    var element: XPathTreeNode? = if (node.kind == XPathKind.Element) node else node.parent
    var written: String? = null
    while (element != null && written == null) {
        written = element.attributes.firstOrNull { it.name == XML_LANG }?.attributeValue
        element = element.parent
    }
    return written != null &&
        (written.equals(language, ignoreCase = true) || written.startsWith("$language-", ignoreCase = true))
}
