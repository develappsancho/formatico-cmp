package com.developersancho.formatico

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.storage.memoryStorageModule
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.pantrixui.foundation.diagnostics.PantrixFindings
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.testing.PantrixTestTheme
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * The boards of a tablet and a phone as Android draws them, in Robolectric, in the light theme and the dark:
 * `recordRoborazziAndroidHostTest` writes each under `screenshots/android/`, named as its board is
 * (`Workspace-390.png`, `Workspace-390-dark.png`), and `verifyRoborazziAndroidHostTest` compares against them. Android
 * draws with its own renderer, not Skiko as the desktop and iOS do: its text and its shadows are its own.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
@OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
class BoardScreenshotAndroidTest {
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

    /** [board] on a tablet and a phone, light and dark: Robolectric's screen is the board's, at one pixel to the dp. */
    private fun boards(board: AppBoard) = listOf(Board.Tablet, Board.Phone).forEach { window ->
        listOf(false, true).forEach { dark ->
            RuntimeEnvironment.setQualifiers("w${window.width}dp-h${window.height}dp-mdpi")
            try {
                draw(board, window, dark)
            } finally {
                stopKoin()
            }
        }
    }

    /**
     * Draws the app as Android does, brings it to [board] and writes the screen, with a window a board shows over the
     * app, as `screenshots/android/<board>-<width>[-dark].png` of the module.
     */
    private fun draw(board: AppBoard, window: Board, dark: Boolean) = runComposeUiTest {
        val findings = PantrixFindings()
        val files = BoardFiles()
        setContent {
            PantrixTestTheme(
                platform = PlatformType.Android,
                dark = dark,
                window = window.window,
                findings = findings,
                clipboard = board.clipboard(),
                filePicker = files
            ) {
                // Robolectric draws a thumbnail read after the screen's last frame only now and then: Android's boards
                // are drawn with no pictures, their places as the boards' `nopicture` state draws them.
                App(systemDark = dark, storage = memoryStorageModule, network = noPicturesNetworkModule)
            }
        }
        tour(board, window, BoardWords.English, files)
        // What the tour waited for is in the semantics once it is laid out; the screen is written once it is drawn too.
        waitForIdle()
        captureScreenRoboImage("screenshots/android/${board.fileName}-${window.width}${if (dark) "-dark" else ""}.png")
        assertNoFindingsOnBoard(findings)
    }
}
