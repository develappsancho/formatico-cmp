package com.developersancho.formatico.feature.workspace

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.testing.awaitNode
import com.developersancho.formatico.core.testing.awaitText
import com.developersancho.formatico.core.testing.awaitTexts
import com.developersancho.formatico.feature.workspace.strings.TableStringsTurkish
import com.developersancho.formatico.feature.workspace.strings.tableStringsIn
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.diagnostics.PantrixFindings
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.assertNoFindings
import com.pantrixui.testing.hasPantrixTag
import com.pantrixui.testing.onPantrixNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** A CSV's or a TSV's first row read as its header, or as a record whose columns are named by their places. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class CsvHeaderTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val countries = "code,numeric,name\nAF,004,Afghanistan\nAX,248,Åland Islands\n"

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun tableOf(text: String, header: CsvHeader, format: Format = Format.Csv): TableModel {
        val root = assertIs<Reading.Read>(readDocument(text, format, header)).root
        return assertNotNull(tableOf(root, selected = null))
    }

    @Test
    fun theFirstRowIsTheHeaderUnlessItIsReadAsARecord() {
        val withHeader = tableOf(countries, CsvHeader.FirstRow)
        assertEquals(listOf("code", "numeric", "name"), withHeader.columns)
        assertEquals(2, withHeader.rows.size)

        val withoutHeader = tableOf(countries, CsvHeader.Places(tableStringsIn("en")))
        assertEquals(listOf("column 1", "column 2", "column 3"), withoutHeader.columns)
        assertEquals(3, withoutHeader.rows.size)
        assertEquals("code", withoutHeader.rows[0].cell("column 1")?.shownText)
        assertEquals("Afghanistan", withoutHeader.rows[1].cell("column 3")?.shownText)
    }

    @Test
    fun aColumnWithAWordAtItsHeadIsTextAndItsCodesKeepTheirZeros() {
        val table = tableOf(countries, CsvHeader.Places(tableStringsIn("en")))
        // "numeric" is a word: the column is no column of numbers, and 004 stays as it is written.
        assertTrue(table.rows.all { it.cell("column 2")?.kind == ValueKind.String })
        assertEquals("004", table.rows[1].cell("column 2")?.shownText)
    }

    @Test
    fun theColumnsAreNamedInTheReadersLanguageAndATsvToo() {
        val table = tableOf(countries.replace(',', '\t'), CsvHeader.Places(TableStringsTurkish), Format.Tsv)
        assertEquals(listOf("sütun 1", "sütun 2", "sütun 3"), table.columns)
    }

    @Test
    fun aDocumentReadsItsColumnsAsItsOptionSays() {
        val document = OpenDocument(id = 1, name = "c.csv", text = countries, format = Format.Csv)
        assertEquals(CsvHeader.FirstRow, document.csvHeader("en"))
        assertEquals(CsvHeader.Places(TableStringsTurkish), document.copy(firstRowHeader = false).csvHeader("tr"))
    }

    @Test
    fun theChosenRecordStaysOnItsLineAndTheColumnsChoicesAreLetGo() {
        val document = OpenDocument(
            id = 1,
            name = "c.csv",
            text = countries,
            format = Format.Csv,
            view = DocumentView(
                selected = "$[1].name",
                expanded = setOf("$", "$[1]"),
                tableSort = TableSort("name", descending = true),
                hiddenColumns = setOf("code"),
                tableFilter = "A"
            )
        )

        val off = document.withFirstRowHeader(false)
        assertFalse(off.firstRowHeader)
        // The second record after the header is on the text's third line: the third record without one.
        assertEquals("$[2]", off.view.selected)
        assertNull(off.view.expanded)
        assertNull(off.view.tableSort)
        assertEquals(emptySet(), off.view.hiddenColumns)
        assertEquals("A", off.view.tableFilter)

        assertEquals("$[1]", off.withFirstRowHeader(true).view.selected)
        // The first row chosen, which becomes the header, leaves nothing chosen.
        assertNull(off.copy(view = off.view.copy(selected = "$[0]['column 1']")).withFirstRowHeader(true).view.selected)
        assertSame(document, document.withFirstRowHeader(true))
    }

    @Test
    fun aReadingIsKeptForTheWayItsColumnsWereNamed() {
        val workbench = Workbench()
        val id = workbench.open("c.csv", countries, Format.Csv)
        val readings = Readings(workbench)
        val places = CsvHeader.Places(tableStringsIn("en"))
        readings.keep(id, countries, Format.Csv, readDocument(countries, Format.Csv) to TextStats.of(countries))

        assertNotNull(readings.of(id, countries, Format.Csv))
        assertNull(readings.of(id, countries, Format.Csv, places))
    }

    @Test
    fun theViewModelReadsTheFirstRowAsARecordWhenAsked() {
        val workbench = Workbench(worker = dispatcher)
        val id = workbench.open("c.csv", countries, Format.Csv)
        val viewModel = WorkspaceViewModel(documentId = id, workbench = workbench, readings = Readings(workbench), reader = dispatcher)
        viewModel.dispatch(WorkspaceIntent.Select("$[0]"))

        viewModel.dispatch(WorkspaceIntent.FirstRowHeader(false))

        val state = viewModel.state.value
        assertEquals(false, state.document?.firstRowHeader)
        assertEquals(listOf("column 1", "column 2", "column 3"), state.table?.columns)
        assertEquals(3, state.table?.rows?.size)
        assertEquals("$[1]", state.selected?.key)

        viewModel.dispatch(WorkspaceIntent.FirstRowHeader(true))
        assertEquals(listOf("code", "numeric", "name"), viewModel.state.value.table?.columns)
        assertEquals("$[0]", viewModel.state.value.selected?.key)
    }

    @Test
    fun aCsvWithNoHeaderNamesItsColumnsAgainWhenTheLanguageChanges() {
        val workbench = Workbench(worker = dispatcher)
        val id = workbench.open("c.csv", countries, Format.Csv)
        workbench.change(id) { it.withFirstRowHeader(false) }
        val language = MutableStateFlow("en")
        val viewModel = WorkspaceViewModel(
            documentId = id,
            workbench = workbench,
            readings = Readings(workbench),
            reader = dispatcher,
            language = language
        )
        assertEquals(listOf("column 1", "column 2", "column 3"), viewModel.state.value.table?.columns)

        language.value = "tr"

        assertEquals(listOf("sütun 1", "sütun 2", "sütun 3"), viewModel.state.value.table?.columns)
    }

    @Test
    fun aCsvsColumnsChoiceHasTheHeadersCheckboxAndPressingItAsksForIt() = runComposeUiTest {
        val findings = PantrixFindings()
        val asked = mutableListOf<WorkspaceIntent>()
        val state = readState(
            OpenDocument(id = 1, name = "c.csv", text = countries, format = Format.Csv, view = DocumentView(pane = DocumentPane.Table))
        )
        setContent {
            PantrixTestTheme(window = PantrixWindow(DpSize(1440.dp, 900.dp)), findings = findings) {
                WorkspaceScreen(state = state, onIntent = { asked += it }, commands = rememberDocumentCommands(Workbench()))
            }
        }

        onPantrixNode("Button", "columns").performClick()
        awaitText("Off, the first row is a record, and the columns are named column 1, column 2…")
        onPantrixNode("Checkbox", "first_row_header").assertIsOn().performClick()

        assertEquals(WorkspaceIntent.FirstRowHeader(false), asked.last())
        assertNoFindings(findings)
    }

    @Test
    fun aCsvReadWithNoHeaderHasItsCheckboxOff() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = PantrixWindow(DpSize(1440.dp, 900.dp))) {
                WorkspaceScreen(state = CountriesWithoutHeader, onIntent = {}, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitTexts("column 5")
        onPantrixNode("Button", "columns").performClick()
        onPantrixNode("Checkbox", "first_row_header").assertIsOff()
    }

    @Test
    fun aJsonTablesColumnsChoiceHasNoHeader() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = PantrixWindow(DpSize(1440.dp, 900.dp))) {
                WorkspaceScreen(state = OrderItems, onIntent = {}, commands = rememberDocumentCommands(Workbench()))
            }
        }

        onPantrixNode("Button", "columns").performClick()
        awaitNode(hasPantrixTag("Checkbox", "column_0"), "The first column's checkbox")
        assertTrue(onAllNodes(hasPantrixTag("Checkbox", "first_row_header")).fetchSemanticsNodes().isEmpty())
    }
}
