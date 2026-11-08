package com.developersancho.formatico.core.format.convert

import com.developersancho.formatico.core.format.Format

/**
 * [value] written as a document of [format], as [options] say, with what it does not carry as it is and what it did
 * to carry the rest.
 *
 * Every format writes the values it has as they are: a number keeps its spelling where the format writes it so, a
 * string the format would read as another value is quoted. Where it has no place for one, it says so:
 * - JSON, JSONC and JSON5 hold every value but a TOML date, which is a string, and an infinity or a NaN, which JSON
 *   writes as a string; NDJSON writes an array a value to a line.
 * - YAML holds every value, and a key once.
 * - TOML has no null, its document is a table, its integers are 64 bits, and a table's own keys come before its
 *   tables.
 * - XML has only text, one root element, attributes for members named `@` and a name, and text for `#text`, as
 *   Formatico's tree reads XML.
 * - CSV and TSV are a table: an array's items are its rows, an object a row, and a value inside another a column of its
 *   own, named by its path.
 * - Properties are keys of one level, named by their paths; INI is keys before its sections, and keys of one level
 *   in each section.
 */
fun convert(value: DataValue, format: Format, options: ConvertOptions = ConvertOptions()): Conversion {
    val table = format == Format.Csv || format == Format.Tsv
    val log = ConvertLog(if (table) NullHandling.EmptyString else options.nulls)
    val arranged = value.arranged(options.sortKeys, unique = format !in KeepsDuplicates, log = log, path = "")
    val indent = options.indentation.unit
    val text = when (format) {
        Format.Json, Format.Jsonc -> JsonWriter(indent, json5 = false, log).document(arranged)
        Format.Json5 -> JsonWriter(indent, json5 = true, log).document(arranged)
        Format.Ndjson -> ndjsonDocument(arranged, log)
        Format.Yaml -> YamlWriter(indent, log).document(arranged)
        Format.Toml -> TomlWriter(options, log).document(arranged)
        Format.Xml -> XmlWriter(indent, options, log).document(arranged)
        Format.Csv -> csvDocument(arranged, ',', log)
        Format.Tsv -> csvDocument(arranged, '\t', log)
        Format.Properties -> propertiesDocument(arranged, options, log)
        Format.Ini -> iniDocument(arranged, options, log)
    }
    return Conversion(text = text, losses = log.losses(), notes = log.notes(), nullLines = log.markedLines)
}

/** The formats that write a name given twice in an object twice, as JSON does. */
private val KeepsDuplicates = setOf(Format.Json, Format.Jsonc, Format.Json5, Format.Ndjson)

/**
 * The value with every object's members in the order of their names where [sort] says so, and where the format keeps
 * a name once ([unique]), the last member of each name alone, each one dropped told to [log].
 */
private fun DataValue.arranged(sort: Boolean, unique: Boolean, log: ConvertLog, path: String): DataValue = when (this) {
    is DataObject -> {
        val last = if (unique) members.withIndex().associate { (index, member) -> member.name to index } else null
        val kept = members.filterIndexed { index, member ->
            (last == null || last.getValue(member.name) == index).also { if (!it) log.lose(Lost.Duplicates, memberPath(path, member.name)) }
        }
        val ordered = if (sort) kept.sortedBy { it.name } else kept
        DataObject(ordered.map { DataMember(it.name, it.value.arranged(sort, unique, log, memberPath(path, it.name))) })
    }

    is DataArray -> DataArray(items.mapIndexed { index, item -> item.arranged(sort, unique, log, itemPath(path, index)) })

    else -> this
}

/** An NDJSON document: an array's items, or the value alone, a line each, as compact JSON. */
private fun ndjsonDocument(value: DataValue, log: ConvertLog): String {
    val lines = if (value is DataArray) {
        value.items.mapIndexed { index, item -> JsonWriter(indent = null, json5 = false, log).line(item, itemPath("", index)) }
    } else {
        log.wrap("[0]")
        listOf(JsonWriter(indent = null, json5 = false, log).line(value, ""))
    }
    return lines.joinToString("") { it + "\n" }
}
