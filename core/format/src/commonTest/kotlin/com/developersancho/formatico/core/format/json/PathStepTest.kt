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
}
