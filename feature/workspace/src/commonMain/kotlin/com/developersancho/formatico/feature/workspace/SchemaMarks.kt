package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.json.PathStep
import com.developersancho.formatico.core.format.text.TextRange

/** The value [path] leads to from [root], or null where it leads to none: a name given twice, its first value. */
internal fun nodeAtPath(root: ValueNode, path: List<PathStep>): ValueNode? =
    path.fold<PathStep, ValueNode?>(root) { here, step -> here?.children?.firstOrNull { it.path.lastOrNull() == step } }

/**
 * What a problem of [node] marks in [text]: a single value itself; an object or an array by its key, where it has
 * one written before it as `"customer": {` or `customer:` writes it, which a line under its whole value would hide.
 */
internal fun markOf(node: ValueNode, text: String): TextRange {
    val range = node.range
    val branch = node.kind == ValueKind.Object || node.kind == ValueKind.Array
    if (!branch) return range
    return keyBefore(text, range.start) ?: TextRange(range.start, minOf(range.start + 1, text.length).coerceAtLeast(range.start))
}

/** Where the key of a value written at [start] is: before it, over spaces, past its `:` or `=` and spaces again. */
private fun keyBefore(text: String, start: Int): TextRange? {
    val sign = text.lastNonSpaceBefore(start)
    if (sign < 0 || (text[sign] != ':' && text[sign] != '=')) return null
    val end = text.lastNonSpaceBefore(sign) + 1
    if (end <= 0) return null
    val last = text[end - 1]
    return if (last == '"' || last == '\'') quotedKeyEndingAt(text, end) else bareKeyEndingAt(text, end)
}

/** Where in [this] the last character before [index] that is no space is, or -1. */
private fun String.lastNonSpaceBefore(index: Int): Int {
    var at = index - 1
    while (at >= 0 && this[at].isWhitespace()) at--
    return at
}

/** The key in quotes whose closing quote stands just before [end], its quotes with it; null where it opens nowhere. */
private fun quotedKeyEndingAt(text: String, end: Int): TextRange? {
    val quote = text[end - 1]
    var from = end - 2
    while (from >= 0 && !opensAt(text, from, quote)) from--
    return if (from >= 0) TextRange(from, end) else null
}

/** Whether [quote] at [at] in [text] opens a key: it is not escaped. */
private fun opensAt(text: String, at: Int, quote: Char): Boolean = text[at] == quote && (at == 0 || text[at - 1] != '\\')

/** The key with no quotes that ends just before [end]: letters, digits and `_`, `-`, `$`, back to a space or a bracket. */
private fun bareKeyEndingAt(text: String, end: Int): TextRange? {
    if (!text[end - 1].let { it.isLetterOrDigit() || it in "_-$" }) return null
    var from = end - 1
    while (from > 0 && !text[from - 1].isWhitespace() && text[from - 1] !in "{,[") from--
    return TextRange(from, end)
}
