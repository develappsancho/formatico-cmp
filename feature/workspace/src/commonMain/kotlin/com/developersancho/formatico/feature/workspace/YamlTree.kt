package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.json.PathStep
import com.developersancho.formatico.core.format.text.TextRange
import com.developersancho.formatico.core.format.yaml.YamlAlias
import com.developersancho.formatico.core.format.yaml.YamlDocument
import com.developersancho.formatico.core.format.yaml.YamlMapping
import com.developersancho.formatico.core.format.yaml.YamlNode
import com.developersancho.formatico.core.format.yaml.YamlScalar
import com.developersancho.formatico.core.format.yaml.YamlScalarKind
import com.developersancho.formatico.core.format.yaml.YamlScalarStyle
import com.developersancho.formatico.core.format.yaml.YamlSequence

/**
 * Reads [text] as YAML, for its tree. A stream of one document is that document's tree; a stream of more is an array
 * of them, a document to an item.
 */
internal fun readYaml(text: String): Reading {
    val document = YamlDocument.parse(text)
    val tree = YamlTree(text)
    val documents = document.documents
    val root = when (documents.size) {
        0 -> null
        1 -> documents.single()?.let { tree.node(it, path = emptyList(), name = ROOT, key = ROOT) }
        else -> tree.stream(documents)
    }
    return Reading.Read(
        format = Format.Yaml,
        root = root,
        problems = document.problems
    )
}

/** Builds the tree's nodes of a YAML text, keeping where each anchored value is for the aliases after it. */
private class YamlTree(private val text: String) {
    /** The anchored values met so far, by the value: the same object, as an alias's target is. */
    private val anchored = HashMap<YamlNode, ValueReference>()

    fun stream(documents: List<YamlNode?>): ValueNode {
        val children = documents.mapIndexed { index, document ->
            val step = PathStep.Index(index)
            val key = childKey(ROOT, step)
            document?.let { node(it, listOf(step), "$index", key) }
                ?: ValueNode(key, listOf(step), "$index", ValueKind.Null, "", null, null, emptyList(), TextRange(0, 0))
        }
        val text = "[${documents.size}]"
        return ValueNode(
            ROOT,
            emptyList(),
            ROOT,
            ValueKind.Array,
            text,
            null,
            null,
            children,
            TextRange(
                0,
                this.text.length
            )
        )
    }

    fun node(value: YamlNode, path: List<PathStep>, name: String, key: String): ValueNode {
        // An alias inside its own anchor's value points at a node still being made: its place is known already.
        if (value.anchor != null) anchored[value] = ValueReference(key, path)
        return when (value) {
            is YamlMapping -> {
                mapping(value, path, name, key)
            }

            is YamlSequence -> {
                val children = value.items.mapIndexed { index, item ->
                    val step = PathStep.Index(index)
                    node(item, path + step, "$index", childKey(key, step))
                }
                ValueNode(
                    key,
                    path,
                    name,
                    ValueKind.Array,
                    "[${value.items.size}]",
                    null,
                    null,
                    children,
                    value.range,
                    anchor = value.anchor
                )
            }

            is YamlScalar -> {
                scalar(value, path, name, key)
            }

            is YamlAlias -> {
                ValueNode(
                    key = key,
                    path = path,
                    name = name,
                    kind = ValueKind.Alias,
                    text = "*${value.name}",
                    value = null,
                    preview = null,
                    children = emptyList(),
                    range = value.range,
                    target = value.target?.let { anchored[it] }
                )
            }
        }
    }

    private fun mapping(value: YamlMapping, path: List<PathStep>, name: String, key: String): ValueNode {
        val seen = HashSet<String>()
        val children = value.entries.mapIndexed { index, entry ->
            val keyName =
                (entry.key as? YamlScalar)?.value ?: text.substring(entry.key.range.start, entry.key.range.end)
            val step = PathStep.Key(keyName)
            node(
                entry.value,
                path + step,
                keyName,
                childKey(
                    key,
                    step,
                    duplicate = index.takeUnless {
                        seen.add(keyName)
                    }
                )
            )
        }
        return ValueNode(
            key,
            path,
            name,
            ValueKind.Object,
            "{${value.entries.size}}",
            null,
            null,
            children,
            value.range,
            anchor = value.anchor
        )
    }

    private fun scalar(value: YamlScalar, path: List<PathStep>, name: String, key: String): ValueNode {
        val kind = when (value.kind) {
            YamlScalarKind.Null -> ValueKind.Null
            YamlScalarKind.Boolean -> ValueKind.Boolean
            YamlScalarKind.Integer, YamlScalarKind.Float -> ValueKind.Number
            YamlScalarKind.String -> ValueKind.String
        }
        val string = value.value.takeIf { kind == ValueKind.String }
        return ValueNode(
            key = key,
            path = path,
            name = name,
            kind = kind,
            text = shown(value),
            value = string,
            preview = string?.let(::previewOf),
            children = emptyList(),
            range = value.range,
            anchor = value.anchor
        )
    }

    /** A scalar as a row shows it: as it is written, and a block scalar as its indicator and its first line. */
    private fun shown(value: YamlScalar): String = when (value.style) {
        YamlScalarStyle.Literal, YamlScalarStyle.Folded -> {
            val indicator = if (value.style == YamlScalarStyle.Literal) "|" else ">"
            val lines = value.value.trimEnd('\n').lines()
            "$indicator ${lines.first()}" + if (lines.size > 1) " …" else ""
        }

        else -> {
            value.raw.lines().joinToString(" ") { it.trim() }
        }
    }
}
