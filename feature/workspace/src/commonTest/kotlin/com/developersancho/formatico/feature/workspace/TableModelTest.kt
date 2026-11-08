package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class TableModelTest {
    private val orders = """
        {
          "store": "north",
          "orders": [
            {"id": "a", "total": 129.90, "items": ["x", "y"], "coupon": "AUTUMN"},
            {"id": "b", "total": 4.5, "items": [], "coupon": null},
            {"id": "c", "total": 1250, "items": ["z"], "paid": true}
          ]
        }
    """.trimIndent()

    private fun root(text: String = orders, format: Format = Format.Json) =
        assertIs<Reading.Read>(readDocument(text, format)).root

    @Test
    fun aCsvIsAnArrayOfItsRecordsKeyedByTheHeader() {
        val root = root("code,name,numeric\nAD,\"Andorra, Principality\",020\nAL,Albania,008\n", Format.Csv)!!

        assertEquals("[2]", root.text)
        val first = root.children.first()
        assertEquals(listOf("code", "name", "numeric"), first.children.map { it.name })
        assertEquals("Andorra, Principality", first.children[1].value)
        assertEquals(ValueKind.Number, first.children[2].kind)
        assertEquals("020", first.children[2].text)
        assertEquals("$[0].name", first.children[1].key)
    }

    @Test
    fun aTsvIsReadByItsTabs() {
        val root = root("a\tb\n1, 2\t3\n", Format.Tsv)!!
        assertEquals("1, 2", root.children.single().children.first().value)
    }

    @Test
    fun theTableIsTheArrayItsColumnsTheKeysAsTheyAreFirstMet() {
        val table = tableOf(root(), selected = null)!!

        assertEquals("$.orders", table.source.key)
        assertEquals(listOf("id", "total", "items", "coupon", "paid"), table.columns)
        assertEquals(ValueKind.Null, table.rows[1].cell("coupon")?.kind)
        assertNull(table.rows[2].cell("coupon"))
    }

    @Test
    fun theChosenValueSaysTheTableItsRowAndItsColumn() {
        val root = root()
        val table = tableOf(root, "$.orders[1].total")!!

        assertEquals(TableChoice(row = 1, column = "total"), tableChoice(table, trail(root, "$.orders[1].total")))
        assertEquals(TableChoice(row = 2, column = null), tableChoice(table, trail(root, "$.orders[2]")))
        assertNull(tableChoice(table, trail(root, "$.store")))
    }

    @Test
    fun aNestedArrayChosenIsTheTable() {
        val table = tableOf(root(), "$.orders[0].items")!!

        assertEquals("$.orders[0].items", table.source.key)
        assertEquals(listOf(VALUE_COLUMN), table.columns)
        assertEquals("y", table.rows[1].cell(VALUE_COLUMN)?.shownText)
    }

    @Test
    fun aDocumentWithNoArrayHasNoTable() {
        assertNull(tableOf(root("""{"a": {"b": 1}}"""), selected = null))
    }

    @Test
    fun rowsAreFilteredByWhatTheirCellsHold() {
        val table = tableOf(root(), null)!!

        assertEquals(listOf(0), filtered(table.rows, table.columns, "autumn").map { it.index })
        assertEquals(listOf(2), filtered(table.rows, table.columns, "1250").map { it.index })
        assertEquals(3, filtered(table.rows, table.columns, "  ").size)
    }

    @Test
    fun numbersSortAsNumbersAndNullsAndMissingKeysComeLast() {
        val table = tableOf(root(), null)!!

        assertEquals(listOf(1, 0, 2), sorted(table.rows, TableSort("total", descending = false)).map { it.index })
        assertEquals(listOf(2, 0, 1), sorted(table.rows, TableSort("total", descending = true)).map { it.index })
        assertEquals(listOf(0, 1, 2), sorted(table.rows, TableSort("coupon", descending = true)).map { it.index })
        assertEquals(table.rows, sorted(table.rows, null))
    }

    @Test
    fun aColumnSumsUpItsNumbersOrItsValues() {
        val table = tableOf(root(), null)!!

        assertEquals(ColumnStats.Numbers(3, 4.5, 1250.0, (129.9 + 4.5 + 1250) / 3), statsOf(table.rows, "total"))
        assertEquals(ColumnStats.Values(count = 3, different = 1, empty = 2), statsOf(table.rows, "coupon"))
    }

    @Test
    fun rowsAreWrittenOutAsCsvRows() {
        val table = tableOf(root(), null)!!

        assertEquals(
            listOf(
                listOf("id", "items", "coupon"),
                listOf("a", "[\"x\", \"y\"]", "AUTUMN"),
                listOf("b", "[]", ""),
                listOf("c", "[\"z\"]", "")
            ),
            csvRows(table.rows, listOf("id", "items", "coupon"), orders, valueName = "value")
        )
    }
}
