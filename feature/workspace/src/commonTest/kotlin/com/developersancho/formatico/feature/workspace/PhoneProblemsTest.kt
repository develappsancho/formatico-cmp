package com.developersancho.formatico.feature.workspace

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.testing.awaitStatus
import com.developersancho.formatico.core.testing.awaitText
import com.developersancho.formatico.core.testing.awaitUntil
import com.pantrixui.components.data.codeeditor.CodeEditorState
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.onPantrixNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PhoneProblemsTest {
    private val phone = PantrixWindow(DpSize(390.dp, 844.dp))
    private val settings =
        "{\n  // a note\n  \"hosts\": [\n    \"localhost\"\n    \"127.0.0.1\"\n  ],\n  timeout: 30,\n  \"ai\": false,\n}\n"

    private fun stateOf(text: String = settings, pane: DocumentPane = DocumentPane.Text) =
        readState(OpenDocument(id = 1, name = "settings.json", text = text, format = Format.Json, view = DocumentView(pane = pane)))

    private fun List<WorkspaceIntent>.repairs() = filter { it is WorkspaceIntent.Repair || it == WorkspaceIntent.RepairAll }

    @Test
    fun theStripSaysTheTextIsNotValidAndRepairsItAll() = runComposeUiTest {
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = phone) {
                WorkspaceScreen(stateOf(), { intents += it }, rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("Not valid JSON: 4 problems")
        onPantrixNode("Button", "repair_all").performClick()
        onPantrixNode("Button", "repair").performClick()

        assertEquals(listOf(WorkspaceIntent.RepairAll, WorkspaceIntent.RepairAll), intents.repairs())
        // Where Repair leads the row, Minify leaves it, as the Invalid board draws it.
        onPantrixNode("Button", "format").assertExists()
        onPantrixNode("Button", "minify").assertDoesNotExist()
    }

    @Test
    fun anInvalidDocumentHasThreeViewsItsProblemsInTheTablesPlace() = runComposeUiTest {
        val state = stateOf()
        assertTrue(state.table != null, "The settings have a table: their hosts")
        setContent {
            PantrixTestTheme(window = phone) {
                WorkspaceScreen(state, {}, rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("Not valid JSON: 4 problems")
        // Text, Tree and Problems, as Invalid-390 draws them: the table opens once the document is repaired.
        onPantrixNode("Segment", "text", scope = "SegmentButton_view").assertExists()
        onPantrixNode("Segment", "tree", scope = "SegmentButton_view").assertExists()
        onPantrixNode("Segment", "problems", scope = "SegmentButton_view").assertExists()
        onPantrixNode("Segment", "table", scope = "SegmentButton_view").assertDoesNotExist()
        onPantrixNode("Segment", "graph", scope = "SegmentButton_view").assertDoesNotExist()
    }

    @Test
    fun theCardShowsTheProblemTheCaretIsAtAndStepsToTheOthers() = runComposeUiTest {
        val state = stateOf()
        val problems = state.problems
        val editor = CodeEditorState(settings, initialSelection = TextRange(settings.indexOf("\"127.0.0.1\"")))
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = phone) {
                WorkspaceScreen(state, { intents += it }, rememberDocumentCommands(Workbench()), editor)
            }
        }

        awaitText("Problem 2 of 4 · line 5, column 5")
        awaitText("Add a comma after the value on line 4.")

        onPantrixNode("IconButton", "next_problem").performClick()
        awaitText("Problem 3 of 4 · line 7, column 3")
        assertEquals(TextRange(problems[2].range.start, problems[2].range.end), editor.selection)

        onPantrixNode("IconButton", "previous_problem").performClick()
        onPantrixNode("IconButton", "previous_problem").performClick()
        awaitText("Problem 1 of 4 · line 2, column 3")
        onPantrixNode("IconButton", "previous_problem").assertIsNotEnabled()

        onPantrixNode("Button", "repair_problem").performClick()
        assertEquals(listOf(WorkspaceIntent.Repair(problems[0])), intents.repairs())
    }

    @Test
    fun aProblemPressedInThePhonesListIsShownInTheText() = runComposeUiTest {
        val state = stateOf(pane = DocumentPane.Problems)
        val editor = CodeEditorState(settings)
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = phone) {
                WorkspaceScreen(state, { intents += it }, rememberDocumentCommands(Workbench()), editor)
            }
        }

        awaitText("Put this key in double quotes: \"timeout\".")
        onNodeWithText("Put this key in double quotes: \"timeout\".").performClick()

        val timeout = state.problems[2].range
        awaitUntil("The text's view") { WorkspaceIntent.ShowPane(DocumentPane.Text) in intents }
        assertEquals(TextRange(timeout.start, timeout.end), editor.selection)
    }

    @Test
    fun aValidTextHasNoStripAndNoCard() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = phone) {
                WorkspaceScreen(stateOf("{\"a\": 1}\n"), {}, rememberDocumentCommands(Workbench()))
            }
        }
        awaitStatus("Valid JSON")

        onPantrixNode("Tag", "invalid").assertDoesNotExist()
        onPantrixNode("Card", "problem").assertDoesNotExist()
        onPantrixNode("Button", "minify").assertExists()
    }

    @Test
    fun theCurrentProblemIsTheOneTheCaretIsInOrTheNextOrTheLast() {
        val problems = stateOf().problems

        assertEquals(0, currentProblem(problems, 0))
        assertEquals(1, currentProblem(problems, problems[1].range.start + 1))
        assertEquals(2, currentProblem(problems, problems[1].range.end + 1))
        assertEquals(3, currentProblem(problems, settings.length))
        assertTrue(problems.zipWithNext().all { (a, b) -> a.range.start <= b.range.start })
    }
}
