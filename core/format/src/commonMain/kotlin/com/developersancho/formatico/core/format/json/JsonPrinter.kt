package com.developersancho.formatico.core.format.json

import com.developersancho.formatico.core.format.text.Indentation
import com.developersancho.formatico.core.format.text.LineEnding
import com.developersancho.formatico.core.format.text.TextLayout

/**
 * The document written out again, a value to a line and each level indented by [indentation]: its keys in their
 * order, or sorted by name where [sortKeys] says so, its strings and numbers as they are written, and its comments
 * where they stand. A container that ends with a trailing comma, as JSONC and JSON5 allow, keeps it after its last
 * entry.
 *
 * A comment stays with the value it is written against: one on the line of a value, after it, follows it; one on a
 * line of its own goes before the value under it. A block comment between a member's key and its value stays there; a
 * line comment there goes before the member, since it would end the line the value is on. Sorting the keys takes each
 * member's comments with it.
 *
 * The lines end as the document's first line does, and the text ends with a line break where the document's did.
 *
 * @throws IllegalArgumentException when the document is not valid: there is no value to write out.
 */
fun JsonDocument.formatted(indentation: Indentation = Indentation.Spaces(2), sortKeys: Boolean = false): String {
    val root = requireValid()
    val lineEnding = TextLayout.of(text).lineEnding ?: LineEnding.Lf
    return JsonPrinter(this, indentation.unit, lineEnding.characters, sortKeys).print(root)
}

/**
 * The document on one line, with no space and no comment: its keys in their order, its strings and numbers as they are
 * written.
 *
 * @throws IllegalArgumentException when the document is not valid: there is no value to write out.
 */
fun JsonDocument.minified(): String {
    val root = requireValid()
    return buildString(text.length) { appendMinified(root) }
}

/** [value] on one line with no space: what [minified] writes of a document. */
internal fun minifiedJson(value: JsonValue): String = buildString { appendMinified(value) }

/**
 * [value] on one line, a space after each colon and comma, `{"id": 7, "tags": ["a", "b"]}`: its keys in their order,
 * or sorted by name where [sortKeys] says so, its strings and numbers as they are written.
 */
internal fun inlineJson(value: JsonValue, sortKeys: Boolean = false): String = buildString { appendInline(value, sortKeys) }

private fun StringBuilder.appendInline(value: JsonValue, sortKeys: Boolean) {
    when (value) {
        is JsonObject -> {
            val members = if (sortKeys) value.members.sortedBy { it.key.name } else value.members
            append('{')
            members.forEachIndexed { index, member ->
                if (index > 0) append(", ")
                append(member.key.raw).append(": ")
                member.value?.let { appendInline(it, sortKeys) }
            }
            append('}')
        }

        is JsonArray -> {
            append('[')
            value.items.forEachIndexed { index, item ->
                if (index > 0) append(", ")
                appendInline(item, sortKeys)
            }
            append(']')
        }

        else -> {
            appendMinified(value)
        }
    }
}

private fun JsonDocument.requireValid(): JsonValue {
    require(isValid) { "Only a valid document is written out again" }
    return requireNotNull(root) { "A valid document has a value" }
}

private fun StringBuilder.appendMinified(value: JsonValue) {
    when (value) {
        is JsonObject -> {
            append('{')
            value.members.forEachIndexed { index, member ->
                if (index > 0) append(',')
                append(member.key.raw).append(':')
                member.value?.let { appendMinified(it) }
            }
            append('}')
        }

        is JsonArray -> {
            append('[')
            value.items.forEachIndexed { index, item ->
                if (index > 0) append(',')
                appendMinified(item)
            }
            append(']')
        }

        is JsonString -> {
            append(value.raw)
        }

        is JsonNumber -> {
            append(value.raw)
        }

        is JsonBoolean -> {
            append(if (value.value) "true" else "false")
        }

        is JsonNull -> {
            append("null")
        }
    }
}

/**
 * Writes a valid document out a value to a line, its comments with the values they are written against.
 *
 * @param indent one level of indentation.
 * @param newline the line ending.
 */
private class JsonPrinter(
    private val document: JsonDocument,
    private val indent: String,
    private val newline: String,
    private val sortKeys: Boolean
) {
    private val text = document.text
    private val comments: List<JsonToken> = document.comments
    private val out = StringBuilder(text.length + text.length / 2)

    /** A member or an item, with the comments that go before it, between its key and its value, and after it. */
    private class Entry(
        val member: JsonMember?,
        val value: JsonValue,
        val leading: List<JsonToken>,
        val inner: List<JsonToken>,
        val trailing: List<JsonToken>
    )

    fun print(root: JsonValue): String {
        commentsIn(0, root.range.start).forEach { comment ->
            out.append(text, comment.start, comment.end).append(newline)
        }
        printValue(root, depth = 0)
        val after = commentsIn(root.range.end, text.length)
        after.forEachIndexed { index, comment ->
            if (index == 0 && text.onOneLine(root.range.end, comment.start)) out.append(' ') else out.append(newline)
            out.append(text, comment.start, comment.end)
        }
        if (text.endsWith('\n') || text.endsWith('\r')) out.append(newline)
        return out.toString()
    }

    private fun printValue(value: JsonValue, depth: Int) {
        if (value is JsonObject || value is JsonArray) {
            printContainer(value, depth)
        } else {
            out.append(text, value.range.start, value.range.end)
        }
    }

    /**
     * Writes a container: its opening bracket, each of its entries on a line of its own with its comments, the
     * comments after the last entry, and its closing bracket.
     */
    private fun printContainer(container: JsonValue, depth: Int) {
        val (open, close) = if (container is JsonObject) '{' to '}' else '[' to ']'
        val parts = when (container) {
            is JsonObject -> container.members.map { it to requireNotNull(it.value) { "A valid member has a value" } }
            is JsonArray -> container.items.map { null to it }
            else -> emptyList()
        }
        val inside = container.range.start + 1
        val closing = container.range.end - 1
        val entries = parts.indices.map { entry(parts, it, inside, closing) }
        val lastEnd = parts.lastOrNull()?.second?.range?.end
        val dangling = if (lastEnd == null) {
            commentsIn(inside, closing)
        } else {
            commentsIn(lastEnd, closing).filterNot { text.onOneLine(lastEnd, it.start) }
        }
        if (entries.isEmpty() && dangling.isEmpty()) {
            out.append(open).append(close)
            return
        }
        val written = if (sortKeys && container is JsonObject) entries.sortedBy { it.member?.key?.name } else entries
        out.append(open).append(newline)
        printEntries(written, depth + 1, trailingComma = lastEnd != null && hasComma(lastEnd, closing))
        dangling.forEach { comment -> line(depth + 1, comment) }
        indent(depth)
        out.append(close)
    }

    /**
     * Writes [entries] at [depth], a line each with their comments, and a comma after the last where [trailingComma]
     * says so.
     */
    private fun printEntries(entries: List<Entry>, depth: Int, trailingComma: Boolean) {
        entries.forEachIndexed { position, entry ->
            entry.leading.forEach { comment -> line(depth, comment) }
            indent(depth)
            entry.member?.let { out.append(it.key.raw).append(": ") }
            entry.inner.forEach { comment -> out.append(text, comment.start, comment.end).append(' ') }
            printValue(entry.value, depth)
            if (position < entries.lastIndex || trailingComma) out.append(',')
            entry.trailing.forEach { comment -> out.append(' ').append(text, comment.start, comment.end) }
            out.append(newline)
        }
    }

    /**
     * The entry at [index] of [parts], with its comments: before it, those after the entry before it that do not stand
     * on that entry's line, and the line comments between a member's key and its value; between them, the block
     * comments there; after it, those on its own line.
     *
     * @param inside where the container's inside starts, after its opening bracket.
     * @param closing where its closing bracket stands.
     */
    private fun entry(parts: List<Pair<JsonMember?, JsonValue>>, index: Int, inside: Int, closing: Int): Entry {
        val (member, value) = parts[index]
        val start = member?.range?.start ?: value.range.start
        val end = value.range.end
        val previousEnd = if (index == 0) inside else parts[index - 1].second.range.end
        val before = commentsIn(previousEnd, start).filterNot { index > 0 && text.onOneLine(previousEnd, it.start) }
        val (lineComments, blockComments) =
            (if (member != null) commentsIn(member.key.range.end, value.range.start) else emptyList())
                .partition { text.startsWith("//", it.start) }
        val next = parts.getOrNull(index + 1)
        val nextStart =
            next?.let { (nextMember, nextValue) -> nextMember?.range?.start ?: nextValue.range.start } ?: closing
        val after = commentsIn(end, nextStart).filter { text.onOneLine(end, it.start) }
        return Entry(member, value, before + lineComments, blockComments, after)
    }

    /** The comments that start at [from] or after it and before [until]. */
    private fun commentsIn(from: Int, until: Int): List<JsonToken> {
        if (from >= until) return emptyList()
        val first = lowerBound(from)
        val last = lowerBound(until)
        return comments.subList(first, last)
    }

    private fun lowerBound(offset: Int): Int {
        var low = 0
        var high = comments.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (comments[middle].start < offset) low = middle + 1 else high = middle
        }
        return low
    }

    /** Whether a comma stands between [from] and [to], outside the comments there. */
    private fun hasComma(from: Int, to: Int): Boolean {
        var at = from
        for (comment in commentsIn(from, to)) {
            if ((at until comment.start).any { text[it] == ',' }) return true
            at = comment.end
        }
        return (at until to).any { text[it] == ',' }
    }

    private fun indent(depth: Int) {
        repeat(depth) { out.append(indent) }
    }

    /** Writes [comment] on a line of its own, at [depth]. */
    private fun line(depth: Int, comment: JsonToken) {
        indent(depth)
        out.append(text, comment.start, comment.end).append(newline)
    }
}

/** Whether no line break stands between [from] and [to]. */
private fun String.onOneLine(from: Int, to: Int): Boolean =
    (from until to).none { this[it] == '\n' || this[it] == '\r' }
