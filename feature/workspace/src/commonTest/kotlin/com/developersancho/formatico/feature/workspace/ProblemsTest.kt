package com.developersancho.formatico.feature.workspace

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.testing.awaitText
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.onPantrixNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class ProblemsTest {
    private val desktop = PantrixWindow(DpSize(1440.dp, 900.dp))
    private val phone = PantrixWindow(DpSize(390.dp, 844.dp))
    private val settings =
        "{\n  // a note\n  \"hosts\": [\n    \"localhost\"\n    \"127.0.0.1\"\n  ],\n  timeout: 30,\n  \"ai\": false,\n}\n"

    private fun stateOf(view: DocumentView = DocumentView(pane = DocumentPane.Problems)) =
        readState(OpenDocument(id = 1, name = "settings.json", text = settings, format = Format.Json, view = view))

    @Test
    fun theProblemsSayWhatToDoAndRepairOneAtATimeOrAll() = runComposeUiTest {
        val intents = mutableListOf<WorkspaceIntent>()
        val state = stateOf()
        setContent {
            PantrixTestTheme(window = desktop) {
                WorkspaceScreen(state, { intents += it }, rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("This is not valid JSON")
        awaitText("Take out the comment: JSON has none. Or read the document as JSONC.")
        awaitText("Add a comma after the value on line 4.")
        awaitText("Put this key in double quotes: \"timeout\".")
        awaitText("JSON5 allows 3 of the 4.")

        onPantrixNode("Button", "repair_1").performClick()
        onPantrixNode("Button", "repair_all").performClick()
        onPantrixNode("Button", "read_as_json5").performClick()
        onPantrixNode("Button", "repair").performClick()

        assertEquals(
            listOf(
                WorkspaceIntent.Repair(state.problems[1]),
                WorkspaceIntent.RepairAll,
                WorkspaceIntent.ChangeFormat(Format.Json5),
                WorkspaceIntent.RepairAll
            ),
            intents
        )
    }

    @Test
    fun aProblemsRowIsTaggedApartFromItsRepairAndGoesToTheProblem() = runComposeUiTest {
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = phone) {
                WorkspaceScreen(stateOf(), { intents += it }, rememberDocumentCommands(Workbench()))
            }
        }
        awaitText("Add a comma after the value on line 4.")

        // The row goes to its problem, which a phone shows in the text; its button repairs it and is tagged apart.
        onPantrixNode("ProblemRow", "problem_1").performClick()
        assertEquals(listOf<WorkspaceIntent>(WorkspaceIntent.ShowPane(DocumentPane.Text)), intents)
        onPantrixNode("Button", "repair_1").assertExists()
        onPantrixNode("Button", "problem_1").assertDoesNotExist()
    }

    @Test
    fun aValidDocumentHasNoProblemsToShowOrRepair() = runComposeUiTest {
        val state = readState(OpenDocument(id = 1, name = "a.json", text = "{\"a\": 1}\n", format = Format.Json))
        setContent {
            PantrixTestTheme(window = desktop) {
                WorkspaceScreen(state, {}, rememberDocumentCommands(Workbench()))
            }
        }
        awaitText("Valid JSON")

        onPantrixNode("Segment", "problems", scope = "SegmentButton_view").assertDoesNotExist()
        onPantrixNode("Button", "repair").assertDoesNotExist()
    }

    @Test
    fun theViewModelRepairsAProblemAndThenTheRest() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val worker = UnconfinedTestDispatcher(testScheduler)
            val workbench = Workbench(worker = worker)
            val id = workbench.open("settings.json", settings, Format.Json)
            val viewModel = WorkspaceViewModel(documentId = id, workbench = workbench, reader = worker)
            val comma = viewModel.state.value.problems.first { it.code == ProblemCode.ExpectedCommaOrEnd }

            viewModel.dispatch(WorkspaceIntent.Repair(comma))
            assertTrue("\"localhost\",\n" in workbench.state.value.documents.single().text)

            viewModel.dispatch(WorkspaceIntent.RepairAll)
            assertEquals(emptyList(), viewModel.state.value.problems)
            assertTrue(workbench.state.value.documents.single().edited)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
