package com.developersancho.formatico.core.format.yaml

/** The marker a document starts with: `---`. */
internal const val DOCUMENT_START = "---"

private const val ESCAPE_DIGITS = 4
private const val HEX_RADIX = 16

/**
 * Each document on one line, in YAML's flow style, with no comment: `{a: 1,b: [2,3]}`. Every anchor, alias and tag is
 * kept, and every scalar's value; a scalar that cannot stand in a flow collection as it is written is written in double
 * quotes.
 *
 * @throws IllegalArgumentException when the document is not valid: there is no value to write out.
 */
fun YamlDocument.minified(): String {
    require(isValid) { "Only a valid document is written out again" }
    val lines = ArrayList<String>()
    documents.forEachIndexed { index, root ->
        val start = starts.getOrNull(index) ?: YamlDocumentStart(explicit = false, directives = emptyList())
        lines += start.directives
        if (index > 0 || start.explicit || start.directives.isNotEmpty()) lines += DOCUMENT_START
        root?.let { lines += flow(it, text) }
    }
    return lines.joinToString("\n", postfix = "\n")
}

/** What a plain scalar cannot hold and still stand in a flow collection, or as a key. */
private val FlowUnsafe = Regex("""[,\[\]{}#:]|^[-?|>!&*%@`'"]|\s$|^\s""")

/** [node] in flow style, on one line. */
internal fun flow(node: YamlNode, text: String): String {
    val properties = propertiesOf(node)
    val value = when (node) {
        is YamlMapping -> node.entries.joinToString(",", prefix = "{", postfix = "}") {
            flow(it.key, text) + ": " + flow(it.value, text)
        }

        is YamlSequence -> node.items.joinToString(",", prefix = "[", postfix = "]") { flow(it, text) }

        is YamlAlias -> "*${node.name}"

        is YamlScalar -> flowScalar(node)
    }
    return if (properties.isEmpty()) value else "$properties $value"
}

/** A scalar as it can stand in a flow collection: as it is written where it can, or else in double quotes. */
private fun flowScalar(scalar: YamlScalar): String {
    val oneLine = scalar.raw.none { it == '\n' || it == '\r' }
    return when (scalar.style) {
        YamlScalarStyle.Plain -> when {
            scalar.raw.isEmpty() -> if (scalar.tag == null) "null" else "\"\""
            oneLine && (scalar.kind != YamlScalarKind.String || !FlowUnsafe.containsMatchIn(scalar.raw)) -> scalar.raw
            else -> doubleQuoted(scalar.value)
        }

        YamlScalarStyle.SingleQuoted, YamlScalarStyle.DoubleQuoted -> if (oneLine) {
            scalar.raw
        } else {
            doubleQuoted(
                scalar.value
            )
        }

        YamlScalarStyle.Literal, YamlScalarStyle.Folded -> doubleQuoted(scalar.value)
    }
}

/** [value] in double quotes, its backslashes, quotes and control characters escaped. */
internal fun doubleQuoted(value: String): String = buildString(value.length + 2) {
    append('"')
    value.forEach { char ->
        when (char) {
            '\\' -> append("\\\\")

            '"' -> append("\\\"")

            '\n' -> append("\\n")

            '\r' -> append("\\r")

            '\t' -> append("\\t")

            in '\u0000'..'\u001F', '\u007F' ->
                append("\\u").append(char.code.toString(HEX_RADIX).padStart(ESCAPE_DIGITS, '0'))

            else -> append(char)
        }
    }
    append('"')
}

/** A value's anchor and tag, `&labels !!map`, as they are written before it, or nothing. */
internal fun propertiesOf(node: YamlNode): String =
    listOfNotNull(node.anchor?.let { "&$it" }, node.tag).joinToString(" ")

internal val YamlScalar.isBlock: Boolean
    get() = style == YamlScalarStyle.Literal || style == YamlScalarStyle.Folded

/** Whether [node] is in block style: a mapping or a sequence with something in it, not in braces or brackets. */
internal fun isBlockCollection(node: YamlNode): Boolean = when (node) {
    is YamlMapping -> !node.flow && node.entries.isNotEmpty()
    is YamlSequence -> !node.flow && node.items.isNotEmpty()
    else -> false
}
