package com.developersancho.formatico.core.format.query

import kotlin.math.ceil
import kotlin.math.floor

/** XPath 1.0's core function library, on [index]'s model of a document. */
internal class XPathFunctions(private val index: XPathIndex, private val evaluator: XPathEvaluator) {
    /** What [call] gives in [context]. */
    fun call(call: XPathExpression.Call, context: XPathContext): XPathValue {
        val arguments = XPathArguments(index, evaluator, call.arguments, context)
        return when (call.function) {
            XPathFunction.Last -> number(context.size.toDouble())
            XPathFunction.Position -> number(context.position.toDouble())
            XPathFunction.Count -> number(arguments.nodes(0).size.toDouble())
            XPathFunction.Id -> XPathValue.Nodes(id(arguments.value(0)))
            XPathFunction.LocalName -> text(arguments.nodeOrContext(0)?.localName.orEmpty())
            XPathFunction.NamespaceUri -> text("")
            XPathFunction.Name -> text(arguments.nodeOrContext(0)?.name.orEmpty())
            XPathFunction.StringOf -> text(arguments.stringOrContext(0))
            XPathFunction.Concat -> text(call.arguments.indices.joinToString("") { arguments.string(it) })
            XPathFunction.StartsWith -> truth(arguments.string(0).startsWith(arguments.string(1)))
            XPathFunction.Contains -> truth(arguments.string(0).contains(arguments.string(1)))
            XPathFunction.SubstringBefore -> text(arguments.string(0).substringBefore(arguments.string(1), missingDelimiterValue = ""))
            XPathFunction.SubstringAfter -> text(arguments.string(0).substringAfter(arguments.string(1), missingDelimiterValue = ""))
            XPathFunction.Substring -> text(xpathSubstring(arguments.string(0), arguments.number(1), arguments.numberOrNull(2)))
            XPathFunction.StringLength -> number(codePointCount(arguments.stringOrContext(0)).toDouble())
            XPathFunction.NormalizeSpace -> text(normalizeSpace(arguments.stringOrContext(0)))
            XPathFunction.Translate -> text(xpathTranslate(arguments.string(0), arguments.string(1), arguments.string(2)))
            XPathFunction.BooleanOf -> truth(arguments.truth(0))
            XPathFunction.Not -> truth(!arguments.truth(0))
            XPathFunction.True -> truth(true)
            XPathFunction.False -> truth(false)
            XPathFunction.Lang -> truth(lang(context.node, arguments.string(0)))
            XPathFunction.NumberOf -> number(arguments.numberOrContext(0))
            XPathFunction.Sum -> number(arguments.nodes(0).sumOf { xpathNumber(index.stringValue(it)) })
            XPathFunction.Floor -> number(floor(arguments.number(0)))
            XPathFunction.Ceiling -> number(ceil(arguments.number(0)))
            XPathFunction.Round -> number(xpathRound(arguments.number(0)))
        }
    }

    /**
     * `id()`: the elements whose `xml:id` is one of the names in [value], its string, or each of its nodes'
     * string-values, split at whitespace.
     */
    private fun id(value: XPathValue): List<XPathTreeNode> {
        val written = if (value is XPathValue.Nodes) value.nodes.map(index::stringValue) else listOf(index.string(value))
        val names = written.flatMap { it.split(' ', '\t', '\n', '\r') }.filter { it.isNotEmpty() }
        return inDocumentOrder(names.mapNotNull { index.ids[it] })
    }

    private fun number(value: Double): XPathValue = XPathValue.Number(value)

    private fun text(value: String): XPathValue = XPathValue.Text(value)

    private fun truth(value: Boolean): XPathValue = XPathValue.Truth(value)
}

/** The arguments of a call, [expressions], evaluated in [context] when the function asks for them. */
private class XPathArguments(
    private val index: XPathIndex,
    private val evaluator: XPathEvaluator,
    private val expressions: List<XPathExpression>,
    private val context: XPathContext
) {
    fun value(at: Int): XPathValue = evaluator.evaluate(expressions[at], context)

    fun nodes(at: Int): List<XPathTreeNode> = (value(at) as? XPathValue.Nodes)?.nodes.orEmpty()

    fun string(at: Int): String = index.string(value(at))

    fun number(at: Int): Double = index.number(value(at))

    fun truth(at: Int): Boolean = boolean(value(at))

    /** The number argument [at], or null where the call has none there. */
    fun numberOrNull(at: Int): Double? = if (at < expressions.size) number(at) else null

    /** The first node of the node-set argument [at], or the context node where the call has none there. */
    fun nodeOrContext(at: Int): XPathTreeNode? = if (at < expressions.size) nodes(at).firstOrNull() else context.node

    /** The string argument [at], or the context node's string-value where the call has none there. */
    fun stringOrContext(at: Int): String = if (at < expressions.size) string(at) else index.stringValue(context.node)

    /** The number argument [at], or the context node's string-value as a number where the call has none there. */
    fun numberOrContext(at: Int): Double = if (at < expressions.size) number(at) else xpathNumber(index.stringValue(context.node))
}

/**
 * `substring()`: the characters of [value], counted from 1, from [start] rounded up to, and not including, [start]
 * plus [length], each rounded, or to the end where [length] is null. NaN and the infinities fall as IEEE 754 has them.
 */
internal fun xpathSubstring(value: String, start: Double, length: Double?): String {
    val first = xpathRound(start)
    val end = if (length == null) Double.POSITIVE_INFINITY else first + xpathRound(length)
    return buildString {
        characters(value).forEachIndexed { index, character ->
            val position = (index + 1).toDouble()
            if (position >= first && position < end) append(character)
        }
    }
}

/** `normalize-space()`: [value] without whitespace at its start and its end, and each run of it inside one space. */
internal fun normalizeSpace(value: String): String =
    value.split(' ', '\t', '\n', '\r').filter { it.isNotEmpty() }.joinToString(" ")

/**
 * `translate()`: [value] with each character that is in [from] turned into the one at its place in [to], or taken
 * out where [to] is shorter; a character given twice in [from] is translated as its first is.
 */
internal fun xpathTranslate(value: String, from: String, to: String): String {
    val targets = characters(to)
    val translations = HashMap<String, String>()
    characters(from).forEachIndexed { index, character ->
        if (character !in
            translations
        ) {
            translations[character] = targets.getOrElse(index) { "" }
        }
    }
    return characters(value).joinToString("") { translations[it] ?: it }
}
