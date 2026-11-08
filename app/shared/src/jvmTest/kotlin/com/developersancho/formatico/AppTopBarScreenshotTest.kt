package com.developersancho.formatico

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.developersancho.formatico.core.designsystem.FormaticoPreview
import com.developersancho.formatico.core.designsystem.PreviewCase
import com.developersancho.formatico.core.testing.captureOpenMenu
import com.developersancho.formatico.core.testing.capturePreview
import com.developersancho.formatico.feature.workspace.DocumentMoreMenu
import com.developersancho.formatico.feature.workspace.Workbench
import kotlin.test.Test

/**
 * The bar's previews, every case on its board: `recordRoborazziJvm` writes each under `screenshots/AppTopBar/`, named
 * by its case and its board's width, and `verifyRoborazziJvm` compares against them.
 */
class AppTopBarScreenshotTest {
    @Test
    fun appTopBar() = AppTopBarCases().values.forEach { case ->
        capturePreview("AppTopBar", case, case.value.board) { AppTopBarPreview(case) }
    }

    @Test
    fun aPhonesMenus() = AppTopBarCases().values.filter { it.name == "document on a phone" }.forEach { case ->
        val board = case.value.board
        captureOpenMenu("DocumentSwitcher", case, board, opener = "switch_documents") { BarInItsWindow(case) }
        captureOpenMenu("DocumentMoreMenu", case, board, opener = "more") { BarInItsWindow(case) }
    }
}

/**
 * The bar of [case] at the top of its board's whole window. A phone's menu opens as a sheet over the window it is in:
 * in the preview's frame, which is as tall as the bar, it would have no room.
 */
@Composable
private fun BarInItsWindow(case: PreviewCase<AppTopBarCase>) {
    // The menus read the documents from the workbench: the case's, so that a YAML document can be sorted.
    val workbench = remember { Workbench().apply { reopen(case.value.workbench.documents, case.value.workbench.selectedId) } }
    FormaticoPreview(case, Modifier.fillMaxSize()) {
        AppTopBar(
            insets = PaddingValues(),
            workbench = it.workbench,
            onSelectTab = {},
            darkTheme = case.dark,
            onToggleTheme = {},
            documentMenu = { id -> DocumentMoreMenu(documentId = id, workbench = workbench) }
        )
    }
}
