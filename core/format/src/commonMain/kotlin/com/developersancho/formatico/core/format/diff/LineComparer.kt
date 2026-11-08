package com.developersancho.formatico.core.format.diff

/**
 * How two texts, [a] and [b], differ a line at a time, every character counting: the runs of lines that are not in
 * both, in order. A run with lines on both sides is changed; one with lines on one side alone is added or removed. A
 * line ends at a line feed, a carriage return before it or alone; the line after a last line break is no line.
 *
 * The lines at both ends that the texts share are set aside first. Of the rest, past [LCS_LIMIT] pairs of lines to
 * weigh, the texts are taken to differ in one run.
 */
fun compareLines(a: String, b: String): List<LineDifference> {
    val aLines = linesOf(a)
    val bLines = linesOf(b)
    var head = 0
    while (head < aLines.size && head < bLines.size && aLines[head] == bLines[head]) head++
    var tail = 0
    while (tail < minOf(aLines.size, bLines.size) - head && aLines[aLines.size - 1 - tail] == bLines[bLines.size - 1 - tail]) tail++
    val aMiddle = aLines.subList(head, aLines.size - tail)
    val bMiddle = bLines.subList(head, bLines.size - tail)
    if (aMiddle.isEmpty() && bMiddle.isEmpty()) return emptyList()
    val matched = commonRun(aMiddle.size, bMiddle.size) { i, j -> aMiddle[i] == bMiddle[j] }
    val runs = ArrayList<LineDifference>()
    var i = 0
    var j = 0
    (matched + (aMiddle.size to bMiddle.size)).forEach { (aNext, bNext) ->
        if (aNext > i || bNext > j) runs += run(head + i, aNext - i, head + j, bNext - j)
        i = aNext + 1
        j = bNext + 1
    }
    return runs
}

private fun run(aStart: Int, aCount: Int, bStart: Int, bCount: Int): LineDifference {
    val change = when {
        aCount == 0 -> Change.Added
        bCount == 0 -> Change.Removed
        else -> Change.Changed
    }
    return LineDifference(change, aStart, aCount, bStart, bCount)
}

/** The lines of [text], without their line breaks; a text that ends with a line break has no empty line after it. */
private fun linesOf(text: String): List<String> {
    if (text.isEmpty()) return emptyList()
    val lines = text.split("\r\n", "\n", "\r")
    return if (lines.last().isEmpty()) lines.dropLast(1) else lines
}
