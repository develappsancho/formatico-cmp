package com.developersancho.formatico.core.format.query

import com.developersancho.formatico.core.format.xml.isXmlWhitespace
import kotlin.math.abs
import kotlin.math.floor

/** Half of one, where rounding goes up from. */
private const val HALF = 0.5

private const val NEGATIVE_ZERO = -0.0

/** A value of XPath 1.0. */
internal sealed interface XPathValue {
    /** A node-set, in document order, each node once. */
    class Nodes(val nodes: List<XPathTreeNode>) : XPathValue

    /** A string. */
    class Text(val value: String) : XPathValue

    /** A number. */
    class Number(val value: Double) : XPathValue

    /** A boolean. */
    class Truth(val value: Boolean) : XPathValue
}

/** Where an expression is evaluated: [node], and its [position] among the [size] nodes it is one of, from 1. */
internal class XPathContext(val node: XPathTreeNode, val position: Int, val size: Int)

/** [value] as a string, as `string()` turns it into one: a node-set gives its first node's string-value. */
internal fun XPathIndex.string(value: XPathValue): String = when (value) {
    is XPathValue.Nodes -> value.nodes.firstOrNull()?.let(::stringValue).orEmpty()
    is XPathValue.Text -> value.value
    is XPathValue.Number -> xpathNumberText(value.value)
    is XPathValue.Truth -> value.value.toString()
}

/** [value] as a number, as `number()` turns it into one. */
internal fun XPathIndex.number(value: XPathValue): Double = when (value) {
    is XPathValue.Number -> value.value
    is XPathValue.Truth -> if (value.value) 1.0 else 0.0
    is XPathValue.Nodes, is XPathValue.Text -> xpathNumber(string(value))
}

/**
 * [value] as a boolean, as `boolean()` turns it into one: a number that is neither zero nor NaN, a string or a node-set
 * that is not empty.
 */
internal fun boolean(value: XPathValue): Boolean = when (value) {
    is XPathValue.Nodes -> value.nodes.isNotEmpty()
    is XPathValue.Text -> value.value.isNotEmpty()
    is XPathValue.Number -> value.value != 0.0 && !value.value.isNaN()
    is XPathValue.Truth -> value.value
}

/**
 * The number [text] writes, as XPath 1.0 reads one: whitespace, an optional `-`, digits with a `.` among them or not,
 * and whitespace; NaN for any other text, an exponent or a `+` among them.
 */
internal fun xpathNumber(text: String): Double {
    val written = text.trim(::isXmlWhitespace)
    val unsigned = written.removePrefix("-")
    val point = unsigned.indexOf('.')
    val whole = if (point < 0) unsigned else unsigned.substring(0, point)
    val fraction = if (point < 0) "" else unsigned.substring(point + 1)
    val digits = whole + fraction
    if (digits.isEmpty() || !digits.all { it in '0'..'9' }) return Double.NaN
    // Kotlin reads `1.` and `.5` alike everywhere once a digit stands on either side of the point.
    val value = "${whole.ifEmpty { "0" }}.${fraction.ifEmpty { "0" }}".toDouble()
    return if (written.startsWith('-')) -value else value
}

/**
 * [value] as XPath 1.0's `string()` writes a number: `NaN`, `Infinity`, `-Infinity`, `0` for either zero, an integer
 * without a point, and any other number in as few digits as tell it from every other, never with an exponent.
 */
internal fun xpathNumberText(value: Double): String = when {
    value.isNaN() -> "NaN"
    value == Double.POSITIVE_INFINITY -> "Infinity"
    value == Double.NEGATIVE_INFINITY -> "-Infinity"
    value == 0.0 -> "0"
    else -> (if (value < 0) "-" else "") + plainDecimal(abs(value))
}

/**
 * [value], a finite positive number, in decimal: the shortest digits Kotlin writes it in, whichever notation the
 * platform writes them in, `1.0E-5` or `1e-5`, set out without an exponent.
 */
private fun plainDecimal(value: Double): String {
    val written = value.toString()
    val exponentAt = written.indexOfFirst { it == 'e' || it == 'E' }
    val mantissa = if (exponentAt < 0) written else written.substring(0, exponentAt)
    val exponent = if (exponentAt < 0) 0 else written.substring(exponentAt + 1).removePrefix("+").toInt()
    val point = mantissa.indexOf('.')
    val whole = if (point < 0) mantissa else mantissa.substring(0, point)
    var digits = whole + (if (point < 0) "" else mantissa.substring(point + 1))
    var pointAt = whole.length + exponent
    val leadingZeros = digits.indexOfFirst { it != '0' }
    digits = digits.substring(leadingZeros).trimEnd('0')
    pointAt -= leadingZeros
    return when {
        pointAt <= 0 -> "0." + "0".repeat(-pointAt) + digits
        pointAt >= digits.length -> digits + "0".repeat(pointAt - digits.length)
        else -> digits.substring(0, pointAt) + "." + digits.substring(pointAt)
    }
}

/** [value] rounded as XPath 1.0's `round()` does: to the nearest integer, halves upwards, and `-0` for -0.5 up to zero. */
internal fun xpathRound(value: Double): Double {
    if (value.isNaN() || value.isInfinite()) return value
    if (value < 0 && value >= -HALF) return NEGATIVE_ZERO
    val down = floor(value)
    return if (value - down >= HALF) down + 1 else down
}
