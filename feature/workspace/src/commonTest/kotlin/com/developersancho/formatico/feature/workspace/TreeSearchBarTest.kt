package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.testing.awaitNode
import com.developersancho.formatico.core.testing.awaitTexts
import com.developersancho.formatico.core.testing.checkPreviewCases
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.diagnostics.PantrixFindings
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.assertNoFindings
import com.pantrixui.testing.hasPantrixTag
import com.pantrixui.testing.onPantrixNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TreeSearchBarTest {
    private val desktop = PantrixWindow(DpSize(1440.dp, 900.dp))

    @Test
    fun aNarrowPaneCutsTheFieldAndTheCountNotTheCloseButton() = runComposeUiTest {
        val findings = PantrixFindings()
        var closed = 0
        setContent {
            PantrixTestTheme(window = desktop, findings = findings) {
                Box(modifier = Modifier.width(340.dp)) {
                    TreeSearchBar(
                        query = rememberTextFieldState("elif yilmaz"),
                        position = searchPosition("elif yilmaz", current = 0, count = 0),
                        layout = WorkspaceLayout.Wide,
                        onStep = {},
                        onClose = { closed++ }
                    )
                }
            }
        }

        awaitNode(hasPantrixTag("IconButton", "close_search"), "The close button")
        // A small icon button is 32 wide: the bar no longer squeezes it to what the field and the count leave.
        onPantrixNode("IconButton", "close_search").assertWidthIsAtLeast(32.dp).performClick()
        onPantrixNode("IconButton", "next_match").assertWidthIsAtLeast(32.dp)
        onPantrixNode("IconButton", "previous_match").assertWidthIsAtLeast(32.dp)
        assertEquals(1, closed)
        assertNoFindings(findings)
    }

    @Test
    fun eachCaseShowsItsSearchAndWhereItStands() = checkPreviewCases(
        TreeSearchBarCases(),
        boardsOf = { listOf(it.board) },
        content = { TreeSearchBarPreview(it) }
    ) { case, _ ->
        val search = case.value
        if (search.query.isNotEmpty()) awaitTexts(search.query)
        when {
            search.query.isEmpty() -> assertTrue(onAllNodes(hasText("No matches")).fetchSemanticsNodes().isEmpty())
            search.count == 0 -> awaitTexts("No matches")
            else -> awaitTexts("${search.current + 1}", "${search.count}", substring = true)
        }
        onPantrixNode("IconButton", "close_search").assertExists()
    }
}
