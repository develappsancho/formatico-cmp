package com.developersancho.formatico.core.format.toml

import com.developersancho.formatico.core.format.text.Indentation
import com.developersancho.formatico.core.format.text.LineEnding
import com.developersancho.formatico.core.format.text.TextLayout

/**
 * The document written out again: every line unindented, a key/value as its key, ` = ` and its value, keys and headers
 * with no space around their dots and their quoted parts as they are written, every string, number and date as it is
 * written, multi-line strings among them.
 *
 * An array on one line is written `[1, 2, 3]`. One over several lines keeps a line to each value, indented by a level
 * of [indentation], its closing bracket on a line of its own and a comma after its last value where it had one; one
 * with a comment in it is written as it is. An inline table is written `{ a = 1, b = "x" }`, an empty one `{}`.
 *
 * A comment on a line of its own stays on its own line, before what follows it; one after a statement stays after it,
 * a space before its `#`. A run of blank lines becomes one, a blank line goes before every header but at the start,
 * and the comments right above a header go with it, after that blank line. No blank line opens or ends the text.
 *
 * Where [sortKeys] says so, the key/values of each table, between two headers, are sorted by key, each taking along the
 * comment lines above it since the key/value before, and are written with no blank line between them; the headers keep
 * their order, and the keys of inline tables are sorted too.
 *
 * The lines end as the document's first line does, and the text ends with a line break where the document's did.
 *
 * @throws IllegalArgumentException when the document is not valid.
 */
fun TomlDocument.formatted(indentation: Indentation = Indentation.Spaces(2), sortKeys: Boolean = false): String {
    require(isValid) { "Only a valid document is written out again" }
    return TomlFormatter(this, indentation.unit, newlineOf(text), sortKeys).print()
}

/**
 * The document with no comment, no blank line and no indentation: a statement to a line, a key/value as `key=value`,
 * arrays and inline tables with no space in them, `[1,2]` and `{a=1}`, and its headers. Strings, numbers and dates are
 * written as they are, multi-line strings among them.
 *
 * @throws IllegalArgumentException when the document is not valid.
 */
fun TomlDocument.minified(): String {
    require(isValid) { "Only a valid document is written out again" }
    val newline = newlineOf(text)
    val out = StringBuilder(text.length)
    statements.forEachIndexed { index, statement ->
        if (index > 0) out.append(newline)
        when (statement) {
            is TomlHeaderStatement -> {
                out.appendHeader(statement)
            }

            is TomlKeyValueStatement -> {
                out.appendKey(statement.pair.key).append('=')
                out.appendMinified(text, statement.pair.value)
            }
        }
    }
    if (statements.isNotEmpty() && text.endsWithLineBreak()) out.append(newline)
    return out.toString()
}

/** The line ending of [text]'s first line, or `\n` for a text of one line. */
private fun newlineOf(text: String): String = (TextLayout.of(text).lineEnding ?: LineEnding.Lf).characters

/** Whether the text ends with a line break. */
internal fun String.endsWithLineBreak(): Boolean = endsWith('\n') || endsWith('\r')

/** Appends [key] with no space around its dots, its parts as they are written. */
internal fun StringBuilder.appendKey(key: TomlKey): StringBuilder {
    key.parts.forEachIndexed { index, part ->
        if (index > 0) append('.')
        append(part.raw)
    }
    return this
}

/** Appends [header] as `[name]` or `[[name]]`. */
internal fun StringBuilder.appendHeader(header: TomlHeaderStatement): StringBuilder {
    val key = requireNotNull(header.key) { "A valid header has a name" }
    val brackets = if (header.ofArray) 2 else 1
    repeat(brackets) { append('[') }
    appendKey(key)
    repeat(brackets) { append(']') }
    return this
}

/** Appends [value], of [text], with no space and no comment in it. */
private fun StringBuilder.appendMinified(text: String, value: TomlValueSyntax) {
    when (value) {
        is TomlScalarSyntax -> {
            append(text, value.range.start, value.range.end)
        }

        is TomlArraySyntax -> {
            append('[')
            value.items.forEachIndexed { index, item ->
                if (index > 0) append(',')
                appendMinified(text, item)
            }
            append(']')
        }

        is TomlInlineTableSyntax -> {
            append('{')
            value.pairs.forEachIndexed { index, pair ->
                if (index > 0) append(',')
                appendKey(pair.key).append('=')
                appendMinified(text, pair.value)
            }
            append('}')
        }
    }
}
