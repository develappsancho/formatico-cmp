package com.developersancho.formatico.core.format.yaml

import com.developersancho.formatico.core.format.text.Indentation
import com.developersancho.formatico.core.format.text.LineEnding
import com.developersancho.formatico.core.format.text.TextLayout
import com.developersancho.formatico.core.format.text.TextRange

/**
 * The document written out again in YAML's block style: a key or an item to a line, each level indented by
 * [indentation] (YAML indents with spaces only: tabs are written as two spaces), and every object's keys in their
 * order, or sorted by name where [sortKeys] says so.
 *
 * What is written stays as it was: every scalar's spelling and style, every anchor, alias and tag, a flow collection on
 * one line as it is written, the comments, a blank line between two entries where there was one, and the documents'
 * markers and directives. A block scalar's lines are moved to the new indentation; a scalar or a flow collection
 * written over several lines is written on one. A comment stays with what it is written against: one at the end of a
 * line, at the end of that line; one on a line of its own, before what comes under it. Sorting the keys takes each
 * entry's comments with it.
 *
 * Sorting the keys can put an alias before the anchor it points at, which YAML does not allow: whoever sorts reads the
 * result again to find out.
 *
 * @throws IllegalArgumentException when the document is not valid: there is no value to write out.
 */
fun YamlDocument.formatted(indentation: Indentation = Indentation.Spaces(2), sortKeys: Boolean = false): String {
    require(isValid) { "Only a valid document is written out again" }
    val unit = (indentation as? Indentation.Spaces)?.count ?: DEFAULT_INDENT
    val newline = (TextLayout.of(text).lineEnding ?: LineEnding.Lf).characters
    return YamlFormatter(this, unit, newline, sortKeys).print()
}

private const val DEFAULT_INDENT = 2

/** What a key is sorted by: its value, or its text where it is no scalar. */
private fun sortName(node: YamlNode, text: String): String =
    (node as? YamlScalar)?.value ?: text.substring(node.range.start, node.range.end)

private fun String.onOneLine(from: Int, to: Int): Boolean = (from until to).none {
    this[it] == '\n' || this[it] == '\r'
}

/** How many spaces the line of [offset] starts with. */
private fun String.indentOfLine(offset: Int): Int {
    var start = offset
    while (start > 0 && this[start - 1] != '\n' && this[start - 1] != '\r') start--
    var index = start
    while (index < length && this[index] == ' ') index++
    return index - start
}

/** Whether a line with nothing but spaces on it stands between [from] and [until]. */
private fun String.hasBlankLine(from: Int, until: Int): Boolean {
    var breaks = 0
    for (index in from until until) {
        val char = this[index]
        when {
            char == '\n' -> breaks++
            char == '\r' -> if (index + 1 >= until || this[index + 1] != '\n') breaks++
            !char.isWhitespace() -> breaks = 0
        }
        if (breaks >= 2) return true
    }
    return false
}

/** [line] moved [delta] spaces: right, or left as far as its spaces go. */
private fun shiftLine(line: String, delta: Int): String = if (delta >=
    0
) {
    " ".repeat(delta) + line
} else {
    line.drop(minOf(-delta, line.indexOfFirst { it != ' ' }.coerceAtLeast(0)))
}

/** [raw]'s lines after its first moved [delta] spaces, joined by [newline]. */
private fun shifted(raw: String, delta: Int, newline: String): String =
    raw.lines().mapIndexed { index, line -> if (index == 0 || line.isBlank()) line else shiftLine(line, delta) }
        .joinToString(newline)

/** [first] and [second] with a space between them, either left out where it is empty. */
private fun spaced(first: String, second: String): String =
    listOf(first, second).filter { it.isNotEmpty() }.joinToString(" ")

private fun StringBuilder.indent(columns: Int) {
    repeat(columns) { append(' ') }
}

/** A document's comments, each written once: at the end of a line, before an entry, or at the end of the document. */
private class CommentBook(private val text: String, private val ranges: List<TextRange>) {
    private val used = BooleanArray(ranges.size)

    /** The comments not yet written that start at [from] or after it and before [until]. */
    fun between(from: Int, until: Int): List<Int> =
        ranges.indices.filter { !used[it] && ranges[it].start >= from && ranges[it].start < until }

    /** Takes the comment not yet written after [from] on the same line, if there is one. */
    fun sameLine(from: Int): TextRange? {
        val index =
            ranges.indices.firstOrNull {
                !used[it] && ranges[it].start >= from && text.onOneLine(from, ranges[it].start)
            }
                ?: return null
        used[index] = true
        return ranges[index]
    }

    /**
     * The comments before each of a collection's entries, by where the entries start and end: those after the entry
     * before it that are not on its line, and before the first, those after [regionStart].
     */
    fun leading(starts: List<Int>, ends: List<Int>, regionStart: Int): List<List<Int>> = starts.indices.map { index ->
        val from = if (index == 0) regionStart else ends[index - 1]
        between(from, starts[index]).filter { index == 0 || !text.onOneLine(ends[index - 1], ranges[it].start) }
    }

    /** Where the first comment of [indices] starts, or null for none. */
    fun firstStart(indices: List<Int>): Int? = indices.firstOrNull()?.let { ranges[it].start }

    /** Takes [indices], to be written as they are written in a flow collection. */
    fun take(indices: List<Int>) {
        indices.forEach { used[it] = true }
    }

    /** Writes the comments of [indices] not yet written, each on a line of its own at [columns]. */
    fun write(indices: List<Int>, out: StringBuilder, columns: Int, newline: String) {
        indices.filterNot { used[it] }.forEach { index ->
            used[index] = true
            out.indent(columns)
            out.append(text, ranges[index].start, ranges[index].end).append(newline)
        }
    }

    /** The comments not written yet. */
    fun remaining(): List<Int> = ranges.indices.filterNot { used[it] }
}

/**
 * Writes a valid document out in block style, as [formatted] says.
 *
 * @param unit how many spaces a level is indented.
 * @param newline the line ending.
 */
private class YamlFormatter(
    private val document: YamlDocument,
    private val unit: Int,
    private val newline: String,
    private val sortKeys: Boolean
) {
    private val text = document.text
    private val comments = CommentBook(text, document.comments)
    private val out = StringBuilder(text.length + text.length / 2)

    fun print(): String {
        var regionStart = 0
        document.documents.forEachIndexed { index, root ->
            val start =
                document.starts.getOrNull(index) ?: YamlDocumentStart(explicit = false, directives = emptyList())
            start.directives.forEach { out.append(it).append(newline) }
            if (index > 0 || start.explicit || start.directives.isNotEmpty()) out.append(DOCUMENT_START).append(newline)
            if (root != null) {
                // The comments before a block collection's first entry go with that entry, and move with it.
                if (!isBlockCollection(root) || root.anchor != null || root.tag != null) {
                    comments.write(comments.between(regionStart, root.start), out, columns = 0, newline = newline)
                }
                root(root, regionStart)
                regionStart = root.range.end
            }
        }
        comments.write(comments.remaining(), out, columns = 0, newline = newline)
        return out.toString()
    }

    /** Writes a document's value. */
    private fun root(root: YamlNode, regionStart: Int) {
        val properties = propertiesOf(root)
        when {
            isBlockCollection(root) -> {
                if (properties.isNotEmpty()) {
                    out.append(properties)
                    endLine(root.start)
                }
                if (root is YamlMapping) mapping(root, depth = 0, inlineFirst = false, regionStart = regionStart)
                if (root is YamlSequence) sequence(root, depth = 0, inlineFirst = false, regionStart = regionStart)
            }

            root is YamlScalar && root.isBlock -> {
                if (properties.isNotEmpty()) out.append(properties).append(' ')
                blockScalar(root, lineIndent = 0)
            }

            else -> {
                out.append(spaced(properties, inline(root, lineIndent = 0)))
                endLine(root.range.end)
            }
        }
    }

    /**
     * Writes a block mapping's entries at [depth], each with the comments before it; the first on the line already
     * begun where [inlineFirst] says so, after a sequence's dash.
     */
    private fun mapping(node: YamlMapping, depth: Int, inlineFirst: Boolean, regionStart: Int) {
        val entries = node.entries
        val leading = comments.leading(entries.map { it.key.start }, entries.map { it.value.range.end }, regionStart)
        val order = if (sortKeys) {
            entries.indices.sortedBy {
                sortName(entries[it].key, text)
            }
        } else {
            entries.indices.toList()
        }
        order.forEachIndexed { position, index ->
            val entry = entries[index]
            if (position > 0 || !inlineFirst) {
                if (index > 0) blankLine(entries[index - 1].value, leading[index], entry.key.start)
                comments.write(leading[index], out, depth * unit, newline)
                out.indent(depth * unit)
            }
            out.append(key(entry.key)).append(':')
            value(entry.value, depth, headerEnd = entry.key.range.end, afterDash = false)
        }
    }

    /** Writes a block sequence's items at [depth], each after its dash. */
    private fun sequence(node: YamlSequence, depth: Int, inlineFirst: Boolean, regionStart: Int) {
        val items = node.items
        val leading = comments.leading(items.map { it.start }, items.map { it.range.end }, regionStart)
        items.forEachIndexed { index, item ->
            if (index > 0 || !inlineFirst) {
                if (index > 0) blankLine(items[index - 1], leading[index], item.start)
                comments.write(leading[index], out, depth * unit, newline)
                out.indent(depth * unit)
            }
            out.append('-')
            value(item, depth, headerEnd = item.start, afterDash = true)
        }
    }

    /**
     * Writes [node] after its key's colon, or after a sequence's dash where [afterDash] says so, the key or the dash
     * standing at [depth] and its line's own text ending at [headerEnd]. After a dash, a mapping or a sequence with no
     * anchor and no tag starts on the dash's line, its later lines lined up with its first.
     */
    private fun value(node: YamlNode, depth: Int, headerEnd: Int, afterDash: Boolean) {
        val properties = propertiesOf(node)
        when {
            isBlockCollection(node) -> {
                val inlineFirst = afterDash && properties.isEmpty()
                if (inlineFirst) {
                    out.append(" ".repeat(unit - 1))
                } else {
                    if (properties.isNotEmpty()) out.append(' ').append(properties)
                    endLine(headerEnd)
                }
                val regionStart = if (inlineFirst) node.start else headerEnd
                if (node is YamlMapping) mapping(node, depth + 1, inlineFirst, regionStart)
                if (node is YamlSequence) sequence(node, depth + 1, inlineFirst, regionStart)
            }

            node is YamlScalar && node.isBlock -> {
                out.append(' ')
                if (properties.isNotEmpty()) out.append(properties).append(' ')
                blockScalar(node, lineIndent = depth * unit)
            }

            else -> {
                val written = listOf(properties, inline(node, depth * unit)).filter {
                    it.isNotEmpty()
                }.joinToString(" ")
                if (written.isNotEmpty()) out.append(' ').append(written)
                endLine(node.range.end)
            }
        }
    }

    /** A key as it is written, with its anchor and its tag; a key that is a block collection, in flow style. */
    private fun key(node: YamlNode): String {
        val properties = propertiesOf(node)
        val written = if (isBlockCollection(
                node
            )
        ) {
            flow(node, text).removePrefix(properties).trimStart()
        } else {
            inline(node, 0)
        }
        return spaced(properties, written)
    }

    /**
     * A value that stands on its key's line, its anchor and its tag left out: a scalar or a flow collection as it is
     * written where it is on one line, or else on one; a flow collection with comments in it as it is written, its
     * later lines moved to [lineIndent] as its first line is.
     */
    private fun inline(node: YamlNode, lineIndent: Int): String = when (node) {
        is YamlAlias -> "*${node.name}"

        is YamlScalar -> when {
            node.raw.none { it == '\n' || it == '\r' } -> node.raw
            node.style == YamlScalarStyle.Plain -> node.value
            else -> doubleQuoted(node.value)
        }

        else -> {
            val raw = text.substring(node.range.start, node.range.end)
            val inside = comments.between(node.range.start, node.range.end)
            when {
                inside.isNotEmpty() -> {
                    comments.take(inside)
                    shifted(raw, lineIndent - text.indentOfLine(node.range.start), newline)
                }

                raw.none { it == '\n' || it == '\r' } -> raw

                else -> flow(node, text).removePrefix(propertiesOf(node)).trimStart()
            }
        }
    }

    /**
     * Writes a block scalar: its indicator line as it is written, then its lines, moved so that they stand a level
     * under [lineIndent], the indentation of the line it starts on. An explicit indentation is kept as it is.
     */
    private fun blockScalar(node: YamlScalar, lineIndent: Int) {
        val lines = node.raw.lines()
        val header = lines.first().trimEnd()
        out.append(header).append(newline)
        val content = lines.drop(1)
        val explicit = header.any { it.isDigit() }
        val firstIndent = content.firstOrNull { it.isNotBlank() }?.let { line -> line.indexOfFirst { it != ' ' } } ?: 0
        val delta = if (explicit) lineIndent - text.indentOfLine(node.range.start) else lineIndent + unit - firstIndent
        content.forEach { line -> out.append(if (line.isBlank()) "" else shiftLine(line, delta)).append(newline) }
        if (header.contains('+')) {
            val kept = node.value.takeLastWhile { it == '\n' }.length
            repeat(maxOf(0, kept - 1)) { out.append(newline) }
        }
    }

    /** Ends the line: the comment written at its end, after [from] on the same line, if there is one, and a break. */
    private fun endLine(from: Int) {
        comments.sameLine(from)?.let { out.append(' ').append(text, it.start, it.end) }
        out.append(newline)
    }

    /**
     * Writes a blank line where the text has one between the value before an entry, [previous], and the comments
     * before the entry, or the entry. The blank lines after a block scalar that keeps them (`|+`) are its own.
     */
    private fun blankLine(previous: YamlNode, leading: List<Int>, start: Int) {
        if (previous is YamlScalar && previous.isBlock && previous.raw.lineSequence().first().contains('+')) return
        val until = comments.firstStart(leading) ?: start
        val from = previous.range.end
        if (from < until && text.hasBlankLine(from, until)) out.append(newline)
    }
}
