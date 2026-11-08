package com.developersancho.formatico.core.format.toml

import com.developersancho.formatico.core.format.text.TextRange

/**
 * A part of a key, between its dots.
 *
 * @property raw the part as it is written, its quotes included.
 * @property name its name: [raw] without its quotes, its escapes read.
 */
internal class TomlKeyPart(val range: TextRange, val raw: String, val name: String)

/** A key as it is written: a part, or more between dots, `name`, `"quoted"`, `a.b.c`. */
internal class TomlKey(val parts: List<TomlKeyPart>) {
    /** Where it is written, from its first part to its last. */
    val range: TextRange get() = TextRange(parts.first().range.start, parts.last().range.end)
}

/** A value as it is written: a string, a number, a boolean or a date, an array, or an inline table. */
internal sealed class TomlValueSyntax {
    /** The text of the value. */
    abstract val range: TextRange
}

/** A string, a number, a boolean or a date: the value itself. */
internal class TomlScalarSyntax(val value: TomlValue) : TomlValueSyntax() {
    override val range: TextRange get() = value.range
}

/**
 * An array in brackets.
 *
 * @property items its values in the order they are written.
 * @property trailingComma whether a comma follows its last value.
 */
internal class TomlArraySyntax(override val range: TextRange, val items: List<TomlValueSyntax>, val trailingComma: Boolean) :
    TomlValueSyntax()

/**
 * An inline table.
 *
 * @property pairs its key/values in the order they are written.
 */
internal class TomlInlineTableSyntax(override val range: TextRange, val pairs: List<TomlKeyValue>) : TomlValueSyntax()

/** A key, its `=` and its value. */
internal class TomlKeyValue(val key: TomlKey, val value: TomlValueSyntax) {
    /** Where it is written, from its key to the end of its value. */
    val range: TextRange get() = TextRange(key.range.start, value.range.end)
}

/** A line of a document that says something: a key/value or a header. */
internal sealed class TomlStatement {
    /** Where it is written, its comment left out. */
    abstract val range: TextRange
}

/** A key/value of a table, on a line of its own. */
internal class TomlKeyValueStatement(val pair: TomlKeyValue) : TomlStatement() {
    override val range: TextRange get() = pair.range
}

/**
 * A header, `[name]` or `[[name]]`.
 *
 * @property key its name, or null where it cannot be read.
 * @property ofArray whether it is a header of an array of tables, `[[name]]`.
 */
internal class TomlHeaderStatement(override val range: TextRange, val key: TomlKey?, val ofArray: Boolean) : TomlStatement()

/** The order keys are sorted in: by the names of their parts, a part at a time. */
internal val TomlKeyOrder: Comparator<TomlKey> = Comparator { first, second ->
    val common = minOf(first.parts.size, second.parts.size)
    (0 until common).asSequence()
        .map { first.parts[it].name.compareTo(second.parts[it].name) }
        .firstOrNull { it != 0 }
        ?: first.parts.size.compareTo(second.parts.size)
}
