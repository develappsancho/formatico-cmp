package com.developersancho.formatico.core.format.query

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonPathTest {
    /** RFC 9535's bookstore, from its section 1.5. */
    private val store =
        """
        { "store": {
            "book": [
              { "category": "reference", "author": "Nigel Rees", "title": "Sayings of the Century", "price": 8.95 },
              { "category": "fiction", "author": "Evelyn Waugh", "title": "Sword of Honour", "price": 12.99 },
              { "category": "fiction", "author": "Herman Melville", "title": "Moby Dick", "isbn": "0-553-21311-3", "price": 8.99 },
              { "category": "fiction", "author": "J. R. R. Tolkien", "title": "The Lord of the Rings", "isbn": "0-395-19395-8",
                "price": 22.99 }
            ],
            "bicycle": { "color": "red", "price": 399 }
          }
        }
        """.trimIndent()

    @Test
    fun theRootIdentifierAloneSelectsTheWholeDocument() {
        assertEquals(listOf("[1, 2]"), select("$", "[1, 2]"))
    }

    @Test
    fun theBookstoreIsQueriedAsTheRfcsFirstTableHasIt() {
        val authors = quoted("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien")

        assertEquals(authors, select("$.store.book[*].author", store))
        assertEquals(authors, select("$..author", store))
        assertEquals(listOf('[', '{'), select("$.store.*", store).map { it.first() })
        assertEquals(listOf("8.95", "12.99", "8.99", "22.99", "399"), select("$.store..price", store))
        assertEquals(quoted("Moby Dick"), select("$..book[2].title", store))
        assertEquals(quoted("Herman Melville"), select("$..book[2].author", store))
        assertEquals(emptyList(), select("$..book[2].publisher", store))
        assertEquals(quoted("The Lord of the Rings"), select("$..book[-1].title", store))
        assertEquals(quoted("Sayings of the Century", "Sword of Honour"), select("$..book[0,1].title", store))
        assertEquals(quoted("Sayings of the Century", "Sword of Honour"), select("$..book[:2].title", store))
        assertEquals(quoted("Moby Dick", "The Lord of the Rings"), select("$..book[?@.isbn].title", store))
        assertEquals(quoted("Sayings of the Century", "Moby Dick"), select("$..book[?@.price<10].title", store))
        assertEquals(27, select("$..*", store).size)
    }

    @Test
    fun aNameSelectsTheMemberOfThatNameWrittenInEitherQuotes() {
        val json = """{ "o": {"j j": {"k.k": 3}}, "'": {"@": 2} }"""

        assertEquals(listOf("""{"k.k": 3}"""), select("$.o['j j']", json))
        assertEquals(listOf("3"), select("$.o['j j']['k.k']", json))
        assertEquals(listOf("3"), select("""$.o["j j"]["k.k"]""", json))
        assertEquals(listOf("2"), select("""$["'"]["@"]""", json))
        assertEquals(listOf("2"), select("""$['\'']['@']""", json))
    }

    @Test
    fun aWildcardSelectsEveryMemberOrItemAndAsOftenAsItIsGiven() {
        val json = """{ "o": {"j": 1, "k": 2}, "a": [5, 3] }"""

        assertEquals(listOf("""{"j": 1, "k": 2}""", "[5, 3]"), select("$[*]", json))
        assertEquals(listOf("1", "2"), select("$.o[*]", json))
        assertEquals(listOf("1", "2", "1", "2"), select("$.o[*, *]", json))
        assertEquals(listOf("5", "3"), select("$.a[*]", json))
        assertEquals(emptyList(), select("$.a[0][*]", json))
    }

    @Test
    fun anIndexCountsFromTheStartOrFromTheEndOfAnArrayOnly() {
        val json = """["a", "b"]"""

        assertEquals(quoted("b"), select("$[1]", json))
        assertEquals(quoted("a"), select("$[-2]", json))
        assertEquals(emptyList(), select("$[2]", json))
        assertEquals(emptyList(), select("$[-3]", json))
        assertEquals(emptyList(), select("$[0]", """{"0": 1}"""))
        assertEquals(quoted("a"), select("$[9007199254740991, -9007199254740991, 0]", json))
    }

    @Test
    fun aSliceSelectsFromItsStartToItsEndByItsStepNegativeStepsBackwards() {
        val json = """["a", "b", "c", "d", "e", "f", "g"]"""

        assertEquals(quoted("b", "c"), select("$[1:3]", json))
        assertEquals(quoted("f", "g"), select("$[5:]", json))
        assertEquals(quoted("b", "d"), select("$[1:5:2]", json))
        assertEquals(quoted("f", "d"), select("$[5:1:-2]", json))
        assertEquals(quoted("g", "f", "e", "d", "c", "b", "a"), select("$[::-1]", json))
        assertEquals(quoted("g", "f", "e"), select("$[-1:-4:-1]", json))
        assertEquals(quoted("e", "f", "g"), select("$[-3:]", json))
        assertEquals(emptyList(), select("$[0:7:0]", json))
        assertEquals(emptyList(), select("$[3:1]", json))
        assertEquals(quoted("a"), select("$[-9007199254740991:9007199254740991:9007199254740991]", json))
        assertEquals(quoted("g"), select("$[9007199254740991::-9007199254740991]", json))
    }

    @Test
    fun sliceIndexesFollowTheRfcsBounds() {
        val slice = { start: Long?, end: Long?, step: Long? -> sliceIndexes(5, Selector.Slice(start, end, step)) }

        assertEquals(listOf(0, 1, 2, 3, 4), slice(null, null, null))
        assertEquals(listOf(4, 3, 2, 1, 0), slice(null, null, -1))
        assertEquals(listOf(3, 1), slice(-2, -10, -2))
        assertEquals(listOf(0, 3), slice(-10, 10, 3))
        assertEquals(emptyList(), sliceIndexes(0, Selector.Slice(null, null, -1)))
    }

    @Test
    fun aDescendantSegmentVisitsEachValueBeforeTheValuesInsideIt() {
        val json = """{ "o": {"j": 1, "k": 2}, "a": [5, 3, [{"j": 4}, {"k": 6}]] }"""

        assertEquals(listOf("1", "4"), select("$..j", json))
        assertEquals(listOf("5", """{"j": 4}"""), select("$..[0]", json))
        val everything = listOf(
            """{"j": 1, "k": 2}""",
            """[5, 3, [{"j": 4}, {"k": 6}]]""",
            "1",
            "2",
            "5",
            "3",
            """[{"j": 4}, {"k": 6}]""",
            """{"j": 4}""",
            """{"k": 6}""",
            "4",
            "6"
        )
        assertEquals(everything, select("$..[*]", json))
        assertEquals(everything, select("$..*", json))
        assertEquals(listOf("""{"j": 1, "k": 2}"""), select("$..o", json))
        assertEquals(listOf("1", "2", "1", "2"), select("$.o..[*, *]", json))
        assertEquals(listOf("5", "3", """{"j": 4}""", """{"k": 6}"""), select("$.a..[0, 1]", json))
    }

    @Test
    fun nullIsAValueAndAMissingMemberIsNone() {
        val json = """{"a": null, "b": [null], "c": [{}], "null": 1}"""

        assertEquals(listOf("null"), select("$.a", json))
        assertEquals(emptyList(), select("$.a[0]", json))
        assertEquals(emptyList(), select("$.a.d", json))
        assertEquals(listOf("null"), select("$.b[0]", json))
        assertEquals(listOf("null"), select("$.b[*]", json))
        assertEquals(listOf("null"), select("$.b[?@]", json))
        assertEquals(listOf("null"), select("$.b[?@==null]", json))
        assertEquals(emptyList(), select("$.c[?@.d==null]", json))
        assertEquals(listOf("1"), select("$.null", json))
    }

    @Test
    fun namesMayHoldAnyCharacterBeyondAscii() {
        val json = """{"café": 1, "日本": 2, "😀": 3, "é": 4, "a b": 5}"""

        assertEquals(listOf("1"), select("$.café", json))
        assertEquals(listOf("2"), select("$.日本", json))
        assertEquals(listOf("3"), select("$.😀", json))
        assertEquals(listOf("4"), select("""$["é"]""", json))
        assertEquals(listOf("1"), select("$['caf\\u00e9']", json))
        assertEquals(listOf("3"), select("$['\\uD83D\\uDE00']", json))
        assertEquals(listOf("5"), select("$['a\\u0020b']", json))
    }

    @Test
    fun aNameGivenTwiceSelectsEveryMemberOfThatName() {
        assertEquals(listOf("1", "2"), select("$.a", """{"a": 1, "a": 2}"""))
        // A query that selects two values gives none to a comparison, as `value()` does.
        assertEquals(listOf("""{"a": 1}"""), select("$[?@.a == 1]", """[{"a": 1, "a": 1}, {"a": 1}]"""))
    }

    @Test
    fun whitespaceStandsBeforeSegmentsAndInsideBrackets() {
        val json = """{"a": [1, 2, 3]}"""

        assertEquals(listOf("1", "2"), select("$ .a [ 0 , 1 ]", json))
        assertEquals(listOf("2", "3"), select("$.a[ 1 : 3 : 1 ]", json))
        assertEquals(listOf("[1, 2, 3]"), select("$[ ?\t@ [ 2 ] \n]", """[[1, 2, 3], [4]]"""))
    }

    @Test
    fun aDeepDocumentIsWalkedWithoutRecursion() {
        fun chain(depth: Int): Built {
            var value = Built(JsonKind.Number, number = 1.0)
            repeat(depth) { value = Built(JsonKind.Array, listOf(value)) }
            return value
        }
        val deep = chain(DEPTH)
        val root = Built(JsonKind.Array, listOf(chain(DEPTH), chain(DEPTH)))

        assertEquals(DEPTH, jsonPath("$..[0]").select(deep, BuiltValues).size)
        assertEquals(DEPTH, jsonPath("$..*").select(deep, BuiltValues).size)
        assertEquals(2, jsonPath("$[?@ == $[1]]").select(root, BuiltValues).size)
        assertEquals(1, jsonPath("$..[?@ == 1]").select(deep, BuiltValues).size)
    }

    @Test
    fun aWideDocumentIsFilteredWhole() {
        val items = List(WIDTH) { Built(JsonKind.Number, number = it.toDouble()) }
        val root = Built(JsonKind.Array, items)

        assertEquals(listOf(WIDTH - 2.0, WIDTH - 1.0), jsonPath("$[?@ >= ${WIDTH - 2}]").select(root, BuiltValues).map { it.number })
        assertEquals(listOf(WIDTH - 1.0), jsonPath("$[-1]").select(root, BuiltValues).map { it.number })
    }

    private companion object {
        const val DEPTH = 2_000
        const val WIDTH = 100_000
    }
}
