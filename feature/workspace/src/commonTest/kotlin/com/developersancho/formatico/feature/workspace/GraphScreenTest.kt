package com.developersancho.formatico.feature.workspace

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.testing.awaitNode
import com.developersancho.formatico.core.testing.awaitText
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.diagnostics.PantrixFindings
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.TestClock
import com.pantrixui.testing.assertNoFindings
import com.pantrixui.testing.hasPantrixTag
import com.pantrixui.testing.onPantrixNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class GraphScreenTest {
    private val desktop = PantrixWindow(DpSize(1440.dp, 900.dp))
    private val phone = PantrixWindow(DpSize(390.dp, 844.dp))
    private val clock = TestClock(Instant.parse("2026-11-02T09:41:00Z"))

    @Test
    fun aWideWindowDrawsTheCardsBesideTheTextAndChoosesAValueByItsCard() = runComposeUiTest {
        val findings = PantrixFindings()
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = desktop, findings = findings, clock = clock) {
                WorkspaceScreen(state = OrderGraph, onIntent = { intents += it }, commands = rememberDocumentCommands(Workbench()))
            }
        }

        onPantrixNode("Segment", "graph", scope = "SegmentButton_view").assertIsSelected()
        // Each card is read by its label; the customer, which holds the chosen value, is chosen.
        val customer = onPantrixNode("Node", "7", scope = "Graph_document")
        customer.assert(hasContentDescription("customer, an object of 4 keys")).assertIsSelected()
        onPantrixNode("Node", "root", scope = "Graph_document").assert(hasContentDescription("order.json, an object of 11 keys"))
        // The test draws the window 1024 wide, whatever it is told (PANTRIX_FINDINGS, "Test temasına verilen pencere,
        // testin çizdiği boy değil"): a pane that narrow has the zoom in the graph's corner, not in its header.
        onPantrixNode("IconButton", "fit", scope = "document").assertExists()
        awaitText("85%")

        onPantrixNode("Node", "8", scope = "Graph_document").performClick()
        onPantrixNode("Node", "10", scope = "Graph_document").performSemanticsAction(SemanticsActions.Collapse)
        onPantrixNode("Button", "level_1", scope = "ButtonGroup_levels").performClick()
        onPantrixNode("Segment", "tree", scope = "SegmentButton_view").performClick()
        assertEquals(WorkspaceIntent.Select("$.theme"), intents[0])
        val folded = intents[1] as WorkspaceIntent.Expand
        assertTrue("$.shipping" !in folded.keys && "$.customer" in folded.keys, "${folded.keys}")
        assertEquals(WorkspaceIntent.Expand(setOf("$")), intents[2])
        assertEquals(WorkspaceIntent.ShowPane(DocumentPane.Tree), intents[3])
        assertNoFindings(findings)
    }

    @Test
    fun aLargeArrayOpensWithItsRootsCardInView() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                WorkspaceScreen(
                    state = graphed("people.json", peopleOf(150), Format.Json),
                    onIntent = {},
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }

        // The root stands across from the middle of its 150 cards: with no value chosen, the graph is brought to it.
        awaitNode(hasPantrixTag("Node", "root", scope = "Graph_document"), "The root's card")
        onPantrixNode("Node", "root", scope = "Graph_document").assertIsDisplayed()
    }

    @Test
    fun aClosedCardHidesWhatIsInIt() = runComposeUiTest {
        val closed = previewState(
            "order.json",
            PreviewBoardOrder,
            Format.Json,
            DocumentView(pane = DocumentPane.Graph, expanded = setOf("$"))
        )
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                WorkspaceScreen(state = closed, onIntent = {}, commands = rememberDocumentCommands(Workbench()))
            }
        }

        onPantrixNode("Node", "9", scope = "Graph_document").assertExists()
        onPantrixNode("Node", "9_0", scope = "Graph_document").assertDoesNotExist()
        onPantrixNode("Node", "9", scope = "Graph_document").assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
    }

    @Test
    fun aPhoneDrawsTheCardsFromTheTopDownWithTheChosenValueUnderThem() = runComposeUiTest {
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = phone, clock = clock) {
                WorkspaceScreen(state = OrderGraph, onIntent = { intents += it }, commands = rememberDocumentCommands(Workbench()))
            }
        }

        onPantrixNode("Segment", "graph", scope = "SegmentButton_view").assertIsSelected()
        onPantrixNode("Node", "7", scope = "Graph_document").assertIsSelected()
        // The graph's own zoom stands in its corner, at 70%.
        onPantrixNode("IconButton", "fit", scope = "document").assertExists()
        awaitText("70%")
        onPantrixNode("Card", "selected_value").assertExists()
        onPantrixNode("Button", "show_in_text").performClick()
        assertEquals(WorkspaceIntent.ShowPane(DocumentPane.Text), intents.last())
    }

    @Test
    fun aPhonesInvalidDocumentHasItsProblemsInTheGraphsPlace() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = phone, clock = clock) {
                WorkspaceScreen(
                    state = previewState("settings.json", PreviewInvalid, Format.Json),
                    onIntent = {},
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }

        onPantrixNode("Segment", "problems", scope = "SegmentButton_view").assertExists()
        onPantrixNode("Segment", "graph", scope = "SegmentButton_view").assertDoesNotExist()
    }

    @Test
    fun plainTextHasNoGraph() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                GraphPane(
                    state = previewState("notes.txt", "Things to check.\n", null, DocumentView(pane = DocumentPane.Graph)),
                    layout = WorkspaceLayout.Wide,
                    onIntent = {},
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }

        awaitText("Plain text has no tree", substring = true)
        onPantrixNode("Graph", "document").assertDoesNotExist()
    }
}
