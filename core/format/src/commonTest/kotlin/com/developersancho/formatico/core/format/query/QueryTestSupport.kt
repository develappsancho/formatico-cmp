package com.developersancho.formatico.core.format.query

import com.developersancho.formatico.core.format.json.JsonArray
import com.developersancho.formatico.core.format.json.JsonBoolean
import com.developersancho.formatico.core.format.json.JsonDocument
import com.developersancho.formatico.core.format.json.JsonNull
import com.developersancho.formatico.core.format.json.JsonNumber
import com.developersancho.formatico.core.format.json.JsonObject
import com.developersancho.formatico.core.format.json.JsonString
import com.developersancho.formatico.core.format.json.JsonValue
import com.developersancho.formatico.core.format.xml.XmlDocument
import kotlin.test.assertTrue
import kotlin.test.fail

/** The values of a JSON document as JSONPath reads them, each member's name found by its value. */
internal class DocumentValues(document: JsonDocument) : JsonValues<JsonValue> {
    private val names = HashMap<JsonValue, String>()

    init {
        val pending = ArrayList<JsonValue>()
        document.root?.let { pending += it }
        while (pending.isNotEmpty()) {
            val value = pending.removeAt(pending.lastIndex)
            if (value is JsonObject) value.members.forEach { member -> member.value?.let { names[it] = member.key.name } }
            pending += childrenOf(value)
        }
    }

    override fun kindOf(node: JsonValue): JsonKind = when (node) {
        is JsonObject -> JsonKind.Object
        is JsonArray -> JsonKind.Array
        is JsonString -> JsonKind.String
        is JsonNumber -> JsonKind.Number
        is JsonBoolean -> JsonKind.Boolean
        is JsonNull -> JsonKind.Null
    }

    override fun childrenOf(node: JsonValue): List<JsonValue> = when (node) {
        is JsonObject -> node.members.mapNotNull { it.value }
        is JsonArray -> node.items
        else -> emptyList()
    }

    override fun nameOf(member: JsonValue): String = names.getValue(member)

    override fun stringOf(node: JsonValue): String = (node as JsonString).value

    override fun numberOf(node: JsonValue): Double = (node as JsonNumber).raw.toDouble()

    override fun booleanOf(node: JsonValue): Boolean = (node as JsonBoolean).value
}

/** A value built in code, for documents deeper or wider than a parser is given. */
internal class Built(val kind: JsonKind, val children: List<Built> = emptyList(), val name: String = "", val number: Double = 0.0)

/** The values of a [Built] document. */
internal object BuiltValues : JsonValues<Built> {
    override fun kindOf(node: Built): JsonKind = node.kind

    override fun childrenOf(node: Built): List<Built> = node.children

    override fun nameOf(member: Built): String = member.name

    override fun stringOf(node: Built): String = node.name

    override fun numberOf(node: Built): Double = node.number

    override fun booleanOf(node: Built): Boolean = false
}

/** [query], read; the test fails where it cannot be. */
internal fun jsonPath(query: String): JsonPath = when (val parsed = JsonPath.parse(query)) {
    is QueryParse.Parsed -> parsed.query
    is QueryParse.Failed -> fail("Not read: $query: ${parsed.error}")
}

/** What [query] selects in [json], each value as it is written. */
internal fun select(query: String, json: String): List<String> {
    val document = JsonDocument.parse(json)
    assertTrue(document.isValid, "Not valid: ${document.problems}")
    return jsonPath(query).select(requireNotNull(document.root), DocumentValues(document)).map { document.text(it.range) }
}

/** Why [query] cannot be read, as its code and its offset: `NotSingular 3`. */
internal fun jsonPathError(query: String): String = when (val parsed = JsonPath.parse(query)) {
    is QueryParse.Failed -> "${parsed.error.code} ${parsed.error.offset}"
    is QueryParse.Parsed -> fail("Read: $query")
}

/** [values], each between double quotes, as JSON writes a string. */
internal fun quoted(vararg values: String): List<String> = values.map { "\"$it\"" }

/** [expression], read; the test fails where it cannot be. */
internal fun xpath(expression: String): XPath = when (val parsed = XPath.parse(expression)) {
    is QueryParse.Parsed -> parsed.query
    is QueryParse.Failed -> fail("Not read: $expression: ${parsed.error}")
}

/** What [expression] evaluates to in [xml]. */
internal fun evaluate(expression: String, xml: String): XPathResult = xpath(expression).evaluate(XmlDocument.parse(xml))

/** The nodes [expression] selects in [xml]. */
internal fun matches(expression: String, xml: String): List<XPathMatch> =
    (evaluate(expression, xml) as? XPathResult.Nodes)?.matches ?: fail("Not a node-set: $expression")

/** The paths of the nodes [expression] selects in [xml]. */
internal fun paths(expression: String, xml: String): List<String> = matches(expression, xml).map { it.path }

/** What [expression] evaluates to in [xml], a string, a number or a boolean, as `string()` writes it. */
internal fun written(expression: String, xml: String = "<r/>"): String = when (val result = evaluate(expression, xml)) {
    is XPathResult.Text -> result.value
    is XPathResult.Number -> result.text
    is XPathResult.Truth -> result.value.toString()
    is XPathResult.Nodes -> fail("A node-set: $expression")
}

/** Why [expression] cannot be read, as its code and its offset: `WrongType 6`. */
internal fun xpathError(expression: String): String = when (val parsed = XPath.parse(expression)) {
    is QueryParse.Failed -> "${parsed.error.code} ${parsed.error.offset}"
    is QueryParse.Parsed -> fail("Read: $expression")
}
