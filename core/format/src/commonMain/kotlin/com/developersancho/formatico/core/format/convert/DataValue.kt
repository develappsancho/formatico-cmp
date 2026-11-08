package com.developersancho.formatico.core.format.convert

/**
 * A value as a conversion carries it from one format to another: what every format Formatico reads holds, whatever it
 * is written as.
 */
sealed class DataValue

/**
 * An object, its members in their order, a name given twice included.
 *
 * @property members its members, each a name and its value.
 */
data class DataObject(val members: List<DataMember>) : DataValue()

/** A member of an object: its [name] and its [value]. */
data class DataMember(val name: String, val value: DataValue)

/** An array, its [items] in order. */
data class DataArray(val items: List<DataValue>) : DataValue()

/** A string, its [value] read: no quotes and no escapes. */
data class DataString(val value: String) : DataValue()

/**
 * A number, as its own format writes it: `129.90`, `0x1F`, `1_000`, `.inf`. A format that writes it another way says
 * so; one that writes it so keeps the spelling.
 */
data class DataNumber(val written: String) : DataValue()

/** `true` or `false`. */
data class DataBoolean(val value: Boolean) : DataValue()

/** A value that is none: JSON's and YAML's `null`. */
data object DataNull : DataValue()

/**
 * A date, a time or both, as TOML writes them: `2026-11-04`, `14:32:00`, `2026-10-28T14:32:00Z`. Only TOML has them
 * apart from its strings; every other format writes one as a string.
 */
data class DataDate(val written: String) : DataValue()
