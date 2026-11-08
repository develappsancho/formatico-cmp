package com.developersancho.formatico.core.format.types

import com.developersancho.formatico.core.format.convert.appendJsonString

/**
 * [types] as Go, in the package [packageName]: a struct for each, for encoding/json, a blank line between each. A
 * field is a member's name in PascalCase, Go's initialisms in capitals (`ID`, `URL`) as in the names of the types
 * named after members, with its JSON name in its tag; one that is optional has `omitempty` in its tag, and one
 * optional or nullable is a pointer where it is a scalar or a struct (a slice and `any` hold a nil as they are). The
 * fields are indented by a tab and aligned in columns as gofmt aligns them: each column as wide as its widest cell
 * and one space.
 */
internal fun goTypes(types: TypeSet, packageName: String): String = buildString {
    append("package ").append(packageName).append('\n')
    types.declarations.forEach { append('\n').append(structOf(it, types)) }
}

/**
 * The name of the package the types of a root named [rootName] are in: the root's type name in small letters, and
 * "types" after it where that is one of Go's keywords (`maptypes`).
 */
internal fun goPackageName(rootName: String): String {
    val name = lowered(typeNameOf(rootName))
    return if (name in GoKeywords) name + "types" else name
}

/**
 * [word] as a word of a Go name: in capitals where it is one of Go's initialisms ("id", "Url" → `ID`, `URL`), with its
 * first letter a capital otherwise.
 */
internal fun goCapitalised(word: String): String {
    val upper = capitals(word)
    return if (upper in GoInitialisms) upper else capitalised(word)
}

private fun structOf(declaration: Declaration, types: TypeSet): String {
    val members = declaration.record.members
    if (members.isEmpty()) return "type ${declaration.name} struct{}\n"
    val names = uniqueNames(members.map { fieldName(it.name) })
    val rows = members.mapIndexed { index, member ->
        Triple(names[index], typeOf(member.type, types, pointer = member.optional || member.nullable), tagOf(member))
    }
    val nameWidth = rows.maxOf { width(it.first) }
    val typeWidth = rows.maxOf { width(it.second) }
    return buildString {
        append("type ").append(declaration.name).append(" struct {\n")
        rows.forEach { (name, type, tag) ->
            append('\t').append(padded(name, nameWidth)).append(padded(type, typeWidth)).append(tag).append('\n')
        }
        append("}\n")
    }
}

/** The field the member named [jsonName] is: its words in PascalCase, "Field" before a name that is not exported. */
private fun fieldName(jsonName: String): String {
    val name = wordsOf(jsonName).joinToString("") { goCapitalised(it) }
    return if (name.firstOrNull()?.isUpperCase() == true) name else FIELD + name
}

/** [type] in Go, a pointer where [pointer] says so and it is a scalar or a struct. */
private fun typeOf(type: ValueType, types: TypeSet, pointer: Boolean): String {
    val star = if (pointer) "*" else ""
    return when (type) {
        ValueType.Text -> star + "string"
        ValueType.Whole -> star + "int"
        ValueType.LongWhole -> star + "int64"
        ValueType.Decimal -> star + "float64"
        ValueType.Truth -> star + "bool"
        ValueType.Anything, ValueType.OnlyNull -> "any"
        is ValueType.Record -> star + types.nameOf(type)
        is ValueType.ListOf -> "[]" + typeOf(type.item, types, pointer = type.nullableItems)
    }
}

/**
 * The tag of [member]'s field: its JSON name, and `omitempty` where it is optional, in a raw string, or in a quoted
 * one where the name holds a backtick.
 */
private fun tagOf(member: Member): String {
    val tag = "json:" + goQuoted(member.name + if (member.optional) ",omitempty" else "")
    return if ('`' in tag) goQuoted(tag) else "`$tag`"
}

/** [text] as a Go string literal: JSON's escapes are Go's. */
private fun goQuoted(text: String): String = buildString { appendJsonString(text) }

/** How wide gofmt counts [text]: a character for each code point. */
private fun width(text: String): Int = text.length - text.count { it.isLowSurrogate() }

/** [text] and the spaces that make it a cell of a column [columnWidth] wide, one space after the widest. */
private fun padded(text: String, columnWidth: Int): String = text + " ".repeat(columnWidth - width(text) + 1)

/** What goes before a field's name that would not be exported. */
private const val FIELD = "Field"

/** The initialisms Go writes in capitals in a name. */
private val GoInitialisms =
    setOf("ID", "URL", "URI", "HTTP", "HTTPS", "API", "JSON", "XML", "HTML", "UUID", "IP", "SQL", "CSS", "TCP", "UDP")

/** Go's keywords, which no package can be named. */
private val GoKeywords = setOf(
    "break",
    "case",
    "chan",
    "const",
    "continue",
    "default",
    "defer",
    "else",
    "fallthrough",
    "for",
    "func",
    "go",
    "goto",
    "if",
    "import",
    "interface",
    "map",
    "package",
    "range",
    "return",
    "select",
    "struct",
    "switch",
    "type",
    "var"
)
