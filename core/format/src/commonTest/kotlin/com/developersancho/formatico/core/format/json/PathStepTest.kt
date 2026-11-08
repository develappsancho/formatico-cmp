package com.developersancho.formatico.core.format.json

import kotlin.test.Test
import kotlin.test.assertEquals

class PathStepTest {
    @Test
    fun theRootIsADollar() {
        assertEquals("$", jsonPath(emptyList()))
    }

    @Test
    fun plainKeysAreDottedAndIndexesBracketed() {
        val steps = listOf(PathStep.Key("items"), PathStep.Index(0), PathStep.Key("sku"))
        assertEquals("$.items[0].sku", jsonPath(steps))
    }

    @Test
    fun anyOtherKeyIsQuotedInBrackets() {
        assertEquals("$['first name']", jsonPath(listOf(PathStep.Key("first name"))))
        assertEquals("$['2fa']", jsonPath(listOf(PathStep.Key("2fa"))))
        assertEquals("$['şehir']", jsonPath(listOf(PathStep.Key("şehir"))))
        assertEquals("$['it\\'s']['a\\\\b']['x\\ny']", jsonPath(listOf("it's", "a\\b", "x\ny").map(PathStep::Key)))
        assertEquals("$['']", jsonPath(listOf(PathStep.Key(""))))
    }

    @Test
    fun aJsonPointerIsTheKeysAndIndexesAfterSlashes() {
        assertEquals("", jsonPointer(emptyList()))
        assertEquals("/items/0/sku", jsonPointer(listOf(PathStep.Key("items"), PathStep.Index(0), PathStep.Key("sku"))))
        // RFC 6901: a tilde is written ~0 and a slash ~1, the tilde first.
        assertEquals("/a~1b/m~0n/~01", jsonPointer(listOf("a/b", "m~n", "~1").map(PathStep::Key)))
        assertEquals("/first name/", jsonPointer(listOf(PathStep.Key("first name"), PathStep.Key(""))))
    }

    @Test
    fun aJqPathDotsPlainKeysAndQuotesTheRest() {
        assertEquals(".", jqPath(emptyList()))
        assertEquals(".items[0].sku", jqPath(listOf(PathStep.Key("items"), PathStep.Index(0), PathStep.Key("sku"))))
        assertEquals(".[0][1]", jqPath(listOf(PathStep.Index(0), PathStep.Index(1))))
        assertEquals(".\"first name\".\"@version\"", jqPath(listOf("first name", "@version").map(PathStep::Key)))
        assertEquals(".\"say \\\"hi\\\"\".\"a\\\\b\".\"x\\ny\"", jqPath(listOf("say \"hi\"", "a\\b", "x\ny").map(PathStep::Key)))
    }
}
