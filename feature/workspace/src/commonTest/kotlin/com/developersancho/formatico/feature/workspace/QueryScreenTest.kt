package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.testing.awaitText
import com.developersancho.formatico.core.testing.awaitUntil
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.diagnostics.PantrixFindings
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.TestClipboard
import com.pantrixui.testing.TestClock
import com.pantrixui.testing.assertNoFindings
import com.pantrixui.testing.onPantrixNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class QueryScreenTest {
    private val desktop = PantrixWindow(DpSize(1440.dp, 900.dp))
    private val tablet = PantrixWindow(DpSize(834.dp, 1194.dp))
    private val phone = PantrixWindow(DpSize(390.dp, 844.dp))
    private val clock = TestClock(Instant.parse("2026-11-02T09:41:00Z"))

    @Test
    fun aWideWindowShowsWhatTheQueryFoundBesideTheTextAndChoosesAMatch() = runComposeUiTest {
        val findings = PantrixFindings()
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = desktop, findings = findings, clock = clock) {
                WorkspaceScreen(state = OrderNames, onIntent = { intents += it }, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("3 matches. XPath is for XML documents.")
        onPantrixNode("TextField", "query").assert(hasText("$.items[*].name", substring = true))
        // The document's format chooses the language; the other is there, not to be chosen.
        onPantrixNode("Segment", "xpath", scope = "SegmentButton_query_language").assertIsNotEnabled()
        onPantrixNode("Segment", "query", scope = "SegmentButton_view").assertIsSelected()
        onPantrixNode("ListItem", "match0").assert(hasText("$.items[0].name")).assert(hasText("\"Keyboard\"")).assertIsSelected()
        awaitText("Match 1 of 3")
        awaitText("A match is marked in the text; choosing one moves the cursor to it.")
        // The query asked now is not among the recent ones offered.
        onPantrixNode("Chip", "recent0").assert(hasText("$..price"))

        onPantrixNode("ListItem", "match1").performClick()
        assertTrue(WorkspaceIntent.Select("$.items[1].name") in intents)
        assertTrue(WorkspaceIntent.UseQuery() in intents)
        onPantrixNode("Chip", "recent0").performClick()
        awaitUntil("The recent query is asked") { WorkspaceIntent.Query("$..price") in intents }
        // Pressed again, the toolbar's Query gives the tree back.
        onPantrixNode("Button", "query").performClick()
        assertEquals(WorkspaceIntent.ShowPane(DocumentPane.Tree), intents.last())
        assertNoFindings(findings)
    }

    @Test
    fun theResultsAreCopiedAndOpenedAsADocumentOfTheirOwn() = runComposeUiTest {
        val findings = PantrixFindings()
        val clipboard = TestClipboard()
        val workbench = Workbench()
        setContent {
            PantrixTestTheme(window = desktop, findings = findings, clock = clock, clipboard = clipboard) {
                WorkspaceScreen(state = OrderNames, onIntent = {}, commands = rememberDocumentCommands(workbench))
            }
        }

        awaitText("3 matches")
        onPantrixNode("Button", "copy_results").performClick()
        awaitUntil("The results are copied") { clipboard.entries.isNotEmpty() }
        assertEquals("[\n  \"Keyboard\",\n  \"Mouse\",\n  \"Desk pad\"\n]", clipboard.text()?.trimEnd())

        onPantrixNode("Button", "open_results").performClick()
        val opened = workbench.state.value.selected
        assertEquals("order-query.json", opened?.name)
        assertEquals(Format.Json, opened?.format)
        assertNoFindings(findings)
    }

    @Test
    fun theKeyboardsActionUsesTheQueryAsked() = runComposeUiTest {
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                WorkspaceScreen(state = OrderNames, onIntent = { intents += it }, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("3 matches. XPath is for XML documents.")
        // The field names its action: the default one never reaches the handler, and Enter used nothing.
        onPantrixNode("TextField", "query").assert(SemanticsMatcher.expectValue(SemanticsProperties.ImeAction, ImeAction.Search))
        onPantrixNode("TextField", "query").performImeAction()
        // The field's text goes with it: it reaches the document's query a frame later.
        assertEquals(WorkspaceIntent.UseQuery("$.items[*].name"), intents.last())
    }

    @Test
    fun aTabletQueriesAnXmlDocumentWithAnXPathUnderItsText() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = tablet, clock = clock) {
                WorkspaceScreen(state = FeedReleases, onIntent = {}, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("2 matches. JSONPath is for JSON documents.")
        onPantrixNode("Segment", "jsonpath", scope = "SegmentButton_query_language").assertIsNotEnabled()
        onPantrixNode("ListItem", "match0").assert(hasText("/rss/channel/item[1]/title")).assert(hasText("Tabs that remember"))
        // A tablet copies with an icon.
        onPantrixNode("IconButton", "copy_results").assert(hasContentDescription("Copy the results"))
        // What the diagnostics find here is not asked about: the test draws in less than the tablet's window, which
        // leaves the field and the results' row less height than their touch targets (PANTRIX_FINDINGS, "Test
        // temasına verilen pencere, testin çizdiği boy değil").
    }

    @Test
    fun anXPathThatComesToAValueShowsTheValue() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = tablet, clock = clock) {
                WorkspaceScreen(state = feed("count(//item)"), onIntent = {}, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("The XPath comes to a value. JSONPath is for JSON documents.")
        awaitText("Value")
        onPantrixNode("ListItem", "match0").assert(hasText("count(//item)")).assert(hasText("3"))
    }

    @Test
    fun aQueryThatIsNoneSaysWhereAndAnEmptyOneSaysHowToAsk() = runComposeUiTest {
        var state by mutableStateOf(order("$.items["))
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                WorkspaceScreen(state = state, onIntent = {}, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("(character", substring = true)
        onPantrixNode("ListItem", "match0").assertDoesNotExist()
        state = order("")
        awaitText("Write a JSONPath, as $.items[*].name. XPath is for XML documents.")
        state = order("$.discount")
        awaitText("No matches. XPath is for XML documents.")
        onPantrixNode("Button", "open_results").assertIsNotEnabled()
    }

    @Test
    fun typingAQueryAsksItOfTheDocument() = runComposeUiTest {
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                WorkspaceScreen(state = order(""), onIntent = { intents += it }, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("Write a JSONPath", substring = true)
        onPantrixNode("TextField", "query").performTextReplacement("$.id")
        awaitUntil("The query is asked") { WorkspaceIntent.Query("$.id") in intents }
    }

    @Test
    fun aPhoneShowsTheQueryInTheTablesPlaceAndAMatchLeadsToTheText() = runComposeUiTest {
        val findings = PantrixFindings()
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = phone, findings = findings, clock = clock) {
                WorkspaceScreen(state = OrderPrices, onIntent = { intents += it }, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("Choose one to see it in the text")
        onPantrixNode("Segment", "query", scope = "SegmentButton_view").assertIsSelected()
        onPantrixNode("Segment", "table", scope = "SegmentButton_view").assertDoesNotExist()
        onPantrixNode("ListItem", "match0").assert(hasText("89.90")).assert(hasText("$.items[0].price"))
        onPantrixNode("Button", "open_results").assert(hasText("Open as a document"))
        onPantrixNode("IconButton", "copy_results").assert(hasContentDescription("Copy the results"))

        onPantrixNode("ListItem", "match1").performClick()
        assertEquals(
            listOf(WorkspaceIntent.Select("$.items[1].price"), WorkspaceIntent.UseQuery(), WorkspaceIntent.ShowPane(DocumentPane.Text)),
            intents.takeLast(3)
        )
        assertNoFindings(findings)
    }

    @Test
    fun aPhonesMenuOpensTheQuery() = runComposeUiTest {
        var queried = false
        setContent {
            PantrixTestTheme(window = phone, clock = clock) {
                DocumentMenu(
                    items = DocumentMenuItems.FindSummarySortCompareClose,
                    size = ButtonSize.Large,
                    onQuery = { queried = true },
                    canSort = true,
                    onSortKeys = {},
                    onSave = {},
                    onClose = {}
                )
            }
        }

        onPantrixNode("IconButton", "more").performClick()
        // The test draws in less than the phone's window, and the menu's third row stands past its foot: it is pressed
        // as a screen reader presses it (PANTRIX_FINDINGS, "Test temasına verilen pencere, testin çizdiği boy değil").
        onPantrixNode("MenuItem", "query", scope = "Menu_document").performSemanticsAction(SemanticsActions.OnClick)
        awaitUntil("The query is opened") { queried }
    }

    private fun order(query: String) =
        readState(
            OpenDocument(
                id = 1,
                name = "order.json",
                text = PreviewOrder,
                format = Format.Json,
                view = DocumentView(pane = DocumentPane.Query, query = query)
            )
        )

    private fun feed(query: String) =
        readState(
            OpenDocument(
                id = 2,
                name = "feed.xml",
                text = PreviewFeed,
                format = Format.Xml,
                view = DocumentView(pane = DocumentPane.Query, query = query)
            )
        )
}
