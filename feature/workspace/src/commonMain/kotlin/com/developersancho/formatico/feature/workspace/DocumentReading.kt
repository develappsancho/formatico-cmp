package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.Severity
import com.developersancho.formatico.core.format.json.JsonArray
import com.developersancho.formatico.core.format.json.JsonBoolean
import com.developersancho.formatico.core.format.json.JsonDocument
import com.developersancho.formatico.core.format.json.JsonNumber
import com.developersancho.formatico.core.format.json.JsonObject
import com.developersancho.formatico.core.format.json.JsonString
import com.developersancho.formatico.core.format.json.JsonValue
import com.developersancho.formatico.core.format.json.PathStep
import com.developersancho.formatico.core.format.json.jsonPath
import com.developersancho.formatico.core.format.text.LineMap
import com.developersancho.formatico.core.format.text.TextLayout
import com.developersancho.formatico.core.format.text.TextRange

/** What kind of value a node of the tree is. */
internal enum class ValueKind { Object, Array, String, Number, Boolean, Null }

/**
 * A value of a document, as its tree shows it.
 *
 * @property key its JSONPath, which no other node of the document has: what the tree opens and chooses it by.
 * @property path the steps from the root to it.
 * @property name its key in its object, its place in its array, or `$` for the root.
 * @property kind what kind of value it is.
 * @property text a value as it is written; for an object or an array, how much it holds: `{11}`, `[2]`.
 * @property value a string's value, its quotes taken off and its escapes read; null for any other kind.
 * @property preview what a string looks like it is: a date, a colour, a link, a picture.
 * @property children what an object or an array holds, in order.
 * @property range where the value is written in the document's text.
 */
internal data class ValueNode(
    val key: String,
    val path: List<PathStep>,
    val name: String,
    val kind: ValueKind,
    val text: String,
    val value: String?,
    val preview: ValuePreview?,
    val children: List<ValueNode>,
    val range: TextRange
)

/** What Formatico could make of a document. */
internal sealed interface Reading {
    /** It is being read. */
    data object Pending : Reading

    /** It is plain text, or in [format], which Formatico does not read yet. */
    data class Unread(val format: Format?) : Reading

    /**
     * It was read.
     *
     * @property root its value, as much as could be read of it, or null where none could.
     * @property errors how many errors it has: none, for a valid document.
     */
    data class Read(val format: Format, val root: ValueNode?, val errors: Int) : Reading
}

/**
 * How a document's text is laid out.
 *
 * @property lines how many lines it has; a last line break starts none.
 * @property bytes how many bytes it is in UTF-8.
 */
internal data class TextStats(val lines: Int, val bytes: Long, val layout: TextLayout) {
    companion object {
        fun of(text: String): TextStats {
            val endsWithBreak = text.endsWith('\n') || text.endsWith('\r')
            val lines = if (text.isEmpty()) 0 else LineMap(text).lineCount - if (endsWithBreak) 1 else 0
            return TextStats(lines, text.encodeToByteArray().size.toLong(), TextLayout.of(text))
        }
    }
}

/** Reads [text] as [format], for its tree. */
internal fun readDocument(text: String, format: Format?): Reading {
    val dialect = format?.jsonDialect ?: return Reading.Unread(format)
    val document = JsonDocument.parse(text, dialect)
    return Reading.Read(
        format = format,
        root = document.root?.let { node(it, path = emptyList(), name = ROOT) },
        errors = document.problems.count { it.severity == Severity.Error }
    )
}

private const val ROOT = "$"

/**
 * The node of [value], at [path] and keyed [key]. A child's key is its parent's and its own step: a key given twice in
 * one object gives its second member a key of its own, `#` and its place, as the tree asks.
 */
private fun node(value: JsonValue, path: List<PathStep>, name: String, key: String = jsonPath(path)): ValueNode =
    when (value) {
        is JsonObject -> {
            val seen = HashSet<String>()
            val children = value.members.mapIndexedNotNull { index, member ->
                val step = PathStep.Key(member.key.name)
                val own = jsonPath(listOf(step)).drop(1) + if (seen.add(member.key.name)) "" else "#$index"
                member.value?.let { node(it, path + step, member.key.name, key + own) }
            }
            ValueNode(key, path, name, ValueKind.Object, "{${value.members.size}}", null, null, children, value.range)
        }

        is JsonArray -> {
            val children = value.items.mapIndexed { index, item ->
                node(item, path + PathStep.Index(index), "$index", "$key[$index]")
            }
            ValueNode(key, path, name, ValueKind.Array, "[${value.items.size}]", null, null, children, value.range)
        }

        else -> leaf(key, path, name, value)
    }

/** The node of a string, a number, `true`, `false` or `null`. */
private fun leaf(key: String, path: List<PathStep>, name: String, value: JsonValue): ValueNode {
    val (kind, text, string) = when (value) {
        is JsonString -> Triple(ValueKind.String, value.raw, value.value)
        is JsonNumber -> Triple(ValueKind.Number, value.raw, null)
        is JsonBoolean -> Triple(ValueKind.Boolean, "${value.value}", null)
        else -> Triple(ValueKind.Null, "null", null)
    }
    return ValueNode(key, path, name, kind, text, string, string?.let(::previewOf), emptyList(), value.range)
}

/** The branches a document opens with: the root, and the objects in it. */
internal fun defaultExpanded(root: ValueNode?): Set<String> {
    if (root == null) return emptySet()
    return setOf(root.key) +
        root.children.filter { it.kind == ValueKind.Object && it.children.isNotEmpty() }.map { it.key }
}

/** The nodes from [root] down to the one whose key is [key], both included, or nothing where there is none. */
internal fun trail(root: ValueNode?, key: String?): List<ValueNode> {
    if (root == null || key == null) return emptyList()
    if (root.key == key) return listOf(root)
    for (child in root.children) {
        val below = trail(child, key)
        if (below.isNotEmpty()) return listOf(root) + below
    }
    return emptyList()
}
