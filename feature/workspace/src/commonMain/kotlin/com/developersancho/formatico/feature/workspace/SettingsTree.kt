package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.ini.IniDocument
import com.developersancho.formatico.core.format.ini.IniEntry
import com.developersancho.formatico.core.format.json.PathStep
import com.developersancho.formatico.core.format.properties.PropertiesDocument
import com.developersancho.formatico.core.format.text.TextRange

/**
 * Reads [text] as a properties file, for its tree: an object of its keys in their order, each value a string. A dotted
 * key, `server.port`, is one key: a properties file has no levels.
 */
internal fun readProperties(text: String): Reading {
    val document = PropertiesDocument.parse(text)
    val values = KeyedValues(ROOT, emptyList())
    document.entries.forEach { entry ->
        val written = text.substring(entry.valueRange.start, entry.valueRange.end)
        // A value carried on over several lines is shown as it is read, on one.
        val shown = if (written.any { it == '\n' || it == '\r' }) entry.value else written
        values.add(entry.key, shown, entry.value, entry.valueRange.takeIf { it.length > 0 } ?: entry.keyRange)
    }
    val root = values.node(name = ROOT, range = TextRange(0, text.length)).takeIf { document.entries.isNotEmpty() }
    return Reading.Read(format = Format.Properties, root = root, problems = document.problems, comments = document.comments.size)
}

/**
 * Reads [text] as an INI file, for its tree: an object of the keys before the first section and of the sections, each
 * an object of its keys in their order, each value a string. A section given twice is given twice.
 */
internal fun readIni(text: String): Reading {
    val document = IniDocument.parse(text)
    val root = KeyedValues(ROOT, emptyList())
    document.sections.forEach { section ->
        val name = section.name
        if (name == null) {
            section.entries.forEach { root.add(it, text) }
        } else {
            root.add(name) { key, path ->
                val values = KeyedValues(key, path)
                section.entries.forEach { values.add(it, text) }
                values.node(name, section.range)
            }
        }
    }
    val tree = root.node(name = ROOT, range = TextRange(0, text.length)).takeIf { document.sections.isNotEmpty() }
    return Reading.Read(format = Format.Ini, root = tree, problems = document.problems, comments = document.comments.size)
}

/** [entry] of [text] as a string of these values: its value as written, quotes and all, and as read. */
private fun KeyedValues.add(entry: IniEntry, text: String) {
    val range = entry.valueRange.takeIf { it.length > 0 } ?: entry.keyRange
    add(entry.key, text.substring(entry.valueRange.start, entry.valueRange.end), entry.value, range)
}

/**
 * The children of an object of the tree, keyed under [key] at [path], built a key at a time: a key given again gets a
 * key of its own, `#` and its place, as JSON's members do.
 */
private class KeyedValues(private val key: String, private val path: List<PathStep>) {
    private val children = ArrayList<ValueNode>()
    private val seen = HashSet<String>()

    /** Adds a string named [name], written as [text] at [range], whose value is [value]. */
    fun add(name: String, text: String, value: String, range: TextRange) = add(name) { key, path ->
        ValueNode(key, path, name, ValueKind.String, text, value, previewOf(value), emptyList(), range)
    }

    /** Adds the child named [name] that [make] makes with its key and its path. */
    fun add(name: String, make: (key: String, path: List<PathStep>) -> ValueNode) {
        val step = PathStep.Key(name)
        children += make(childKey(key, step, duplicate = children.size.takeUnless { seen.add(name) }), path + step)
    }

    /** The object of the children added, named [name] and written at [range]. */
    fun node(name: String, range: TextRange): ValueNode =
        ValueNode(key, path, name, ValueKind.Object, "{${children.size}}", null, null, children.toList(), range)
}
