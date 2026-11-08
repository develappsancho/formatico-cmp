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

/** The Start screen's cases: a first start, documents opened before, and a file it did not take. */
internal class StartScreenCases :
    PreviewCases<StartState>(
        "first start" to StartState(),
        "recent" to StartState(recent = PreviewRecent),
        "refused file" to StartState(
            recent = PreviewRecent.take(2),
            refusal = FileRefusal.TooLarge("logs.json", maxSize = 10_000_000)
        )
    )

@BoardPreviews
@Composable
internal fun StartScreenPreview(@PreviewParameter(StartScreenCases::class) case: PreviewCase<StartState>) {
    FormaticoPreview(case) {
        StartScreen(state = it, onIntent = {})
    }
}

/** A text whose format cannot be told, as the Start boards' question shows it. */
internal const val PREVIEW_UNTOLD = "2026-11-05 21:04:11 INFO  Server started on port 8080\n" +
    "2026-11-05 21:04:12 WARN  Slow query took 812 ms\n2026-11-05 21:04:15 INFO  Shutting down"

/** The question's cases: a text pasted, and a file. */
internal class UnknownFormatCases :
    PreviewCases<OpenedDocument>(
        "pasted" to OpenedDocument(name = null, text = PREVIEW_UNTOLD, format = null),
        "file" to OpenedDocument(name = "notes.txt", text = PREVIEW_UNTOLD, format = null)
    )

@BoardPreviews
@Composable
internal fun UnknownFormatPreview(@PreviewParameter(UnknownFormatCases::class) case: PreviewCase<OpenedDocument>) {
    FormaticoPreview(case) {
        StartScreen(state = StartState(), onIntent = {})
        UnknownFormatDialog(document = it, onOpen = {}, onDismiss = {})
    }
}
