package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.diff.LineDifference
import com.developersancho.formatico.core.format.diff.compareLines
import com.pantrixui.components.data.diffview.DiffKind
import com.pantrixui.components.data.diffview.DiffLine
import com.pantrixui.components.data.diffview.diffLines

/**
 * The lines [a] and [b] are shown in, compared, as `PantrixDiffView` takes them: the lines both have plain, and the
 * runs of lines that differ, a line of A across from the line of B most like it and the words that differ marked.
 *
 * By text these are the lines that differ. By meaning the same lines stand across from each other, but only the ones
 * the [differences] stand on are marked as changed, taken out or put in: a key moved, a number written otherwise and a
 * comma that follows them are plain, as the differences do not count them.
 */
internal fun comparedLines(a: String, b: String, by: CompareBy, differences: List<ShownDifference>): List<DiffLine> {
    val lines = linesOf(a, b)
    if (by == CompareBy.Text) return lines
    val aMarked = HashSet<Int>()
    val bMarked = HashSet<Int>()
    differences.forEach { difference ->
        difference.aLines.forEach { aMarked += it + 1 }
        difference.bLines.forEach { bMarked += it + 1 }
    }
    return lines.flatMap { line -> asMeant(line, aMarked, bMarked) }
}

/**
 * The places, among the changes of [lines] as `PantrixDiffView` counts them, of each of [differences]: the first change
 * that takes or puts in a line the difference stands on, or null where none does.
 */
internal fun changesOf(lines: List<DiffLine>, differences: List<ShownDifference>): List<Int?> {
    val changes = runsOf(lines)
    return differences.map { difference ->
        val aNumbers = difference.aLines.map { it + 1 }.toSet()
        val bNumbers = difference.bLines.map { it + 1 }.toSet()
        changes.indexOfFirst { run -> run.before.any { it in aNumbers } || run.after.any { it in bNumbers } }.takeIf { it >= 0 }
    }
}

/** The lines of [a] and [b] a line at a time, as [compareLines] finds them to differ, each run paired by [pairedRun]. */
private fun linesOf(a: String, b: String): List<DiffLine> {
    val aLines = splitLines(a)
    val bLines = splitLines(b)
    val lines = ArrayList<DiffLine>(maxOf(aLines.size, bLines.size))
    var i = 0
    var j = 0

    fun same(until: Int) {
        while (i < until) {
            lines += DiffLine(DiffKind.Unchanged, aLines[i], bLines[j], beforeNumber = i + 1, afterNumber = j + 1)
            i++
            j++
        }
    }
    compareLines(a, b).forEach { run ->
        same(run.aStart)
        lines += pairedRun(run, aLines, bLines)
        i = run.aStart + run.aCount
        j = run.bStart + run.bCount
    }
    same(aLines.size)
    return lines
}

/**
 * The lines of [run]: where they are few enough to weigh one against another, as `diffLines` pairs them, the most
 * alike across from each other and their words marked; past that, in their order, the first of A's across from the
 * first of B's.
 */
private fun pairedRun(run: LineDifference, aLines: List<String>, bLines: List<String>): List<DiffLine> {
    val aRun = aLines.subList(run.aStart, run.aStart + run.aCount)
    val bRun = bLines.subList(run.bStart, run.bStart + run.bCount)
    if (aRun.isEmpty() || bRun.isEmpty() || aRun.size.toLong() * bRun.size > PAIRS_WEIGHED) {
        return (0 until maxOf(aRun.size, bRun.size)).map { at ->
            val before = aRun.getOrNull(at)
            val after = bRun.getOrNull(at)
            DiffLine(
                kind = when {
                    before == null -> DiffKind.Added
                    after == null -> DiffKind.Removed
                    else -> DiffKind.Changed
                },
                before = before,
                after = after,
                beforeNumber = before?.let { run.aStart + at + 1 },
                afterNumber = after?.let { run.bStart + at + 1 }
            )
        }
    }
    return diffLines(aRun.joinToString("\n"), bRun.joinToString("\n")).map { line ->
        DiffLine(
            kind = line.kind,
            before = line.before,
            after = line.after,
            beforeNumber = line.beforeNumber?.plus(run.aStart),
            afterNumber = line.afterNumber?.plus(run.bStart),
            beforeMarks = line.beforeMarks,
            afterMarks = line.afterMarks
        )
    }
}

/**
 * [line] as the comparison by meaning shows it, where only the lines of A in [aMarked] and of B in [bMarked], by their
 * numbers, differ: a changed line whose two sides differ stays so; one whose sides do not is plain; one with a side
 * that differs and one that does not is the one taken out or put in, beside the other plain.
 */
private fun asMeant(line: DiffLine, aMarked: Set<Int>, bMarked: Set<Int>): List<DiffLine> {
    val aDiffers = line.beforeNumber in aMarked
    val bDiffers = line.afterNumber in bMarked
    val aAlone = line.before?.let { DiffLine(DiffKind.Unchanged, it, null, beforeNumber = line.beforeNumber) }
    val bAlone = line.after?.let { DiffLine(DiffKind.Unchanged, null, it, afterNumber = line.afterNumber) }
    return when (line.kind) {
        DiffKind.Unchanged -> listOf(line)

        DiffKind.Removed -> listOfNotNull(if (aDiffers) line else aAlone)

        DiffKind.Added -> listOfNotNull(if (bDiffers) line else bAlone)

        DiffKind.Changed -> when {
            aDiffers && bDiffers -> listOf(line)
            aDiffers -> listOfNotNull(DiffLine(DiffKind.Removed, line.before, null, beforeNumber = line.beforeNumber), bAlone)
            bDiffers -> listOfNotNull(aAlone, DiffLine(DiffKind.Added, null, line.after, afterNumber = line.afterNumber))
            else -> listOf(DiffLine(DiffKind.Unchanged, line.before, line.after, line.beforeNumber, line.afterNumber))
        }
    }
}

/** A change among lines: the numbers of the lines of A it takes and of B it puts in. */
private class LineRun(val before: List<Int>, val after: List<Int>)

/** The changes among [lines], as `PantrixDiffView` counts them: each run of lines that are not unchanged. */
private fun runsOf(lines: List<DiffLine>): List<LineRun> {
    val runs = ArrayList<LineRun>()
    var at = 0
    while (at < lines.size) {
        if (lines[at].kind == DiffKind.Unchanged) {
            at++
            continue
        }
        val first = at
        while (at < lines.size && lines[at].kind != DiffKind.Unchanged) at++
        val run = lines.subList(first, at)
        runs += LineRun(run.mapNotNull { it.beforeNumber }, run.mapNotNull { it.afterNumber })
    }
    return runs
}

/** The lines of [text], as [compareLines] counts them: a line break after the last line starts no line. */
private fun splitLines(text: String): List<String> {
    if (text.isEmpty()) return emptyList()
    val lines = text.split("\r\n", "\n", "\r")
    return if (lines.last().isEmpty()) lines.dropLast(1) else lines
}

/**
 * How many pairs of lines a run may have, one of A's with one of B's, for its lines to be paired by what they hold: a
 * run of a hundred lines on each side, and no more, or the pairing would take as long as the texts are large.
 */
private const val PAIRS_WEIGHED = 10_000L
