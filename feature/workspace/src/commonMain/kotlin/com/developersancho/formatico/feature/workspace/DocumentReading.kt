package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.Problem
import com.developersancho.formatico.core.format.Severity
import com.developersancho.formatico.core.format.json.PathStep
import com.developersancho.formatico.core.format.json.jsonPath
import com.developersancho.formatico.core.format.text.LineMap
import com.developersancho.formatico.core.format.text.TextLayout
import com.developersancho.formatico.core.format.text.TextRange

/** What kind of value a node of the tree is: a YAML alias among them, which stands for the value it points at. */
internal enum class ValueKind { Object, Array, String, Number, Boolean, Null, Alias }

/** Where another node of the tree is: its key, and its path from the root. */
internal data class ValueReference(val key: String, val path: List<PathStep>)

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
 * @property anchor the YAML anchor the value is given, `labels` for `&labels`, or null.
 * @property target where the value a YAML alias points at is, or null for any other value and an alias with no anchor.
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
    val range: TextRange,
    val anchor: String? = null,
    val target: ValueReference? = null
)

/** What Formatico could make of a document. */
internal sealed interface Reading {
    /** It is being read. */
    data object Pending : Reading

    /** It is plain text: in no format, and so with no tree. */
    data object PlainText : Reading

    /**
     * It was read.
     *
     * @property root its value, as much as could be read of it, or null where none could.
     * @property problems what is wrong in it.
     * @property comments how many comments it has, which its tree does not show.
     */
    data class Read(val format: Format, val root: ValueNode?, val problems: List<Problem>, val comments: Int = 0) : Reading {
        /** How many of the [problems] make the document invalid. */
        val errors: Int get() = problems.count { it.severity == Severity.Error }
    }
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

/** Reads [text] as [format], for its tree; a CSV's or a TSV's columns named as [header] says. */
internal fun readDocument(text: String, format: Format?, header: CsvHeader = CsvHeader.FirstRow): Reading = when (format) {
    null -> Reading.PlainText
    Format.Json, Format.Jsonc, Format.Json5 -> readJson(text, format)
    Format.Ndjson -> readNdjson(text)
    Format.Yaml -> readYaml(text)
    Format.Csv, Format.Tsv -> readCsv(text, format, header)
    Format.Toml -> readToml(text)
    Format.Xml -> readXml(text)
    Format.Properties -> readProperties(text)
    Format.Ini -> readIni(text)
}

internal const val ROOT = "$"

/** The key of a child of the node keyed [parent], at [step]; [duplicate], a key's place where it is given again. */
internal fun childKey(parent: String, step: PathStep, duplicate: Int? = null): String =
    parent + jsonPath(listOf(step)).drop(1) + (duplicate?.let { "#$it" } ?: "")

/**
 * The branches a document opens with: the root, and the objects in it, the first first, while the tree shows no more
 * than [OPENED_ROWS] rows: the records of a large log or table are left for the reader to open. A branch that holds
 * nothing but one branch is gone through, as an XML document's root holds its root element alone: what that branch
 * holds is opened in its place.
 */
internal fun defaultExpanded(root: ValueNode?): Set<String> {
    if (root == null) return emptySet()
    val opened = mutableSetOf<String>()
    var node: ValueNode = root
    while (true) {
        opened += node.key
        node = node.children.singleOrNull()?.takeIf { it.children.isNotEmpty() } ?: break
    }
    var rows = node.children.size
    for (child in node.children) {
        if (child.kind != ValueKind.Object || child.children.isEmpty()) continue
        rows += child.children.size
        if (rows > OPENED_ROWS) break
        opened += child.key
    }
    return opened
}

/**
 * How many rows the branches a document opens with show at most, besides those of the branch they stand in: the lazy
 * tree asks for every open row's node up front (PANTRIX_FINDINGS, "Tembel ağaç, açık her satırın `TreeNode`'unu baştan
 * istiyor").
 */
internal const val OPENED_ROWS = 500

/** What [text] read as [format] has wrong in it. */
internal fun problemsOf(text: String, format: Format?): List<Problem> = (readDocument(text, format) as? Reading.Read)?.problems.orEmpty()

/**
 * How many of the errors of [reading], [text] read as JSON or JSONC, JSON5 allows: the comments, the trailing commas,
 * the keys in no quotes and the single quotes that reading it as JSON5 leaves out. None for any other format.
 */
internal fun json5Allows(text: String, format: Format?, reading: Reading): Int {
    if (format != Format.Json && format != Format.Jsonc) return 0
    if (reading !is Reading.Read || reading.errors == 0) return 0
    val asJson5 = readDocument(text, Format.Json5) as? Reading.Read ?: return 0
    return (reading.errors - asJson5.errors).coerceAtLeast(0)
}

/**
 * The innermost value of [root] whose text holds the place [offset], or null where none does. Of two children that hold
 * it, the narrower is taken: a TOML table's text takes in its sub-tables, and a table given between them as well.
 */
internal fun nodeAt(root: ValueNode?, offset: Int): ValueNode? = NodeLocator(root).at(offset)

/**
 * Finds [nodeAt]'s value for many places of one document's text, as an XPath's matches are: where a node's children
 * stand one after another in the text, as most do, the child that holds a place is looked for by halves, and a node
 * is told to be so once.
 */
internal class NodeLocator(private val root: ValueNode?) {
    private val inOrder = HashMap<String, Boolean>()

    /** The innermost value of the root whose text holds the place [offset], or null where none does. */
    fun at(offset: Int): ValueNode? {
        var node = root?.takeIf { offset in it.range.start..it.range.end } ?: return null
        while (true) {
            node = childAt(node, offset) ?: return node
        }
    }

    private fun childAt(node: ValueNode, offset: Int): ValueNode? {
        val children = node.children
        if (children.isEmpty()) return null
        if (!inOrder.getOrPut(node.key) { standInOrder(children) }) {
            return children.filter { offset in it.range.start..it.range.end }.minByOrNull { it.range.length }
        }
        // The last child that starts at the place or before it, and those before it that end there too.
        var low = 0
        var high = children.lastIndex
        var last = -1
        while (low <= high) {
            val middle = (low + high) ushr 1
            if (children[middle].range.start <= offset) {
                last = middle
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        var narrowest: ValueNode? = null
        var place = last
        while (place >= 0 && children[place].range.end >= offset) {
            val child = children[place--]
            if (narrowest == null || child.range.length <= narrowest.range.length) narrowest = child
        }
        return narrowest
    }

    private fun standInOrder(children: List<ValueNode>): Boolean {
        for (place in 1 until children.size) {
            if (children[place - 1].range.end > children[place].range.start) return false
        }
        return true
    }
}

/** The nodes from [root] down to the one whose key is [key], both included, or nothing where there is none. */
internal fun trail(root: ValueNode?, key: String?): List<ValueNode> {
    if (root == null || key == null) return emptyList()
    if (root.key == key) return listOf(root)
    // A child's key is its parent's and more (childKey): only a child whose key starts the one sought can lead to it.
    for (child in root.children) {
        if (!key.startsWith(child.key)) continue
        val below = trail(child, key)
        if (below.isNotEmpty()) return listOf(root) + below
    }
    return emptyList()
}
