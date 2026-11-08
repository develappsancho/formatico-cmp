package com.developersancho.formatico

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.storage.memoryStorageModule
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.roborazziSystemPropertyProjectPath
import com.pantrixui.foundation.diagnostics.PantrixFindings
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.testing.PantrixTestTheme
import io.github.takahirom.roborazzi.captureRoboImage
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * The boards of a tablet and a phone as iOS draws them, in the simulator, in the light theme and the dark:
 * `recordRoborazziIosSimulatorArm64` writes each under `screenshots/ios/`, named as its board is
 * (`Workspace-390.png`, `Workspace-390-dark.png`), and `verifyRoborazziIosSimulatorArm64` compares against them. The
 * desktop's `BoardScreenshotTest` draws every width; this one draws what iOS draws its own way: its controls, its
 * touch targets, its text.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
class BoardScreenshotIosTest {
    // KoinApplication keeps its context once the app is gone: each board starts with nothing open.
    @AfterTest
    fun closeTheApp() {
        stopKoin()
    }

    @Test
    fun start() = boards(AppBoard.Start)

    @Test
    fun workspace() = boards(AppBoard.Workspace)

    @Test
    fun yaml() = boards(AppBoard.Yaml)

    @Test
    fun table() = boards(AppBoard.Table)

    @Test
    fun invalid() = boards(AppBoard.Invalid)

    @Test
    fun query() = boards(AppBoard.Query)

    @Test
    fun convert() = boards(AppBoard.Convert)

    @Test
    fun compare() = boards(AppBoard.Compare)

    @Test
    fun types() = boards(AppBoard.Types)

    @Test
    fun schema() = boards(AppBoard.Schema)

    @Test
    fun graph() = boards(AppBoard.Graph)

    @Test
    fun summary() = boards(AppBoard.Summary)

    @Test
    fun palette() = boards(AppBoard.Palette)

    @Test
    fun settings() = boards(AppBoard.Settings)

    @Test
    fun unknownFormat() = boards(AppBoard.Ask)

    @Test
    fun share() = boards(AppBoard.Share)

    @Test
    fun brokenLink() = boards(AppBoard.BrokenLink)

    /** [board] on a tablet and a phone, light and dark. */
    private fun boards(board: AppBoard) = listOf(Board.Tablet, Board.Phone).forEach { window ->
        listOf(false, true).forEach { dark ->
            try {
                draw(board, window, dark)
            } finally {
                stopKoin()
            }
        }
    }

    /**
     * Draws the app as iOS does, in a test of [window]'s size, brings it to [board] and writes what it shows, or the
     * window a board shows over the app, as `screenshots/ios/<board>-<width>[-dark].png` of the module.
     */
    private fun draw(board: AppBoard, window: Board, dark: Boolean) = runSkikoComposeUiTest(
        size = Size(window.width.toFloat(), window.height.toFloat())
    ) {
        val findings = PantrixFindings()
        val files = BoardFiles()
        setContent {
            PantrixTestTheme(
                platform = PlatformType.Ios,
                dark = dark,
                window = window.window,
                findings = findings,
                clipboard = board.clipboard(),
                filePicker = files
            ) {
                App(systemDark = dark, storage = memoryStorageModule)
            }
        }
        tour(board, window, BoardWords.English, files)
        val name = "${board.fileName}-${window.width}${if (dark) "-dark" else ""}"
        val file = "${roborazziSystemPropertyProjectPath()}/screenshots/ios/$name.png"
        val shown = if (board.overlay) onAllNodes(isRoot()).onLast() else onRoot()
        shown.captureRoboImage(this, file)
        assertNoFindingsOnBoard(findings)
    }
}
