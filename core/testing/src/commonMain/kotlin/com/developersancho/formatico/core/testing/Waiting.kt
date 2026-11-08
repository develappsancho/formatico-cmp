package com.developersancho.formatico.core.testing

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Waits, letting the platform run, until [done] says so, for [within] at most; [what] is what did not happen. A browser
 * fetches a screen's fonts after its first frame, and moves in a tab's history come back as events: both happen only
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

/** Waits, as [awaitUntil] does, for one node that [matcher] finds, called [what] where none is. */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.awaitNode(matcher: SemanticsMatcher, what: String) =
    awaitUntil("$what was shown") { onAllNodes(matcher).fetchSemanticsNodes().size == 1 }

/** Waits, as [awaitNode] does, for [text] to be shown once, or a text holding it where [substring] says so. */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.awaitText(text: String, substring: Boolean = false) =
    awaitNode(hasText(text, substring = substring), "\"$text\"")
