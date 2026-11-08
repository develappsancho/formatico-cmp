package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.convert.DataArray
import com.developersancho.formatico.core.format.convert.convert

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
                QueryLanguage.JsonPath -> jsonResults(root ?: return null, document.format, matches)
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

/**
 * The values [matches] found in the tree whose root is [root], read as [format], as a JSON array: written by the
 * converter, as Convert writes them, an alias as the value it points at and a number JSON cannot write as JSON does.
 */
private fun jsonResults(root: ValueNode, format: Format, matches: List<QueryMatch>): String {
    val byKey = buildMap { root.forEachValue { put(it.key, it) } }
    val values = dataValuesOf(root, format, matches.map { match -> match.key?.let(byKey::get) })
    return convert(DataArray(values), Format.Json).text
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
