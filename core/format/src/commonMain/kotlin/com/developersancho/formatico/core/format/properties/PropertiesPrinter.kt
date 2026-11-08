package com.developersancho.formatico.core.format.properties

import com.developersancho.formatico.core.format.linesOf
import com.developersancho.formatico.core.format.text.LineEnding
import com.developersancho.formatico.core.format.text.TextLayout
import com.developersancho.formatico.core.format.text.TextRange

/**
 * The document written out again: each comment and each entry on a line, unindented, every key and value between the
 * separator the first entry is written with, `=`, ` = `, `:`, `: ` or a space; a value carried on over several lines
 * keeps its lines, each that goes on indented by four spaces. Runs of blank lines become one, and none is left at the
 * start or the end. Where [sortKeys] says so, the entries are sorted by key, each with the comments right above it,
 * and the comments before the first blank line stay at the top.
 *
 * The lines end as the document's first line does, and the text ends with a line break where the document's did.
 *
 * @throws IllegalArgumentException when the document is not valid.
 */
fun PropertiesDocument.formatted(sortKeys: Boolean = false): String {
    require(isValid) { "Only a valid document is written out again" }
    val separator = entries.firstOrNull()?.let { separatorOf(it) } ?: "="
    val lines = PropertiesLines(this).read()
    val written = if (sortKeys) sorted(lines) else lines
    return joined(written.flatMap { it.written(this, separator) })
}

/**
 * The document with no comment, no blank line and no room: an entry to a line, `key=value`, each value on one line.
 *
 * @throws IllegalArgumentException when the document is not valid.
 */
fun PropertiesDocument.minified(): String {
    require(isValid) { "Only a valid document is written out again" }
    return joined(entries.map { text.substring(it.keyRange.start, it.keyRange.end) + "=" + it.joined })
}

/** A line of the document as it is written out: a comment, a blank line, or an entry over its lines. */
private sealed interface PropertiesLine {
    /** The lines it is written as, with [separator] between a key and its value. */
    fun written(document: PropertiesDocument, separator: String): List<String>

    class Comment(val range: TextRange) : PropertiesLine {
        override fun written(document: PropertiesDocument, separator: String) =
            listOf(document.text.substring(range.start, range.end).trimEnd())
    }

    data object Blank : PropertiesLine {
        override fun written(document: PropertiesDocument, separator: String) = listOf("")
    }

    class Entry(val entry: PropertiesEntry) : PropertiesLine {
        override fun written(document: PropertiesDocument, separator: String): List<String> {
            val text = document.text
            val key = text.substring(entry.keyRange.start, entry.keyRange.end)
            if (entry.valueRange.length == 0) return listOf(key + separator.trimEnd())
            val value = linesOf(text.substring(entry.valueRange.start, entry.valueRange.end))
                .map { text.substring(entry.valueRange.start + it.start, entry.valueRange.start + it.end) }
            return listOf(key + separator + value.first()) + value.drop(1).map { CONTINUED + it.trimStart() }
        }
    }
}

/** Reads a document's lines in order, its blank ones among them. */
private class PropertiesLines(private val document: PropertiesDocument) {
    fun read(): List<PropertiesLine> {
        val text = document.text
        val comments = document.comments.associateBy { lineOf(it.start) }
        val entries = document.entries.associateBy { lineOf(it.range.start) }
        val lines = ArrayList<PropertiesLine>()
        var line = 0
        val all = linesOf(text)
        while (line < all.size) {
            val entry = entries[line]
            when {
                entry != null -> {
                    lines += PropertiesLine.Entry(entry)
                    line = lineOf(entry.range.end) + 1
                }

                comments[line] != null -> {
                    lines += PropertiesLine.Comment(requireNotNull(comments[line]))
                    line++
                }

                else -> {
                    if (lines.isNotEmpty() && lines.last() != PropertiesLine.Blank) lines += PropertiesLine.Blank
                    line++
                }
            }
        }
        while (lines.lastOrNull() == PropertiesLine.Blank) lines.removeAt(lines.lastIndex)
        return lines
    }

    private val starts = linesOf(document.text).map { it.start }

    /** The index of the line [offset] is on. */
    private fun lineOf(offset: Int): Int {
        val found = starts.binarySearch(offset)
        return if (found >= 0) found else -found - 2
    }
}

/**
 * [lines] with the entries sorted by key, each with the comments right above it; the comments before the first blank
 * line stay first, and a blank line after them.
 */
private fun sorted(lines: List<PropertiesLine>): List<PropertiesLine> {
    val firstEntry = lines.indexOfFirst { it is PropertiesLine.Entry }
    if (firstEntry < 0) return lines
    val blankBefore = lines.subList(0, firstEntry).lastIndexOf(PropertiesLine.Blank)
    val top = if (blankBefore < 0) emptyList() else lines.subList(0, blankBefore + 1)
    val groups = ArrayList<List<PropertiesLine>>()
    var comments = ArrayList<PropertiesLine>()
    lines.drop(top.size).forEach { line ->
        when (line) {
            is PropertiesLine.Comment -> comments += line

            is PropertiesLine.Entry -> {
                groups += comments + line
                comments = ArrayList()
            }

            PropertiesLine.Blank -> {}
        }
    }
    val sorted = groups.sortedBy { (it.last() as PropertiesLine.Entry).entry.key }.flatten()
    return top + sorted + comments
}

/** [lines] a line each, ended as [PropertiesDocument.text]'s lines are, and as it is. */
private fun PropertiesDocument.joined(lines: List<String>): String {
    val newline = (TextLayout.of(text).lineEnding ?: LineEnding.Lf).characters
    val ending = if (lines.isNotEmpty() && (text.endsWith('\n') || text.endsWith('\r'))) newline else ""
    return lines.joinToString(newline) + ending
}

/** The separator [entry] is written with, as one of those Format writes: its room kept on each side, once. */
private fun PropertiesDocument.separatorOf(entry: PropertiesEntry): String {
    val between = text.substring(entry.keyRange.end, entry.valueRange.start)
    val mark = between.firstOrNull { it == '=' || it == ':' } ?: return " "
    val before = if (between.substringBefore(mark).isNotEmpty()) " " else ""
    val spacedAfter = between.substringAfter(mark).isNotEmpty() || (entry.valueRange.length == 0 && before.isNotEmpty())
    val after = if (spacedAfter) " " else ""
    return before + mark + after
}

/** How a line that goes on a value is indented. */
private const val CONTINUED = "    "
