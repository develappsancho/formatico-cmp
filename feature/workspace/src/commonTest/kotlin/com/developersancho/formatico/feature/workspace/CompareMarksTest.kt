package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.diff.Change
import com.pantrixui.components.data.codeblock.CodeMark
import com.pantrixui.components.data.codeblock.CodeTone
import com.pantrixui.components.data.codeeditor.CodeRangeStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompareMarksTest {
    private val orderA = OpenDocument(id = 1, name = "order.json", text = PreviewCompareA, format = Format.Json)
    private val orderB = OpenDocument(id = 2, name = "order-v2.json", text = PreviewCompareB, format = Format.Json)

    private fun compare(by: CompareBy = CompareBy.Meaning) = assertIs<CompareOutcome.Compared>(
        comparisonOf(orderA, readDocument(orderA.text, orderA.format), orderB, readDocument(orderB.text, orderB.format), by)
    )

    @Test
    fun eachDifferencesLinesAreBandedInItsChangesToneWithItsMarkBeforeTheirNumbersAndTheShownOnesAlsoBarred() {
        val compared = compare()
        assertEquals(
            mapOf(
                2 to CodeMark(CodeTone.Warning, gutter = "~", bar = CodeTone.Primary),
                5 to CodeMark(CodeTone.Danger, gutter = "−", showBar = false),
                8 to CodeMark(CodeTone.Warning, gutter = "~", showBar = false)
            ),
            sideMarks(compared, Side.A, current = 0)
        )
        assertEquals(
            mapOf(
                1 to CodeMark(CodeTone.Warning, gutter = "~", showBar = false),
                4 to CodeMark(CodeTone.Success, gutter = "+", bar = CodeTone.Primary),
                8 to CodeMark(CodeTone.Warning, gutter = "~", showBar = false)
            ),
            sideMarks(compared, Side.B, current = 1)
        )
    }

    @Test
    fun eachMarkedLineSaysItsChangeToAScreenReader() {
        val names = mapOf(Change.Changed to "Changed", Change.Added to "Added", Change.Removed to "Removed")

        val marks = sideMarks(compare(), Side.A, current = 0, names = names)

        assertEquals(mapOf(2 to "Changed", 5 to "Removed", 8 to "Changed"), marks.mapValues { it.value.description })
    }

    @Test
    fun theShownDifferenceHasTheBarOnlyOnTheSideThatHasItsLines() {
        val compared = compare()
        // The coupon was taken out: A has its line, B none.
        val coupon = sideMarks(compared, Side.A, current = 2).getValue(5)
        assertEquals(CodeTone.Primary, coupon.bar)
        assertTrue(coupon.showBar)
        assertTrue(sideMarks(compared, Side.B, current = 2).values.none { it.showBar })
        // No difference there: no bar anywhere.
        assertTrue(sideMarks(compared, Side.A, current = 9).values.none { it.showBar })
    }

    @Test
    fun theValuesThatChangedHaveAFillOfTheirTone() {
        val compared = compare()
        val a = sideValues(compared, Side.A)
        assertEquals(listOf("\"shipped\"", "1"), a.map { PreviewCompareA.substring(it.start, it.end) })
        assertTrue(a.all { it.style == CodeRangeStyle.Highlight && it.mark.tone == CodeTone.Warning })
        assertEquals(listOf("\"delivered\"", "2"), sideValues(compared, Side.B).map { PreviewCompareB.substring(it.start, it.end) })
        assertEquals(emptyList(), sideValues(compare(CompareBy.Text), Side.A))
    }

    @Test
    fun theShownLinesAreTheDifferencesOrTheLineItWouldComeBefore() {
        val compared = compare()
        assertEquals(2..2, shownLines(compared, Side.A, current = 0))
        // The delivery date was added: A shows the line after the status's.
        assertEquals(3 until 3, shownLines(compared, Side.A, current = 1))
        assertEquals(4..4, shownLines(compared, Side.B, current = 1))
        assertNull(shownLines(compared, Side.A, current = 4))
    }
}
