package com.developersancho.formatico.core.format.query

import kotlin.test.Test
import kotlin.test.assertEquals

class XPathAxesTest {
    private val tree = "<a><b><c/><d/></b><e x=\"1\" y=\"2\"><f/></e><g/></a>"

    /** What [expression] selects in [tree], each element by its name, each attribute by `@` and its name, the root as `/`. */
    private fun found(expression: String): List<String> = matches(expression, tree).map { match ->
        when (val node = match.node) {
            is XPathNode.Element -> node.element.name
            is XPathNode.Attribute -> "@" + node.attribute.name
            else -> match.path
        }
    }

    @Test
    fun theChildAxisIsTheOneLeftUnwritten() {
        assertEquals(listOf("b", "e", "g"), found("/a/*"))
        assertEquals(listOf("b", "e", "g"), found("/child::a/child::*"))
        assertEquals(listOf("a"), found("/node()"))
        assertEquals(emptyList(), found("//@x/node()"))
    }

    @Test
    fun theDescendantAxesTakeEverythingInsideInDocumentOrder() {
        assertEquals(listOf("b", "c", "d", "e", "f", "g"), found("/a/descendant::*"))
        assertEquals(listOf("a", "b", "c", "d", "e", "f", "g"), found("/a/descendant-or-self::*"))
        assertEquals(listOf("a", "b", "c", "d", "e", "f", "g"), found("//*"))
        assertEquals(listOf("@x"), found("//@x/descendant-or-self::node()"))
        assertEquals(emptyList(), found("//@x/descendant::node()"))
    }

    @Test
    fun theParentOfAnAttributeIsItsElement() {
        assertEquals(listOf("e"), found("//f/parent::*"))
        assertEquals(listOf("e"), found("//@x/.."))
        assertEquals(listOf("/"), found("/a/.."))
        assertEquals(emptyList(), found("/.."))
        assertEquals(listOf("a", "b", "e"), found("//*[*]"))
    }

    @Test
    fun theAncestorAxesGoUpToTheRoot() {
        assertEquals(listOf("a", "e"), found("//f/ancestor::*"))
        assertEquals(listOf("a", "e", "f"), found("//f/ancestor-or-self::*"))
        assertEquals(listOf("/", "a", "e"), found("//@x/ancestor::node()"))
    }

    @Test
    fun theSiblingAxesStayAmongAParentsChildren() {
        assertEquals(listOf("e", "g"), found("//b/following-sibling::*"))
        assertEquals(listOf("b", "e"), found("//g/preceding-sibling::*"))
        assertEquals(emptyList(), found("//@x/following-sibling::node()"))
        assertEquals(emptyList(), found("//@y/preceding-sibling::node()"))
        assertEquals(emptyList(), found("/a/following-sibling::node()"))
    }

    @Test
    fun followingAndPrecedingLeaveOutDescendantsAncestorsAndAttributes() {
        assertEquals(listOf("e", "f", "g"), found("//d/following::*"))
        assertEquals(listOf("b", "c", "d"), found("//f/preceding::*"))
        assertEquals(emptyList(), found("//e/following::node()[self::f]"))
        // The nodes after an attribute are its element's children, and what follows the element.
        assertEquals(listOf("f", "g"), found("//@x/following::*"))
        assertEquals(listOf("b", "c", "d"), found("//@x/preceding::*"))
        assertEquals(emptyList(), found("//@x/following::node()[self::node()[name() = 'y']]"))
    }

    @Test
    fun theAttributeAxisHoldsAttributesAndTheSelfAxisTheNodeItself() {
        assertEquals(listOf("@x", "@y"), found("//e/attribute::*"))
        assertEquals(listOf("@x", "@y"), found("//@*"))
        assertEquals(listOf("@y"), found("/a/e/@y"))
        assertEquals(listOf("e"), found("//*/self::e"))
        assertEquals(listOf("@x"), found("//@x/self::node()"))
        assertEquals(emptyList(), found("//@x/self::*"))
        assertEquals(listOf("e"), found("//e/."))
    }

    @Test
    fun aReverseAxisCountsItsPositionsFromTheNearestNode() {
        assertEquals(listOf("e"), found("//f/ancestor::*[1]"))
        assertEquals(listOf("a"), found("//f/ancestor::*[last()]"))
        assertEquals(listOf("e"), found("//g/preceding-sibling::*[1]"))
        assertEquals(listOf("d"), found("//f/preceding::*[1]"))
        assertEquals(listOf("c"), found("//f/preceding::*[2]"))
        assertEquals(listOf("b"), found("//d/ancestor-or-self::*[2]"))
        // A filter's predicate counts in document order, whatever axis selected its nodes.
        assertEquals(listOf("a"), found("(//f/ancestor::*)[1]"))
    }

    @Test
    fun aPredicateThatIsANumberIsAPosition() {
        assertEquals(listOf("e"), found("/a/*[2]"))
        assertEquals(listOf("e"), found("/a/*[1 + 1]"))
        assertEquals(listOf("e"), found("/a/*[position() = 2]"))
        assertEquals(listOf("g"), found("/a/*[last()]"))
        assertEquals(listOf("e"), found("/a/*[position() = last() - 1]"))
        assertEquals(listOf("b", "e"), found("/a/*[position() < 3]"))
        assertEquals(emptyList(), found("/a/*[1.5]"))
        assertEquals(emptyList(), found("/a/*[4]"))
        assertEquals(listOf("d", "e"), found("//*[2]"))
        assertEquals(listOf("b"), found("(//*)[2]"))
    }

    @Test
    fun anyOtherPredicateIsABoolean() {
        assertEquals(listOf("e"), found("/a/*[@x]"))
        assertEquals(listOf("e"), found("/a/*[@x = 1]"))
        assertEquals(listOf("b", "e", "g"), found("/a/*['x']"))
        assertEquals(emptyList(), found("/a/*['']"))
        assertEquals(listOf("e"), found("/a/*[position() > 1][1]"))
        assertEquals(listOf("g"), found("/a/*[position() > 1][2]"))
        assertEquals(listOf("e"), found("/a/*[f][@y = '2']"))
    }

    @Test
    fun aUnionIsInDocumentOrderEachNodeOnce() {
        assertEquals(listOf("b", "c", "g"), found("//g | //b | //c"))
        assertEquals(listOf("b"), found("//b | //b"))
        assertEquals(listOf("b"), found("(//g | //b)[1]"))
        assertEquals(listOf("e", "@y", "f"), found("//f | //@y | //e"))
        assertEquals(listOf("c", "d", "f"), found("(//b | //e)/*[1] | //d"))
    }
}
