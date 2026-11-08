package com.developersancho.formatico.core.format.toml

import com.developersancho.formatico.core.format.text.TextRange

/**
 * A line written out: a statement with the comment after it on its line, or a comment on a line of its own.
 *
 * @property blankBefore whether a blank line stands before it in the document.
 * @property opensGroup whether it opens a header's group, the header and the comments right above it: a blank line
 * goes before it.
 */
private data class Line(
    val statement: TomlStatement?,
    val comment: TextRange?,
    val blankBefore: Boolean,
    val opensGroup: Boolean = false
) {
    val isHeader: Boolean get() = statement is TomlHeaderStatement
}

/**
 * Writes a valid document out again, a statement or a comment to a line, as [formatted] describes.
 *
 * @param indent one level of indentation, for the values of an array over several lines.
 * @param newline the line ending.
 */
internal class TomlFormatter(
    private val document: TomlDocument,
    indent: String,
    private val newline: String,
    private val sortKeys: Boolean
) {
    private val text = document.text
    private val values = TomlValueWriter(text, document.comments, indent, newline, sortKeys)

    /** The document written out. */
    fun print(): String {
        val grouped = grouped(lines())
        val lines = if (sortKeys) sorted(grouped) else grouped
        val out = StringBuilder(text.length)
        lines.forEachIndexed { index, line ->
            if (index > 0) {
                out.append(newline)
                if (line.blankBefore || line.opensGroup) out.append(newline)
            }
            write(out, line)
        }
        if (lines.isNotEmpty() && text.endsWithLineBreak()) out.append(newline)
        return out.toString()
    }

    /** The document's lines: its statements, each with the comment after it on its line, and its comments on their own. */
    private fun lines(): List<Line> {
        val comments = document.comments
        val lines = ArrayList<Line>()
        var next = 0
        var previousEnd = 0
        for (statement in document.statements) {
            while (next < comments.size && comments[next].start < statement.range.start) {
                lines += Line(statement = null, comments[next], text.hasBlankLine(previousEnd, comments[next].start))
                previousEnd = comments[next++].end
            }
            // A comment inside a statement is part of the array it is written in.
            while (next < comments.size && comments[next].start < statement.range.end) next++
            val after = comments.getOrNull(next)?.takeIf { text.onOneLine(statement.range.end, it.start) }
            if (after != null) next++
            lines += Line(statement, after, text.hasBlankLine(previousEnd, statement.range.start))
            previousEnd = after?.end ?: statement.range.end
        }
        for (comment in comments.subList(next, comments.size)) {
            lines += Line(statement = null, comment, text.hasBlankLine(previousEnd, comment.start))
            previousEnd = comment.end
        }
        return lines
    }

    /** [lines] with each header's group opened: by the first of the comment lines right above it, or by the header. */
    private fun grouped(lines: List<Line>): List<Line> {
        val opens = BooleanArray(lines.size)
        lines.forEachIndexed { index, line ->
            if (line.isHeader) {
                var first = index
                while (first > 0 && lines[first - 1].statement == null && !lines[first].blankBefore) first--
                opens[first] = true
            }
        }
        return lines.mapIndexed { index, line -> if (opens[index]) line.copy(opensGroup = true) else line }
    }

    /** [lines] with the key/values of each table, between two headers' groups, sorted by key. */
    private fun sorted(lines: List<Line>): List<Line> {
        val sorted = ArrayList<Line>(lines.size)
        var start = 0
        while (start < lines.size) {
            // A table's lines: its header's group, if it has one, and the lines up to the next group.
            val bodyStart = if (lines[start].opensGroup) (start until lines.size).first { lines[it].isHeader } + 1 else start
            var end = bodyStart
            while (end < lines.size && !lines[end].opensGroup) end++
            sorted += lines.subList(start, bodyStart)
            sorted += sortedBody(lines.subList(bodyStart, end))
            start = end
        }
        return sorted
    }

    /**
     * The key/values of [body] sorted by key, each with the comment lines above it since the one before, and no blank
     * line between them; the comment lines after the last stay at the end.
     */
    private fun sortedBody(body: List<Line>): List<Line> {
        val blocks = ArrayList<List<Line>>()
        var pending = ArrayList<Line>()
        body.forEach { line ->
            pending += line
            if (line.statement != null) {
                blocks += pending
                pending = ArrayList()
            }
        }
        return blocks
            .sortedWith(compareBy(TomlKeyOrder) { block -> (block.last().statement as TomlKeyValueStatement).pair.key })
            .flatMap { block -> listOf(block.first().copy(blankBefore = false)) + block.drop(1) } + pending
    }

    /** Writes [line]: its statement, and its comment a space after it. */
    private fun write(out: StringBuilder, line: Line) {
        line.statement?.let { values.statement(out, it) }
        line.comment?.let { comment ->
            if (line.statement != null) out.append(' ')
            out.append(text, comment.start, comment.end)
        }
    }
}

/**
 * Writes the statements of a valid document and their values out again, as [formatted] describes.
 *
 * @property comments the document's comments, in order: an array with one in it is written as it is.
 */
private class TomlValueWriter(
    private val text: String,
    private val comments: List<TextRange>,
    private val indent: String,
    private val newline: String,
    private val sortKeys: Boolean
) {
    /** Writes [statement]: a header, or a key, ` = ` and its value. */
    fun statement(out: StringBuilder, statement: TomlStatement) {
        when (statement) {
            is TomlHeaderStatement -> {
                out.appendHeader(statement)
            }

            is TomlKeyValueStatement -> {
                out.appendKey(statement.pair.key).append(" = ")
                value(out, statement.pair.value, depth = 0)
            }
        }
    }

    /** Writes [value], whose line is indented [depth] levels. */
    private fun value(out: StringBuilder, value: TomlValueSyntax, depth: Int) {
        when (value) {
            is TomlScalarSyntax -> out.append(text, value.range.start, value.range.end)
            is TomlArraySyntax -> array(out, value, depth)
            is TomlInlineTableSyntax -> inlineTable(out, value, depth)
        }
    }

    /** Writes [array]: as it is where a comment is in it, else on one line or a value to a line, as it was written. */
    private fun array(out: StringBuilder, array: TomlArraySyntax, depth: Int) {
        when {
            hasComment(array.range) -> {
                out.append(text, array.range.start, array.range.end)
            }

            array.items.isEmpty() -> {
                out.append("[]")
            }

            isOverLines(array) -> {
                overLines(out, array, depth)
            }

            else -> {
                out.append('[')
                array.items.forEachIndexed { index, item ->
                    if (index > 0) out.append(", ")
                    value(out, item, depth)
                }
                out.append(']')
            }
        }
    }

    /** Writes [array] a value to a line, each indented a level deeper than the array's line. */
    private fun overLines(out: StringBuilder, array: TomlArraySyntax, depth: Int) {
        out.append('[').append(newline)
        array.items.forEachIndexed { index, item ->
            repeat(depth + 1) { out.append(indent) }
            value(out, item, depth + 1)
            if (index < array.items.lastIndex || array.trailingComma) out.append(',')
            out.append(newline)
        }
        repeat(depth) { out.append(indent) }
        out.append(']')
    }

    /** Writes [table] as `{ a = 1, b = 2 }`, its keys sorted where the document's are. */
    private fun inlineTable(out: StringBuilder, table: TomlInlineTableSyntax, depth: Int) {
        if (table.pairs.isEmpty()) {
            out.append("{}")
            return
        }
        val pairs = if (sortKeys) table.pairs.sortedWith(compareBy(TomlKeyOrder) { it.key }) else table.pairs
        out.append("{ ")
        pairs.forEachIndexed { index, pair ->
            if (index > 0) out.append(", ")
            out.appendKey(pair.key).append(" = ")
            value(out, pair.value, depth)
        }
        out.append(" }")
    }

    /** Whether a line break stands in [array] outside its values: between its brackets, its values and its commas. */
    private fun isOverLines(array: TomlArraySyntax): Boolean {
        var from = array.range.start
        array.items.forEach { item ->
            if (text.hasLineBreak(from, item.range.start)) return true
            from = item.range.end
        }
        return text.hasLineBreak(from, array.range.end)
    }

    /** Whether a comment starts inside [range]. */
    private fun hasComment(range: TextRange): Boolean {
        var low = 0
        var high = comments.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (comments[middle].start < range.start) low = middle + 1 else high = middle
        }
        return low < comments.size && comments[low].start < range.end
    }
}

/** Whether a line break stands from [from] up to [until]. */
private fun String.hasLineBreak(from: Int, until: Int): Boolean {
    for (index in from until until) {
        if (this[index] == '\n' || this[index] == '\r') return true
    }
    return false
}

/** Whether no line break stands from [from] up to [until]. */
private fun String.onOneLine(from: Int, until: Int): Boolean = !hasLineBreak(from, until)

/** Whether a blank line stands from [from] up to [until]: two line breaks with nothing but whitespace between them. */
private fun String.hasBlankLine(from: Int, until: Int): Boolean {
    var breaks = 0
    for (index in from until until) {
        when (this[index]) {
            '\n' -> breaks++
            ' ', '\t', '\r' -> Unit
            else -> breaks = 0
        }
        if (breaks >= 2) return true
    }
    return false
}
