package com.developersancho.formatico.core.format.query

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonPathErrorsTest {
    @Test
    fun aQueryStartsWithTheRootAndHasNothingAfterItsEnd() {
        assertEquals("Empty 0", jsonPathError(""))
        assertEquals("Empty 0", jsonPathError("  "))
        assertEquals("MissingRoot 0", jsonPathError("a.b"))
        assertEquals("MissingRoot 0", jsonPathError(" $.a"))
        assertEquals("UnexpectedCharacter 3", jsonPathError("$.a "))
        assertEquals("UnexpectedCharacter 4", jsonPathError("$.a b"))
        assertEquals("UnexpectedCharacter 1", jsonPathError("\$a"))
    }

    @Test
    fun aSegmentCutShortEndsTheQueryTooEarly() {
        assertEquals("UnexpectedEnd 2", jsonPathError("$."))
        assertEquals("UnexpectedEnd 3", jsonPathError("$.."))
        assertEquals("UnexpectedEnd 2", jsonPathError("$["))
        assertEquals("UnexpectedEnd 3", jsonPathError("$[0"))
        assertEquals("UnexpectedEnd 5", jsonPathError("$[?@."))
    }

    @Test
    fun aSelectorIsOneTheGrammarHas() {
        assertEquals("UnexpectedCharacter 3", jsonPathError("$..."))
        assertEquals("UnexpectedCharacter 2", jsonPathError("$.1a"))
        assertEquals("UnexpectedCharacter 2", jsonPathError("$. a"))
        assertEquals("UnexpectedCharacter 2", jsonPathError("$[]"))
        assertEquals("UnexpectedCharacter 4", jsonPathError("$[0,]"))
        assertEquals("UnexpectedCharacter 3", jsonPathError("$[1.0]"))
        assertEquals("UnexpectedCharacter 3", jsonPathError("$[-]"))
        assertEquals("UnexpectedCharacter 4", jsonPathError("$[1 2]"))
    }

    @Test
    fun anIndexIsAnIntegerWithinIJsonsRange() {
        assertEquals("InvalidNumber 2", jsonPathError("$[01]"))
        assertEquals("InvalidNumber 2", jsonPathError("$[-0]"))
        assertEquals("InvalidNumber 3", jsonPathError("$[:00]"))
        assertEquals("IntegerOutOfRange 2", jsonPathError("$[9007199254740992]"))
        assertEquals("IntegerOutOfRange 2", jsonPathError("$[-9007199254740992]"))
        assertEquals("IntegerOutOfRange 4", jsonPathError("$[::99999999999999999999]"))
    }

    @Test
    fun aStringIsClosedAndEscapesWhatJsonPathEscapes() {
        assertEquals("UnclosedString 2", jsonPathError("$['a"))
        assertEquals("UnclosedString 2", jsonPathError("$['a\\"))
        assertEquals("InvalidEscape 3", jsonPathError("$['\\a']"))
        assertEquals("InvalidEscape 4", jsonPathError("$[\"a\\'\"]"))
        assertEquals("InvalidEscape 4", jsonPathError("$['a\\\"']"))
        assertEquals("InvalidEscape 3", jsonPathError("$['\\u12']"))
        assertEquals("InvalidEscape 3", jsonPathError("$['\\uD800']"))
        assertEquals("InvalidEscape 3", jsonPathError("$['\\uDC00']"))
        assertEquals("InvalidEscape 9", jsonPathError("$['\\uD800\\u0041']"))
        assertEquals("UnexpectedCharacter 4", jsonPathError("$['a\u0001']"))
        // Made as it runs: Kotlin/JS writes a lone surrogate in a literal, a string's or a character's, as "?".
        assertEquals("UnexpectedCharacter 3", jsonPathError("$['" + Char(0xD800) + "']"))
    }

    @Test
    fun aFilterIsWrittenWithJsonPathsOperatorsAndLiterals() {
        assertEquals("UnexpectedCharacter 7", jsonPathError("$[?@.a = 1]"))
        assertEquals("UnexpectedCharacter 3", jsonPathError("$[?]"))
        assertEquals("UnexpectedCharacter 8", jsonPathError("$[?!@.a == 1]"))
        assertEquals("UnexpectedCharacter 9", jsonPathError("$[?(@.a) == 1]"))
        assertEquals("UnexpectedCharacter 12", jsonPathError("$[?@.a == 1 == 2]"))
        assertEquals("UnexpectedCharacter 8", jsonPathError("$[?@ == nul]"))
        assertEquals("UnexpectedCharacter 8", jsonPathError("$[?@ == True]"))
        assertEquals("InvalidNumber 8", jsonPathError("$[?@ == 01]"))
        assertEquals("InvalidNumber 8", jsonPathError("$[?@ == 1.]"))
        assertEquals("InvalidNumber 8", jsonPathError("$[?@ == 1e]"))
        assertEquals("UnexpectedCharacter 8", jsonPathError("$[?@ == .5]"))
    }

    @Test
    fun aComparisonComparesSingularQueries() {
        assertEquals("NotSingular 3", jsonPathError("$[?@.* == 1]"))
        assertEquals("NotSingular 3", jsonPathError("$[?@..a == 1]"))
        assertEquals("NotSingular 3", jsonPathError("$[?@['a', 'b'] == 1]"))
        assertEquals("NotSingular 8", jsonPathError("$[?1 == @[0:1]]"))
        assertEquals("NotSingular 8", jsonPathError("$[?1 == $[?@]]"))
        assertEquals("NotSingular 10", jsonPathError("$[?length(@.*) < 3]"))
    }

    @Test
    fun functionsAreGivenAndGiveWhatTheirTypesSay() {
        assertEquals("WrongType 3", jsonPathError("$[?1]"))
        assertEquals("WrongType 3", jsonPathError("$[?'a']"))
        assertEquals("WrongType 3", jsonPathError("$[?true]"))
        assertEquals("WrongType 4", jsonPathError("$[?!null]"))
        assertEquals("WrongType 3", jsonPathError("$[?length(@)]"))
        assertEquals("WrongType 3", jsonPathError("$[?count(@.*)]"))
        assertEquals("WrongType 3", jsonPathError("$[?value(@..color)]"))
        assertEquals("WrongType 3", jsonPathError("$[?match(@.a, 'x') == true]"))
        assertEquals("WrongType 9", jsonPathError("$[?count(1) == 1]"))
        assertEquals("WrongType 9", jsonPathError("$[?count(@.a == 1) == 1]"))
        assertEquals("WrongType 10", jsonPathError("$[?length(@.a == 1) == 1]"))
        assertEquals("WrongType 10", jsonPathError("$[?length(match(@, 'a')) == 1]"))
        assertEquals("WrongType 9", jsonPathError("$[?value(length(@)) == 1]"))
    }

    @Test
    fun aFunctionIsOneOfTheRfcsFiveWithItsArguments() {
        assertEquals("UnknownFunction 3", jsonPathError("$[?foo(@)]"))
        assertEquals("UnknownFunction 9", jsonPathError("$[?count(foo(@.*)) == 1]"))
        assertEquals("WrongArgumentCount 3", jsonPathError("$[?length(@, 1) == 1]"))
        assertEquals("WrongArgumentCount 3", jsonPathError("$[?length() == 1]"))
        assertEquals("WrongArgumentCount 3", jsonPathError("$[?match(@)]"))
    }

    @Test
    fun wellTypedQueriesAreRead() {
        listOf(
            "$[?length(@) < 3]",
            "$[?count(@.*) == 1]",
            "$[?match(@.timezone, 'Europe/.*')]",
            "$[?value(@..color) == \"red\"]",
            "$[?length(value(@.a)) == 1]",
            "$[?@.a == length('x')]",
            "$[?!match(@, 'a') && (search(@, 'b') || @.c)]"
        ).forEach { jsonPath(it) }
    }

    @Test
    fun aQueryNestedTooDeeplyIsNotRead() {
        assertEquals("TooDeep 66", jsonPathError("$[?" + "(".repeat(70) + "@" + ")".repeat(70) + "]"))
        assertEquals("TooDeep 193", jsonPathError("$" + "[?@".repeat(70) + "]".repeat(70)))
        jsonPath("$[?" + "(".repeat(60) + "@" + ")".repeat(60) + "]")
    }
}
