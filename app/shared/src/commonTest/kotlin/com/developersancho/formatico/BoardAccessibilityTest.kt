package com.developersancho.formatico

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.storage.memoryStorageModule
import com.pantrixui.foundation.adaptive.PantrixWindowFrame
import com.pantrixui.foundation.diagnostics.PantrixFindings
import com.pantrixui.testing.PantrixTestTheme
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * Every board, at each of its three widths, on every target, with the theme's diagnostics on: no control without a
 * name, no words too faint, no touch target squeezed, no tag on two nodes. The app is drawn in a frame of the board's
 * size, larger than what some targets' tests draw in, so that its layout is the board's. The desktop's screenshots
 * look at the dark theme and at Turkish too (`BoardScreenshotTest`).
 */
@OptIn(ExperimentalTestApi::class)
class BoardAccessibilityTest {
    // KoinApplication keeps its context once the app is gone: each board starts with nothing open.
    @AfterTest
    fun closeTheApp() {
        stopKoin()
    }

    @Test fun startOnADesktop() = check(AppBoard.Start, Board.Desktop)

    @Test fun startOnATablet() = check(AppBoard.Start, Board.Tablet)

    @Test fun startOnAPhone() = check(AppBoard.Start, Board.Phone)

    @Test fun workspaceOnADesktop() = check(AppBoard.Workspace, Board.Desktop)

    @Test fun workspaceOnATablet() = check(AppBoard.Workspace, Board.Tablet)

    @Test fun workspaceOnAPhone() = check(AppBoard.Workspace, Board.Phone)

    @Test fun yamlOnADesktop() = check(AppBoard.Yaml, Board.Desktop)

    @Test fun yamlOnATablet() = check(AppBoard.Yaml, Board.Tablet)

    @Test fun yamlOnAPhone() = check(AppBoard.Yaml, Board.Phone)

    @Test fun tableOnADesktop() = check(AppBoard.Table, Board.Desktop)

    @Test fun tableOnATablet() = check(AppBoard.Table, Board.Tablet)

    @Test fun tableOnAPhone() = check(AppBoard.Table, Board.Phone)

    @Test fun invalidOnADesktop() = check(AppBoard.Invalid, Board.Desktop)

    @Test fun invalidOnATablet() = check(AppBoard.Invalid, Board.Tablet)

    @Test fun invalidOnAPhone() = check(AppBoard.Invalid, Board.Phone)

    @Test fun queryOnADesktop() = check(AppBoard.Query, Board.Desktop)

    @Test fun queryOnATablet() = check(AppBoard.Query, Board.Tablet)

    @Test fun queryOnAPhone() = check(AppBoard.Query, Board.Phone)

    @Test fun convertOnADesktop() = check(AppBoard.Convert, Board.Desktop)

    @Test fun convertOnATablet() = check(AppBoard.Convert, Board.Tablet)

    @Test fun convertOnAPhone() = check(AppBoard.Convert, Board.Phone)

    @Test fun compareOnADesktop() = check(AppBoard.Compare, Board.Desktop)

    @Test fun compareOnATablet() = check(AppBoard.Compare, Board.Tablet)

    @Test fun compareOnAPhone() = check(AppBoard.Compare, Board.Phone)

    @Test fun summaryOnADesktop() = check(AppBoard.Summary, Board.Desktop)

    @Test fun summaryOnATablet() = check(AppBoard.Summary, Board.Tablet)

    @Test fun summaryOnAPhone() = check(AppBoard.Summary, Board.Phone)

    @Test fun paletteOnADesktop() = check(AppBoard.Palette, Board.Desktop)

    @Test fun paletteOnATablet() = check(AppBoard.Palette, Board.Tablet)

    @Test fun paletteOnAPhone() = check(AppBoard.Palette, Board.Phone)

    @Test fun settingsOnADesktop() = check(AppBoard.Settings, Board.Desktop)

    @Test fun settingsOnATablet() = check(AppBoard.Settings, Board.Tablet)

    @Test fun settingsOnAPhone() = check(AppBoard.Settings, Board.Phone)

    @Test fun askOnADesktop() = check(AppBoard.Ask, Board.Desktop)

    @Test fun askOnATablet() = check(AppBoard.Ask, Board.Tablet)

    @Test fun askOnAPhone() = check(AppBoard.Ask, Board.Phone)

    @Test fun shareOnADesktop() = check(AppBoard.Share, Board.Desktop)

    @Test fun shareOnATablet() = check(AppBoard.Share, Board.Tablet)

    @Test fun shareOnAPhone() = check(AppBoard.Share, Board.Phone)

    @Test fun brokenLinkOnADesktop() = check(AppBoard.BrokenLink, Board.Desktop)

    @Test fun brokenLinkOnATablet() = check(AppBoard.BrokenLink, Board.Tablet)

    @Test fun brokenLinkOnAPhone() = check(AppBoard.BrokenLink, Board.Phone)

    /** Brings the app on [window] to [board] in English and the light theme, and asserts the diagnostics found nothing. */
    private fun check(board: AppBoard, window: Board) = runComposeUiTest {
        val findings = PantrixFindings()
        val files = BoardFiles()
        setContent {
            PantrixTestTheme(
                platform = window.platform,
                window = window.window,
                findings = findings,
                clipboard = board.clipboard(),
                filePicker = files
            ) {
                PantrixWindowFrame(window.window, Modifier.requiredSize(window.width.dp, window.height.dp)) {
                    App(systemDark = false, storage = memoryStorageModule)
                }
            }
        }
        tour(board, window, BoardWords.English, files)
        assertNoFindingsOnBoard(findings)
    }
}
