package com.developersancho.formatico.core.format.toml

import com.developersancho.formatico.core.format.text.TextRange

/** A value of a TOML document, where it is written. */
sealed class TomlValue {
    /** The text of the value; a table's is as its kind says (see [TomlTable]). */
    abstract val range: TextRange
}

/** What made a table. */
enum class TomlTableKind {
    /** The document itself: the table of what comes before the first header. */
    Root,

    /** A header, `[name]`. */
    Header,

    /** A dotted key, `a.b.c = 1` for `a` and `a.b`, or a header for a table under it, `[a.b]` for `a`. */
    Implicit,

    /** An inline table, `{ a = 1 }`. */
    Inline,

    /** A header of an array of tables, `[[name]]`: one item of the array. */
    ArrayItem
}

/**
 * A table.
 *
 * Where it is written depends on its [kind]: the root covers the whole text; a header's table, and an item of an array
 * of tables, runs from the header's `[` to the end of its last key/value, or to the header's end when it has none; an
 * implicit table runs over everything that went into it, from the smallest start to the largest end; an inline table
 * from its `{` to its `}`.
 *
 * @property entries its keys and their values, in the order the keys are first given in the text.
 * @property kind what made it.
 */
class TomlTable(override val range: TextRange, val entries: List<TomlEntry>, val kind: TomlTableKind) : TomlValue()

/**
 * A key of a table and its value.
 *
 * @property key the key's name: its quotes taken off and its escapes read.
 * @property keyRange where the key's last part is written, or null where no key is written: for an implicit table a
 * header makes, `a` of `[a.b]`.
 */
class TomlEntry(val key: String, val keyRange: TextRange?, val value: TomlValue)

/**
 * An array: values in brackets, or the tables of the `[[name]]` headers of one name, which then runs from its first
 * `[[` to the end of its last item.
 *
 * @property items its values in order.
 * @property ofTables whether it is an array of tables, `[[name]]`.
 */
class TomlArray(override val range: TextRange, val items: List<TomlValue>, val ofTables: Boolean) : TomlValue()

/**
 * A string, of any of TOML's four kinds: basic, literal, and their multi-line kinds.
 *
 * @property raw the string as it is written, its quotes included.
 * @property value its value: its escapes read and, in a multi-line string, the line break right after the opening
 * quotes left out, as is every backslash that ends a line with the whitespace after it.
 */
class TomlString(override val range: TextRange, val raw: String, val value: String) : TomlValue()

/**
 * An integer, kept as it is written: `1_000`, `0xFF` and `+3` are not turned into another spelling.
 *
 * @property raw the integer as it is written.
 */
class TomlInteger(override val range: TextRange, val raw: String) : TomlValue()

/**
 * A float, kept as it is written: `6.626e-34`, `+inf`, `nan`.
 *
 * @property raw the float as it is written.
 */
class TomlFloat(override val range: TextRange, val raw: String) : TomlValue()

/** `true` or `false`. */
class TomlBoolean(override val range: TextRange, val value: Boolean) : TomlValue()

/** The kind of a [TomlDateTime]. */
enum class TomlDateTimeKind {
    /** A date and a time with its offset from UTC: `1979-05-27T07:32:00Z`, `1979-05-27 00:32:00-07:00`. */
    OffsetDateTime,

    /** A date and a time in no time zone: `1979-05-27T07:32:00`. */
    LocalDateTime,

    /** A date alone: `1979-05-27`. */
    LocalDate,

    /** A time alone: `07:32:00.999`. */
    LocalTime
}

/**
 * A date, a time or both, kept as it is written.
 *
 * @property raw the date or the time as it is written.
 * @property kind which of TOML's four it is.
 */
class TomlDateTime(override val range: TextRange, val raw: String, val kind: TomlDateTimeKind) : TomlValue()
