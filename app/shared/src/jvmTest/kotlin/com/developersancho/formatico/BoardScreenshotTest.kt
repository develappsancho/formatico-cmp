package com.developersancho.formatico

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.storage.memoryStorageModule
import com.pantrixui.foundation.diagnostics.PantrixFindings
import com.pantrixui.foundation.l10n.PantrixLocale
import com.pantrixui.testing.PantrixTestTheme
import io.github.takahirom.roborazzi.captureRoboImage
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * The app as the canvas's boards draw it, at their three widths: in English in the light theme and the dark, and in
 * Turkish in the light. `recordRoborazziJvm` writes each under `screenshots/`, named as its board is
 * (`Workspace-1440.png`, `Workspace-1440-dark.png` and `Workspace-1440-tr.png`), and `verifyRoborazziJvm` compares
 * against them. Each board is drawn with the theme's diagnostics on, and has to find nothing: no control without a
 * name, no words too faint, no touch target squeezed, no tag on two nodes, in either language or theme.
 */
@OptIn(ExperimentalTestApi::class)
class BoardScreenshotTest {
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

    /** A way a board is drawn: in Turkish or not, its theme, and what its screenshot's name ends with. */
    private enum class Look(val turkish: Boolean, val dark: Boolean, val suffix: String) {
        Light(turkish = false, dark = false, suffix = ""),
        Dark(turkish = false, dark = true, suffix = "-dark"),
        Turkish(turkish = true, dark = false, suffix = "-tr")
    }

    /** [board] at every width, in every look. */
    private fun boards(board: AppBoard) = Board.entries.forEach { window ->
        Look.entries.forEach { look ->
            try {
                draw(board, window, look)
            } catch (failure: Throwable) {
                throw AssertionError("$board at ${window.width}, ${look.name.lowercase()}: ${failure.message}", failure)
            } finally {
                stopKoin()
            }
        }
    }

    /**
     * Draws the app on [window] in [look], brings it to [board], writes the picture as
     * `screenshots/<board>-<width><suffix>.png`, and asserts the diagnostics found nothing on the way.
     */
    private fun draw(board: AppBoard, window: Board, look: Look) =
        runDesktopComposeUiTest(width = window.width, height = window.height) {
            val findings = PantrixFindings()
            val files = BoardFiles()
            val turkish = look.turkish
            setContent {
                PantrixTestTheme(
                    platform = window.platform,
                    dark = look.dark,
                    locale = if (turkish) PantrixLocale.Turkish else PantrixLocale.English,
                    window = window.window,
                    findings = findings,
                    clipboard = board.clipboard(),
                    filePicker = files
                ) {
                    App(systemDark = look.dark, storage = memoryStorageModule)
                }
            }
            try {
                tour(board, window, if (turkish) BoardWords.Turkish else BoardWords.English, files)
            } catch (failure: Throwable) {
                System.err.println(onAllNodes(isRoot(), useUnmergedTree = true).printToString(maxDepth = 60))
                onRoot().captureRoboImage("build/board-failures/${board.fileName}-${window.width}${look.suffix}.png")
                throw failure
            }
            val file = "screenshots/${board.fileName}-${window.width}${look.suffix}.png"
            if (board.overlay) onAllNodes(isRoot()).onLast().captureRoboImage(file) else onRoot().captureRoboImage(file)
            assertNoFindingsOnBoard(findings)
        }
}
