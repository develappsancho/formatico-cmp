package com.developersancho.formatico.core.format.diff

import com.developersancho.formatico.core.format.convert.DataDate
import com.developersancho.formatico.core.format.convert.DataMember
import com.developersancho.formatico.core.format.convert.DataNull
import com.developersancho.formatico.core.format.convert.DataNumber
import com.developersancho.formatico.core.format.convert.DataObject
import com.developersancho.formatico.core.format.convert.DataString
import com.developersancho.formatico.core.format.convert.dataOf
import com.developersancho.formatico.core.format.json.jsonPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompareTest {
    /** The two orders of the Compare boards. */
    private val boardA =
        """
        {
          "id": "ord_8842",
          "status": "shipped",
          "createdAt": "2026-10-28T14:32:00Z",
          "total": 129.90,
          "coupon": null,
          "items": [
            { "sku": "TS-001", "qty": 2 },
            { "sku": "MG-014", "qty": 1 }
          ]
        }
        """.trimIndent()

    private val boardB =
        """
        {
          "status": "delivered",
          "id": "ord_8842",
          "createdAt": "2026-10-28T14:32:00Z",
          "deliveredAt": "2026-11-01T10:05:00Z",
          "total": 129.9,
          "items": [
            { "sku": "TS-001", "qty": 2 },
            { "sku": "MG-014", "qty": 2 }
          ]
        }
        """.trimIndent()

    /** Each difference as the boards' list writes it: its change, its path, and its values. */
    private fun ValueComparison.written() = differences.map { "${it.change} ${jsonPath(it.path)} ${it.before} ${it.after}" }

    @Test
    fun theBoardsOrdersDifferInFourValuesAndNotInTheirKeysOrderOrTheirTotalsSpelling() {
        val comparison = compareValues(dataOf(boardA), dataOf(boardB))
        assertEquals(
            listOf(
                "Changed $.status DataString(value=shipped) DataString(value=delivered)",
                "Added $.deliveredAt null DataString(value=2026-11-01T10:05:00Z)",
                "Removed $.coupon DataNull null",
                "Changed $.items[1].qty DataNumber(written=1) DataNumber(written=2)"
            ),
            comparison.written()
        )
        assertEquals(KeysMoved("status", "id", 1), comparison.moved)
        assertEquals(NumbersRespelled("129.90", "129.9", 1), comparison.respelled)
    }

    @Test
    fun aDocumentIsTheSameAsItself() {
        val comparison = compareValues(dataOf(boardA), dataOf(boardA))
        assertEquals(emptyList(), comparison.differences)
        assertNull(comparison.moved)
        assertNull(comparison.respelled)
    }

    @Test
    fun anItemPutInAnArrayIsAddedAndTheItemsAfterItAreNotChanged() {
        val comparison = compareValues(dataOf("""{"tags": ["a", "b", "c"]}"""), dataOf("""{"tags": ["a", "x", "b", "c", "d"]}"""))
        assertEquals(listOf("Added $.tags[1] null DataString(value=x)", "Added $.tags[4] null DataString(value=d)"), comparison.written())
        val gone = compareValues(dataOf("""[1, 2, 3]"""), dataOf("""[1, 3]"""))
        assertEquals(listOf("Removed $[1] DataNumber(written=2) null"), gone.written())
    }

    @Test
    fun aValueOfAnotherKindIsChangedWhole() {
        val comparison = compareValues(dataOf("""{"a": {"b": 1}, "c": "1", "d": [1]}"""), dataOf("""{"a": [1], "c": 1, "d": {"0": 1}}"""))
        assertEquals(listOf("$.a", "$.c", "$.d"), comparison.differences.map { jsonPath(it.path) })
        assertTrue(comparison.differences.all { it.change == Change.Changed })
    }

    @Test
    fun numbersAreTheSameNumberHoweverTheyAreWritten() {
        assertEquals(canonicalNumber("129.90"), canonicalNumber("129.9"))
        assertEquals(canonicalNumber("1e3"), canonicalNumber("1000"))
        assertEquals(canonicalNumber("0x1F"), canonicalNumber("31"))
        assertEquals(canonicalNumber("1_000"), canonicalNumber("1000.0"))
        assertEquals(canonicalNumber("0"), canonicalNumber("-0.0"))
        assertTrue(canonicalNumber("1.5") != canonicalNumber("15"))
        assertTrue(sameScalar(DataDate("2026-11-04"), DataString("2026-11-04")))
        assertTrue(!sameScalar(DataNumber("1"), DataString("1")))
        assertTrue(sameScalar(DataNull, DataNull))
    }

    @Test
    fun aNameGivenTwiceIsMatchedByItsPlaceAmongThoseOfItsName() {
        val a = DataObject(listOf(DataMember("a", DataNumber("1")), DataMember("a", DataNumber("2"))))
        val b = DataObject(listOf(DataMember("a", DataNumber("1")), DataMember("a", DataNumber("3"))))
        assertEquals(listOf("Changed $.a DataNumber(written=2) DataNumber(written=3)"), compareValues(a, b).written())
    }

    @Test
    fun keysThatMovedPastMoreThanOneAreCountedAndTheFirstNamed() {
        val comparison = compareValues(dataOf("""{"a": 1, "b": 2, "c": 3}"""), dataOf("""{"c": 3, "a": 1, "b": 2}"""))
        assertEquals(emptyList(), comparison.differences)
        assertEquals(KeysMoved("c", null, 1), comparison.moved)
    }

    @Test
    fun linesDifferInRunsChangedAddedAndRemoved() {
        val a = "one\ntwo\nthree\nfour\nfive\n"
        val b = "one\n2\nthree\nfour\nfour and a half\nfive\nsix\n"
        assertEquals(
            listOf(
                LineDifference(Change.Changed, aStart = 1, aCount = 1, bStart = 1, bCount = 1),
                LineDifference(Change.Added, aStart = 4, aCount = 0, bStart = 4, bCount = 1),
                LineDifference(Change.Added, aStart = 5, aCount = 0, bStart = 6, bCount = 1)
            ),
            compareLines(a, b)
        )
        assertEquals(listOf(LineDifference(Change.Removed, 0, 1, 0, 0)), compareLines("a\nb", "b"))
        assertEquals(emptyList(), compareLines("same\r\ntext\n", "same\ntext"))
    }

    @Test
    fun theBoardsTextsDifferInFourRunsOfLines() {
        // As text, the two keys that changed places are a line added and a line removed.
        assertEquals(
            listOf(
                LineDifference(Change.Added, aStart = 1, aCount = 0, bStart = 1, bCount = 1),
                LineDifference(Change.Removed, aStart = 2, aCount = 1, bStart = 3, bCount = 0),
                LineDifference(Change.Changed, aStart = 4, aCount = 2, bStart = 4, bCount = 2),
                LineDifference(Change.Changed, aStart = 8, aCount = 1, bStart = 8, bCount = 1)
            ),
            compareLines(boardA, boardB)
        )
    }
}
