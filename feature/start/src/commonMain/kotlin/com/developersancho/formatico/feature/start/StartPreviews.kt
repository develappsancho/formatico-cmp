package com.developersancho.formatico.feature.start

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.developersancho.formatico.core.designsystem.BoardPreviews
import com.developersancho.formatico.core.designsystem.FormaticoPreview
import com.developersancho.formatico.core.designsystem.PreviewCase
import com.developersancho.formatico.core.designsystem.PreviewCases
import com.developersancho.formatico.core.designsystem.PreviewNow
import com.developersancho.formatico.core.format.Format
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** The Start screen as a case shows it: with [state], on a platform that reads the clipboard or not ([readsClipboard]). */
internal class StartScreenCase(val state: StartState, val readsClipboard: Boolean = true)

/**
 * Documents opened before, one at each distance from [PreviewNow] that Recent words differently: minutes, today,
 * yesterday, a day of this year and one of another. One was pasted, and one is plain text.
 */
private val PreviewRecent = listOf(
    RecentDocument(id = 1, name = "order.json", format = Format.Json, size = 665, at = PreviewNow - 2.minutes, edited = true),
    RecentDocument(id = 2, name = "deployment.yaml", format = Format.Yaml, size = 1946, at = PreviewNow - 3.hours),
    RecentDocument(id = 3, name = null, format = Format.Json, size = 118, at = PreviewNow - 1.days, edited = true),
    RecentDocument(id = 4, name = "countries.csv", format = Format.Csv, size = 12_311, at = Instant.parse("2026-09-14T15:20:00Z")),
    RecentDocument(id = 5, name = "notes.txt", format = null, size = 2048, at = Instant.parse("2025-03-02T08:05:00Z"))
)

/**
 * The Start screen's cases: a first start, documents opened before, a file it did not take, and a browser, which
 * cannot paste with the keyboard.
 */
internal class StartScreenCases :
    PreviewCases<StartScreenCase>(
        "first start" to StartScreenCase(StartState()),
        "recent" to StartScreenCase(StartState(recent = PreviewRecent)),
        "refused file" to StartScreenCase(
            StartState(recent = PreviewRecent.take(2), refusal = FileRefusal.TooLarge("logs.json", maxSize = 10_000_000))
        ),
        "in a browser" to StartScreenCase(StartState(), readsClipboard = false)
    )

@BoardPreviews
@Composable
internal fun StartScreenPreview(@PreviewParameter(StartScreenCases::class) case: PreviewCase<StartScreenCase>) {
    FormaticoPreview(case) {
        StartScreen(state = it.state, onIntent = {}, onPaste = {}, readsClipboard = it.readsClipboard)
    }
}
