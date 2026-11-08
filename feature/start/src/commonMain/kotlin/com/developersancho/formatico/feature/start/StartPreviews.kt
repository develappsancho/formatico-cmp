package com.developersancho.formatico.feature.start

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.developersancho.formatico.core.designsystem.BoardPreviews
import com.developersancho.formatico.core.designsystem.FormaticoPreview
import com.developersancho.formatico.core.designsystem.PreviewCase
import com.developersancho.formatico.core.designsystem.PreviewCases
import com.developersancho.formatico.core.designsystem.PreviewNow
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.network.AddressReading
import com.pantrixui.foundation.theme.PantrixTheme
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

/**
 * The Start screen's cases: a first start, documents opened before, and a file it did not take for each reason: too
 * large, of a kind it does not read, and one the platform could not read.
 */
internal class StartScreenCases :
    PreviewCases<StartState>(
        "first start" to StartState(),
        "recent" to StartState(recent = PreviewRecent),
        "refused file" to StartState(
            recent = PreviewRecent.take(2),
            refusal = FileRefusal.TooLarge("logs.json", maxSize = 10_000_000)
        ),
        "wrong type" to StartState(recent = PreviewRecent.take(2), refusal = FileRefusal.WrongType("invoice.pdf")),
        "unreadable" to StartState(recent = PreviewRecent.take(2), refusal = FileRefusal.Unreadable("orders.json"))
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

/** The web address's cases: asked for, typed, being opened, and each reason a document was not opened. */
internal class WebAddressCases :
    PreviewCases<AddressAsk>(
        "empty" to AddressAsk(),
        "typed" to AddressAsk(text = PREVIEW_ADDRESS),
        "opening" to AddressAsk(text = PREVIEW_ADDRESS, opening = true),
        "not there" to AddressAsk(text = PREVIEW_ADDRESS, refusal = AddressReading.Answered(NOT_FOUND)),
        "answered" to AddressAsk(text = PREVIEW_ADDRESS, refusal = AddressReading.Answered(UNAVAILABLE)),
        "not allowed" to AddressAsk(text = "ftp://example.com/orders/8842.json", refusal = AddressReading.NotAllowed),
        "too large" to AddressAsk(text = PREVIEW_ADDRESS, refusal = AddressReading.TooLarge(LARGE_BYTES)),
        "too large untold" to AddressAsk(text = PREVIEW_ADDRESS, refusal = AddressReading.TooLarge(null)),
        "not text" to AddressAsk(text = PREVIEW_ADDRESS, refusal = AddressReading.NotText),
        "unreachable" to AddressAsk(text = PREVIEW_ADDRESS, refusal = AddressReading.Unreachable),
        "server unreachable" to AddressAsk(text = PREVIEW_ADDRESS, refusal = AddressReading.ServerUnreachable)
    )

/** The address the Start boards open. */
private const val PREVIEW_ADDRESS = "https://example.com/orders/8842.json"

/** What an address answers where it holds no document, and where it cannot give one now. */
private const val NOT_FOUND = 404
private const val UNAVAILABLE = 503

/** A document of 24 MB: too large to open from an address on a phone and on the web. */
private const val LARGE_BYTES = 24_000_000L

@BoardPreviews
@Composable
internal fun WebAddressPreview(@PreviewParameter(WebAddressCases::class) case: PreviewCase<AddressAsk>) {
    FormaticoPreview(case) {
        StartScreen(state = StartState(), onIntent = {})
        WebAddressDialog(ask = it, maxFileSize = maxFileSize(PantrixTheme.platform.type), onChange = {}, onOpen = {}, onDismiss = {})
    }
}

/** The wait for an encrypted link's share: its one case, while the share is fetched. */
internal class SharedLinkWaitCases : PreviewCases<Boolean>("opening" to true)

/** The wait for an encrypted link's share, over Start, as the Start boards draw it (link: opening). */
@BoardPreviews
@Composable
internal fun SharedLinkWaitPreview(@PreviewParameter(SharedLinkWaitCases::class) case: PreviewCase<Boolean>) {
    FormaticoPreview(case) {
        StartScreen(state = StartState(), onIntent = {})
        SharedLinkWait(fetching = it, onCancel = {})
    }
}
