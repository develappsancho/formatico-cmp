package com.developersancho.formatico.core.format.types

import com.developersancho.formatico.core.format.convert.appendJsonString

/**
 * [types] as TypeScript: an exported interface for each, indented by two spaces, a blank line between each. A member
 * keeps its JSON name, in quotes where it is no identifier; `?` marks one that is optional, `| null` one that is
 * nullable, and `unknown` stands for a type that cannot be told.
 */
internal fun typeScriptTypes(types: TypeSet): String = types.declarations.joinToString("\n") { interfaceOf(it, types) }

private fun interfaceOf(declaration: Declaration, types: TypeSet): String {
    val head = "export interface ${declaration.name}"
    val members = declaration.record.members
    if (members.isEmpty()) return "$head {}\n"
    return members.joinToString("", prefix = "$head {\n", postfix = "}\n") { "  ${propertyOf(it, types)}\n" }
}

private fun propertyOf(member: Member, types: TypeSet): String {
    val optional = if (member.optional) "?" else ""
    val nullable = if (member.nullable && !member.type.isUnknown) " | null" else ""
    return "${keyOf(member.name)}$optional: ${typeOf(member.type, types)}$nullable;"
}

private fun typeOf(type: ValueType, types: TypeSet): String = when (type) {
    ValueType.Text -> "string"
    ValueType.Whole, ValueType.LongWhole, ValueType.Decimal -> "number"
    ValueType.Truth -> "boolean"
    ValueType.Anything, ValueType.OnlyNull -> "unknown"
    is ValueType.Record -> types.nameOf(type)
    is ValueType.ListOf -> itemsOf(type, types) + "[]"
}

/** The type of [list]'s items, in brackets with `| null` where they may be null. */
private fun itemsOf(list: ValueType.ListOf, types: TypeSet): String {
    val item = typeOf(list.item, types)
    return if (list.nullableItems && !list.item.isUnknown) "($item | null)" else item
}

/** [name] as a property's key: as it is where it is an identifier, in quotes where it is not. */
private fun keyOf(name: String): String {
    val identifier = name.isNotEmpty() && !name.first().isDigit() && name.all { it.isLetterOrDigit() || it == '_' || it == '$' }
    return if (identifier) name else buildString { appendJsonString(name) }
}
