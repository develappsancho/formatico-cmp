package com.developersancho.formatico.core.testing

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import com.pantrixui.foundation.diagnostics.PantrixFindings
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.foundation.platform.currentPlatformType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Waits, letting the platform run, until [done] says so, for [within] at most; [what] is what did not happen. A browser
 * fetches a screen's words after its first frame, and moves in a tab's history come back as events: both happen only
 * while the page's event loop runs, which `waitUntil` holds.
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.awaitUntil(what: String, within: Duration = 10.seconds, done: () -> Boolean) {
    val waited = TimeSource.Monotonic.markNow()
    while (!done()) {
        check(waited.elapsedNow() < within) { "$what did not happen in $within" }
        withContext(Dispatchers.Default) { delay(20.milliseconds) }
        awaitIdle()
    }
}

/**
 * Waits, as [awaitUntil] does, for one node that [matcher] finds, called [what] where none is.
 *
 * In a browser a row or a button has no name until its words come, and the diagnostics say so a frame later: what they
 * found until then is [forgetting]'s to forget (PANTRIX_FINDINGS: "MissingName, kelimeler yüklenirken"). Elsewhere the
 * words are there from the first frame, and every finding is kept.
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.awaitNode(matcher: SemanticsMatcher, what: String, forgetting: PantrixFindings? = null) {
    awaitUntil("$what was shown") { onAllNodes(matcher).fetchSemanticsNodes().size == 1 }
    forgetWhileWordsCame(forgetting)
}

/**
 * In a browser, forgets what the diagnostics found in [findings] while a screen's words were still coming, a frame
 * after they came: a row or a button has no name until then (PANTRIX_FINDINGS: "MissingName, kelimeler yüklenirken").
 * Elsewhere the words are there from the first frame, and every finding is kept.
 */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.forgetWhileWordsCame(findings: PantrixFindings?) {
    if (findings != null && currentPlatformType() == PlatformType.Web) {
        withContext(Dispatchers.Default) { delay(100.milliseconds) }
        awaitIdle()
        findings.clear()
    }
}

/** Waits, as [awaitNode] does, for [text] to be shown once, or a text holding it where [substring] says so. */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.awaitText(text: String, substring: Boolean = false, forgetting: PantrixFindings? = null) =
    awaitNode(hasText(text, substring = substring), "\"$text\"", forgetting)
