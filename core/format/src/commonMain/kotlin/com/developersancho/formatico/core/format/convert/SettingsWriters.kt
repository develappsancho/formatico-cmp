package com.developersancho.formatico.core.format.convert

/**
 * [value] as a properties file: keys of one level, each named by its path, `customer.name`, `items[0].sku`, as Spring
 * names them; an array or any other value that is no object put under a key of its own. A properties file has only
 * text, and no null: one is left out, a comment marking where it was, or written as an empty value, as [options] say.
 * What it cannot write as it is goes to [log].
 */
internal fun propertiesDocument(value: DataValue, options: ConvertOptions, log: ConvertLog): String {
    val leaves = uniqueLeaves(levelOf(value, log), log)
    return buildString {
        leaves.forEachIndexed { line, leaf ->
            if (leaf.value == DataNull && options.nulls == NullHandling.LeaveOut) {
                log.leaveOut(leaf.path, line)
                append("# ").append(options.commentFor(leaf.name).lineSafe()).append('\n')
            } else {
                append(propertiesKey(leaf.name)).append('=').append(propertiesValue(textOf(leaf, log))).append('\n')
            }
        }
    }
}

/** The values of a document of one level: an object's members, or [value] under a key of its own. */
private fun levelOf(value: DataValue, log: ConvertLog): List<FlatLeaf> = when (value) {
    is DataObject -> value.members.flatMap { flatten(it.value, it.name, it.name, log) }

    is DataArray -> {
        log.wrap(ARRAY_KEY)
        value.items.flatMapIndexed { index, item -> flatten(item, itemPath(ARRAY_KEY, index), itemPath("", index), log, nested = true) }
    }

    else -> {
        log.wrap(VALUE_KEY)
        listOf(FlatLeaf(VALUE_KEY, "", value))
    }
}

/**
 * [value] as an INI file: the keys of the document's own first, then a section for each object in it, `[customer]`,
 * holding its keys; a value inside one, and an array, keys of one level named by their paths. An INI file has only
 * text, and no null: one is left out, a comment marking where it was, or written as an empty value, as [options] say.
 * What it cannot write as it is goes to [log].
 */
internal fun iniDocument(value: DataValue, options: ConvertOptions, log: ConvertLog): String {
    val root = value as? DataObject
    val sections = root?.members?.filter { it.value is DataObject }.orEmpty()
    val own = root?.members?.filter { it.value !is DataObject }?.flatMap { flatten(it.value, it.name, it.name, log) }
        ?: levelOf(value, log)
    var afterSection = false
    root?.members?.forEach { member ->
        if (member.value is DataObject) {
            afterSection = true
        } else if (afterSection) {
            log.lose(Lost.Moved, member.name)
        }
    }
    val writer = IniLines(options, log)
    uniqueLeaves(own, log).forEach(writer::entry)
    sections.forEach { section ->
        writer.header(iniSection(section.name, log))
        val members = (section.value as DataObject).members
        val leaves = members.flatMap { flatten(it.value, it.name, memberPath(section.name, it.name), log) }
        uniqueLeaves(leaves, log).forEach(writer::entry)
    }
    return writer.text()
}

/** The lines of an INI file, written a header or an entry at a time. */
private class IniLines(private val options: ConvertOptions, private val log: ConvertLog) {
    private val out = StringBuilder()
    private var lines = 0

    fun header(name: String) {
        if (out.isNotEmpty()) {
            out.append('\n')
            lines++
        }
        out.append('[').append(name).append("]\n")
        lines++
    }

    fun entry(leaf: FlatLeaf) {
        lines++
        if (leaf.value == DataNull && options.nulls == NullHandling.LeaveOut) {
            log.leaveOut(leaf.path, lines - 1)
            out.append("; ").append(options.commentFor(leaf.name).lineSafe()).append('\n')
            return
        }
        val key = iniKey(leaf.name)
        if (key != leaf.name) log.lose(Lost.Renamed, leaf.path, key)
        val value = textOf(leaf, log)
        out.append(key).append(" =")
        if (value.isNotEmpty()) out.append(' ').append(iniValue(value, leaf.path))
        out.append('\n')
    }

    fun text(): String = out.toString()

    /** [value] on one line, its line breaks written `\n`, and in quotes where a reader would take its edges off. */
    private fun iniValue(value: String, path: String): String {
        val written = value.replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "\\n")
        if (written != value) log.lose(Lost.OneLine, path)
        val quoted = written.first().isWhitespace() ||
            written.last().isWhitespace() ||
            (written.length >= 2 && written.first() in QUOTES && written.last() == written.first())
        return if (quoted) "\"$written\"" else written
    }
}

/** [name] as an INI key, which has no escapes: a separator in it, or a comment's or a header's mark first, as `_`. */
private fun iniKey(name: String): String {
    val parted = name.replace('=', '_').replace(':', '_')
    return if (parted.firstOrNull()?.let { it == ';' || it == '#' || it == '[' } == true) "_" + parted.drop(1) else parted.ifEmpty { "_" }
}

/** [name] as a section's name, which cannot hold `]`: as `_`, told to [log]. */
private fun iniSection(name: String, log: ConvertLog): String {
    val written = name.replace(']', '_').lineSafe()
    if (written != name) log.lose(Lost.Renamed, name, written)
    return written
}

/** [key] as a properties key: what would end it, or start a comment, escaped. */
private fun propertiesKey(key: String): String = buildString {
    key.forEachIndexed { index, char ->
        when (char) {
            ' ', '=', ':' -> append('\\').append(char)
            '#', '!' -> if (index == 0) append('\\').append(char) else append(char)
            else -> appendEscaped(char)
        }
    }
}

/** [value] as a properties value: a backslash, a line break and a first space escaped. */
private fun propertiesValue(value: String): String = buildString {
    value.forEachIndexed { index, char ->
        if (index == 0 && char == ' ') append("\\ ") else appendEscaped(char)
    }
}

private fun StringBuilder.appendEscaped(char: Char) {
    when (char) {
        '\\' -> append("\\\\")
        '\n' -> append("\\n")
        '\r' -> append("\\r")
        '\t' -> append("\\t")
        '\u000C' -> append("\\f")
        else -> append(char)
    }
}

/** The text with its line breaks as spaces, for a comment or a header that ends at the line's end. */
private fun String.lineSafe(): String = replace('\n', ' ').replace('\r', ' ')

/** The key an array is put under, as a document of one level is keys. */
private const val ARRAY_KEY = "items"

/** The key any other value that is no object is put under. */
private const val VALUE_KEY = "value"

private const val QUOTES = "\"'"
