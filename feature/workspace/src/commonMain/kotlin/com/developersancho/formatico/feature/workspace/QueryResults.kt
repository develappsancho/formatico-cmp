package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format

/**
 * The results of [outcome], found in [document], whose value is [root], as a document of their own: JSONPath's values
 * as a JSON array, whatever the format they were found in; XPath's nodes as an XML document's `<results>`, an element
 * as it is written and any other node by its string-value in a `<result>`; an XPath's own value in a `<result>`
 * alone. Null where it found nothing.
 */
internal fun queryResults(document: OpenDocument, root: ValueNode?, outcome: QueryOutcome?): NewDocument? {
    val language = document.format?.queryLanguage ?: return null
    val written = when (outcome) {
        is QueryOutcome.Found -> outcome.matches.takeIf { it.isNotEmpty() }?.let { matches ->
            when (language) {
                QueryLanguage.JsonPath -> jsonResults(root ?: return null, matches)
                QueryLanguage.XPath -> xmlResults(document.text, matches)
            }
        }

        is QueryOutcome.Computed -> if (language == QueryLanguage.XPath) "<result>${escapedXml(outcome.value)}</result>" else null

        else -> null
    } ?: return null
    val format = if (language == QueryLanguage.XPath) Format.Xml else Format.Json
    val name = "${stemOf(document.name)}-query.${format.extensions.first()}"
    // Laid out as Format lays out a document; what cannot be laid out is kept as it was put together.
    val laid = edited(OpenDocument(id = 0, name = name, text = written, format = format), DocumentEdit.Format)
    return NewDocument(name = name, text = (laid as? Edited.Text)?.text ?: written, format = format)
}

/** A file's name without its extension; `query` for a document with no name. */
private fun stemOf(name: String?): String = name?.substringBeforeLast('.')?.takeIf { it.isNotEmpty() } ?: "query"

private fun jsonResults(root: ValueNode, matches: List<QueryMatch>): String {
    val byKey = buildMap { root.forEachValue { put(it.key, it) } }
    val writer = JsonWriter(byKey)
    matches.forEachIndexed { index, match ->
        if (index > 0) writer.out.append(',')
        val node = match.key?.let(byKey::get)
        if (node == null) writer.out.append("null") else writer.write(node, inside = emptySet())
    }
    return "[${writer.out}]"
}

/**
 * Writes values of a tree, whose values are [byKey], as JSON: a string's value quoted, a number as it is written where
 * JSON writes it so, and an alias as the value it points at; one inside that value, or one past the [MAX_ALIASES] a
 * document is written with, as its name, so that a document of aliases on aliases is not written out without end.
 */
private class JsonWriter(private val byKey: Map<String, ValueNode>) {
    val out = StringBuilder()
    private var aliases = 0

    fun write(node: ValueNode, inside: Set<String>) {
        when (node.kind) {
            ValueKind.Object -> {
                out.append('{')
                node.children.forEachIndexed { index, child ->
                    if (index > 0) out.append(',')
                    out.appendQuoted(child.name).append(':')
                    write(child, inside)
                }
                out.append('}')
            }

            ValueKind.Array -> {
                out.append('[')
                node.children.forEachIndexed { index, child ->
                    if (index > 0) out.append(',')
                    write(child, inside)
                }
                out.append(']')
            }

            ValueKind.String -> out.appendQuoted(node.value ?: node.text)

            ValueKind.Number -> if (JsonNumber.matches(node.text)) out.append(node.text) else out.appendNumber(node.text)

            ValueKind.Boolean -> out.append(node.text.equals("true", ignoreCase = true))

            ValueKind.Null -> out.append("null")

            ValueKind.Alias -> {
                val target = node.target?.let { byKey[it.key] }
                if (target == null || target.key in inside || aliases == MAX_ALIASES) {
                    out.appendQuoted(node.text)
                } else {
                    aliases++
                    write(target, inside + target.key)
                }
            }
        }
    }
}

/** A number JSON cannot write as it is written: as JSON writes its value, or quoted where JSON has none, as `inf`. */
private fun StringBuilder.appendNumber(text: String) {
    val number = numberWritten(text)
    when {
        !number.isFinite() -> appendQuoted(text)
        number == kotlin.math.floor(number) && kotlin.math.abs(number) < LONG_EXACT -> append(number.toLong())
        else -> append(number)
    }
}

private fun StringBuilder.appendQuoted(text: String): StringBuilder {
    append('"')
    text.forEach { char ->
        when (char) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            in '\u0000'..'\u001F' -> append("\\u").append(char.code.toString(HEX_RADIX).padStart(UNICODE_DIGITS, '0'))
            else -> append(char)
        }
    }
    return append('"')
}

private fun xmlResults(text: String, matches: List<QueryMatch>): String = buildString {
    append("<results>")
    matches.forEach { match ->
        val written = text.substring(match.range.start, match.range.end)
        if (written.length > 1 && written[0] == '<' && written[1].isLetter()) {
            append(written)
        } else {
            append("<result>").append(escapedXml(match.value)).append("</result>")
        }
    }
    append("</results>")
}

private fun escapedXml(text: String): String = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

private fun ValueNode.forEachValue(action: (ValueNode) -> Unit) {
    val left = ArrayDeque(listOf(this))
    while (left.isNotEmpty()) {
        val node = left.removeLast()
        action(node)
        left.addAll(node.children)
    }
}

private val JsonNumber = Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")

private const val MAX_ALIASES = 1_000
private const val LONG_EXACT = 9.007199254740992E15
private const val HEX_RADIX = 16
private const val UNICODE_DIGITS = 4
