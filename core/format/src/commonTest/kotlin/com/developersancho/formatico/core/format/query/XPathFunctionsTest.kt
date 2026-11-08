package com.developersancho.formatico.core.format.query

import kotlin.test.Test
import kotlin.test.assertEquals

class XPathFunctionsTest {
    private val prices = "<r><p>3</p><p>4.5</p><p>x</p><q a=\"1\" b=\"2\"/></r>"

    @Test
    fun numbersAreWrittenAsXPathWritesThemNeverWithAnExponent() {
        val table = mapOf(
            "3" to "3",
            "-3.25" to "-3.25",
            "2.50" to "2.5",
            "1 div 3" to "0.3333333333333333",
            "0.1 + 0.2" to "0.30000000000000004",
            "1 div 0" to "Infinity",
            "-1 div 0" to "-Infinity",
            "0 div 0" to "NaN",
            "-0" to "0",
            "1000000 * 1000000 * 1000000 * 1000" to "1000000000000000000000",
            "123456789012345678901234567890" to "123456789012345680000000000000",
            "0.000001" to "0.000001",
            "0.0000001" to "0.0000001",
            "1 div 1024" to "0.0009765625",
            "12345678.9" to "12345678.9",
            ".5" to "0.5",
            "5." to "5"
        )

        table.forEach { (expression, text) -> assertEquals(text, written(expression), expression) }
    }

    @Test
    fun arithmeticTakesItsPrecedenceAndModKeepsTheSignOfTheDividend() {
        assertEquals("7", written("1 + 2 * 3"))
        assertEquals("9", written("(1 + 2) * 3"))
        assertEquals("1", written("-2 + 3"))
        assertEquals("2", written("--2"))
        assertEquals("2", written("1 - -1"))
        assertEquals("2", written("7 mod 3 * 2"))
        assertEquals("1", written("10 mod 3"))
        assertEquals("-1", written("-10 mod 3"))
        assertEquals("1", written("5 mod -2"))
        assertEquals("2.5", written("5 div 2"))
        assertEquals("7.5", written("//p[1] + //p[2]", prices))
        assertEquals("NaN", written("//p[3] * 2", prices))
    }

    @Test
    fun logicalOperatorsGiveBooleans() {
        assertEquals("false", written("1 and 0"))
        assertEquals("true", written("0 or 2"))
        assertEquals("true", written("1 < 2 = true()"))
        assertEquals("true", written("1 = 1 or 1 div 0"))
        assertEquals("false", written("not(1) and true()"))
    }

    @Test
    fun valuesThatAreNoNodeSetsCompareAsBooleansThenNumbersThenStrings() {
        assertEquals("true", written("true() = 1"))
        assertEquals("true", written("false() = ''"))
        assertEquals("true", written("'1' = 1"))
        assertEquals("true", written("'abc' = 'abc'"))
        assertEquals("false", written("'abc' = 'ABC'"))
        assertEquals("true", written("'2' < '10'"))
        assertEquals("false", written("'a' < 'b'"))
        assertEquals("false", written("0 div 0 = 0 div 0"))
        assertEquals("true", written("0 div 0 != 0 div 0"))
    }

    @Test
    fun aNodeSetComparesTrueWhereAnyOfItsNodesDoes() {
        assertEquals("true", written("//p = 3", prices))
        assertEquals("true", written("//p != 3", prices))
        assertEquals("true", written("//p = 'x'", prices))
        assertEquals("true", written("//p > 4", prices))
        assertEquals("false", written("//p > 5", prices))
        assertEquals("true", written("3 < //p", prices))
        assertEquals("false", written("5 < //p", prices))
        assertEquals("true", written("//p = //q/@a | //p", prices))
        assertEquals("false", written("//q/@* = //p", prices))
        assertEquals("true", written("//q/@b < //p", prices))
        assertEquals("false", written("//nothing = //nothing", prices))
        assertEquals("false", written("//nothing != //nothing", prices))
        assertEquals("true", written("//nothing = false()", prices))
        assertEquals("true", written("not(//nothing = 'x')", prices))
        assertEquals("true", written("//q/@a != //q/@b", prices))
        assertEquals("false", written("//q/@a != //q/@a", prices))
    }

    @Test
    fun theNodeSetFunctionsCountAndNameNodes() {
        val xml = "<r xmlns:x=\"urn:x\"><x:a xml:lang=\"en\"/><b/><?pi data?><c/></r>"

        assertEquals("3", written("count(/r/*)", xml))
        assertEquals("true", written("count(/r/*[last()]) = 1 and name(/r/*[last()]) = 'c'", xml))
        assertEquals(listOf("/r/b"), paths("/r/*[position() = 2]", xml))
        assertEquals("x:a", written("name(/r/*)", xml))
        assertEquals("a", written("local-name(/r/*)", xml))
        assertEquals("xml:lang", written("name(//@*)", xml))
        assertEquals("lang", written("local-name(//@*)", xml))
        assertEquals("pi", written("name(//processing-instruction())", xml))
        assertEquals("", written("name(//nothing)", xml))
        assertEquals("", written("name()", xml))
        assertEquals(listOf("/r/b"), paths("//*[name() = 'b']", xml))
    }

    @Test
    fun idFindsTheElementsWhoseXmlIdIsOneOfItsNames() {
        val xml = "<r><a xml:id=\"one\"/><b xml:id=\" two \"/><ref>two one</ref></r>"

        assertEquals(listOf("/r/a", "/r/b"), paths("id('two one')", xml))
        assertEquals(listOf("/r/a", "/r/b"), paths("id(//ref)", xml))
        assertEquals(emptyList(), paths("id('none')", xml))
        assertEquals("1", written("count(id('one one'))", xml))
    }

    @Test
    fun theStringFunctionsAreAsXPathHasThem() {
        assertEquals("abc", written("concat('a', 'b', 'c')"))
        assertEquals("1", written("concat(1, '')"))
        assertEquals("true", written("starts-with('abc', 'ab')"))
        assertEquals("true", written("contains('abc', '')"))
        assertEquals("false", written("contains('abc', 'd')"))
        assertEquals("1999", written("substring-before('1999/04/01', '/')"))
        assertEquals("04/01", written("substring-after('1999/04/01', '/')"))
        assertEquals("99/04/01", written("substring-after('1999/04/01', '19')"))
        assertEquals("", written("substring-before('abc', 'x')"))
        assertEquals("", written("substring-after('abc', 'x')"))
        assertEquals("a b c", written("normalize-space('  a   b\n\tc  ')"))
        assertEquals("BAr", written("translate('bar', 'abc', 'ABC')"))
        assertEquals("AAA", written("translate('--aaa--', 'abc-', 'ABC')"))
        assertEquals("xxz", written("translate('aab', 'aab', 'xyz')"))
        assertEquals("é😀", written("translate('e😀x', 'ex', 'é')"))
        assertEquals("true", written("string(1 = 1)"))
        assertEquals("NaN", written("string(0 div 0)"))
    }

    @Test
    fun substringRoundsItsPositionsAsTheSpecsExamplesHaveIt() {
        val table = mapOf(
            "substring('12345', 2, 3)" to "234",
            "substring('12345', 2)" to "2345",
            "substring('12345', 1.5, 2.6)" to "234",
            "substring('12345', 0, 3)" to "12",
            "substring('12345', 0 div 0, 3)" to "",
            "substring('12345', 1, 0 div 0)" to "",
            "substring('12345', -42, 1 div 0)" to "12345",
            "substring('12345', -1 div 0, 1 div 0)" to "",
            "substring('😀ab', 2)" to "ab"
        )

        table.forEach { (expression, text) -> assertEquals(text, written(expression), expression) }
    }

    @Test
    fun aFunctionWithoutItsArgumentReadsTheContextNode() {
        val xml = "<r><w> two  words </w><n>12</n></r>"

        assertEquals(listOf("/r/w"), paths("//*[string-length() = 12]", xml))
        assertEquals(listOf("/r/w"), paths("//*[normalize-space() = 'two words']", xml))
        assertEquals(listOf("/r/n"), paths("//*[number() = 12]", xml))
        assertEquals(listOf("/r/n"), paths("//*[string() = '12']", xml))
        assertEquals("2", written("string-length('😀é')"))
    }

    @Test
    fun theBooleanFunctionsAreAsXPathHasThem() {
        assertEquals("false", written("boolean('')"))
        assertEquals("true", written("boolean('false')"))
        assertEquals("false", written("boolean(0 div 0)"))
        assertEquals("true", written("boolean(-1)"))
        assertEquals("true", written("boolean(/r)"))
        assertEquals("false", written("not(true())"))
        assertEquals("false", written("false()"))
    }

    @Test
    fun langReadsTheNearestXmlLang() {
        val xml = "<doc xml:lang=\"en-GB\"><p><q a=\"1\"/></p><p xml:lang=\"fr\">t</p></doc>"

        assertEquals(listOf("/doc/p[1]/q"), paths("//q[lang('en')]", xml))
        assertEquals(listOf("/doc/p[1]/q"), paths("//q[lang('EN-gb')]", xml))
        assertEquals(emptyList(), paths("//q[lang('en-US')]", xml))
        assertEquals(emptyList(), paths("//q[lang('e')]", xml))
        assertEquals(listOf("/doc/p[2]/text()"), paths("//text()[lang('fr')]", xml))
        assertEquals(listOf("/doc/p[1]/q/@a"), paths("//@a[lang('en')]", xml))
        assertEquals("false", written("lang('en')", "<r/>"))
    }

    @Test
    fun theNumberFunctionsAreAsXPathHasThem() {
        assertEquals("12", written("number('  12 ')"))
        assertEquals("-0.5", written("number('-.5')"))
        assertEquals("NaN", written("number('1e3')"))
        assertEquals("NaN", written("number('+1')"))
        assertEquals("NaN", written("number('')"))
        assertEquals("1", written("number(true())"))
        assertEquals("3", written("number(//p)", prices))
        assertEquals("NaN", written("sum(//p)", prices))
        assertEquals("7.5", written("sum(//p[position() < 3])", prices))
        assertEquals("0", written("sum(//nothing)", prices))
        assertEquals("-2", written("floor(-1.5)"))
        assertEquals("2", written("ceiling(1.2)"))
        assertEquals("3", written("round(2.5)"))
        assertEquals("-2", written("round(-2.5)"))
        assertEquals("-Infinity", written("1 div round(-0.4)"))
        assertEquals("NaN", written("round(0 div 0)"))
        assertEquals("Infinity", written("round(1 div 0)"))
    }
}
