package com.developersancho.formatico.feature.workspace

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.testing.SendsSystemBack
import com.developersancho.formatico.core.testing.TestSystemBack
import com.developersancho.formatico.core.testing.awaitNode
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.hasPantrixTag
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PhoneBackTest {
    private val phone = PantrixWindow(DpSize(390.dp, 844.dp))
    private val order = "{\"id\": \"ord_8842\", \"items\": [{\"price\": 39.95}, {\"price\": 50.00}]}"

    private fun orderShowing(pane: DocumentPane) =
        readState(OpenDocument(id = 1, name = "order.json", text = order, format = Format.Json, view = DocumentView(pane = pane)))

    private fun theSystemsBackFrom(pane: DocumentPane) = runComposeUiTest {
        val back = TestSystemBack()
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(platform = PlatformType.Android, window = phone) {
                SendsSystemBack(back)
                WorkspaceScreen(state = orderShowing(pane), onIntent = { intents += it }, commands = rememberDocumentCommands(Workbench()))
            }
        }
        awaitNode(hasPantrixTag("SegmentButton", "view"), "The views' choice")

        runOnIdle { back.send() }

        // The back goes to the view the document opens with, and the tab stays open.
        runOnIdle {
            assertEquals(listOf(WorkspaceIntent.ShowPane(DocumentPane.Tree)), intents.filterIsInstance<WorkspaceIntent.ShowPane>())
            assertEquals(emptyList(), intents.filterIsInstance<WorkspaceIntent.Close>())
        }
    }

    @Test
    fun aPhonesBackLeavesQueryForTheTree() = theSystemsBackFrom(DocumentPane.Query)

    @Test
    fun aPhonesBackLeavesTheSummaryForTheTree() = theSystemsBackFrom(DocumentPane.Summary)
}
