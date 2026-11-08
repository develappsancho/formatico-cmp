package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.json.PathStep
import com.developersancho.formatico.core.format.text.TextRange
import com.developersancho.formatico.core.format.toml.TomlArray
import com.developersancho.formatico.core.format.toml.TomlBoolean
import com.developersancho.formatico.core.format.toml.TomlDateTime
import com.developersancho.formatico.core.format.toml.TomlDocument
import com.developersancho.formatico.core.format.toml.TomlFloat
import com.developersancho.formatico.core.format.toml.TomlInteger
import com.developersancho.formatico.core.format.toml.TomlString
import com.developersancho.formatico.core.format.toml.TomlTable
import com.developersancho.formatico.core.format.toml.TomlValue

/**
 * Reads [text] as TOML, for its tree: its tables are objects, an array of tables an array of them, and a date or a time
 * a string, written as it is and previewed as a date.
 */
internal fun readToml(text: String): Reading {
    val document = TomlDocument.parse(text)
    return Reading.Read(
        format = Format.Toml,
        root = TomlNodes().node(document.root, NodePath.Root, ROOT, ROOT),
        problems = document.problems,
        comments = document.comments.size
    )
}

/** The nodes of one TOML document, which share their names, their steps and their paths' steps. */
private class TomlNodes {
    private val parts = SharedParts()

    /** The node of [value], at [path] and keyed [key]. */
    fun node(value: TomlValue, path: NodePath, name: String, key: String): ValueNode = when (value) {
        is TomlTable -> {
            val seen = HashSet<String>()
            val children = value.entries.mapIndexed { index, entry ->
                val step = parts.key(entry.key)
                node(entry.value, path + step, step.name, childKey(key, step, duplicate = index.takeUnless { seen.add(entry.key) }))
            }
            // A table's sub-tables may be given headers of their own further down: its range takes them in, so that the
            // caret in one is in the table as well.
            val start = children.minOfOrNull { it.range.start }?.coerceAtMost(value.range.start) ?: value.range.start
            val end = children.maxOfOrNull { it.range.end }?.coerceAtLeast(value.range.end) ?: value.range.end
            ValueNode(key, path, name, ValueKind.Object, "{${children.size}}", null, null, children, TextRange(start, end))
        }

        is TomlArray -> {
            val children = value.items.mapIndexed { index, item ->
                val step = PathStep.Index(index)
                node(item, path + step, parts.place(index), childKey(key, step))
            }
            ValueNode(key, path, name, ValueKind.Array, "[${children.size}]", null, null, children, value.range)
        }

        is TomlString -> ValueNode(
            key,
            path,
            name,
            ValueKind.String,
            value.raw,
            value.value,
            previewOf(value.value),
            emptyList(),
            value.range
        )

        is TomlDateTime -> ValueNode(
            key,
            path,
            name,
            ValueKind.String,
            value.raw,
            value.raw,
            previewOf(value.raw),
            emptyList(),
            value.range
        )

        is TomlInteger -> ValueNode(key, path, name, ValueKind.Number, value.raw, null, null, emptyList(), value.range)

        is TomlFloat -> ValueNode(key, path, name, ValueKind.Number, value.raw, null, null, emptyList(), value.range)

        is TomlBoolean -> ValueNode(
            key,
            path,
            name,
            ValueKind.Boolean,
            if (value.value) "true" else "false",
            null,
            null,
            emptyList(),
            value.range
        )
    }
}
