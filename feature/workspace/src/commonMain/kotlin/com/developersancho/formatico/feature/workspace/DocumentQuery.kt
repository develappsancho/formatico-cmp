package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Immutable
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.json.jsonPath
import com.developersancho.formatico.core.format.query.JsonKind
import com.developersancho.formatico.core.format.query.JsonPath
import com.developersancho.formatico.core.format.query.JsonValues
import com.developersancho.formatico.core.format.query.QueryError
import com.developersancho.formatico.core.format.query.QueryParse
import com.developersancho.formatico.core.format.query.XPath
import com.developersancho.formatico.core.format.query.XPathResult
import com.developersancho.formatico.core.format.text.TextRange
import com.developersancho.formatico.core.format.xml.XmlDocument

/** The language a document is queried in: XPath for XML, JSONPath for the tree of every other format. */
internal enum class QueryLanguage(val displayName: String) {
    JsonPath("JSONPath"),
    XPath("XPath")
}

/** The language a document of this format is queried in. */
internal val Format.queryLanguage: QueryLanguage get() = if (this == Format.Xml) QueryLanguage.XPath else QueryLanguage.JsonPath

/**
 * A value a query found: [path], where it is in the document as the query's language writes a path; [value], as a row
 * shows it, in the colour of its [kind] where it has one; [key], the tree's value choosing it chooses, or null where it
 * is no value of the tree; and [range], where it is written in the document's text.
 */
@Immutable
internal data class QueryMatch(val path: String, val value: String, val kind: ValueKind?, val key: String?, val range: TextRange)

/** What a query came to. */
internal sealed interface QueryOutcome {
    /** Nothing is asked: the query is blank. */
    data object Unasked : QueryOutcome

    /** The document could not be read: it has no tree to ask. */
    data object Unread : QueryOutcome

    /** The query is no query in its language: [error] says where, in the query as it was written, and why. */
    data class Failed(val error: QueryError) : QueryOutcome

    /** The values the query found, in the order its language gives them: none where it found nothing. */
    data class Found(val matches: List<QueryMatch>) : QueryOutcome

    /** An XPath that comes to a value of its own, a number, a string or a truth, as XPath writes it, and not to nodes. */
    data class Computed(val value: String) : QueryOutcome
}

/** The values [outcome] found, or none. */
internal val QueryOutcome?.matches: List<QueryMatch> get() = (this as? QueryOutcome.Found)?.matches.orEmpty()

/**
 * What [query] finds in [document], read as [reading]: a JSONPath in the tree of any format but XML, an XPath in an
 * XML document's own nodes, as [xml] reads its text. The space around the query is no part of it.
 */
internal fun queryOutcome(
    document: OpenDocument,
    reading: Reading,
    query: String,
    xml: (String) -> XmlDocument = XmlDocument::parse
): QueryOutcome {
    val start = query.indexOfFirst { !it.isWhitespace() }
    if (start < 0) return QueryOutcome.Unasked
    val read = reading as? Reading.Read
    val root = read?.root ?: return QueryOutcome.Unread
    val asked = query.trim()
    val outcome = when (read.format.queryLanguage) {
        QueryLanguage.JsonPath -> jsonPathOutcome(root, asked)
        QueryLanguage.XPath -> xPathOutcome(xml(document.text), root, asked)
    }
    return if (outcome is QueryOutcome.Failed) {
        QueryOutcome.Failed(outcome.error.copy(offset = outcome.error.offset + start))
    } else {
        outcome
    }
}

private fun jsonPathOutcome(root: ValueNode, query: String): QueryOutcome = when (val parsed = JsonPath.parse(query)) {
    is QueryParse.Failed -> QueryOutcome.Failed(parsed.error)

    is QueryParse.Parsed -> {
        val values = TreeValues(root)
        QueryOutcome.Found(
            parsed.query.select(root, values).map { node ->
                QueryMatch(path = jsonPath(node.path), value = node.text, kind = node.kind, key = node.key, range = node.range)
            }
        )
    }
}

private fun xPathOutcome(document: XmlDocument, root: ValueNode, query: String): QueryOutcome = when (val parsed = XPath.parse(query)) {
    is QueryParse.Failed -> QueryOutcome.Failed(parsed.error)

    is QueryParse.Parsed -> when (val result = parsed.query.evaluate(document)) {
        is XPathResult.Nodes -> QueryOutcome.Found(
            result.matches.map { match ->
                // A node is chosen in the tree by the value written where it is: an element's, an attribute's, or the
                // element a text stands in.
                QueryMatch(
                    path = match.path,
                    value = match.value,
                    kind = null,
                    key = nodeAt(root, match.range.start)?.key,
                    range = match.range
                )
            }
        )

        is XPathResult.Text -> QueryOutcome.Computed(result.value)

        is XPathResult.Number -> QueryOutcome.Computed(result.text)

        is XPathResult.Truth -> QueryOutcome.Computed(result.value.toString())
    }
}

/**
 * The tree of [root] as JSONPath reads it. A YAML alias stands for the value it points at, as YAML reads it, unless that
 * value holds the alias itself: then it is the leaf it is written as, so that going down the tree comes to an end.
 */
private class TreeValues(root: ValueNode) : JsonValues<ValueNode> {
    private val byKey: Map<String, ValueNode> by lazy {
        buildMap { root.forEachNode { put(it.key, it) } }
    }

    private fun resolved(node: ValueNode): ValueNode {
        if (node.kind != ValueKind.Alias) return node
        val target = node.target?.let { byKey[it.key] } ?: return node
        return if (node.key.isInside(target.key)) node else target
    }

    override fun kindOf(node: ValueNode): JsonKind = when (resolved(node).kind) {
        ValueKind.Object -> JsonKind.Object

        ValueKind.Array -> JsonKind.Array

        ValueKind.String -> JsonKind.String

        ValueKind.Number -> JsonKind.Number

        ValueKind.Boolean -> JsonKind.Boolean

        ValueKind.Null -> JsonKind.Null

        // An alias that cannot stand for its value is the name it is written with.
        ValueKind.Alias -> JsonKind.String
    }

    override fun childrenOf(node: ValueNode): List<ValueNode> = resolved(node).children

    override fun nameOf(member: ValueNode): String = member.name

    override fun stringOf(node: ValueNode): String = resolved(node).let { it.value ?: it.text }

    override fun numberOf(node: ValueNode): Double = numberWritten(resolved(node).text)

    override fun booleanOf(node: ValueNode): Boolean = resolved(node).text.equals("true", ignoreCase = true)
}

/** Whether a value keyed so stands inside the one keyed [outer], or is it. */
private fun String.isInside(outer: String): Boolean =
    startsWith(outer) && (length == outer.length || this[outer.length] in ".[#")

private fun ValueNode.forEachNode(action: (ValueNode) -> Unit) {
    val left = ArrayDeque(listOf(this))
    while (left.isNotEmpty()) {
        val node = left.removeLast()
        action(node)
        left.addAll(node.children)
    }
}

/**
 * The number [text] writes, in any format's way of writing one: JSON's, JSON5's hexadecimal and its infinities, TOML's
 * underscores and its bases, YAML's `.inf` and `.nan`. Not a number where it is none of them.
 */
internal fun numberWritten(text: String): Double {
    val plain = text.replace("_", "")
    val negative = plain.startsWith('-')
    val digits = plain.removePrefix("-").removePrefix("+")
    val magnitude = when (digits.lowercase()) {
        "inf", ".inf", "infinity" -> Double.POSITIVE_INFINITY
        "nan", ".nan" -> Double.NaN
        else -> radixOf(digits)?.let { (radix, number) -> number.toLongOrNull(radix)?.toDouble() } ?: digits.toDoubleOrNull()
    } ?: Double.NaN
    return if (negative) -magnitude else magnitude
}

/** The base a number's [digits] are written in, by their prefix, and the digits after it; null for the tenth base. */
private fun radixOf(digits: String): Pair<Int, String>? = when (digits.take(2).lowercase()) {
    "0x" -> HEXADECIMAL to digits.drop(2)
    "0o" -> OCTAL to digits.drop(2)
    "0b" -> BINARY to digits.drop(2)
    else -> null
}

private const val HEXADECIMAL = 16
private const val OCTAL = 8
private const val BINARY = 2
