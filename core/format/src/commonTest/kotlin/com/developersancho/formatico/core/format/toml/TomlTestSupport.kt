package com.developersancho.formatico.core.format.toml

import kotlin.test.assertTrue

/** Three double quotes, which open and close TOML's multi-line basic strings. */
internal const val TRIPLE_QUOTE = "\"\"\""

/** [lines] as a text, each ending with `\n`. */
internal fun toml(vararg lines: String): String = lines.joinToString(separator = "\n", postfix = "\n")

/** The value under [path], a key at a time from [this] table. */
internal fun TomlTable.at(vararg path: String): TomlValue {
    var value: TomlValue = this
    path.forEach { key -> value = (value as TomlTable).entries.first { it.key == key }.value }
    return value
}

/** The entry of [key] in [this] table. */
internal fun TomlTable.entry(key: String): TomlEntry = entries.first { it.key == key }

/** What [range] covers of the document's text. */
internal fun TomlDocument.textOf(value: TomlValue): String = text.substring(value.range.start, value.range.end)

/**
 * The values of [text] in a form that says nothing of where or how they are written, only what they are: what two
 * documents with the same values share. Where [sorted] says so, each table's keys are in the order of their names.
 */
internal fun valuesOf(text: String, sorted: Boolean = false): String {
    val document = TomlDocument.parse(text)
    assertTrue(document.isValid, "Not valid: <$text> ${document.problems}")
    return canonical(document.root, sorted)
}

internal fun canonical(value: TomlValue, sorted: Boolean): String = when (value) {
    is TomlTable -> {
        val entries = if (sorted) value.entries.sortedBy { it.key } else value.entries
        entries.joinToString(", ", "{", "}") { "${it.key}=${canonical(it.value, sorted)}" }
    }

    is TomlArray -> {
        value.items.joinToString(", ", if (value.ofTables) "[[" else "[", if (value.ofTables) "]]" else "]") {
            canonical(it, sorted)
        }
    }

    is TomlString -> "s(${value.value})"

    is TomlInteger -> "i(${value.raw})"

    is TomlFloat -> "f(${value.raw})"

    is TomlBoolean -> "b(${value.value})"

    is TomlDateTime -> "${value.kind}(${value.raw})"
}
