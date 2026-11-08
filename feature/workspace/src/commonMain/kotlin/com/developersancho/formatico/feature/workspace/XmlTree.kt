package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.json.PathStep
import com.developersancho.formatico.core.format.text.TextRange
import com.developersancho.formatico.core.format.xml.XmlDocument
import com.developersancho.formatico.core.format.xml.XmlElement
import com.developersancho.formatico.core.format.xml.XmlText

/** The name the tree gives an element's text, beside its attributes and its elements. */
internal const val XML_TEXT = "#text"

/**
 * Reads [text] as XML, for its tree: the root element under `$`, named by its tag. An element is an object of its
 * attributes, named `@` and their name, of its elements, those of one name gathered into an array where there are more
 * than one, and of its text, `#text`, where it has any. An element with text alone, and no attribute, is that text; one
 * with nothing in it is an empty string. Comments and processing instructions are not in the tree.
 */
internal fun readXml(text: String): Reading {
    val document = XmlDocument.parse(text)
    val root = document.root?.let { element ->
        val nodes = XmlNodes()
        val step = nodes.parts.key(element.name)
        val child = nodes.node(element, NodePath.Root + step, step.name, childKey(ROOT, step))
        ValueNode(ROOT, NodePath.Root, ROOT, ValueKind.Object, "{1}", null, null, listOf(child), TextRange(0, text.length))
    }
    return Reading.Read(format = Format.Xml, root = root, problems = document.problems, comments = document.comments.size)
}

/** The nodes of one XML document, which share their names, their steps and their paths' steps. */
private class XmlNodes {
    val parts = SharedParts()

    /** The node of [element], at [path] and keyed [key]. */
    fun node(element: XmlElement, path: NodePath, name: String, key: String): ValueNode {
        val texts = element.children.filterIsInstance<XmlText>().filter { it.cdata || it.value.isNotBlank() }
        val elements = element.children.filterIsInstance<XmlElement>()
        val read = texts.joinToString("") { it.value }.trim()
        val written = parts.sameOrBoth(texts.joinToString("") { it.raw }.trim(), read)
        if (element.attributes.isEmpty() && elements.isEmpty()) {
            return ValueNode(key, path, name, ValueKind.String, written, read, previewOf(read), emptyList(), element.range)
        }
        val children = ArrayList<ValueNode>()
        element.attributes.forEach { attribute ->
            val step = parts.key("@${attribute.name}")
            children += ValueNode(
                childKey(key, step),
                path + step,
                step.name,
                ValueKind.String,
                parts.sameOrBoth(attribute.raw, attribute.value),
                attribute.value,
                previewOf(attribute.value),
                emptyList(),
                attribute.range
            )
        }
        elements.groupBy { it.name }.forEach { (tag, named) -> children += elementsNamed(tag, named, path, key) }
        if (texts.isNotEmpty()) {
            val step = parts.key(XML_TEXT)
            val range = TextRange(texts.first().range.start, texts.last().range.end)
            children +=
                ValueNode(childKey(key, step), path + step, XML_TEXT, ValueKind.String, written, read, previewOf(read), emptyList(), range)
        }
        return ValueNode(key, path, name, ValueKind.Object, "{${children.size}}", null, null, children, element.range)
    }

    /** The elements of one [tag] in an element keyed [key]: the element where there is one, an array of them otherwise. */
    private fun elementsNamed(tag: String, named: List<XmlElement>, path: NodePath, key: String): ValueNode {
        val step = parts.key(tag)
        val own = childKey(key, step)
        if (named.size == 1) return node(named.single(), path + step, step.name, own)
        val items = named.mapIndexed { index, element ->
            val item = PathStep.Index(index)
            node(element, path + step + item, parts.place(index), childKey(own, item))
        }
        val range = TextRange(named.first().range.start, named.last().range.end)
        return ValueNode(own, path + step, step.name, ValueKind.Array, "[${items.size}]", null, null, items, range)
    }
}
