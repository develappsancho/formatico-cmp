package com.developersancho.formatico.feature.workspace

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.testing.awaitNode
import com.developersancho.formatico.core.testing.awaitText
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.diagnostics.PantrixFindings
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.assertNoFindings
import com.pantrixui.testing.hasPantrixTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class ColumnTypesTest {
    private val orders =
        """
        [
          {"id": "a", "createdAt": "2026-10-28T14:32:00Z", "total": 129.90, "paid": true, "items": [1], "coupon": "AUTUMN"},
          {"id": "b", "createdAt": "2026-10-29", "total": 4.5, "paid": false, "items": [], "coupon": null},
          {"id": "c", "createdAt": "2026-10-30", "total": 1250, "paid": true, "items": [2], "note": {"by": "x"}}
        ]
        """.trimIndent()

    private fun table(text: String = orders, format: Format = Format.Json): TableModel =
        requireNotNull(tableOf(assertIs<Reading.Read>(readDocument(text, format)).root, selected = null))

    @Test
    fun aColumnsTypesAreWhatItsValuesAreTheirNullLast() {
        val table = table()
        val types = { column: String -> typesOf(table.rows, column) }
        assertEquals(listOf(ColumnType.Text), types("id"))
        assertEquals(listOf(ColumnType.Date), types("createdAt"))
        assertEquals(listOf(ColumnType.Number), types("total"))
        assertEquals(listOf(ColumnType.Boolean), types("paid"))
        assertEquals(listOf(ColumnType.Array), types("items"))
        // A missing key is no type; a null is the last of them.
        assertEquals(listOf(ColumnType.Text, ColumnType.Null), types("coupon"))
        assertEquals(listOf(ColumnType.Object), types("note"))
        // Strings are dates only where every one of them is.
        val mixed = table("""[{"when": "2026-10-28"}, {"when": "soon"}]""")
        assertEquals(listOf(ColumnType.Text), typesOf(mixed.rows, "when"))
        // Items that are no objects are the table's value column.
        val values = table("""[1, "two", null, 3]""")
        assertEquals(listOf(ColumnType.Number, ColumnType.Text, ColumnType.Null), typesOf(values.rows, VALUE_COLUMN))
    }

    @Test
    fun theTypesAreWrittenAsTheirWordsJoined() {
        val words = ColumnType.entries.associateWith { it.name.lowercase() }
        assertEquals("text, null", typesLine(listOf(ColumnType.Text, ColumnType.Null), words))
        assertEquals("date", typesLine(listOf(ColumnType.Date), words))
        assertNull(typesLine(emptyList(), words))
    }

    @Test
    fun aWideTableWritesEachColumnsTypesUnderItsNameAndAPhoneLeavesThemOut() = runComposeUiTest {
        val findings = PantrixFindings()
        val window = PantrixWindow(DpSize(1440.dp, 900.dp))
        val state = readState(
            OpenDocument(id = 1, name = "orders.json", text = orders, format = Format.Json, view = DocumentView(pane = DocumentPane.Table))
        )
        setContent {
            PantrixTestTheme(window = window, findings = findings) {
                WorkspaceScreen(state = state, onIntent = {}, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("text, null", forgetting = findings)
        awaitText("date")
        awaitText("boolean")
        assertNoFindings(findings)
    }

    @Test
    fun aPhonesRecordsHaveNoTypes() = runComposeUiTest {
        val state = readState(
            OpenDocument(id = 1, name = "orders.json", text = orders, format = Format.Json, view = DocumentView(pane = DocumentPane.Table))
        )
        setContent {
            PantrixTestTheme(window = PantrixWindow(DpSize(390.dp, 844.dp))) {
                WorkspaceScreen(state = state, onIntent = {}, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitNode(hasPantrixTag("DataTable", "table"), "The records")
        onAllNodesWithText("text, null").assertCountEquals(0)
    }
}
