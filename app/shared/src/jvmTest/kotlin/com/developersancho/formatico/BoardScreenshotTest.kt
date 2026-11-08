package com.developersancho.formatico

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.compose.ui.test.waitUntilDoesNotExist
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.storage.memoryStorageModule
import com.developersancho.formatico.core.testing.inEnglish
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.onPantrixNode
import io.github.takahirom.roborazzi.captureRoboImage
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * The app as the canvas's boards draw it, at their three widths, in the light theme and the dark: `recordRoborazziJvm`
 * writes each under `screenshots/`, named as its board is (`Workspace-1440.png`, and `Workspace-1440-dark.png` in the
 * dark theme), and `verifyRoborazziJvm` compares against them.
 */
@OptIn(ExperimentalTestApi::class)
class BoardScreenshotTest {
    // KoinApplication keeps its context once the app is gone: each board starts with nothing open.
    @AfterTest
    fun closeTheApp() {
        stopKoin()
    }

    @Test
    fun start() = boards { board, dark -> capture("Start", board, dark) }

    @Test
    fun workspace() = boards { board, dark -> capture("Workspace", board, dark, sample = "order", shows = "customer") }

    @Test
    fun yaml() = boards { board, dark -> capture("Yaml", board, dark, sample = "deployment", shows = "apiVersion") }

    @Test
    fun table() = boards { board, dark -> capture("Table", board, dark, sample = "countries", shows = "Afghanistan") }

    @Test
    fun convert() = boards { board, dark -> capture("Convert", board, dark, sample = "order", shows = "customer", convert = true) }

    @Test
    fun palette() = boards { board, dark -> capture("Palette", board, dark, sample = "order", shows = "customer", palette = true) }

    /** Every board, in the light theme and the dark. */
    private fun boards(capture: (Board, Boolean) -> Unit) = Board.entries.forEach { board ->
        listOf(false, true).forEach { dark -> capture(board, dark) }
    }

    /**
     * Draws the app on [board], as dark as the system is where [dark] says so, opens [sample] where one is named and
     * waits for what it [shows], converts it where [convert] says so or opens the command palette where [palette] does,
     * and writes the picture as `[name]-width.png`, or `[name]-width-dark.png`.
     */
    private fun capture(
        name: String,
        board: Board,
        dark: Boolean,
        sample: String? = null,
        shows: String? = null,
        convert: Boolean = false,
        palette: Boolean = false
    ) {
        inEnglish { draw(name, board, dark, sample, shows, convert, palette) }
        stopKoin()
    }

    @Suppress("LongParameterList") // What a board shows, as capture is given it.
    private fun draw(name: String, board: Board, dark: Boolean, sample: String?, shows: String?, convert: Boolean, palette: Boolean) =
        runDesktopComposeUiTest(width = board.width, height = board.height) {
            setContent {
                PantrixTestTheme(platform = board.platform, dark = dark, window = board.window) {
                    App(systemDark = dark, storage = memoryStorageModule)
                }
            }
            waitUntilExactlyOneExists(hasText("Paste anything"), timeoutMillis = 10_000)
            if (sample != null && shows != null) {
                if (board == Board.Phone) onPantrixNode("Link", "all_samples").performClick()
                onPantrixNode("ListItem", "sample_$sample").performScrollTo().performClick()
                waitUntilAtLeastOneExists(hasText(shows, substring = true), timeoutMillis = 10_000)
                // The text shows what it [shows] at once, and the tree and the status only once the document is read,
                // which on a busy machine is later.
                waitUntilDoesNotExist(hasText("Reading…"), timeoutMillis = 10_000)
            }
            if (convert) {
                // A phone converts from its bar's "more" menu, a wider window from its toolbar. The menu is opened as
                // a screen reader opens it: a pointer left where "more" was would hover over the copy that stands there.
                if (board == Board.Phone) {
                    onPantrixNode("IconButton", "more").performSemanticsAction(SemanticsActions.OnClick)
                    onPantrixNode("MenuItem", "convert", scope = "Menu_document").performSemanticsAction(SemanticsActions.OnClick)
                } else {
                    onPantrixNode("Button", "convert").performClick()
                }
                waitUntilAtLeastOneExists(hasText("Nothing is lost"), timeoutMillis = 10_000)
            }
            val file = "screenshots/$name-${board.width}${if (dark) "-dark" else ""}.png"
            if (palette) {
                // The palette is a window of its own, over the app: the last root drawn.
                onPantrixNode(if (board == Board.Desktop) "Button" else "IconButton", "launcher")
                    .performSemanticsAction(SemanticsActions.OnClick)
                waitUntilAtLeastOneExists(hasText("Copy the document"), timeoutMillis = 10_000)
                onAllNodes(isRoot()).onLast().captureRoboImage(file)
            } else {
                onRoot().captureRoboImage(file)
            }
        }
}
