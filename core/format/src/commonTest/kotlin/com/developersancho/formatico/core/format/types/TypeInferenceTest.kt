package com.developersancho.formatico.core.format.types

import com.developersancho.formatico.core.format.convert.DataArray
import com.developersancho.formatico.core.format.convert.DataDate
import com.developersancho.formatico.core.format.convert.DataNumber
import com.developersancho.formatico.core.format.convert.DataString
import com.developersancho.formatico.core.format.convert.dataOf
import com.developersancho.formatico.core.format.convert.lines
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TypeInferenceTest {
    @Test
    fun aMemberSomeObjectsLackIsOptionalAndOneNullInSomeIsNullable() {
        val json = """{"items": [{"sku": "a", "note": "x", "gift": true}, {"sku": "b", "note": null}]}"""
        assertEquals(
            lines(
                "export interface Root {",
                "  items: Item[];",
                "}",
                "",
                "export interface Item {",
                "  sku: string;",
                "  note: string | null;",
                "  gift?: boolean;",
                "}"
            ),
            textOf(json, TypeLanguage.TypeScript)
        )
        val kotlin = textOf(json, TypeLanguage.Kotlin)
        assertContainsLines(kotlin, "    val sku: String,", "    val note: String? = null,", "    val gift: Boolean? = null")
        val swift = textOf(json, TypeLanguage.Swift)
        assertContainsLines(swift, "    let sku: String", "    let note: String?", "    let gift: Bool?")
        val go = textOf(json, TypeLanguage.Go)
        assertContainsLines(
            go,
            "\tSku  string  `json:\"sku\"`",
            "\tNote *string `json:\"note\"`",
            "\tGift *bool   `json:\"gift,omitempty\"`"
        )
    }

    @Test
    fun aMemberBothOptionalAndNullableIsSoInEveryLanguage() {
        val json = """{"rows": [{"a": 1}, {"a": null, "b": "x"}, {"b": null}]}"""
        assertContainsLines(textOf(json, TypeLanguage.TypeScript), "  a?: number | null;", "  b?: string | null;")
        assertContainsLines(textOf(json, TypeLanguage.Kotlin), "    val a: Int? = null,", "    val b: String? = null")
        assertContainsLines(textOf(json, TypeLanguage.Go), "\tA *int    `json:\"a,omitempty\"`", "\tB *string `json:\"b,omitempty\"`")
    }

    @Test
    fun wholeNumbersAreIntsPast32BitsLongsAndBesideDecimalsDoubles() {
        val json =
            """
            {"small": 1, "big": 3000000000, "lowest": -2147483648, "lower": -2147483649, "mixed": [1, 2.5], "hex": 0x1F,
             "huge": 99999999999999999999, "exponent": 1e3, "decimal": 50.00, "infinite": Infinity}
            """.trimIndent()
        assertContainsLines(
            textOf(json, TypeLanguage.Kotlin),
            "    val small: Int,",
            "    val big: Long,",
            "    val lowest: Int,",
            "    val lower: Long,",
            "    val mixed: List<Double>,",
            "    val hex: Int,",
            "    val huge: Double,",
            "    val exponent: Double,",
            "    val decimal: Double,",
            "    val infinite: Double"
        )
        assertContainsLines(textOf("""{"rows": [{"n": 1}, {"n": 3000000000}, {"n": 2}]}""", TypeLanguage.Kotlin), "    val n: Long")
        assertContainsLines(textOf("""{"rows": [{"n": 1}, {"n": 3000000000}, {"n": 2.5}]}""", TypeLanguage.Go), "\tN float64 `json:\"n\"`")
        assertContainsLines(
            textOf("""{"big": 3000000000, "small": 1}""", TypeLanguage.Go),
            "\tBig   int64 `json:\"big\"`",
            "\tSmall int   `json:\"small\"`"
        )
        assertContainsLines(textOf("""{"big": 3000000000, "small": 1}""", TypeLanguage.Swift), "    let big: Int", "    let small: Int")
    }

    @Test
    fun aMemberOnlyEverNullIsOfAnUnknownTypeAndNoted() {
        val json = """{"coupon": null, "slots": [null, null], "rows": [{"gone": null}, {"gone": null}]}"""
        val kotlin = typesOf(json, TypeLanguage.Kotlin)
        assertContainsLines(
            kotlin.text,
            "    val coupon: JsonElement? = null,",
            "    val slots: List<JsonElement?>,",
            "    val gone: JsonElement? = null"
        )
        assertEquals(listOf(TypeNote.OnlyNull(listOf("coupon", "slots", "gone"))), kotlin.notes)
        assertContainsLines(textOf(json, TypeLanguage.TypeScript), "  coupon: unknown;", "  slots: unknown[];", "  gone: unknown;")
        assertContainsLines(
            textOf(json, TypeLanguage.Swift),
            "    let coupon: String? // Only ever null here: its type is a guess",
            "    let slots: [String?] // Only ever null here: its type is a guess"
        )
        assertContainsLines(textOf(json, TypeLanguage.Go), "\tCoupon any   `json:\"coupon\"`", "\tSlots  []any `json:\"slots\"`")
    }

    @Test
    fun aMemberOfMoreThanOneKindIsTheLanguagesAnythingAndNoted() {
        val json = """{"rows": [{"v": 1, "w": 1}, {"v": "x", "w": null}, {"v": {"a": 1}, "w": [1]}], "tags": [1, "a", true]}"""
        val kotlin = typesOf(json, TypeLanguage.Kotlin)
        assertContainsLines(kotlin.text, "    val tags: List<JsonElement>", "    val v: JsonElement,", "    val w: JsonElement? = null")
        assertEquals(
            listOf(TypeNote.Mixed(listOf("rows", "tags"))),
            typesOf("""{"rows": [1, "a"], "tags": [true, 2]}""", TypeLanguage.Go).notes
        )
        assertEquals(listOf(TypeNote.Mixed(listOf("v", "w", "tags"))), kotlin.notes)
        assertEquals(2, kotlin.types)
        assertContainsLines(textOf(json, TypeLanguage.TypeScript), "  v: unknown;", "  w: unknown;", "  tags: unknown[];")
        assertContainsLines(textOf(json, TypeLanguage.Go), "\tV any `json:\"v\"`", "\tW any `json:\"w\"`")
        assertContainsLines(textOf(json, TypeLanguage.Swift), "    let v: JSONValue", "    let w: JSONValue?", "    let tags: [JSONValue]")
    }

    @Test
    fun anArrayEmptyInEveryValueIsAListOfAnythingAndNoted() {
        val json = """{"tags": [], "rows": [{"labels": [], "seen": []}, {"labels": [], "seen": ["a"]}]}"""
        val kotlin = typesOf(json, TypeLanguage.Kotlin)
        assertContainsLines(
            kotlin.text,
            "    val tags: List<JsonElement>,",
            "    val labels: List<JsonElement>,",
            "    val seen: List<String>"
        )
        assertEquals(listOf(TypeNote.EmptyList(listOf("tags", "labels"))), kotlin.notes)
        assertContainsLines(textOf(json, TypeLanguage.TypeScript), "  tags: unknown[];", "  labels: unknown[];", "  seen: string[];")
        assertContainsLines(textOf(json, TypeLanguage.Swift), "    let tags: [JSONValue]", "    let labels: [JSONValue]")
        assertContainsLines(textOf(json, TypeLanguage.Go), "\tTags []any `json:\"tags\"`")
    }

    @Test
    fun anArrayOfArraysIsAListOfListsItsObjectsNamedAfterTheMember() {
        val json = """{"matrix": [[1, 2], [3]], "grid": [[{"x": 1}], [{"x": 2}]], "slots": [[1], null], "rows": [[[]]]}"""
        val kotlin = typesOf(json, TypeLanguage.Kotlin)
        assertContainsLines(
            kotlin.text,
            "    val matrix: List<List<Int>>,",
            "    val grid: List<List<GridItem>>,",
            "    val slots: List<List<Int>?>,",
            "    val rows: List<List<List<JsonElement>>>"
        )
        assertEquals(listOf(TypeNote.EmptyList(listOf("rows"))), kotlin.notes)
        assertContainsLines(
            textOf(json, TypeLanguage.TypeScript),
            "  matrix: number[][];",
            "  grid: GridItem[][];",
            "  slots: (number[] | null)[];",
            "  rows: unknown[][][];"
        )
        assertContainsLines(textOf(json, TypeLanguage.Swift), "    let grid: [[GridItem]]", "    let slots: [[Int]?]")
        assertContainsLines(
            textOf(json, TypeLanguage.Go),
            "\tGrid   [][]GridItem `json:\"grid\"`",
            "\tSlots  [][]int      `json:\"slots\"`"
        )
    }

    @Test
    fun nullableItemsAreMarkedInEachLanguage() {
        val json = """{"ids": [1, null], "tags": [{"a": 1}, null]}"""
        assertContainsLines(textOf(json, TypeLanguage.Kotlin), "    val ids: List<Int?>,", "    val tags: List<Tag?>")
        assertContainsLines(textOf(json, TypeLanguage.TypeScript), "  ids: (number | null)[];", "  tags: (Tag | null)[];")
        assertContainsLines(textOf(json, TypeLanguage.Swift), "    let ids: [Int?]", "    let tags: [Tag?]")
        assertContainsLines(textOf(json, TypeLanguage.Go), "\tIds  []*int `json:\"ids\"`", "\tTags []*Tag `json:\"tags\"`")
    }

    @Test
    fun objectsOfTheSameMembersAreOneTypeTheFirstNameKept() {
        val json = """{"billing": {"city": "a", "country": "b"}, "shipping": {"city": "c", "country": "d"}, "home": {"city": "e"}}"""
        val types = typesOf(json, TypeLanguage.TypeScript)
        assertEquals(3, types.types)
        assertContainsLines(types.text, "  billing: Billing;", "  shipping: Billing;", "  home: Home;")
        assertEquals(listOf("Root", "Billing", "Home"), declaredNames(types.text, "export interface "))
    }

    @Test
    fun objectsOfTheSameNamesButOtherTypesAreTwoTypes() {
        val json = """{"from": {"city": "a"}, "to": {"city": 1}, "via": {"city": "b", "pin": true}}"""
        assertEquals(4, typesOf(json, TypeLanguage.Kotlin).types)
    }

    @Test
    fun anotherTypeWantingANameTakenGetsANumberAfterIt() {
        val json = """{"item": {"a": 1}, "items": [{"b": "x"}], "other": {"item": {"c": true}}, "again": {"item": {"a": 2}}}"""
        val types = typesOf(json, TypeLanguage.TypeScript)
        assertEquals(listOf("Root", "Item", "Item2", "Other", "Again", "Item3"), declaredNames(types.text, "export interface "))
        assertContainsLines(types.text, "  item: Item;", "  items: Item2[];", "  item: Item3;")
    }

    @Test
    fun aNameTheLanguageGivesATypeOfItsOwnIsTakenAlready() {
        val json = """{"string": {"a": 1}, "int": {"b": 1}, "list": [{"c": 1}]}"""
        assertEquals(listOf("Root", "String2", "Int2", "ListItem"), declaredNames(textOf(json, TypeLanguage.Kotlin), "data class "))
        assertEquals(listOf("Root", "String2", "Int2", "ListItem"), declaredNames(textOf(json, TypeLanguage.Swift), "struct "))
        assertEquals(listOf("Root", "String", "Int", "ListItem"), declaredNames(textOf(json, TypeLanguage.TypeScript), "export interface "))
        val keywords = """{"any": {"a": 1}, "self": {"b": 1}, "type": {"c": 1}}"""
        assertEquals(listOf("Root", "Any2", "Self2", "Type"), declaredNames(textOf(keywords, TypeLanguage.Swift), "struct "))
    }

    @Test
    fun typesAreWrittenBreadthFirstFromTheRoot() {
        val json = """{"a": {"deep": {"x": 1}}, "b": {"y": 1}, "c": [{"z": {"w": 1}}]}"""
        assertEquals(
            listOf("Root", "A", "B", "CItem", "Deep", "Z"),
            declaredNames(textOf(json, TypeLanguage.TypeScript), "export interface ")
        )
    }

    @Test
    fun aDocumentThatIsAListIsTypedByItsItemsAndNoted() {
        val ndjson = DataArray(listOf(dataOf("""{"id": 1, "name": "a"}"""), dataOf("""{"id": 2, "tags": []}""")))
        val types = typesOf(ndjson, TypeLanguage.TypeScript, "Event")
        assertEquals(
            lines(
                "export interface Event {",
                "  id: number;",
                "  name?: string;",
                "  tags?: unknown[];",
                "}"
            ),
            types.text
        )
        assertEquals(1, types.types)
        assertEquals(listOf(TypeNote.RootList, TypeNote.EmptyList(listOf("tags"))), types.notes)
        assertEquals(listOf(TypeNote.RootList), typesOf("""[{"a": 1}, null]""", TypeLanguage.Kotlin).notes)
    }

    @Test
    fun aDocumentWithNoObjectToTypeHasNoTypes() {
        val documents = listOf(
            DataString("x"),
            DataNumber("1"),
            dataOf("null"),
            dataOf("[]"),
            dataOf("[1, 2]"),
            dataOf("[null]"),
            dataOf("""[{"a": 1}, 2]"""),
            dataOf("""[[{"a": 1}]]""")
        )
        documents.forEach { value -> TypeLanguage.entries.forEach { assertNull(generateTypes(value, it, "Root"), "$value in $it") } }
    }

    @Test
    fun stringsThatAreAllDatesAndTomlsDatesAreNoted() {
        val toml = objectOf("released" to DataDate("2026-11-04"), "at" to DataDate("14:32:00"), "name" to DataString("x"))
        val types = typesOf(toml, TypeLanguage.Kotlin)
        assertContainsLines(types.text, "    val released: String,", "    val at: String,")
        assertEquals(listOf(TypeNote.Dates(listOf("released", "at"))), types.notes)
        val json =
            """
            {"rows": [{"day": "2026-01-01", "when": "2026-10-28T14:32:00.5+03:00", "time": "14:32", "not": "2026-01-01"},
                      {"day": null, "when": "2026-10-28 14:32:00z", "time": "23:59:60Z", "not": "soon"}],
             "dates": ["2026-01-01", "2026-13-01"], "days": ["2026-02-28"]}
            """.trimIndent()
        assertEquals(listOf(TypeNote.Dates(listOf("day", "when", "time", "days"))), typesOf(json, TypeLanguage.Go).notes)
    }

    @Test
    fun anObjectOfNoMembersIsATypeOfNone() {
        val json = """{"meta": {}}"""
        assertEquals(
            lines(
                "import kotlinx.serialization.Serializable",
                "",
                "@Serializable",
                "data class Root(",
                "    val meta: Meta",
                ")",
                "",
                "@Serializable",
                "class Meta"
            ),
            textOf(json, TypeLanguage.Kotlin)
        )
        assertContainsLines(textOf(json, TypeLanguage.TypeScript), "export interface Meta {}")
        assertContainsLines(textOf(json, TypeLanguage.Swift), "struct Meta: Codable {}")
        assertContainsLines(textOf(json, TypeLanguage.Go), "type Meta struct{}")
        assertEquals(lines("package root", "", "type Root struct{}"), textOf("{}", TypeLanguage.Go))
        assertEquals(1, typesOf("{}", TypeLanguage.Swift).types)
    }

    @Test
    fun aNameGivenTwiceInAnObjectIsTypedByItsFirstValue() {
        val value = objectOf("a" to DataNumber("1"), "a" to DataString("x"), "b" to DataString("y"))
        assertEquals(lines("export interface Root {", "  a: number;", "  b: string;", "}"), typesOf(value, TypeLanguage.TypeScript).text)
    }
}

/** Fails where [text] does not hold each of [lines] as a whole line. */
internal fun assertContainsLines(text: String, vararg lines: String) {
    val all = text.lines()
    lines.forEach { line -> assertTrue(line in all, "No line \"$line\" in:\n$text") }
}

/** The names [text] declares, each after [keyword] at the start of a line. */
internal fun declaredNames(text: String, keyword: String): List<String> =
    text.lines().filter { it.startsWith(keyword) }.map { it.removePrefix(keyword).takeWhile { char -> char.isLetterOrDigit() } }
