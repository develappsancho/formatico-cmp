package com.developersancho.formatico.feature.download

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.performSemanticsAction
import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.testing.awaitTexts
import com.developersancho.formatico.core.testing.checkPreviewCases
import com.pantrixui.testing.onPantrixNode
import kotlin.test.Test

/**
 * Every preview case of the download menu, drawn as its preview draws it at every board, opened by its button: what
 * each shows, and that Pantrix's diagnostics find nothing in it. What choosing does is in DownloadMenuTest.
 */
@OptIn(ExperimentalTestApi::class)
class PreviewCasesTest {
    @Test
    fun theMenuShowsEachRelease() = checkPreviewCases(
        DownloadMenuCases(),
        boardsOf = { Board.entries },
        content = { DownloadMenuPreview(it) }
    ) { case, _ ->
        // The button stands at the board's foot, past the test's window: it is pressed by its action, not by a pointer.
        onPantrixNode("Button", "get_the_app").performSemanticsAction(SemanticsActions.OnClick)
        when (case.name) {
            "release" -> awaitTexts("Formatico 1.0.0 · released Nov 12, 2026", "Macs with Apple silicon · DMG · 95 MB")
            "no windows" -> awaitTexts("Not in this release yet", "Android 11 or later · APK · 42 MB")
            "looking" -> awaitTexts("Looking up the latest release")
            "unreachable" -> awaitTexts("GitHub could not be reached", "Open the releases page")
            else -> awaitTexts("No release on GitHub yet", "Open the releases page")
        }
    }
}
