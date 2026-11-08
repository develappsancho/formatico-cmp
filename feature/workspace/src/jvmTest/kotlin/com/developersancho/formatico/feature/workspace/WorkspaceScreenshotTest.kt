package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.designsystem.PREVIEW_TAG
import com.developersancho.formatico.core.designsystem.PreviewCase
import com.pantrixui.testing.PantrixTestTheme
import io.github.takahirom.roborazzi.captureRoboImage
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * The workspace's previews, every case: the screen at every board, a component at its own. `recordRoborazziJvm`
 * writes each under `screenshots/`, named by what it shows, its case and its board's width
 * (`StatusBar/invalid-dark-1440.png`), and `verifyRoborazziJvm` compares against them.
 */
@OptIn(ExperimentalTestApi::class)
class WorkspaceScreenshotTest {
    private var locale: Locale = Locale.getDefault()

    // The screen's words are Compose resources, which read the JVM's language, not the theme's.
    @BeforeTest
    fun inEnglish() {
        locale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
    }

    @AfterTest
    fun inTheMachinesLanguage() {
        Locale.setDefault(locale)
    }

    @Test
    fun workspaceScreen() = WorkspaceScreenCases().values.forEach { case ->
        Board.entries.forEach { board -> capture("WorkspaceScreen", case, board) { WorkspaceScreenPreview(case) } }
    }

    @Test
    fun statusBar() = StatusBarCases().values.forEach { case ->
        capture("StatusBar", case, case.value.board) { StatusBarPreview(case) }
    }

    @Test
    fun pathTrail() = PathTrailCases().values.forEach { case ->
        capture("PathTrail", case, case.value.board) { PathTrailPreview(case) }
    }

    @Test
    fun tableRecord() = TableRecordCases().values.forEach { case ->
        capture("TableRecord", case, Board.Phone) { TableRecordPreview(case) }
    }

    @Test
    fun columnSummary() = ColumnSummaryCases().values.forEach { case ->
        capture("ColumnSummary", case, Board.Desktop) { ColumnSummaryPreview(case) }
    }

    @Test
    fun sortSummary() = SortSummaryCases().values.forEach { case ->
        capture("SortSummary", case, Board.Desktop) { SortSummaryPreview(case) }
    }

    @Test
    fun treePaneMessage() = TreePaneMessageCases().values.forEach { case ->
        capture("TreePaneMessage", case, Board.Desktop) { TreePaneMessagePreview(case) }
    }

    @Test
    fun editorComing() = EditorComingCases().values.forEach { case ->
        capture("EditorComing", case, Board.Desktop) { EditorComingPreview(case) }
    }

    @Test
    fun viewChoice() = ViewChoiceCases().values.forEach { case ->
        capture("ViewChoice", case, case.value.board) { ViewChoicePreview(case) }
    }

    /**
     * Draws [content] in [board]'s window and platform, light or dark as [case] is, and writes what the preview draws
     * under [name].
     */
    private fun capture(name: String, case: PreviewCase<*>, board: Board, content: @Composable () -> Unit) =
        runDesktopComposeUiTest(width = board.width, height = board.height) {
            setContent {
                PantrixTestTheme(platform = board.platform, dark = case.dark, window = board.window) { content() }
            }
            waitForIdle()
            onNodeWithTag(PREVIEW_TAG).captureRoboImage("screenshots/$name/${case.fileName}-${board.width}.png")
        }
}
