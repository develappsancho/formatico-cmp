package com.developersancho.formatico.core.format.query

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonPathFilterTest {
    /** RFC 9535's document for the filter selector's examples. */
    private val document =
        """
        { "a": [3, 5, 1, 2, 4, 6, {"b": "j"}, {"b": "k"}, {"b": {}}, {"b": "kilo"}],
          "o": {"p": 1, "q": 2, "r": 3, "s": 5, "t": {"u": 6}},
          "e": "f" }
        """.trimIndent()

    /** Whether [comparison] is true in RFC 9535's document for its comparisons: a filter on it selects anything. */
    private fun holds(comparison: String): Boolean =
        select("$[?$comparison]", """{"obj": {"x": "y"}, "arr": [2, 3]}""").isNotEmpty()

    @Test
    fun comparisonsAreAsTheRfcsTableHasThem() {
        val table = mapOf(
            "$.absent1 == $.absent2" to true,
            "$.absent1 <= $.absent2" to true,
            "$.absent == 'g'" to false,
            "$.absent1 != $.absent2" to false,
            "$.absent != 'g'" to true,
            "1 <= 2" to true,
            "1 > 2" to false,
            "13 == '13'" to false,
            "'a' <= 'b'" to true,
            "'a' > 'b'" to false,
            "$.obj == $.arr" to false,
            "$.obj != $.arr" to true,
            "$.obj == $.obj" to true,
            "$.obj != $.obj" to false,
            "$.arr == $.arr" to true,
            "$.arr != $.arr" to false,
            "$.obj == 17" to false,
            "$.obj != 17" to true,
            "$.obj <= $.arr" to false,
            "$.obj < $.arr" to false,
            "$.obj <= $.obj" to true,
            "$.arr <= $.arr" to true,
            "1 <= $.arr" to false,
            "1 >= $.arr" to false,
            "1 > $.arr" to false,
            "1 < $.arr" to false,
            "true <= true" to true,
            "true > true" to false
        )

        table.forEach { (comparison, expected) -> assertEquals(expected, holds(comparison), comparison) }
    }

    @Test
    fun filtersSelectAsTheRfcsTableHasThem() {
        assertEquals(listOf("""{"b": "kilo"}"""), select("$.a[?@.b == 'kilo']", document))
        assertEquals(listOf("""{"b": "kilo"}"""), select("$.a[?(@.b == 'kilo')]", document))
        assertEquals(listOf("5", "4", "6"), select("$.a[?@>3.5]", document))
        assertEquals(listOf("""{"b": "j"}""", """{"b": "k"}""", """{"b": {}}""", """{"b": "kilo"}"""), select("$.a[?@.b]", document))
        assertEquals(listOf('[', '{'), select("$[?@.*]", document).map { it.first() })
        assertEquals(listOf('['), select("$[?@[?@.b]]", document).map { it.first() })
        assertEquals(listOf("1", "2", "1", "2"), select("$.o[?@<3, ?@<3]", document))
        assertEquals(listOf("1", """{"b": "k"}"""), select("""$.a[?@<2 || @.b == "k"]""", document))
        assertEquals(listOf("""{"b": "j"}""", """{"b": "k"}"""), select("""$.a[?match(@.b, "[jk]")]""", document))
        assertEquals(listOf("""{"b": "j"}""", """{"b": "k"}""", """{"b": "kilo"}"""), select("""$.a[?search(@.b, "[jk]")]""", document))
        assertEquals(listOf("2", "3"), select("$.o[?@>1 && @<4]", document))
        assertEquals(listOf("""{"u": 6}"""), select("$.o[?@.u || @.x]", document))
        assertEquals(listOf("3", "5", "1", "2", "4", "6"), select("$.a[?@.b == $.x]", document))
        assertEquals(10, select("$.a[?@ == @]", document).size)
    }

    @Test
    fun aQueryAloneTestsWhetherAnythingIsThere() {
        val json = """[{"a": false}, {"a": null}, {"b": 1}, {"a": [], "c": 2}]"""

        assertEquals(listOf("""{"a": false}""", """{"a": null}""", """{"a": [], "c": 2}"""), select("$[?@.a]", json))
        assertEquals(listOf("""{"b": 1}"""), select("$[?!@.a]", json))
        assertEquals(listOf("""{"a": false}"""), select("$[?@.a == false]", json))
    }

    @Test
    fun numbersCompareByTheirValueWhateverTheirSpelling() {
        val json = """[1, 1.0, 1e0, 10e-1, "1", true, -0, 0, 0.5]"""

        assertEquals(listOf("1", "1.0", "1e0", "10e-1"), select("$[?@ == 1]", json))
        assertEquals(listOf("-0", "0"), select("$[?@ == 0]", json))
        assertEquals(listOf("-0", "0"), select("$[?@ == -0]", json))
        assertEquals(listOf("-0", "0", "0.5"), select("$[?@ < 1]", json))
        assertEquals(listOf("1", "1.0", "1e0", "10e-1", "0.5"), select("$[?@ >= 0.5E0 && @ > 0]", json))
        assertEquals(listOf("\"1\""), select("$[?@ == '1']", json))
    }

    @Test
    fun stringsCompareByTheirCodePoints() {
        // U+E000 comes before U+1F600, though its UTF-16 unit comes after the surrogates that write U+1F600.
        assertEquals(listOf("\"\\uE000\""), select("$[?@ < '😀']", """["\uE000", "😀"]"""))
        assertEquals(quoted("a", "ab", "B"), select("$[?@ < 'b']", """["a", "ab", "b", "ba", "B"]"""))
        assertEquals(quoted("B"), select("$[?@ < 'a']", """["a", "B"]"""))
    }

    @Test
    fun arraysAndObjectsAreEqualWhereEverythingInsideThemIs() {
        val json =
            """
            { "target": {"a": [1, {"b": 2}], "c": null},
              "list": [{"c": null, "a": [1, {"b": 2}]}, {"a": [1, {"b": 3}], "c": null}, {"a": [1, {"b": 2}]}, [1, {"b": 2}]] }
            """.trimIndent()

        assertEquals(listOf("""{"c": null, "a": [1, {"b": 2}]}"""), select("$.list[?@ == $.target]", json))
        assertEquals(listOf("[1, {\"b\": 2}]"), select("$.list[?@ == $.target.a]", json))
        assertEquals(3, select("$.list[?@ != $.target]", json).size)
    }

    @Test
    fun notAndOrAndParenthesesTakeTheirPrecedence() {
        val json = "[1, 2, 3, 4, 9]"

        assertEquals(listOf("2", "3", "9"), select("$[?@ > 1 && @ < 4 || @ == 9]", json))
        assertEquals(listOf("2", "3"), select("$[?@ > 1 && (@ < 4 || @ == 1)]", json))
        assertEquals(listOf("1"), select("$[?!(@ > 1)]", json))
        assertEquals(listOf("1", "4", "9"), select("$[?!(@ > 1 && @ < 4)]", json))
        assertEquals(listOf("4"), select("$[?@ == 4 && !(@ == 1) && @ != 9]", json))
    }

    @Test
    fun aFilterReadsTheRootAndNestsInsideAnother() {
        val json = """{"limit": 2, "items": [1, 2, 3], "groups": [[1], [1, 5], []]}"""

        assertEquals(listOf("3"), select("$.items[?@ > $.limit]", json))
        assertEquals(listOf("[1, 5]"), select("$.groups[?@[?@ > $.limit]]", json))
    }

    @Test
    fun lengthCountsCharactersItemsAndMembers() {
        val json = """["ab", "😀é", [1, 2], {"a": 1, "b": 2}, 2, "abc", true, null]"""

        assertEquals(listOf("\"ab\"", "\"😀é\"", "[1, 2]", """{"a": 1, "b": 2}"""), select("$[?length(@) == 2]", json))
        assertEquals(listOf("2", "true", "null"), select("$[?length(@) == $.absent]", json))
        assertEquals(listOf("\"abc\""), select("$[?length(@) == length('xyz')]", json))
    }

    @Test
    fun countAndValueReadWhatAQuerySelects() {
        val json = """[{"x": 1}, {"y": {"x": 1}}, {"x": 1, "y": {"x": 1}}, {"x": 2}]"""

        assertEquals(listOf("""{"x": 1, "y": {"x": 1}}"""), select("$[?count(@..x) == 2]", json))
        assertEquals(listOf("""{"x": 1}""", """{"y": {"x": 1}}"""), select("$[?value(@..x) == 1]", json))
        assertEquals(listOf("""{"x": 2}"""), select("$[?count(@.*) == 1 && value(@.x) > 1]", json))
    }

    @Test
    fun matchMatchesTheWholeStringAndSearchAnyPartOfIt() {
        val json = """["abc", "xabcx", "ABC"]"""

        assertEquals(quoted("abc"), select("$[?match(@, 'abc')]", json))
        assertEquals(quoted("abc", "xabcx"), select("$[?search(@, 'abc')]", json))
        assertEquals(quoted("abc", "xabcx"), select("$[?match(@, '.*b.*')]", json))
        assertEquals(emptyList(), select("$[?match(@, 'b')]", json))
    }

    @Test
    fun aDotMatchesAnyCharacterButALineBreak() {
        assertEquals(quoted("abc", "a😀c", "a\\u2028c"), select("$[?match(@, 'a.c')]", """["abc", "a\nc", "a\rc", "a😀c", "a\u2028c"]"""))
    }

    @Test
    fun caretAndDollarStandForThemselves() {
        assertEquals(quoted("x^a"), select("$[?search(@, '^a')]", """["abc", "x^a"]"""))
        assertEquals(quoted("$1"), select("$[?match(@, '$1')]", """["1", "$1"]"""))
    }

    @Test
    fun classesRangesCategoriesAndQuantifiersAreAnIRegexpHasThem() {
        val words = """["Hello", "hello", "ÉCOLE", "x-1", "]", "aa", "aaa", "a"]"""

        assertEquals(quoted("Hello"), select("$[?match(@, '\\\\p{Lu}\\\\p{Ll}+')]", words))
        assertEquals(quoted("Hello", "hello", "ÉCOLE"), select("$[?match(@, '\\\\p{L}{5}')]", words))
        assertEquals(quoted("x-1", "]"), select("$[?search(@, '\\\\P{L}')]", words))
        assertEquals(quoted("x-1"), select("$[?match(@, '[a-z][-][0-9]')]", words))
        assertEquals(quoted("]"), select("$[?match(@, '[\\\\]\\\\-]')]", words))
        assertEquals(quoted("Hello", "hello", "ÉCOLE", "x-1", "]"), select("$[?match(@, '[^a].*')]", words))
        assertEquals(quoted("aa", "aaa"), select("$[?match(@, 'a{2,}')]", words))
        assertEquals(quoted("aa"), select("$[?match(@, 'a{2}')]", words))
        assertEquals(quoted("aa", "a"), select("$[?match(@, '(a|b){1,2}')]", words))
        assertEquals(quoted("hello", "a"), select("$[?match(@, 'a|h.*')]", words))
        assertEquals(quoted("aaa"), select("$[?match(@, 'a(aa)?a?') && length(@) == 3]", words))
    }

    @Test
    fun aPatternThatIsNoIRegexpMatchesNothing() {
        val patterns =
            listOf("(?i)abc", "a{2,1}", "\\\\d", "a**", "[", "(a", "a)", "\\\\", "{", "a{,2}", "[]", "\\\\p{Xx}", "[a-\\\\p{L}]", "[z-a]")

        patterns.forEach { pattern ->
            assertEquals(emptyList(), select("$[?match(@, '$pattern') || search(@, '$pattern')]", """["abc", "aa", "1", "a"]"""), pattern)
        }
    }

    @Test
    fun aPatternMayComeFromTheDocument() {
        val json = """[{"text": "2026-11-05", "pattern": "[0-9]{4}-[0-9]{2}-[0-9]{2}"}, {"text": "today", "pattern": "[0-9]+"}]"""

        assertEquals(1, select("$[?match(@.text, @.pattern)]", json).size)
        assertEquals(emptyList(), select("$[?match(@.text, 1)]", json))
    }
}
