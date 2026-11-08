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
        val step = PathStep.Key(element.name)
        val child = xmlNode(element, listOf(step), element.name, childKey(ROOT, step))
        ValueNode(ROOT, emptyList(), ROOT, ValueKind.Object, "{1}", null, null, listOf(child), TextRange(0, text.length))
    }
    return Reading.Read(format = Format.Xml, root = root, problems = document.problems)
}

/** The node of [element], at [path] and keyed [key]. */
private fun xmlNode(element: XmlElement, path: List<PathStep>, name: String, key: String): ValueNode {
    val texts = element.children.filterIsInstance<XmlText>().filter { it.cdata || it.value.isNotBlank() }
    val elements = element.children.filterIsInstance<XmlElement>()
    val written = texts.joinToString("") { it.raw }.trim()
    val read = texts.joinToString("") { it.value }.trim()
    if (element.attributes.isEmpty() && elements.isEmpty()) {
        return ValueNode(key, path, name, ValueKind.String, written, read, previewOf(read), emptyList(), element.range)
    }
    val children = ArrayList<ValueNode>()
    element.attributes.forEach { attribute ->
        val step = PathStep.Key("@${attribute.name}")
        children += ValueNode(
            childKey(key, step),
            path + step,
            step.name,
            ValueKind.String,
            attribute.raw,
            attribute.value,
            previewOf(attribute.value),
            emptyList(),
            attribute.range
        )
    }
    elements.groupBy { it.name }.forEach { (tag, named) -> children += elementsNamed(tag, named, path, key) }
    if (texts.isNotEmpty()) {
        val step = PathStep.Key(XML_TEXT)
        val range = TextRange(texts.first().range.start, texts.last().range.end)
        children +=
            ValueNode(childKey(key, step), path + step, XML_TEXT, ValueKind.String, written, read, previewOf(read), emptyList(), range)
    }
    return ValueNode(key, path, name, ValueKind.Object, "{${children.size}}", null, null, children, element.range)
}

/** The elements of one [tag] in an element keyed [key]: the element where there is one, an array of them otherwise. */
private fun elementsNamed(tag: String, named: List<XmlElement>, path: List<PathStep>, key: String): ValueNode {
    val step = PathStep.Key(tag)
    val own = childKey(key, step)
    if (named.size == 1) return xmlNode(named.single(), path + step, tag, own)
    val items = named.mapIndexed { index, element ->
        val item = PathStep.Index(index)
        xmlNode(element, path + step + item, "$index", childKey(own, item))
    }
    val range = TextRange(named.first().range.start, named.last().range.end)
    return ValueNode(own, path + step, tag, ValueKind.Array, "[${items.size}]", null, null, items, range)
}
