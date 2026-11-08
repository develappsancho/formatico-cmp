package com.developersancho.formatico.core.format.text

import kotlin.test.Test
import kotlin.test.assertEquals

class LineMapTest {
    @Test
    fun anEmptyTextHasOneLine() {
        val lines = LineMap("")
        assertEquals(1, lines.lineCount)
        assertEquals(TextPosition(offset = 0, line = 1, column = 1), lines.position(0))
    }

    @Test
    fun everyKindOfLineBreakStartsALine() {
        val lines = LineMap("a\nb\r\nc\rd")
        assertEquals(4, lines.lineCount)
        assertEquals(TextPosition(offset = 2, line = 2, column = 1), lines.position(2))
        assertEquals(TextPosition(offset = 5, line = 3, column = 1), lines.position(5))
        assertEquals(TextPosition(offset = 7, line = 4, column = 1), lines.position(7))
    }

    @Test
    fun theEndOfTheTextHasAPosition() {
        val lines = LineMap("ab\ncd")
        assertEquals(TextPosition(offset = 5, line = 2, column = 3), lines.position(5))
    }
}
