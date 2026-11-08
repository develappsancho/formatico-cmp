package com.developersancho.formatico

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.feature.workspace.OpenDocument
import com.developersancho.formatico.feature.workspace.Workbench
import com.developersancho.formatico.feature.workspace.WorkbenchState
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.onPantrixNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.compose.KoinApplication
import org.koin.core.context.stopKoin
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@OptIn(ExperimentalTestApi::class)
class AppTopBarTest {
    private val open = listOf(
        OpenDocument(id = 1, name = "order.json", text = "{}", format = Format.Json),
        OpenDocument(id = 2, name = "deployment.yaml", text = "a: 1", format = Format.Yaml, edited = true),
        OpenDocument(id = 3, name = null, text = "", format = null)
    )

    // A phone's bar holds the document's "more" menu, which finds the documents in Koin: each test starts its own.
    @AfterTest
    fun stopTheirKoin() {
        stopKoin()
    }

    /** Draws the bar over [workbench] on [board]; a tab chosen goes to [onSelectTab]. */
    private fun ComposeUiTest.bar(workbench: WorkbenchState, board: Board, onSelectTab: (Long?) -> Unit = {}) {
        setContent {
            KoinApplication(configuration = koinConfiguration { modules(module { single { Workbench() } }) }) {
                PantrixTestTheme(platform = board.platform, window = board.window) {
                    AppTopBar(
                        insets = PaddingValues(),
                        workbench = workbench,
                        onSelectTab = onSelectTab,
                        darkTheme = false,
                        onToggleTheme = {}
                    )
                }
            }
        }
    }

    /** Waits for one node that [matcher] finds, letting the platform run meanwhile: a browser fetches the words. */
    private suspend fun ComposeUiTest.awaitNode(matcher: SemanticsMatcher, what: String) {
        val waited = TimeSource.Monotonic.markNow()
        while (onAllNodes(matcher).fetchSemanticsNodes().size != 1) {
            check(waited.elapsedNow() < 10.seconds) { "$what was not shown in 10 seconds" }
            withContext(Dispatchers.Default) { delay(20.milliseconds) }
            awaitIdle()
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
