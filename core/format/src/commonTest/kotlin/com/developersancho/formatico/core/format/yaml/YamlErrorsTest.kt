package com.developersancho.formatico.core.format.yaml

import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.RepairKind
import com.developersancho.formatico.core.format.Repairs
import kotlin.test.Test
import kotlin.test.assertEquals

class YamlErrorsTest {
    private fun problemOf(text: String) = YamlDocument.parse(text).problems.single()

    private fun repaired(text: String) = Repairs.all(text) { YamlDocument.parse(it).problems }

    @Test
    fun aColonInAValueIsNamedAndTheValueQuoted() {
        val text = "title: Note: read this\nnext: 1\n"
        val problem = problemOf(text)

        assertEquals(ProblemCode.YamlColonInValue, problem.code)
        assertEquals(RepairKind.QuoteValue, Repairs.of(problem, text)?.kind)
        assertEquals("title: \"Note: read this\"\nnext: 1\n", repaired(text))
    }

    @Test
    fun aTabWhereYamlIndentsIsWrittenAsSpaces() {
        val text = "a:\n\tb: 1\n"

        assertEquals(ProblemCode.YamlTab, problemOf(text).code)
        assertEquals(emptyList(), YamlDocument.parse(repaired(text)).problems)
    }

    @Test
    fun aStringNeverClosedRunsFromItsQuoteToItsLinesEnd() {
        val text = "name: \"web\nport: 8080\n"
        val problem = problemOf(text)

        assertEquals(ProblemCode.UnterminatedString, problem.code)
        assertEquals(text.indexOf('"'), problem.range.start)
        assertEquals(text.indexOf('\n'), problem.range.end)
    }

    @Test
    fun aLineIndentedAsNoneAroundItIsSaidSo() {
        assertEquals(ProblemCode.YamlIndentation, problemOf("a:\n  b: 1\n c: 2\n").code)
    }

    @Test
    fun aFlowNeverClosedIsClosedWhereItsLineEnds() {
        val text = "first: 1\nsecond: [1, 2  # two\nthird: 3\n"
        val problem = problemOf(text)

        assertEquals(ProblemCode.YamlUnclosedFlow, problem.code)
        assertEquals(text.indexOf('['), problem.range.start)
        assertEquals("first: 1\nsecond: [1, 2]  # two\nthird: 3\n", repaired(text))
    }

    @Test
    fun aFlowLeftOpenAtTheEndIsClosedThere() {
        assertEquals("tags: {a: 1, b: 2}", repaired("tags: {a: 1, b: 2"))
    }

    @Test
    fun whatIsNotNamedIsStillYamlSyntax() {
        assertEquals(ProblemCode.YamlSyntax, problemOf("a: @b\n").code)
        assertEquals(null, Repairs.of(problemOf("a: @b\n"), "a: @b\n"))
    }
}
