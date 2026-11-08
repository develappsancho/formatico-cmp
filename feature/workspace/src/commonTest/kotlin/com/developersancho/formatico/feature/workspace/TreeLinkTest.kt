package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.testing.awaitNode
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

/** What the device opens an address with: here, a list of what it was asked to open, or nothing at all. */
private class TestLinks(private val opens: Boolean = true) : UriHandler {
    val opened = mutableListOf<String>()

    override fun openUri(uri: String) {
        require(opens) { "Nothing opens $uri" }
        opened += uri
    }
}

@OptIn(ExperimentalTestApi::class)
class TreeLinkTest {
    private val desktop = PantrixWindow(DpSize(1440.dp, 900.dp))
    private val phone = PantrixWindow(DpSize(390.dp, 844.dp))
    private val clock = TestClock(Instant.parse("2026-11-02T09:41:00Z"))
    private val profile =
        "{\"accent\": \"#0F766E\", \"avatar\": \"https://images.example.com/u/elif.png\", \"website\": \"https://elif.dev\"}"

    private val linksRow = hasPantrixTag("Node", "website", scope = "LazyTreeView_document")

    private fun profileState(selected: String? = null) =
        readState(
            OpenDocument(id = 1, name = "profile.json", text = profile, format = Format.Json, view = DocumentView(selected = selected))
        )

    @Test
    fun aLinksRowEndsInTheButtonThatOpensItWithoutChoosingTheRow() = runComposeUiTest {
        val findings = PantrixFindings()
        val links = TestLinks()
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = desktop, findings = findings, clock = clock) {
                CompositionLocalProvider(LocalUriHandler provides links) {
                    WorkspaceScreen(state = profileState(), onIntent = { intents += it }, commands = rememberDocumentCommands(Workbench()))
                }
            }
        }

        awaitNode(linksRow, "The link's row", forgetting = findings)
        // The button is not read apart from the row: it stands in the tree of every node.
        val button = onPantrixNode("Action", "website", scope = "LazyTreeView_document", useUnmergedTree = true)
        button.assertExists()
        // A colour and a picture only show what they hold: their rows have no button.
        onPantrixNode("Action", "accent", scope = "LazyTreeView_document", useUnmergedTree = true).assertDoesNotExist()
        onPantrixNode("Action", "avatar", scope = "LazyTreeView_document", useUnmergedTree = true).assertDoesNotExist()

        button.performClick()
        assertEquals(listOf("https://elif.dev"), links.opened)
        assertTrue(intents.none { it is WorkspaceIntent.Select }, "The press chose a row: $intents")
        assertNoFindings(findings)
    }

    @Test
    fun aScreenReaderIsOfferedTheLinkAmongTheRowsActions() = runComposeUiTest {
        val findings = PantrixFindings()
        val links = TestLinks()
        setContent {
            PantrixTestTheme(window = phone, findings = findings, clock = clock) {
                CompositionLocalProvider(LocalUriHandler provides links) {
                    WorkspaceScreen(state = profileState(), onIntent = {}, commands = rememberDocumentCommands(Workbench()))
                }
            }
        }

        awaitNode(linksRow, "The link's row", forgetting = findings)
        onPantrixNode("Node", "website", scope = "LazyTreeView_document").performCustomAccessibilityActionWithLabel("Open the link")
        assertEquals(listOf("https://elif.dev"), links.opened)
        assertNoFindings(findings)
    }

    @Test
    fun shiftEnterOnTheLinksRowOpensIt() = runComposeUiTest {
        val links = TestLinks()
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                CompositionLocalProvider(LocalUriHandler provides links) {
                    WorkspaceScreen(
                        state = profileState(selected = "$.website"),
                        onIntent = { intents += it },
                        commands = rememberDocumentCommands(Workbench())
                    )
                }
            }
        }

        awaitNode(linksRow, "The link's row")
        // The tree is one stop for Tab, the chosen row: the focus comes to the link's row there.
        val row = onPantrixNode("Node", "website", scope = "LazyTreeView_document")
        row.requestFocus()
        row.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Enter) } }
        assertEquals(listOf("https://elif.dev"), links.opened)
        assertTrue(intents.none { it is WorkspaceIntent.Select }, "Shift+Enter chose a row: $intents")
    }

    @Test
    fun anAddressNothingOpensIsLetGo() = runComposeUiTest {
        val links = TestLinks(opens = false)
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                CompositionLocalProvider(LocalUriHandler provides links) {
                    WorkspaceScreen(state = profileState(), onIntent = {}, commands = rememberDocumentCommands(Workbench()))
                }
            }
        }

        awaitNode(linksRow, "The link's row")
        onPantrixNode("Action", "website", scope = "LazyTreeView_document", useUnmergedTree = true).performClick()
        // The press did nothing, and the screen is still there.
        onPantrixNode("Node", "website", scope = "LazyTreeView_document").assertExists()
        assertTrue(links.opened.isEmpty())
    }
}
