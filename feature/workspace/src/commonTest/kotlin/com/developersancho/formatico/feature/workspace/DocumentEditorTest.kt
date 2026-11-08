package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.testing.awaitUntil
import com.pantrixui.components.data.codeeditor.CodeEditorState
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.testing.PantrixTestTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DocumentEditorTest {
    private val desktop = PantrixWindow(DpSize(1440.dp, 900.dp))
    private val phone = PantrixWindow(DpSize(390.dp, 844.dp))
    private val order = "{\n  \"id\": \"ord_8842\",\n  \"customer\": {\"name\": \"Elif\"}\n}\n"

    private fun stateOf(text: String, view: DocumentView = DocumentView()) =
        readState(OpenDocument(id = 1, name = "order.json", text = text, format = Format.Json, view = view))

    @Test
    fun whatIsTypedReachesTheDocumentOnceTheTypingPauses() = runComposeUiTest {
        val editor = CodeEditorState(order)
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = desktop) {
                WorkspaceScreen(stateOf(order), { intents += it }, rememberDocumentCommands(Workbench()), editor)
            }
        }

        editor.replace(0, 0, "// The order.\n")
        // The test's clock is the editor's: it is moved past the pause the typing waits for.
        mainClock.advanceTimeBy(1_000)

        awaitUntil("The text's write") { intents.any { it is WorkspaceIntent.Write } }
        assertEquals(WorkspaceIntent.Write("// The order.\n$order"), intents.last { it is WorkspaceIntent.Write })
    }

    @Test
    fun aChangeMadeElsewhereComesIntoTheEditorAsAStepToUndo() = runComposeUiTest {
        val editor = CodeEditorState(order)
        var state by mutableStateOf(stateOf(order))
        setContent {
            PantrixTestTheme(window = desktop) {
                WorkspaceScreen(state, {}, rememberDocumentCommands(Workbench()), editor)
            }
        }
        awaitIdle()

        val formatted = "{\"id\":\"ord_8842\",\"customer\":{\"name\":\"Elif\"}}\n"
        state = stateOf(formatted)

        awaitUntil("The editor's text") { editor.text == formatted }
        assertTrue(editor.canUndo)
        editor.undo()
        assertEquals(order, editor.text)
    }

    @Test
    fun aChangeMadeWhileTheTextIsNotShownIsInItOnceItIs() = runComposeUiTest {
        // A phone shows the text or the tree: Minify, run while the tree is shown, is in the text the reader comes back to.
        val editor = CodeEditorState(order)
        var state by mutableStateOf(stateOf(order, DocumentView(pane = DocumentPane.Text)))
        setContent {
            PantrixTestTheme(window = phone) {
                WorkspaceScreen(state, {}, rememberDocumentCommands(Workbench()), editor)
            }
        }
        awaitIdle()

        val minified = "{\"id\":\"ord_8842\",\"customer\":{\"name\":\"Elif\"}}\n"
        state = stateOf(order, DocumentView(pane = DocumentPane.Tree))
        awaitIdle()
        state = stateOf(minified, DocumentView(pane = DocumentPane.Tree))
        awaitIdle()
        state = stateOf(minified, DocumentView(pane = DocumentPane.Text))

        awaitUntil("The editor's text") { editor.text == minified }
    }

    @Test
    fun whatIsTypedIsWrittenWhenTheTextLeavesTheScreenBeforeTheTypingPauses() = runComposeUiTest {
        val editor = CodeEditorState(order)
        val intents = mutableListOf<WorkspaceIntent>()
        var state by mutableStateOf(stateOf(order, DocumentView(pane = DocumentPane.Text)))
        setContent {
            PantrixTestTheme(window = phone) {
                WorkspaceScreen(state, { intents += it }, rememberDocumentCommands(Workbench()), editor)
            }
        }
        awaitIdle()

        editor.replace(0, 0, "// The order.\n")
        state = stateOf(order, DocumentView(pane = DocumentPane.Tree))

        awaitUntil("The text's write") { intents.any { it is WorkspaceIntent.Write } }
        assertEquals(WorkspaceIntent.Write("// The order.\n$order"), intents.last { it is WorkspaceIntent.Write })
    }

    @Test
    fun aValueChosenInTheTreeIsSelectedInTheText() = runComposeUiTest {
        val editor = CodeEditorState(order)
        val state = stateOf(order, DocumentView(selected = "$.customer.name"))
        setContent {
            PantrixTestTheme(window = desktop) {
                WorkspaceScreen(state, {}, rememberDocumentCommands(Workbench()), editor)
            }
        }

        val name = order.indexOf("\"Elif\"")
        awaitUntil("The value's selection") { editor.selection == TextRange(name, name + "\"Elif\"".length) }
    }

    @Test
    fun theCaretChoosesTheValueItComesToRestIn() = runComposeUiTest {
        val editor = CodeEditorState(order)
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = desktop) {
                WorkspaceScreen(stateOf(order), { intents += it }, rememberDocumentCommands(Workbench()), editor)
            }
        }
        awaitIdle()

        editor.selection = TextRange(order.indexOf("Elif") + 2)
        mainClock.advanceTimeBy(1_000)

        awaitUntil("The tree's choice") { WorkspaceIntent.Select("$.customer.name") in intents }
    }
}
