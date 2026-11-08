package com.developersancho.formatico.core.testing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.designsystem.PreviewCase
import com.developersancho.formatico.core.designsystem.PreviewCases
import com.pantrixui.foundation.diagnostics.PantrixFinding
import com.pantrixui.foundation.diagnostics.PantrixFindings
import com.pantrixui.testing.PantrixTestTheme
import kotlin.time.Duration.Companion.minutes

/** A case drawn at a board: what [checkPreviewCases] shows at a time. */
private class Shown<T>(val case: PreviewCase<T>, val board: Board)

/**
 * Draws every case of [cases] in the light theme at each board [boardsOf] gives its value, in that board's window and
 * platform and at its size whatever the test's window is, with [content], as the case's preview and screenshot draw it.
 * Each case is then checked with [check], for what it shows and what pressing it does, and Pantrix's diagnostics must
 * find nothing in it but what [known] lets through: a finding of Pantrix's not put right yet, which the caller names.
 * Each case is drawn anew, as the screen or the component opens in that state; the dark theme shows the same and is
 * left to the screenshots.
 *
 * It is the UI test a screen's or a component's every preview case has, on every target: one test, as a browser runs
 * one composition test at a time.
 */
@OptIn(ExperimentalTestApi::class)
fun <T> checkPreviewCases(
    cases: PreviewCases<T>,
    boardsOf: (T) -> List<Board>,
    content: @Composable (PreviewCase<T>) -> Unit,
    known: (finding: PantrixFinding, case: PreviewCase<T>, board: Board) -> Boolean = { _, _, _ -> false },
    check: suspend ComposeUiTest.(case: PreviewCase<T>, board: Board) -> Unit
) = runComposeUiTest(testTimeout = PREVIEW_CASES_TIMEOUT) {
    val findings = PantrixFindings()
    var shown by mutableStateOf<Shown<T>?>(null)
    setContent {
        shown?.let { now ->
            key(now) {
                PantrixTestTheme(platform = now.board.platform, window = now.board.window, findings = findings) {
                    Box(Modifier.wrapContentSize(Alignment.TopStart, unbounded = true).size(now.board.width.dp, now.board.height.dp)) {
                        content(now.case)
                    }
                }
            }
        }
    }
    for (case in cases.values.filterNot { it.dark }) {
        for (board in boardsOf(case.value)) {
            findings.clear()
            shown = Shown(case, board)
            awaitIdle()
            try {
                check(case, board)
                // A squeezed touch target is reported once it has lasted half a second, a tag on two nodes after a
                // second and a half: the clock is moved past both, as Pantrix's assertNoFindings does.
                mainClock.advanceTimeBy(FINDINGS_SETTLE_MILLIS)
                awaitIdle()
                val found = findings.all.filterNot { known(it, case, board) }
                kotlin.check(found.isEmpty()) { "Pantrix's diagnostics found ${found.joinToString("; ")}" }
            } catch (failure: AssertionError) {
                throw AssertionError("$case at ${board.width}: ${failure.message}", failure)
            } catch (failure: IllegalStateException) {
                throw AssertionError("$case at ${board.width}: ${failure.message}", failure)
            }
        }
    }
}

/**
 * How long every case's drawing and checking may take in all: the share dialog's 14 cases at three boards are 42, which
 * a browser drawing beside the other targets' tests took more than the minute a test is given by default for.
 */
private val PREVIEW_CASES_TIMEOUT = 3.minutes

/** How long a finding takes to be reported at the most: a tag on two nodes, after a second and a half. */
private const val FINDINGS_SETTLE_MILLIS = 2_000L

/**
 * Waits for each of [texts] to be shown, once or more, each whole or, where [substring] says so, within a text; or to
 * be said at the start of a control's name, as a phone's status is a button named by its words and what it does.
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.awaitTexts(vararg texts: String, substring: Boolean = false) {
    for (text in texts) {
        val shown = hasText(text, substring = substring) or hasContentDescription("$text.", substring = true)
        awaitUntil("\"$text\" was shown") { onAllNodes(shown).fetchSemanticsNodes().isNotEmpty() }
    }
}
