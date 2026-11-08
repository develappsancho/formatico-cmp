package com.developersancho.formatico.feature.workspace

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.testing.awaitNode
import com.developersancho.formatico.core.testing.awaitText
import com.developersancho.formatico.core.testing.awaitUntil
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.diagnostics.PantrixFindings
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.TestClock
import com.pantrixui.testing.assertNoFindings
import com.pantrixui.testing.hasPantrixTag
import com.pantrixui.testing.onPantrixNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class CompareScreenTest {
    private val desktop = PantrixWindow(DpSize(1440.dp, 900.dp))
    private val tablet = PantrixWindow(DpSize(834.dp, 1194.dp))
    private val phone = PantrixWindow(DpSize(390.dp, 844.dp))
    private val clock = TestClock(Instant.parse("2026-11-02T09:41:00Z"))

    @Test
    fun aWideWindowListsTheDifferencesBesideTheTwoTexts() = runComposeUiTest {
        val findings = PantrixFindings()
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = desktop, findings = findings, clock = clock) {
                WorkspaceScreen(state = OrdersCompared, onIntent = { intents += it }, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("4 differences")
        awaitText("The order of the keys: status and id changed places. And 129.90 and 129.9, which are the same number.")
        awaitText("A · order.json · 11 lines")
        awaitText("B · order-v2.json · 11 lines")
        awaitText("Difference 1 of 4")
        onPantrixNode(
            "ListItem",
            "difference0"
        ).assert(hasText("$.status")).assert(hasText("\"shipped\" → \"delivered\"")).assertIsSelected()
        onPantrixNode("ListItem", "difference2").assert(hasText("$.coupon"))
        onPantrixNode("Tag", "count_changed").assert(hasText("2 changed"))
        onPantrixNode("Tag", "count_added").assert(hasText("1 added"))
        onPantrixNode("Tag", "count_removed").assert(hasText("1 removed"))
        onPantrixNode("Button", "partner").assert(hasContentDescription("B: order-v2.json. Choose another document"))
        onPantrixNode("Segment", "meaning", scope = "SegmentButton_compare_by").assertIsSelected()
        // The comparison takes the toolbar's place.
        onPantrixNode("Button", "format").assertDoesNotExist()

        onPantrixNode("ListItem", "difference3").performClick()
        onPantrixNode("Segment", "text", scope = "SegmentButton_compare_by").performClick()
        onPantrixNode("IconButton", "stop_comparing").performClick()
        assertEquals(
            listOf(
                WorkspaceIntent.Compare(CompareChoice(current = 3)),
                WorkspaceIntent.Compare(CompareChoice(by = CompareBy.Text)),
                WorkspaceIntent.ShowPane(DocumentPane.Tree)
            ),
            intents
        )
        assertNoFindings(findings)
    }

    @Test
    fun bIsChosenAmongTheOtherOpenDocuments() = runComposeUiTest {
        val intents = mutableListOf<WorkspaceIntent>()
        val third = OpenDocument(id = 3, name = "third.json", text = "{}", format = Format.Json)
        val state = previewState(
            "order.json",
            PreviewCompareA,
            Format.Json,
            DocumentView(pane = DocumentPane.Compare),
            others = listOf(OpenDocument(id = 2, name = "order-v2.json", text = PreviewCompareB, format = Format.Json), third)
        )
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                WorkspaceScreen(state = state, onIntent = { intents += it }, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("4 differences")
        onPantrixNode("Button", "partner").performClick()
        awaitNode(hasText("third.json"), "The menu of documents")
        onPantrixNode("MenuItem", "document_3", scope = "Menu_partners").performClick()
        assertEquals(WorkspaceIntent.Compare(CompareChoice(with = 3)), intents.last())
    }

    @Test
    fun aTabletListsTheDifferencesOverTheTwoTexts() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = tablet, clock = clock) {
                WorkspaceScreen(state = OrdersCompared, onIntent = {}, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("A · order.json")
        awaitText("B · order-v2.json")
        onPantrixNode("ListItem", "difference1").assert(hasText("$.deliveredAt")).assert(hasText("\"2026-11-01T10:05:00Z\""))
        awaitText("Difference 1 of 4")
        // A tablet has no title over the list, nor what the documents were compared by in the footer.
        onPantrixNode("ListItem", "difference0").assertIsSelected()
    }

    @Test
    fun theTextsDifferAsTextInRunsOfLines() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                WorkspaceScreen(state = OrdersComparedAsText, onIntent = {}, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("4 differences")
        // In a browser a row's words are read as it first shows.
        awaitNode(hasPantrixTag("ListItem", "difference0") and hasText("Line 2"), "The first run's row")
        onPantrixNode("ListItem", "difference0").assert(hasText("\"status\": \"delivered\","))
        awaitText("Compared by text: every character counts")
        onPantrixNode("Alert", "not_counted").assertDoesNotExist()
    }

    @Test
    fun aPhoneShowsADifferenceACardAndBothTextsInASheet() = runComposeUiTest {
        val findings = PantrixFindings()
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = phone, findings = findings, clock = clock) {
                WorkspaceScreen(state = OrdersCompared, onIntent = { intents += it }, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("Compare")
        // A card that is not pressed keeps its words apart: they are found on the screen, the card by its tag.
        onPantrixNode("Card", "difference1").assertExists()
        awaitText("$.deliveredAt")
        // What A has of an added value, and B of a removed one.
        awaitUntil("Both sides say what is not there") { onAllNodesWithText("not there").fetchSemanticsNodes().size == 2 }
        awaitText("Not counted as differences: The order of the keys", substring = true)

        onPantrixNode("Button", "show_difference2").performClick()
        assertEquals(WorkspaceIntent.Compare(CompareChoice(current = 2)), intents.last())
        awaitNode(hasPantrixTag("Sheet", "shown"), "The sheet with both texts")
        onPantrixNode("IconButton", "back").performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(WorkspaceIntent.ShowPane(DocumentPane.Tree), intents.last())
    }

    @Test
    fun aDocumentAloneIsToldToOpenAnother() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                WorkspaceScreen(
                    state = previewState("order.json", PreviewCompareA, Format.Json, DocumentView(pane = DocumentPane.Compare)),
                    onIntent = {},
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }

        awaitText("Nothing to compare with")
        awaitText("Open another document in a new tab, and compare this one with it.")
    }

    @Test
    fun theToolbarOpensTheComparison() = runComposeUiTest {
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                WorkspaceScreen(state = OrderUnchosen, onIntent = { intents += it }, commands = rememberDocumentCommands(Workbench()))
            }
        }
        onPantrixNode("Button", "compare").performClick()
        assertEquals(WorkspaceIntent.ShowPane(DocumentPane.Compare), intents.last())
    }

    @Test
    fun aPhonesMenuOpensTheComparison() = runComposeUiTest {
        var comparing = false
        setContent {
            PantrixTestTheme(window = phone, clock = clock) {
                DocumentMenu(
                    items = DocumentMenuItems.FindSummarySortCompareClose,
                    size = ButtonSize.Large,
                    canSort = true,
                    onSortKeys = {},
                    onCompare = { comparing = true },
                    onSave = {},
                    onClose = {}
                )
            }
        }
        onPantrixNode("IconButton", "more").performClick()
        // The menu's row stands past the foot of what the test draws: it is pressed as a screen reader presses it.
        onPantrixNode("MenuItem", "compare", scope = "Menu_document").performSemanticsAction(SemanticsActions.OnClick)
        awaitUntil("The comparison is opened") { comparing }
    }
}
