package com.developersancho.formatico

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.testing.awaitNode
import com.developersancho.formatico.feature.workspace.DocumentMoreMenu
import com.developersancho.formatico.feature.workspace.OpenDocument
import com.developersancho.formatico.feature.workspace.Workbench
import com.developersancho.formatico.feature.workspace.WorkbenchState
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.onPantrixNode
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AppTopBarTest {
    private val documents = Workbench()
    private val open = listOf(
        OpenDocument(id = 1, name = "order.json", text = "{}", format = Format.Json),
        OpenDocument(id = 2, name = "deployment.yaml", text = "a: 1", format = Format.Yaml, edited = true),
        OpenDocument(id = 3, name = null, text = "", format = null)
    )

    /**
     * Draws the bar over [workbench] on [board], over Settings where [settingsShown] says so; a tab chosen goes to
     * [onSelectTab], a document's tab closed to [onCloseTab] and Settings' to [onCloseSettings].
     */
    private fun ComposeUiTest.bar(
        workbench: WorkbenchState,
        board: Board,
        onSelectTab: (Long?) -> Unit = {},
        settingsShown: Boolean = false,
        onCloseTab: (Long) -> Unit = {},
        onCloseSettings: () -> Unit = {}
    ) {
        setContent {
            PantrixTestTheme(platform = board.platform, window = board.window) {
                AppTopBar(
                    insets = PaddingValues(),
                    workbench = workbench,
                    settingsShown = settingsShown,
                    onSelectTab = onSelectTab,
                    onCloseTab = onCloseTab,
                    onCloseSettings = onCloseSettings,
                    darkTheme = false,
                    onToggleTheme = {},
                    onOpenPalette = {},
                    onOpenSettings = {},
                    documentMenu = { DocumentMoreMenu(documentId = it, workbench = documents) }
                )
            }
        }
    }

    @Test
    fun withNothingOpenTheBarHasANewTabAlone() = runComposeUiTest {
        bar(WorkbenchState(), Board.Desktop)

        awaitNode(hasText("New tab"), "The new tab")
        onPantrixNode("Tab", "new", scope = "Tabs_documents").assertExists()
        onPantrixNode("Tab", "document_1", scope = "Tabs_documents").assertDoesNotExist()
        // A new tab is shown already: there is nothing for the button to open.
        onPantrixNode("IconButton", "new_tab").assertIsNotEnabled()
    }

    @Test
    fun aTabChoosesItsDocumentAndTheButtonANewTab() = runComposeUiTest {
        val chosen = mutableListOf<Long?>()
        bar(WorkbenchState(open, selectedId = 1), Board.Desktop, onSelectTab = { chosen += it })
        awaitNode(hasText("deployment.yaml"), "The second tab")

        // A pasted text has no name, and a new tab is not drawn while a document is shown.
        awaitNode(hasText("Untitled"), "The pasted text's tab")
        onPantrixNode("Tab", "new", scope = "Tabs_documents").assertDoesNotExist()

        onPantrixNode("Tab", "document_2", scope = "Tabs_documents").performClick()
        onPantrixNode("IconButton", "new_tab").assertIsEnabled().performClick()

        assertEquals(listOf<Long?>(2, null), chosen)
    }

    @Test
    fun aDocumentsTabClosesTheDocumentByTheScreenReadersActionAndByDelete() = runComposeUiTest {
        val closed = mutableListOf<Long>()
        bar(WorkbenchState(open, selectedId = 1), Board.Desktop, onCloseTab = { closed += it })
        awaitNode(hasText("deployment.yaml"), "The second tab")

        onPantrixNode("Tab", "document_2", scope = "Tabs_documents").performCustomAccessibilityActionWithLabel("Close")
        onPantrixNode("Tab", "document_1", scope = "Tabs_documents").requestFocus().performKeyInput { pressKey(Key.Delete) }

        assertEquals(listOf<Long>(2, 1), closed)
    }

    @Test
    fun settingsTabLeavesSettings() = runComposeUiTest {
        var left = 0
        bar(WorkbenchState(open, selectedId = 1), Board.Desktop, settingsShown = true, onCloseSettings = { left++ })
        awaitNode(hasText("Settings"), "Settings' tab")

        onPantrixNode("Tab", "settings", scope = "Tabs_documents").performCustomAccessibilityActionWithLabel("Close")

        assertEquals(1, left)
    }

    @Test
    fun aNewTabGoesBackToTheLastDocument() = runComposeUiTest {
        val chosen = mutableListOf<Long?>()
        bar(WorkbenchState(open, selectedId = null), Board.Desktop, onSelectTab = { chosen += it })
        awaitNode(hasText("New tab"), "The new tab")

        onPantrixNode("Tab", "new", scope = "Tabs_documents").performCustomAccessibilityActionWithLabel("Close")

        assertEquals(listOf<Long?>(3), chosen)
    }

    @Test
    fun aNewTabAloneCannotBeClosed() = runComposeUiTest {
        bar(WorkbenchState(), Board.Desktop)
        awaitNode(hasText("New tab"), "The new tab")

        // With nothing open there is nothing to go back to.
        onPantrixNode("Tab", "new", scope = "Tabs_documents").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
    }

    @Test
    fun aDocumentWithChangesNotSavedSaysSoInItsTab() = runComposeUiTest {
        bar(WorkbenchState(open, selectedId = 1), Board.Desktop)

        awaitNode(hasContentDescription("changes not saved", substring = true), "The tab's mark")
    }

    @Test
    fun aPhoneSaysWhichDocumentIsShownAndHowManyAreOpen() = runComposeUiTest {
        val chosen = mutableListOf<Long?>()
        bar(WorkbenchState(open, selectedId = 2), Board.Phone, onSelectTab = { chosen += it })
        val switcher = "deployment.yaml, one of 3 open documents. Switch documents"
        awaitNode(hasContentDescription(switcher), switcher)

        onPantrixNode("Button", "switch_documents").performClick()
        // The test's window is not the phone's it is told of, and the sheet the menu becomes is laid out in the
        // first: the item is pressed by its action, not at a place.
        onPantrixNode("MenuItem", "document_1", scope = "Menu_documents").performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(listOf<Long?>(1), chosen)
    }
}
