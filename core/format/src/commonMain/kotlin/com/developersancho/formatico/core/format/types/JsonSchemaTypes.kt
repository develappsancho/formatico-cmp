package com.developersancho.formatico.core.format.types

import com.developersancho.formatico.core.format.convert.appendJsonString

/** The draft the schemas Types writes are of. */
internal const val JSON_SCHEMA_DRAFT = "https://json-schema.org/draft/2020-12/schema"

/**
 * [types] as a JSON Schema of draft 2020-12, indented by two spaces: the root type at the top, titled with its name,
 * and every other in `$defs` under its name, which a `$ref` names it by. A member every object has is `required`; one
 * null in some takes `null` among its types; a type that cannot be told is `{}`, which allows anything. It asks no more
 * than the document shows: no `additionalProperties`, no bounds, no formats. A schema whose members are all single
 * values is written on one line where that is short: `{ "type": "string" }`.
 */
internal fun jsonSchemaTypes(types: TypeSet): String {
    val declarations = types.declarations
    val root = declarations.first()
    val top = buildList {
        add("\$schema" to SchemaNode.Text(JSON_SCHEMA_DRAFT))
        add("title" to SchemaNode.Text(root.name))
        addAll(recordOf(root.record, types, root.name).members)
        if (declarations.size > 1) {
            add("\$defs" to SchemaNode.Object(declarations.drop(1).map { it.name to recordOf(it.record, types, root.name) }))
        }
    }
    return buildString { write(SchemaNode.Object(top), indent = "") }.plus("\n")
}

/** A part of a schema, as it is written. */
private sealed interface SchemaNode {
    data class Text(val value: String) : SchemaNode

    data class Texts(val values: List<String>) : SchemaNode

    data class Items(val nodes: List<SchemaNode>) : SchemaNode

    data class Object(val members: List<Pair<String, SchemaNode>>) : SchemaNode
}

/** The schema of an object of [record]: its members' schemas, and the names of those every object has. */
private fun recordOf(record: ValueType.Record, types: TypeSet, rootName: String): SchemaNode.Object {
    val members = buildList {
        add("type" to SchemaNode.Text("object"))
        if (record.members.isNotEmpty()) {
            add("properties" to SchemaNode.Object(record.members.map { it.name to schemaOf(it.type, it.nullable, types, rootName) }))
        }
        val required = record.members.filter { !it.optional }.map { it.name }
        if (required.isNotEmpty()) add("required" to SchemaNode.Texts(required))
    }
    return SchemaNode.Object(members)
}

/** The schema of a value of [type], `null` allowed where [nullable] says so; a record by its name, `#` for the root. */
private fun schemaOf(type: ValueType, nullable: Boolean, types: TypeSet, rootName: String): SchemaNode = when (type) {
    ValueType.Text -> typed("string", nullable)

    ValueType.Whole, ValueType.LongWhole -> typed("integer", nullable)

    ValueType.Decimal -> typed("number", nullable)

    ValueType.Truth -> typed("boolean", nullable)

    ValueType.Anything, ValueType.OnlyNull -> SchemaNode.Object(emptyList())

    is ValueType.Record -> {
        val name = types.nameOf(type)
        val ref = SchemaNode.Object(listOf("\$ref" to SchemaNode.Text(if (name == rootName) "#" else "#/\$defs/$name")))
        if (nullable) SchemaNode.Object(listOf("anyOf" to SchemaNode.Items(listOf(ref, typed("null", nullable = false))))) else ref
    }

    is ValueType.ListOf -> SchemaNode.Object(
        listOf(
            "type" to kinds("array", nullable),
            "items" to schemaOf(type.item, type.nullableItems, types, rootName)
        )
    )
}

private fun typed(kind: String, nullable: Boolean): SchemaNode.Object = SchemaNode.Object(listOf("type" to kinds(kind, nullable)))

private fun kinds(kind: String, nullable: Boolean): SchemaNode = if (nullable) {
    SchemaNode.Texts(
        listOf(kind, "null")
    )
} else {
    SchemaNode.Text(kind)
}

/**
 * Writes [node], its lines after the first indented by [indent] and two spaces more for each level; a member to a line
 * where it [holdsSchemas], as `properties` and `$defs` do.
 */
private fun StringBuilder.write(node: SchemaNode, indent: String, holdsSchemas: Boolean = false) {
    val line = inline(node)
    val single = node is SchemaNode.Text || node is SchemaNode.Texts
    if (single || (!holdsSchemas && fitsOnALine(node, line))) {
        append(line)
        return
    }
    val inner = "$indent  "
    when (node) {
        is SchemaNode.Object -> {
            append("{\n")
            node.members.forEachIndexed { index, (name, value) ->
                append(inner).appendJsonString(name).append(": ")
                write(value, inner, holdsSchemas = name in SchemaHolders)
                append(if (index < node.members.lastIndex) ",\n" else "\n")
            }
            append(indent).append('}')
        }

        is SchemaNode.Items -> {
            append("[\n")
            node.nodes.forEachIndexed { index, value ->
                append(inner)
                write(value, inner)
                append(if (index < node.nodes.lastIndex) ",\n" else "\n")
            }
            append(indent).append(']')
        }

        else -> Unit
    }
}

/** Whether [node], written as [line], stands on one line: it holds no properties nor types of its own, and is short. */
private fun fitsOnALine(node: SchemaNode, line: String): Boolean {
    val holds = node is SchemaNode.Object && node.members.any { (name, _) -> name in SchemaHolders }
    return !holds && line.length <= MOST_INLINE
}

/** [node] written on one line. */
private fun inline(node: SchemaNode): String = when (node) {
    is SchemaNode.Text -> buildString { appendJsonString(node.value) }

    is SchemaNode.Texts -> node.values.joinToString(", ", "[", "]") { buildString { appendJsonString(it) } }

    is SchemaNode.Items -> node.nodes.joinToString(", ", "[", "]") { inline(it) }

    is SchemaNode.Object -> if (node.members.isEmpty()) {
        "{}"
    } else {
        node.members.joinToString(", ", "{ ", " }") { (name, value) ->
            buildString { appendJsonString(name) }.plus(": ").plus(inline(value))
        }
    }
}

/** The members whose values hold schemas by name, written a member to a line. */
private val SchemaHolders = setOf("properties", "\$defs")

/** How long a schema written on one line may be. */
private const val MOST_INLINE = 72
