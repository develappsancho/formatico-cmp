package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.pantrixui.components.data.diffview.DiffKind
import com.pantrixui.components.data.diffview.DiffLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CompareLinesTest {
    private val orderA = OpenDocument(id = 1, name = "order.json", text = PreviewCompareA, format = Format.Json)
    private val orderB = OpenDocument(id = 2, name = "order-v2.json", text = PreviewCompareB, format = Format.Json)
    private val orderYaml = OpenDocument(
        id = 3,
        name = "order.yaml",
        text = "status: delivered\nid: ord_8842\ncreatedAt: \"2026-10-28T14:32:00Z\"\ndeliveredAt: \"2026-11-01T10:05:00Z\"\n" +
            "total: 129.9\nitems:\n  - sku: TS-001\n    qty: 2\n  - sku: MG-014\n    qty: 2\n",
        format = Format.Yaml
    )

    private fun compared(b: OpenDocument = orderB, by: CompareBy = CompareBy.Meaning) = assertIs<CompareOutcome.Compared>(
        comparisonOf(orderA, readDocument(orderA.text, orderA.format), b, readDocument(b.text, b.format), by)
    )

    /** The numbers of A's lines and of B's that [lines] mark as differing. */
    private fun marked(lines: List<DiffLine>): Pair<Set<Int>, Set<Int>> {
        val differ = lines.filter { it.kind != DiffKind.Unchanged }
        return differ.mapNotNull { it.beforeNumber }.toSet() to differ.mapNotNull { it.afterNumber }.toSet()
    }

    @Test
    fun byMeaningOnlyTheLinesOfTheDifferencesAreMarked() {
        val compared = compared()

        // $.status in A's line 3 and B's 2, $.deliveredAt in B's 5, $.coupon in A's 6, $.items[1].qty in the 9th of both.
        assertEquals(setOf(3, 6, 9) to setOf(2, 5, 9), marked(compared.lines))
        // A key that moved and a number written otherwise are plain: A's id and its total.
        assertTrue(compared.lines.filter { it.beforeNumber == 2 || it.beforeNumber == 5 }.all { it.kind == DiffKind.Unchanged })
        // Every line of both texts is shown, each once.
        assertEquals((1..11).toList(), compared.lines.mapNotNull { it.beforeNumber })
        assertEquals((1..11).toList(), compared.lines.mapNotNull { it.afterNumber })
    }

    @Test
    fun byTextEveryLineThatDiffersIsMarked() {
        val compared = compared(by = CompareBy.Text)

        val (a, b) = marked(compared.lines)
        // The moved key and the number written otherwise differ as text.
        assertTrue(2 in a || 3 in a)
        assertTrue(5 in a && 6 in b)
        assertTrue(9 in a && 9 in b)
    }

    @Test
    fun eachDifferenceIsAChangeOfTheView() {
        val compared = compared()

        val changes = compared.changes
        assertEquals(compared.differences.size, changes.size)
        assertTrue(changes.all { it != null }, "$changes")
        // The changes follow the differences' order in B's text, and two differences may share one.
        assertEquals(changes.filterNotNull().sorted(), changes.filterNotNull().sortedBy { it })
    }

    @Test
    fun aJsonAndAYamlOfTheSameValuesAreMarkedOnlyWhereTheyDiffer() {
        val compared = compared(b = orderYaml)

        val (a, b) = marked(compared.lines)
        // What differs: the status, the coupon taken out, the date put in and the quantity; the rest are written in
        // another format and plain.
        assertTrue(3 in a && 6 in a && 9 in a, "$a")
        assertTrue(1 in b && 4 in b, "$b")
        assertTrue(compared.lines.count { it.kind != DiffKind.Unchanged } < compared.lines.size / 2)
    }

    @Test
    fun aLongRunOfLinesIsPairedInItsOrder() {
        val a = (1..300).joinToString("\n") { "a$it" }
        val b = (1..300).joinToString("\n") { "b$it" }

        val lines = comparedLines(a, b, CompareBy.Text, emptyList())

        assertEquals(300, lines.size)
        assertTrue(lines.all { it.kind == DiffKind.Changed && it.beforeNumber == it.afterNumber })
    }
}
