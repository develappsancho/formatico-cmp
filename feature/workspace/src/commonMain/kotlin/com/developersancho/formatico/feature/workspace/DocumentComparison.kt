package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Immutable
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.convert.DataArray
import com.developersancho.formatico.core.format.convert.DataBoolean
import com.developersancho.formatico.core.format.convert.DataDate
import com.developersancho.formatico.core.format.convert.DataNull
import com.developersancho.formatico.core.format.convert.DataNumber
import com.developersancho.formatico.core.format.convert.DataObject
import com.developersancho.formatico.core.format.convert.DataString
import com.developersancho.formatico.core.format.convert.DataValue
import com.developersancho.formatico.core.format.convert.compactJson
import com.developersancho.formatico.core.format.diff.Change
import com.developersancho.formatico.core.format.diff.KeysMoved
import com.developersancho.formatico.core.format.diff.LineDifference
import com.developersancho.formatico.core.format.diff.NumbersRespelled
import com.developersancho.formatico.core.format.diff.ValueDifference
import com.developersancho.formatico.core.format.diff.compareLines
import com.developersancho.formatico.core.format.diff.compareValues
import com.developersancho.formatico.core.format.json.PathStep
import com.developersancho.formatico.core.format.json.jsonPath
import com.developersancho.formatico.core.format.text.LineMap

/** What two documents are compared by: what their values mean, whatever their format, or their text, a line at a time. */
enum class CompareBy { Meaning, Text }

/**
 * How a document is compared, as its view keeps it.
 *
 * @property with the id of the open document it is compared with, B, or null for the one after it among the tabs.
 * @property by what the two are compared by.
 * @property current the place of the difference shown, among them all.
 */
data class CompareChoice(val with: Long? = null, val by: CompareBy = CompareBy.Meaning, val current: Int = 0)

/** Which of the two documents compared: A, the one Compare is opened on, or B, the one it is compared with. */
internal enum class Side { A, B }

/** A document compared: its [id], [name] and [format], its [text], and how many [lines] that has. */
internal data class ComparedDocument(val id: Long, val name: String?, val format: Format?, val text: String, val lines: Int)

/**
 * A difference as Compare shows it.
 *
 * @property change how A and B differ there.
 * @property path where the value is, where they are compared by meaning; null for a run of lines.
 * @property before what A has there, the value or the run's first line, written as a row shows it; null for none.
 * @property after what B has there; null for none.
 * @property aLines the lines of A it stands on, counted from nought; empty where A has none.
 * @property bLines the lines of B it stands on; empty where B has none.
 */
@Immutable
internal data class ShownDifference(
    val change: Change,
    val path: List<PathStep>?,
    val before: String?,
    val after: String?,
    val aLines: IntRange,
    val bLines: IntRange
)

/** What comparing a document with another came to. */
internal sealed interface CompareOutcome {
    /** It is the one document open: there is none to compare it with. */
    data object Alone : CompareOutcome

    /**
     * The document of [side], named [name], has no value to compare by meaning: it is plain text, or none was read.
     * [partner] is the document compared with.
     */
    data class Unread(val side: Side, val name: String?, val partner: OtherDocument) : CompareOutcome

    /**
     * The document of [side], named [name], has [errors] in [format]: it is repaired before it is compared by meaning.
     * [partner] is the document compared with.
     */
    data class Invalid(val side: Side, val name: String?, val format: Format, val errors: Int, val partner: OtherDocument) : CompareOutcome

    /**
     * [a] and [b] were compared by [by]: their [differences], in order; and where they were compared by meaning, what
     * was not counted as one, the keys that [moved] and the numbers [respelled].
     */
    @Immutable
    data class Compared(
        val a: ComparedDocument,
        val b: ComparedDocument,
        val by: CompareBy,
        val differences: List<ShownDifference>,
        val moved: KeysMoved? = null,
        val respelled: NumbersRespelled? = null
    ) : CompareOutcome
}

/** What [outcome] compared, where it is a comparison. */
internal val CompareOutcome?.compared: CompareOutcome.Compared? get() = this as? CompareOutcome.Compared

/** The document compared with, B, where there is one. */
internal val CompareOutcome?.partner: OtherDocument?
    get() = when (this) {
        is CompareOutcome.Compared -> OtherDocument(b.id, b.name)
        is CompareOutcome.Unread -> partner
        is CompareOutcome.Invalid -> partner
        else -> null
    }

/**
 * The open document the one whose id is [id] is compared with, among [documents], as [choice] says: the one it names,
 * or the one after it among the tabs, or the one before where it is the last. Null where it is the one open.
 */
internal fun partnerOf(id: Long, documents: List<OpenDocument>, choice: CompareChoice): OpenDocument? =
    documents.firstOrNull { it.id == choice.with && it.id != id }
        ?: documents.dropWhile { it.id != id }.drop(1).firstOrNull()
        ?: documents.lastOrNull { it.id != id }

/** [a], read as [aReading], compared with [b], read as [bReading], by [by]; [b] is null where there is none. */
internal fun comparisonOf(a: OpenDocument, aReading: Reading, b: OpenDocument?, bReading: Reading?, by: CompareBy): CompareOutcome {
    if (b == null || bReading == null) return CompareOutcome.Alone
    val first = ComparedDocument(a.id, a.name, a.format, a.text, TextStats.of(a.text).lines)
    val second = ComparedDocument(b.id, b.name, b.format, b.text, TextStats.of(b.text).lines)
    if (by == CompareBy.Text) {
        val differences = compareLines(a.text, b.text).map { textDifference(it, a.text, b.text) }
        return CompareOutcome.Compared(first, second, by, differences)
    }
    val partner = OtherDocument(b.id, b.name)
    return whyNot(Side.A, a, aReading as? Reading.Read, partner)
        ?: whyNot(Side.B, b, bReading as? Reading.Read, partner)
        ?: byMeaning(first, second, aReading as Reading.Read, bReading as Reading.Read)
}

/** [a], read as [aRead], compared with [b], read as [bRead], by what their values mean: both have a value, and no error. */
private fun byMeaning(a: ComparedDocument, b: ComparedDocument, aRead: Reading.Read, bRead: Reading.Read): CompareOutcome {
    val aRoot = checkNotNull(aRead.root)
    val bRoot = checkNotNull(bRead.root)
    val comparison = compareValues(dataValueOf(aRoot, aRead.format), dataValueOf(bRoot, bRead.format))
    val aNodes = NodesByPath(aRoot, a.text)
    val bNodes = NodesByPath(bRoot, b.text)
    val differences = comparison.differences.map { difference -> valueDifference(difference, aNodes, bNodes) }
    return CompareOutcome.Compared(a, b, CompareBy.Meaning, differences, comparison.moved, comparison.respelled)
}

/**
 * Why [document], on [side], read as [read], has no value to compare by meaning with [partner]; null where it has
 * one.
 */
private fun whyNot(side: Side, document: OpenDocument, read: Reading.Read?, partner: OtherDocument): CompareOutcome? = when {
    read?.root == null -> CompareOutcome.Unread(side, document.name, partner)
    read.errors > 0 -> CompareOutcome.Invalid(side, document.name, read.format, read.errors, partner)
    else -> null
}

private fun valueDifference(difference: ValueDifference, a: NodesByPath, b: NodesByPath) = ShownDifference(
    change = difference.change,
    path = difference.path,
    before = difference.before?.let(::shortText),
    after = difference.after?.let(::shortText),
    aLines = difference.aPath?.let(a::linesOf) ?: IntRange.EMPTY,
    bLines = difference.bPath?.let(b::linesOf) ?: IntRange.EMPTY
)

private fun textDifference(run: LineDifference, a: String, b: String): ShownDifference {
    val aLines = run.aStart until run.aStart + run.aCount
    val bLines = run.bStart until run.bStart + run.bCount
    return ShownDifference(
        change = run.change,
        path = null,
        before = if (aLines.isEmpty()) null else lineOf(a, run.aStart),
        after = if (bLines.isEmpty()) null else lineOf(b, run.bStart),
        aLines = aLines,
        bLines = bLines
    )
}

/** The line of [text] at [index], counted from nought, as a row shows it: its indent taken off. */
private fun lineOf(text: String, index: Int): String = text.lineSequence().drop(index).firstOrNull().orEmpty().trim()

/** [value] as a row shows it: a string in quotes, a number as it is written, anything larger on one line, cut short. */
internal fun shortText(value: DataValue): String {
    val written = when (value) {
        is DataNumber -> value.written
        is DataBoolean -> "${value.value}"
        DataNull -> "null"
        is DataDate -> value.written
        is DataString, is DataObject, is DataArray -> compactJson(value)
    }
    return if (written.length > SHORT_TEXT) written.take(SHORT_TEXT - 1) + "…" else written
}

/**
 * The nodes of the tree whose root is [root], read from [text], by their paths: what finds the lines a difference
 * stands on. A path the tree has no node for, inside a YAML alias written out, is found at the alias.
 */
private class NodesByPath(root: ValueNode, private val text: String) {
    private val lines by lazy { LineMap(text) }
    private val byPath: Map<String, ValueNode> = buildMap {
        val left = ArrayDeque(listOf(root))
        while (left.isNotEmpty()) {
            val node = left.removeLast()
            val path = jsonPath(node.path)
            if (path !in this) put(path, node)
            left.addAll(node.children)
        }
    }

    fun linesOf(path: List<PathStep>): IntRange {
        var steps = path
        while (true) {
            val node = byPath[jsonPath(steps)]
            if (node != null) return lineOf(node.range.start)..lineOf(node.range.end)
            if (steps.isEmpty()) return IntRange.EMPTY
            steps = steps.dropLast(1)
        }
    }

    /** The line, from nought, of the place [offset]: a value's end just past a line break is on the line before it. */
    private fun lineOf(offset: Int): Int = lines.position(offset.coerceIn(0, text.length)).line - 1
}

/** How long a value a row shows is, at most, before it is cut short. */
private const val SHORT_TEXT = 80
