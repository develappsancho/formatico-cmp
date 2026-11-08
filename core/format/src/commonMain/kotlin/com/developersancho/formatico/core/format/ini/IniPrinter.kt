package com.developersancho.formatico.core.format.ini

import com.developersancho.formatico.core.format.linesOf
import com.developersancho.formatico.core.format.text.LineEnding
import com.developersancho.formatico.core.format.text.TextLayout
import com.developersancho.formatico.core.format.text.TextRange

/**
 * The document written out again: every line unindented, a key and its value as `key = value`, a header as
 * `[name]`, comments where they stand. One blank line goes before each header, and before the comments right above
 * it; other runs of blank lines become one, and none is left at the start or the end. Where [sortKeys] says so, the
 * keys of each section are sorted, each with the comments right above it; the sections keep their order.
 *
 * The lines end as the document's first line does, and the text ends with a line break where the document's did.
 *
 * @throws IllegalArgumentException when the document is not valid.
 */
fun IniDocument.formatted(sortKeys: Boolean = false): String {
    require(isValid) { "Only a valid document is written out again" }
    val lines = ArrayList<String>()
    IniBlocks(this).read().forEach { block ->
        if (lines.isNotEmpty() && block.header != null) lines += ""
        block.lead.forEach { lines += comment(it) }
        block.header?.let { lines += header(it) }
        val body = if (sortKeys) sorted(block.body) else block.body
        body.forEach { line ->
            when (line) {
                is IniLine.Comment -> lines += comment(line.range)
                is IniLine.Entry -> lines += entry(line.entry, spaced = true)
                IniLine.Blank -> lines += ""
            }
        }
    }
    return joined(lines)
}

/**
 * The document with no comment, no blank line and no room: each header as `[name]`, each key as `key=value`.
 *
 * @throws IllegalArgumentException when the document is not valid.
 */
fun IniDocument.minified(): String {
    require(isValid) { "Only a valid document is written out again" }
    val lines = sections.flatMap { section ->
        listOfNotNull(section.headerRange?.let { "[${section.name}]" }) + section.entries.map { entry(it, spaced = false) }
    }
    return joined(lines)
}

/** A line of a section as it is written out. */
private sealed interface IniLine {
    class Comment(val range: TextRange) : IniLine

    class Entry(val entry: IniEntry) : IniLine

    data object Blank : IniLine
}

/**
 * A section as it is written out: the comments right above its header, its header, and what follows it up to the
 * next section's.
 */
private class IniBlock(val lead: List<TextRange>, val header: IniSection?, val body: List<IniLine>)

/** Reads a document's lines into blocks, a section each. */
private class IniBlocks(private val document: IniDocument) {
    fun read(): List<IniBlock> {
        val headers = document.sections.filter { it.headerRange != null }.associateBy { requireNotNull(it.headerRange).start }
        val entries = document.sections.flatMap { it.entries }.associateBy { it.range.start }
        val comments = document.comments.associateBy { it.start }
        val blocks = ArrayList<IniBlock>()
        var header: IniSection? = null
        var lead: List<TextRange> = emptyList()
        var body = ArrayList<IniLine>()
        linesOf(document.text).forEach { line ->
            val start = (line.start until line.end).firstOrNull { !document.text[it].isWhitespace() }
            val section = start?.let { headers[it] }
            when {
                start == null -> body += IniLine.Blank

                section != null -> {
                    val above = body.takeLastWhile { it is IniLine.Comment }
                    blocks += IniBlock(lead, header, trimmed(body.dropLast(above.size)))
                    lead = above.map { (it as IniLine.Comment).range }
                    header = section
                    body = ArrayList()
                }

                entries[start] != null -> body += IniLine.Entry(requireNotNull(entries[start]))

                else -> body += IniLine.Comment(requireNotNull(comments[start]))
            }
        }
        blocks += IniBlock(lead, header, trimmed(body))
        return blocks.filter { it.header != null || it.body.isNotEmpty() || it.lead.isNotEmpty() }
    }

    /** [lines] with no blank line at the start or the end, and no two together. */
    private fun trimmed(lines: List<IniLine>): List<IniLine> = lines
        .filterIndexed { index, line -> line != IniLine.Blank || (index > 0 && lines[index - 1] != IniLine.Blank) }
        .dropWhile { it == IniLine.Blank }
        .dropLastWhile { it == IniLine.Blank }
}

/** [body] with its keys sorted, each with the comments right above it; the comments after the last key stay last. */
private fun sorted(body: List<IniLine>): List<IniLine> {
    val groups = ArrayList<List<IniLine>>()
    var comments = ArrayList<IniLine>()
    body.forEach { line ->
        when (line) {
            is IniLine.Comment -> comments += line

            is IniLine.Entry -> {
                groups += comments + line
                comments = ArrayList()
            }

            IniLine.Blank -> {}
        }
    }
    return groups.sortedBy { (it.last() as IniLine.Entry).entry.key }.flatten() + comments
}

private fun IniDocument.comment(range: TextRange): String = text.substring(range.start, range.end).trimEnd()

/** [section]'s header as `[name]`, and what follows its `]` on the line, a comment, after a space. */
private fun IniDocument.header(section: IniSection): String {
    val range = requireNotNull(section.headerRange)
    val lineEnd = text.indexOfAny(charArrayOf('\n', '\r'), range.end).let { if (it < 0) text.length else it }
    val after = text.substring(range.end, lineEnd).trim()
    return "[${section.name}]" + if (after.isEmpty()) "" else " $after"
}

/** [entry] as `key = value`, or `key=value` where it is not [spaced]; a key alone stays alone. */
private fun IniDocument.entry(entry: IniEntry, spaced: Boolean): String {
    val value = text.substring(entry.valueRange.start, entry.valueRange.end)
    return when {
        !entry.separated -> entry.key
        !spaced -> "${entry.key}=$value"
        value.isEmpty() -> "${entry.key} ="
        else -> "${entry.key} = $value"
    }
}

/** [lines] a line each, ended as [IniDocument.text]'s lines are, and as it is. */
private fun IniDocument.joined(lines: List<String>): String {
    val newline = (TextLayout.of(text).lineEnding ?: LineEnding.Lf).characters
    val ending = if (lines.isNotEmpty() && (text.endsWith('\n') || text.endsWith('\r'))) newline else ""
    return lines.joinToString(newline) + ending
}
