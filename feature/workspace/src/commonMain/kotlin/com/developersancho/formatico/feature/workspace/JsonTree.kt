package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.json.JsonArray
import com.developersancho.formatico.core.format.json.JsonBoolean
import com.developersancho.formatico.core.format.json.JsonDocument
import com.developersancho.formatico.core.format.json.JsonNumber
import com.developersancho.formatico.core.format.json.JsonObject
import com.developersancho.formatico.core.format.json.JsonString
import com.developersancho.formatico.core.format.json.JsonValue
import com.developersancho.formatico.core.format.json.PathStep
import com.developersancho.formatico.core.format.ndjson.NdjsonDocument
import com.developersancho.formatico.core.format.text.TextRange

/** Reads [text] as JSON, JSONC or JSON5, as [format] says, for its tree. */
internal fun readJson(text: String, format: Format): Reading {
    val document = JsonDocument.parse(text, requireNotNull(format.jsonDialect))
    return Reading.Read(
        format = format,
        root = document.root?.let { JsonNodes().node(it, path = NodePath.Root, name = ROOT, key = ROOT) },
        problems = document.problems,
        comments = document.comments.size
    )
}

/**
 * Reads [text] as newline-delimited JSON, for its tree: an array of its records, a line to an item, each counted by
 * its place among the lines that hold one.
 */
internal fun readNdjson(text: String): Reading {
    val document = NdjsonDocument.parse(text)
    val nodes = JsonNodes()
    val records = document.records.mapIndexedNotNull { index, record ->
        val step = PathStep.Index(index)
        record.value?.let {
            nodes.node(it, NodePath.Root + step, nodes.parts.place(index), childKey(ROOT, step), offset = record.range.start)
        }
    }
    val root = if (document.records.isEmpty()) {
        null
    } else {
        ValueNode(ROOT, emptyList(), ROOT, ValueKind.Array, "[${records.size}]", null, null, records, TextRange(0, text.length))
    }
    return Reading.Read(format = Format.Ndjson, root = root, problems = document.problems)
}

/** The nodes of one JSON document, which share their names, their steps and their paths' steps. */
private class JsonNodes {
    val parts = SharedParts()

    /**
     * The node of [value], at [path] and keyed [key]. A child's key is its parent's and its own step: a key given twice
     * in one object gives its second member a key of its own, `#` and its place, as the tree asks. The value's ranges
     * count from [offset]: the start of its line, for a record of newline-delimited JSON.
     */
    fun node(value: JsonValue, path: NodePath, name: String, key: String, offset: Int = 0): ValueNode {
        val range = TextRange(offset + value.range.start, offset + value.range.end)
        return when (value) {
            is JsonObject -> {
                val seen = HashSet<String>()
                val children = value.members.mapIndexedNotNull { index, member ->
                    val step = parts.key(member.key.name)
                    val own = childKey(key, step, duplicate = index.takeUnless { seen.add(member.key.name) })
                    member.value?.let { node(it, path + step, step.name, own, offset) }
                }
                ValueNode(key, path, name, ValueKind.Object, "{${value.members.size}}", null, null, children, range)
            }

            is JsonArray -> {
                val children = value.items.mapIndexed { index, item ->
                    val step = PathStep.Index(index)
                    node(item, path + step, parts.place(index), childKey(key, step), offset)
                }
                ValueNode(key, path, name, ValueKind.Array, "[${value.items.size}]", null, null, children, range)
            }

            else -> {
                leaf(key, path, name, value, range)
            }
        }
    }
}

/** The node of a string, a number, `true`, `false` or `null`, written at [range]. */
private fun leaf(key: String, path: NodePath, name: String, value: JsonValue, range: TextRange): ValueNode {
    val (kind, text, string) = when (value) {
        is JsonString -> Triple(ValueKind.String, value.raw, value.value)
        is JsonNumber -> Triple(ValueKind.Number, value.raw, null)
        is JsonBoolean -> Triple(ValueKind.Boolean, if (value.value) TRUE else FALSE, null)
        else -> Triple(ValueKind.Null, NULL, null)
    }
    return ValueNode(key, path, name, kind, text, string, string?.let(::previewOf), emptyList(), range)
}

// Written once for every value of the kind, as each is the same text.
private const val TRUE = "true"
private const val FALSE = "false"
private const val NULL = "null"
