package com.developersancho.formatico.core.format.convert

import com.developersancho.formatico.core.format.xml.isNameChar
import com.developersancho.formatico.core.format.xml.isNameStart
import com.developersancho.formatico.core.format.xml.isXmlChar

/**
 * Writes values as an XML document, as Formatico's tree reads XML back: an object is an element, its members named `@`
 * and a name its attributes, its `#text` its text and its other members its elements, an array's items elements of
 * the array's name. Each level is indented by [indent]. XML has only text, and no null: one is left out, a comment
 * marking where it was, or written as an empty element, as [options] say. What it cannot write as it is goes to [log].
 */
internal class XmlWriter(private val indent: String, private val options: ConvertOptions, private val log: ConvertLog) {
    private val out = StringBuilder(DECLARATION)
    private var lines = 1
    private val leavesNulls = options.nulls == NullHandling.LeaveOut

    /** [value] as an XML document: an object of one member is its root element; anything else is put in `<root>`. */
    fun document(value: DataValue): String {
        val single = (value as? DataObject)?.members?.singleOrNull()?.takeIf { it.isElement }
        if (single != null) {
            element(single.name, single.value, single.name, depth = 0)
        } else {
            log.wrap(ROOT)
            element(ROOT, value, path = "", depth = 0)
        }
        return out.toString()
    }

    /** The element named [name] that holds [value], at [path]. */
    private fun element(name: String, value: DataValue, path: String, depth: Int) {
        val tag = xmlName(name, path)
        when (value) {
            is DataObject -> objectElement(tag, value, path, depth)
            is DataArray -> arrayElement(tag, value, path, depth)
            DataNull -> nullElement(name, tag, path, depth)
            else -> textElement(tag, tag, textOf(value, path, log), depth)
        }
    }

    private fun objectElement(tag: String, value: DataObject, path: String, depth: Int) {
        if (value.members.isEmpty()) {
            log.lose(Lost.Emptied, path)
            line(depth, "<$tag/>")
            return
        }
        val (attributes, rest) = value.members.partition { it.name.startsWith(ATTRIBUTE) && it.value.isScalar }
        val text = rest.firstOrNull { it.name == TEXT && it.value.isScalar }
        val children = rest.filter { it !== text }
        val open = tag + attributes.joinToString("") { attribute(it, path) }
        val written = text?.let { textOf(it.value, memberPath(path, TEXT), log) }
        if (children.isEmpty()) {
            textElement(open, tag, written.orEmpty(), depth)
            return
        }
        line(depth, "<$open>")
        written?.let { line(depth + 1, escapedText(it)) }
        children.forEach { member(it, memberPath(path, it.name), depth + 1) }
        line(depth, "</$tag>")
    }

    /** An array that is no member's: the root's value, or an item of another array, its items each an `<item>`. */
    private fun arrayElement(tag: String, value: DataArray, path: String, depth: Int) {
        if (value.items.isEmpty()) {
            log.lose(Lost.Emptied, path)
            line(depth, "<$tag/>")
            return
        }
        log.lose(Lost.ItemsNamed, path)
        line(depth, "<$tag>")
        value.items.forEachIndexed { index, item -> element(ITEM, item, itemPath(path, index), depth + 1) }
        line(depth, "</$tag>")
    }

    private fun nullElement(name: String, tag: String, path: String, depth: Int) {
        if (!leavesNulls) {
            log.lose(Lost.Nulls, path)
            line(depth, "<$tag/>")
            return
        }
        log.leaveOut(path, lines)
        line(depth, "<!-- ${options.commentFor(name).replace("--", "- -")} -->")
    }

    /** A member of an object, an element; an array's items each an element of the member's name. */
    private fun member(member: DataMember, path: String, depth: Int) {
        val value = member.value
        if (value !is DataArray) return element(member.name, value, path, depth)
        if (value.items.isEmpty()) log.lose(Lost.Emptied, path)
        value.items.forEachIndexed { index, item -> element(member.name, item, itemPath(path, index), depth) }
    }

    private fun attribute(member: DataMember, path: String): String {
        val attributePath = memberPath(path, member.name)
        val name = xmlName(member.name.drop(1), attributePath)
        if (member.value == DataNull) {
            log.lose(Lost.Nulls, attributePath)
            if (leavesNulls) return ""
        }
        return " $name=\"${escapedAttribute(textOf(member.value, attributePath, log))}\""
    }

    private fun textElement(open: String, tag: String, text: String, depth: Int) {
        if (text.isEmpty()) line(depth, "<$open/>") else line(depth, "<$open>${escapedText(text)}</$tag>")
    }

    /** [name] as an XML name: what XML cannot have in one written `_`, told to [log]. */
    private fun xmlName(name: String, path: String): String {
        val written = xmlNameOf(name)
        if (written != name) log.lose(Lost.Renamed, path, written)
        return written
    }

    private fun line(depth: Int, text: String) {
        repeat(depth) { out.append(indent) }
        out.append(text).append('\n')
        lines++
    }

    private companion object {
        const val DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"

        /** What the tree names an element's attributes with, before their names. */
        const val ATTRIBUTE = "@"

        /** What the tree names an element's text. */
        const val TEXT = "#text"

        /** The element a document that is no element is put in. */
        const val ROOT = "root"

        /** The elements an array in an array, which has no name of its own, is written as. */
        const val ITEM = "item"
    }
}

/** [value]'s text, a scalar's; a number's or a truth's told to [log], since XML has only text, and a date's. */
private fun textOf(value: DataValue, path: String, log: ConvertLog): String = when (value) {
    is DataString -> value.value
    is DataNumber -> value.written.also { log.lose(Lost.Untyped, path) }
    is DataBoolean -> "${value.value}".also { log.lose(Lost.Untyped, path) }
    is DataDate -> value.written.also { log.lose(Lost.Dates, path) }
    else -> ""
}

private val DataValue.isScalar: Boolean get() = this !is DataObject && this !is DataArray

/** Whether the member can be a root element: one that is no attribute, no text, and no array of elements. */
private val DataMember.isElement: Boolean get() = !name.startsWith("@") && name != "#text" && value !is DataArray

/** [name] as an XML name: `_` for what cannot stand in one, and before a first character that can only follow. */
internal fun xmlNameOf(name: String): String {
    if (name.isEmpty()) return "_"
    val written = StringBuilder(name.length + 1)
    name.forEachIndexed { index, char ->
        when {
            index == 0 && isNameStart(char) -> written.append(char)
            index == 0 && isNameChar(char) -> written.append('_').append(char)
            index > 0 && isNameChar(char) -> written.append(char)
            else -> written.append('_')
        }
    }
    return written.toString()
}

private fun escapedText(text: String): String = buildString {
    text.forEach { char ->
        when (char) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            else -> append(if (isXmlChar(char)) char else REPLACEMENT)
        }
    }
}

private fun escapedAttribute(text: String): String = buildString {
    text.forEach { char ->
        when (char) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '"' -> append("&quot;")
            '\n' -> append("&#10;")
            '\r' -> append("&#13;")
            '\t' -> append("&#9;")
            else -> append(if (isXmlChar(char)) char else REPLACEMENT)
        }
    }
}

/** What stands for a character XML does not have, a control character. */
private const val REPLACEMENT = '�'
