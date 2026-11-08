package com.developersancho.formatico.core.format.query

import kotlin.test.Test
import kotlin.test.assertEquals

class XPathErrorsTest {
    @Test
    fun anExpressionIsNotEmpty() {
        assertEquals("Empty 0", xpathError(""))
        assertEquals("Empty 0", xpathError(" \n\t"))
    }

    @Test
    fun anExpressionCutShortEndsTooEarly() {
        assertEquals("UnexpectedEnd 3", xpathError("/a["))
        assertEquals("UnexpectedEnd 4", xpathError("/a[1"))
        assertEquals("UnexpectedEnd 3", xpathError("1 +"))
        assertEquals("UnexpectedEnd 3", xpathError("//@"))
        assertEquals("UnexpectedEnd 7", xpathError("child::"))
        assertEquals("UnexpectedEnd 2", xpathError("//"))
        assertEquals("UnexpectedEnd 11", xpathError("concat('a',"))
    }

    @Test
    fun aTokenStandsWhereTheGrammarAllowsIt() {
        assertEquals("UnexpectedCharacter 2", xpathError("a b"))
        assertEquals("UnexpectedCharacter 3", xpathError("1 +)"))
        assertEquals("UnexpectedCharacter 3", xpathError("a:b:c"))
        assertEquals("UnexpectedCharacter 6", xpathError("//a[1]]"))
        assertEquals("UnexpectedCharacter 0", xpathError("!a"))
        assertEquals("UnexpectedCharacter 1", xpathError(".[1]"))
        assertEquals("UnexpectedCharacter 5", xpathError("text(1)"))
        assertEquals("UnexpectedCharacter 23", xpathError("processing-instruction(1)"))
        assertEquals("UnexpectedCharacter 2", xpathError("1 e5"))
        assertEquals("UnexpectedCharacter 1", xpathError("a#"))
    }

    @Test
    fun aStringIsClosed() {
        assertEquals("UnclosedString 0", xpathError("'abc"))
        assertEquals("UnclosedString 7", xpathError("concat(\"a, 'b')"))
    }

    @Test
    fun aFunctionIsOneOfTheCoreLibraryWithItsArguments() {
        assertEquals("UnknownFunction 0", xpathError("foo()"))
        assertEquals("UnknownFunction 0", xpathError("fn:count(//a)"))
        assertEquals("UnknownFunction 4", xpathError("//a[ends-with(., 'x')]"))
        assertEquals("WrongArgumentCount 0", xpathError("count()"))
        assertEquals("WrongArgumentCount 0", xpathError("concat('a')"))
        assertEquals("WrongArgumentCount 0", xpathError("true(1)"))
        assertEquals("WrongArgumentCount 0", xpathError("substring('a', 1, 2, 3)"))
    }

    @Test
    fun aNodeSetIsGivenWhereOneIsNeeded() {
        assertEquals("WrongType 6", xpathError("count(1)"))
        assertEquals("WrongType 4", xpathError("sum('a')"))
        assertEquals("WrongType 5", xpathError("name(1 + 1)"))
        assertEquals("WrongType 0", xpathError("1 | //a"))
        assertEquals("WrongType 6", xpathError("//a | 'x'"))
        assertEquals("WrongType 0", xpathError("'x'[1]"))
        assertEquals("WrongType 0", xpathError("(1)/a"))
        assertEquals("WrongType 0", xpathError("string(.)//a"))
        xpath("count((//a | //b)[1])")
        xpath("id('x')/b")
    }

    @Test
    fun variablesAndTheNamespaceAxisAreNotEvaluated() {
        assertEquals("Unsupported 0", xpathError("\$x"))
        assertEquals("Unsupported 4", xpathError("//a[\$x]"))
        assertEquals("Unsupported 0", xpathError("namespace::*"))
        assertEquals("UnknownAxis 4", xpathError("//a/sibling::b"))
    }

    @Test
    fun anExpressionNestedTooDeeplyIsNotRead() {
        assertEquals("TooDeep 64", xpathError("(".repeat(70) + "1" + ")".repeat(70)))
        assertEquals("TooDeep 129", xpathError("a" + "[a".repeat(70) + "]".repeat(70)))
        xpath("(".repeat(60) + "1" + ")".repeat(60))
    }
}
