package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.diff.Change
import com.pantrixui.components.data.codeblock.CodeMark
import com.pantrixui.components.data.codeblock.CodeTone
import com.pantrixui.components.data.codeeditor.CodeRangeMark
import com.pantrixui.components.data.codeeditor.CodeRangeStyle

/**
 * The marks of [side]'s lines in [compared]: each difference's lines on the band of its change's tone alone, with the
 * change's mark, `~`, `+` or `−`, before their numbers, and its name in [names] for a screen reader, which reads no
 * mark; the lines of the difference at [current], the one shown, with a bar in the primary colour as well.
 */
internal fun sideMarks(
    compared: CompareOutcome.Compared,
    side: Side,
    current: Int,
    names: Map<Change, String> = emptyMap()
): Map<Int, CodeMark> = buildMap {
    compared.differences.forEach { difference ->
        val change = difference.change
        difference.linesOn(side).forEach { line ->
            if (line !in this) put(line, CodeMark(change.codeTone, gutter = change.mark, showBar = false, description = names[change]))
        }
    }
    compared.differences.getOrNull(current)?.linesOn(side)?.forEach { line ->
        this[line]?.let { put(line, it.copy(bar = CodeTone.Primary, showBar = true)) }
    }
}

/** The values of [side] in [compared] that changed, each with a fill behind it in its change's tone. */
internal fun sideValues(compared: CompareOutcome.Compared, side: Side): List<CodeRangeMark> =
    compared.differences.mapNotNull { difference ->
        val value = (if (side == Side.A) difference.aValue else difference.bValue) ?: return@mapNotNull null
        CodeRangeMark(value.start, value.end, CodeMark(difference.change.codeTone), style = CodeRangeStyle.Highlight)
    }

/**
 * The lines of [side] the difference at [current] stands on; where it has none there, an empty range at the line after
 * the nearest difference before it that has some, or at the first line.
 */
internal fun shownLines(compared: CompareOutcome.Compared, side: Side, current: Int): IntRange? {
    val difference = compared.differences.getOrNull(current) ?: return null
    val lines = difference.linesOn(side)
    if (!lines.isEmpty()) return lines
    val before = compared.differences.take(current).lastOrNull { !it.linesOn(side).isEmpty() }?.linesOn(side)?.last ?: -1
    return (before + 1) until (before + 1)
}

/** The lines of [side] the difference stands on. */
private fun ShownDifference.linesOn(side: Side): IntRange = if (side == Side.A) aLines else bLines
