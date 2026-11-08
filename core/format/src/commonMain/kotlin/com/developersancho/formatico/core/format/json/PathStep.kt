package com.developersancho.formatico.core.format.json

/** A step from a value to one inside it: a member, by its key, or an item, by its place. */
sealed interface PathStep {
    /** The member whose key is [name]. */
    data class Key(val name: String) : PathStep

    /** The item at [index], counted from 0. */
    data class Index(val index: Int) : PathStep
}

private val PlainKey = Regex("[A-Za-z_][A-Za-z0-9_]*")

private const val HEX_RADIX = 16
private const val UNICODE_ESCAPE_DIGITS = 4

/**
 * The JSONPath of [steps] from the document's root, as RFC 9535 writes it: `$.items[0].sku`, and a key that is no
 * plain name in brackets, `$['first name']`.
 */
fun jsonPath(steps: List<PathStep>): String = buildString {
    append('$')
    steps.forEach { step ->
        when (step) {
            is PathStep.Index -> append('[').append(step.index).append(']')

            is PathStep.Key -> if (PlainKey.matches(step.name)) {
                append('.').append(step.name)
            } else {
                append('[').appendQuoted(step.name, '\'').append(']')
            }
        }
    }
}

/**
 * The JSON Pointer of [steps], as RFC 6901 writes it: `/items/0/sku`, a `~` in a key as `~0` and a `/` as `~1`, and the
 * whole document as nothing at all.
 */
fun jsonPointer(steps: List<PathStep>): String = steps.joinToString(separator = "") { step ->
    when (step) {
        is PathStep.Index -> "/${step.index}"
        is PathStep.Key -> "/" + step.name.replace("~", "~0").replace("/", "~1")
    }
}

/**
 * The path of [steps] as jq writes it: `.items[0].sku`, a key that is no plain name in quotes, `."first name"`, and the
 * whole document as `.`.
 */
fun jqPath(steps: List<PathStep>): String {
    if (steps.isEmpty()) return "."
    return buildString {
        steps.forEach { step ->
            when (step) {
                // An item of the document itself is `.[0]`; of a value inside it, `.items[0]`.
                is PathStep.Index -> append(if (isEmpty()) ".[" else "[").append(step.index).append(']')

                is PathStep.Key -> if (PlainKey.matches(step.name)) {
                    append('.').append(step.name)
                } else {
                    append('.').appendQuoted(step.name, '"')
                }
            }
        }
    }
}

/** [name] between two [quote]s, the quote, a backslash and the characters that cannot be written as they are escaped. */
private fun StringBuilder.appendQuoted(name: String, quote: Char): StringBuilder {
    append(quote)
    name.forEach { char ->
        when (char) {
            quote, '\\' -> append('\\').append(char)
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            in '\u0000'..'\u001F' -> append("\\u").append(char.code.toString(HEX_RADIX).padStart(UNICODE_ESCAPE_DIGITS, '0'))
            else -> append(char)
        }
    }
    return append(quote)
}
