package com.developersancho.formatico.core.format.text

import kotlin.test.Test
import kotlin.test.assertEquals

class TextLayoutTest {
    @Test
    fun theFirstLineBreakSaysTheLineEnding() {
        assertEquals(LineEnding.Lf, TextLayout.of("a\nb\r\n").lineEnding)
        assertEquals(LineEnding.Crlf, TextLayout.of("a\r\nb\n").lineEnding)
        assertEquals(LineEnding.Cr, TextLayout.of("a\rb").lineEnding)
        assertEquals(null, TextLayout.of("""{"a":1}""").lineEnding)
    }

    @Test
    fun theNarrowestIndentationIsALevel() {
        assertEquals(Indentation.Spaces(2), TextLayout.of("{\n  \"a\": {\n    \"b\": 1\n  }\n}").indentation)
        assertEquals(Indentation.Spaces(4), TextLayout.of("a:\n    b: 1\n        c: 2\n").indentation)
        assertEquals(Indentation.Tabs, TextLayout.of("{\n\t\"a\": 1\n}").indentation)
        assertEquals(null, TextLayout.of("a=1\nb=2\n").indentation)
    }

    @Test
    fun aWideAlignmentIsNoLevel() {
        assertEquals(null, TextLayout.of("key:\n                    value\n").indentation)
    }
}
