package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.testing.awaitNode
import com.developersancho.formatico.core.testing.awaitUntil
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.l10n.PantrixLocale
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.hasPantrixTag
import com.pantrixui.testing.onPantrixNode
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class WorkspaceToolbarTest {
    private val tablet = PantrixWindow(DpSize(834.dp, 1194.dp))

    /**
     * The toolbar of the state [state] gives, in Turkish, at a tablet's own width whatever the test's window is: where
     * room runs short.
     */
    private fun ComposeUiTest.turkishTabletToolbar(state: () -> WorkspaceState) = setContent {
        PantrixTestTheme(window = tablet, locale = PantrixLocale.Turkish) {
            Box(Modifier.wrapContentSize(Alignment.TopStart, unbounded = true).size(834.dp, 56.dp)) {
                WorkspaceToolbar(
                    state = state(),
                    document = requireNotNull(state().document),
                    layout = WorkspaceLayout.Tablet,
                    onIntent = {},
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }
    }

    @Test
    fun theLowestButtonGivesWayFirstAndTheViewShownKeepsItsButton() = runComposeUiTest {
        var state by mutableStateOf(previewState("order.json", PreviewOrder, Format.Json))
        turkishTabletToolbar { state }

        // Schema, the lowest, goes into the "…" menu first: it has no icon to be drawn as. In a browser the words are
        // measured again once the fonts come, after the first frame: the bar folds then.
        awaitNode(hasPantrixTag("Action", "types", "Toolbar_document"), "Types in the bar")
        awaitUntil("Schema went into the menu") {
            onAllNodes(hasPantrixTag("Action", "schema", "Toolbar_document")).fetchSemanticsNodes().isEmpty()
        }
        onPantrixNode("Action", "types", scope = "Toolbar_document").assertExists()

        // Shown, it keeps its button, which says which view is open.
        state = previewState("order.json", PreviewOrder, Format.Json, DocumentView(pane = DocumentPane.Schema))
        awaitNode(hasPantrixTag("Action", "schema", "Toolbar_document"), "Schema in the bar")
    }
}
