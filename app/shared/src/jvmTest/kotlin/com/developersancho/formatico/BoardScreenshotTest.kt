package com.developersancho.formatico

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
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
import com.pantrixui.testing.TestClipboard
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
    fun convert() = boards { board, dark -> capture("Convert", board, dark, sample = "order", shows = "customer", then = Then.Convert) }

    @Test
    fun palette() = boards { board, dark -> capture("Palette", board, dark, sample = "order", shows = "customer", then = Then.Palette) }

    @Test
    fun settings() = boards { board, dark -> capture("Settings", board, dark, sample = "order", shows = "customer", then = Then.Settings) }

    @Test
    fun unknownFormat() = boards { board, dark -> capture("Ask", board, dark, then = Then.Ask) }

    @Test
    fun share() = boards { board, dark -> capture("Share", board, dark, sample = "order", shows = "customer", then = Then.Share) }

    @Test
    fun brokenLink() = boards { board, dark -> capture("BrokenLink", board, dark, then = Then.BrokenLink) }

    /** What a board shows once its sample is open: nothing more, or what is opened over it. */
    private enum class Then { Nothing, Convert, Palette, Settings, Ask, Share, BrokenLink }

    /** Every board, in the light theme and the dark. */
    private fun boards(capture: (Board, Boolean) -> Unit) = Board.entries.forEach { board ->
        listOf(false, true).forEach { dark -> capture(board, dark) }
    }

    /**
     * Draws the app on [board], as dark as the system is where [dark] says so, opens [sample] where one is named and
     * waits for what it [shows], does what [then] says, and writes the picture as `[name]-width.png`, or
     * `[name]-width-dark.png`.
     */
    private fun capture(
        name: String,
        board: Board,
        dark: Boolean,
        sample: String? = null,
        shows: String? = null,
        then: Then = Then.Nothing
    ) {
        inEnglish { draw("screenshots/$name-${board.width}${if (dark) "-dark" else ""}.png", board, dark, sample, shows, then) }
        stopKoin()
    }

    @Suppress("LongParameterList") // What a board shows, as capture is given it.
    private fun draw(file: String, board: Board, dark: Boolean, sample: String?, shows: String?, then: Then) =
        runDesktopComposeUiTest(width = board.width, height = board.height) {
            // The board's text of no format told is what the clipboard holds, or a link cut short.
            val clipboard = TestClipboard().apply { putText(if (then == Then.BrokenLink) BROKEN_LINK else UNTOLD) }
            setContent {
                PantrixTestTheme(platform = board.platform, dark = dark, window = board.window, clipboard = clipboard) {
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
            // A control is pressed as a screen reader presses it: a pointer left where it was would hover over what
            // stands there once the screen has changed.
            when (then) {
                Then.Nothing -> onRoot().captureRoboImage(file)

                Then.Convert -> {
                    // A phone converts from its bar's "more" menu, a wider window from its toolbar.
                    if (board == Board.Phone) {
                        onPantrixNode("IconButton", "more").performSemanticsAction(SemanticsActions.OnClick)
                        onPantrixNode("MenuItem", "convert", scope = "Menu_document").performSemanticsAction(SemanticsActions.OnClick)
                    } else {
                        onPantrixNode("Button", "convert").performClick()
                    }
                    waitUntilAtLeastOneExists(hasText("Nothing is lost"), timeoutMillis = 10_000)
                    onRoot().captureRoboImage(file)
                }

                Then.Palette -> {
                    // The palette is a window of its own, over the app: the last root drawn.
                    onPantrixNode(if (board == Board.Desktop) "Button" else "IconButton", "launcher")
                        .performSemanticsAction(SemanticsActions.OnClick)
                    waitUntilAtLeastOneExists(hasText("Copy the document"), timeoutMillis = 10_000)
                    onAllNodes(isRoot()).onLast().captureRoboImage(file)
                }

                Then.Settings -> {
                    // A phone's bar over a document has no settings: its Start has.
                    if (board == Board.Phone) {
                        onPantrixNode("Button", "switch_documents").performSemanticsAction(SemanticsActions.OnClick)
                        onPantrixNode("MenuItem", "new", scope = "Menu_documents").performSemanticsAction(SemanticsActions.OnClick)
                        waitUntilExactlyOneExists(hasText("Paste anything"), timeoutMillis = 10_000)
                    }
                    onPantrixNode("IconButton", "settings").performSemanticsAction(SemanticsActions.OnClick)
                    waitUntilAtLeastOneExists(hasText("Indentation"), timeoutMillis = 10_000)
                    // The pointer that opened the sample would rest over a row of Settings.
                    onRoot().performMouseInput { moveTo(Offset(1f, 1f)) }
                    onRoot().captureRoboImage(file)
                }

                Then.Ask -> {
                    onPantrixNode("Button", "paste").performSemanticsAction(SemanticsActions.OnClick)
                    waitUntilAtLeastOneExists(hasText("What format is this?"), timeoutMillis = 10_000)
                    onAllNodes(isRoot()).onLast().captureRoboImage(file)
                }

                Then.Share -> {
                    // Share is in the document's "more" menu, a phone's in its bar and a wider window's in its toolbar.
                    onPantrixNode("IconButton", "more").performSemanticsAction(SemanticsActions.OnClick)
                    onPantrixNode("MenuItem", "share", scope = "Menu_document").performSemanticsAction(SemanticsActions.OnClick)
                    // The link is made a moment after the share opens: its length is said under it then.
                    waitUntilAtLeastOneExists(hasText(" characters", substring = true), timeoutMillis = 10_000)
                    onAllNodes(isRoot()).onLast().captureRoboImage(file)
                }

                Then.BrokenLink -> {
                    onPantrixNode("Button", "paste").performSemanticsAction(SemanticsActions.OnClick)
                    waitUntilAtLeastOneExists(hasText("This link holds no document", substring = true), timeoutMillis = 10_000)
                    onRoot().captureRoboImage(file)
                }
            }
        }

    private companion object {
        /** The Share board's link, whose checksum is not its text's: a link Start says it cannot read. */
        const val BROKEN_LINK = "https://formatico.pantrixui.com/#d=eJyrVspMUbJSUEpKzSmyUiopTUoqUqoFAGG1CBs"

        /** A text Formatico cannot tell the format of, as the Start boards' question shows it. */
        const val UNTOLD = "2026-11-05 21:04:11 INFO  Server started on port 8080\n" +
            "2026-11-05 21:04:12 WARN  Slow query took 812 ms\n2026-11-05 21:04:15 INFO  Shutting down"
    }
}
