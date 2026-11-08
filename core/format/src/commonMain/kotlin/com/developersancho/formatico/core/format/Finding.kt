package com.developersancho.formatico.core.format

import com.developersancho.formatico.core.format.text.LineMap
import com.developersancho.formatico.core.format.text.TextRange

/** A problem a reader found, by its offsets, before it is given its line and column. */
internal class Finding(val code: ProblemCode, val severity: Severity, val start: Int, val end: Int)

/** [findings] in [text] as problems, in the order of the text, each with its line and column. */
internal fun problemsOf(text: String, findings: List<Finding>): List<Problem> {
    val lines = LineMap(text)
    return findings.sortedBy { it.start }.map {
        Problem(
            code = it.code,
            severity = it.severity,
            range = TextRange(it.start, it.end),
            start = lines.position(it.start),
            end = lines.position(it.end)
        )
    }
}

/**
 * The lines of [text], each without its line break: `\n`, `\r\n` or `\r`. A text that ends with a line break has no
 * line after it.
 */
internal fun linesOf(text: String): List<TextRange> {
    val lines = ArrayList<TextRange>()
    var start = 0
    var index = 0
    while (index < text.length) {
        val char = text[index]
        if (char == '\n' || char == '\r') {
            lines += TextRange(start, index)
            if (char == '\r' && index + 1 < text.length && text[index + 1] == '\n') index++
            start = index + 1
        }
        index++
    }
    if (start < text.length) lines += TextRange(start, text.length)
    return lines
}
