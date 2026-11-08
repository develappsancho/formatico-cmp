package com.developersancho.formatico.feature.workspace.editor

import androidx.compose.ui.text.TextRange
import com.pantrixui.components.data.codeblock.CodeTone
import com.pantrixui.components.data.codeeditor.CodeRangeStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatchMarksTest {
    @Test
    fun eachMatchIsAFillInTheWarningsToneWithNoWords() {
        val marks = matchMarks(listOf(TextRange(12, 22), TextRange(40, 45)))

        assertEquals(listOf(12 to 22, 40 to 45), marks.map { it.start to it.end })
        assertTrue(marks.all { it.style == CodeRangeStyle.Highlight && it.mark.tone == CodeTone.Warning && it.mark.message == null })
    }

    @Test
    fun aMatchOfNoTextMarksTheCharacterThere() {
        val mark = matchMarks(listOf(TextRange(7, 7))).single()

        assertEquals(7 to 8, mark.start to mark.end)
    }

    @Test
    fun noMatchesMarkNothing() {
        assertTrue(matchMarks(emptyList()).isEmpty())
    }
}
