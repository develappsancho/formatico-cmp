package com.developersancho.formatico.core.format.yaml

import com.developersancho.formatico.core.format.ProblemCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Sorting YAML's keys where an alias would come before its anchor (Faz 21): the anchored value is written where the alias
 * first comes, and the alias where the value stood, so that the document stays valid and every value the same.
 */
class YamlAnchorSwapsTest {
    private fun sorted(text: String) = YamlDocument.parse(text).formatted(sortKeys = true)

    private fun assertReadsWithNoAliasBeforeItsAnchor(text: String) {
        val problems = YamlDocument.parse(text).problems
        assertTrue(problems.none { it.code == ProblemCode.UndefinedAlias }, "$text has no alias before its anchor: $problems")
    }

    @Test
    fun aScalarsAnchorMovesUpToItsFirstAlias() {
        val result = sorted("b: &x 1\na: *x\n")
        assertEquals("a: &x 1\nb: *x\n", result)
        assertReadsWithNoAliasBeforeItsAnchor(result)
    }

    @Test
    fun aMappingsAnchorMovesUpWithItsEntriesSortedThere() {
        val text = "z_defaults: &defaults\n  replicas: 3\n  image: web\napp:\n  <<: *defaults\n  name: web\n"
        val result = sorted(text)
        assertEquals(
            "app:\n  <<: &defaults\n    image: web\n    replicas: 3\n  name: web\nz_defaults: *defaults\n",
            result
        )
        assertReadsWithNoAliasBeforeItsAnchor(result)
    }

    @Test
    fun aSecondAliasStaysAnAlias() {
        val result = sorted("c: &x 1\nb: *x\na: *x\n")
        assertEquals("a: &x 1\nb: *x\nc: *x\n", result)
        assertReadsWithNoAliasBeforeItsAnchor(result)
    }

    @Test
    fun anAnchorAlreadyBeforeItsAliasesStaysWhereItIs() {
        assertEquals("a: &x 1\nb: *x\n", sorted("a: &x 1\nb: *x\n"))
        assertEquals("a: &x 1\nb: 2\nc:\n  d: *x\n", sorted("a: &x 1\nc:\n  d: *x\nb: 2\n"))
    }

    @Test
    fun withoutSortingNothingMoves() {
        val text = "b: &x 1\na: *x\n"
        assertEquals(text, YamlDocument.parse(text).formatted())
    }
}
