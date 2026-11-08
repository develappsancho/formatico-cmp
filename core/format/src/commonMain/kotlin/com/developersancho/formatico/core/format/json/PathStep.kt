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
                append("['")
                step.name.forEach { char ->
                    when (char) {
                        '\'' -> {
                            append("\\'")
                        }

                        '\\' -> {
                            append("\\\\")
                        }

                        '\n' -> {
                            append("\\n")
                        }

                        '\r' -> {
                            append("\\r")
                        }

                        '\t' -> {
                            append("\\t")
                        }

                        in '\u0000'..'\u001F' -> {
                            append("\\u").append(char.code.toString(HEX_RADIX).padStart(UNICODE_ESCAPE_DIGITS, '0'))
                        }

                        else -> {
                            append(char)
                        }
                    }
                }
                append("']")
            }
        }
    }
}
