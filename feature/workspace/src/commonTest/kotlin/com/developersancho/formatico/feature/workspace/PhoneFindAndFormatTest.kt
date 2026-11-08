package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.testing.awaitText
import com.developersancho.formatico.core.testing.awaitUntil
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.diagnostics.PantrixFindings
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.TestClock
import com.pantrixui.testing.assertNoFindings
import com.pantrixui.testing.onPantrixNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * A phone's two ways in that a keyboard gives a wide window, as Workspace-390's `more` and `format` states draw them:
 * the editor's find bar from the "more" menu, and the format from the status beside the trail (the owner's decisions,
 * Faz 21).
 */
@OptIn(ExperimentalTestApi::class)
class PhoneFindAndFormatTest {
    private val phone = PantrixWindow(DpSize(390.dp, 844.dp))
    private val clock = TestClock(Instant.parse("2026-11-02T09:41:00Z"))
    private val order = "{\"customer\": {\"name\": \"Elif Yılmaz\", \"email\": \"elif@example.com\"}}"

    @Test
    fun theMoreMenusFindInTheTextOpensTheEditorsFindBarOverTheText() = runComposeUiTest {
        val findings = PantrixFindings()
        val workbench = Workbench()
        val id = workbench.open("customer.json", order, Format.Json)
        setContent {
            PantrixTestTheme(window = phone, findings = findings, clock = clock) {
                val open by workbench.state.collectAsState()
                Column {
                    DocumentMoreMenu(documentId = id, workbench = workbench)
                    WorkspaceScreen(
                        state = readState(open.documents.single()),
                        onIntent = { intent ->
                            if (intent is WorkspaceIntent.FindInText) {
                                workbench.changeView(id) { it.copy(findingText = intent.open) }
                            }
                        },
                        commands = rememberDocumentCommands(workbench)
                    )
                }
            }
        }
        awaitText("Valid JSON")

        // Find in the text is the menu's first item, above Find in the tree.
        onPantrixNode("IconButton", "more").performClick()
        onPantrixNode("MenuItem", "find_text", scope = "Menu_document").performClick()

        // The text is shown, its editor's find bar open, and the wish let go.
        awaitUntil("the text shown") { workbench.state.value.documents.single().view.pane == DocumentPane.Text }
        onPantrixNode("TextField", "find", scope = "CodeEditor_text").assertExists()
        awaitUntil("the wish let go") { !workbench.state.value.documents.single().view.findingText }
        assertNoFindings(findings)
    }

    @Test
    fun aPhonesStatusOpensTheFormatsAndReadsTheTextAsTheOneChosen() = runComposeUiTest {
        val findings = PantrixFindings()
        val workbench = Workbench()
        // A new document typed into: plain text until it is read as a format.
        val id = workbench.open(null, "{\n  \"port\": 8080\n}", null)
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = phone, findings = findings, clock = clock) {
                val open by workbench.state.collectAsState()
                WorkspaceScreen(
                    state = readState(open.documents.single { it.id == id }),
                    onIntent = { intents += it },
                    commands = rememberDocumentCommands(workbench)
                )
            }
        }
        awaitText("Plain text")

        onPantrixNode("Button", "status_format")
            .assert(hasContentDescription("Plain text"))
            .performClick()
        onPantrixNode("MenuItem", "json", scope = "Menu_status_formats").performClick()

        assertEquals(listOf<WorkspaceIntent>(WorkspaceIntent.ChangeFormat(Format.Json)), intents)
        assertNoFindings(findings)
    }
}
