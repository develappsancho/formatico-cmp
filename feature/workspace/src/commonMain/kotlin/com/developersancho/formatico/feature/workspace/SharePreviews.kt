package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.developersancho.formatico.core.designsystem.BoardPreviews
import com.developersancho.formatico.core.designsystem.FormaticoPreview
import com.developersancho.formatico.core.designsystem.PreviewCase
import com.developersancho.formatico.core.designsystem.PreviewCases
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.network.EncryptedLink
import com.developersancho.formatico.core.share.ShareLinks
import com.developersancho.formatico.core.share.ShareOutcome
import com.developersancho.formatico.core.share.SharedDocument
import kotlin.time.Instant

/** The size of the Share-834 board's events.ndjson, too large for a link: 4.2 MB. */
private const val EVENTS_BYTES = 4_200_000

/** The size of the Share-834 board's access.ndjson, too large for an encrypted link as well: 24 MB. */
private const val ACCESS_BYTES = 24_000_000

/** The encrypted links Formatico's server keeps from one network a day. */
private const val DAILY_LIMIT = 50

/** The boards' encrypted link, kept for a week from the day they were drawn. */
private val PreviewEncryptedLink = EncryptedLink(
    link = "https://formatico.pantrixui.com/#s=7Qm2xLp9fK&k=Hq3vN8cTz1Wb5yRk0LmPa2XsD7eFgJ4uV9nQ6tYhC8w",
    id = "7Qm2xLp9fK",
    deleteToken = "preview",
    expiresAt = Instant.parse("2026-11-13T18:30:00Z")
)

/** How many events the long link's document holds: enough for a link some apps cut, as Share-1440's long state has. */
private const val LONG_EVENTS = 300
private const val SESSION_STEP = 2_654_435_761L
private const val SESSION_MASK = 0xFFFFFFFFL
private const val BYTES_FIRST = 1_200
private const val BYTES_STEP = 37
private const val MINUTES = 60
private const val MINUTE_DIGITS = 2
private const val HEX = 16
private val EventNames = listOf("open", "search", "format", "convert", "copy")

/** A day's events, each in its own session: they compress less than the boards' few, into a link of thousands. */
private val PreviewLongEvents = (0 until LONG_EVENTS).joinToString("\n", postfix = "\n") { index ->
    val minute = (index % MINUTES).toString().padStart(MINUTE_DIGITS, '0')
    val event = EventNames[index % EventNames.size]
    val session = ((index * SESSION_STEP) and SESSION_MASK).toString(HEX)
    """{"at": "2026-10-29T09:$minute:12Z", "event": "$event", "bytes": ${BYTES_FIRST + index * BYTES_STEP}, "session": "$session"}"""
}

/**
 * [state] with its document shared, its link made as Formatico makes it unless [outcome] is given, and the share as
 * [shown] makes it: formatted, the way chosen, where its encrypted link stands.
 */
private fun shared(
    state: WorkspaceState,
    outcome: ShareOutcome? = state.document?.let { ShareLinks.share(SharedDocument(it.text, it.name)) },
    shown: DocumentShare.() -> DocumentShare = { this }
): WorkspaceState {
    val document = requireNotNull(state.document)
    return state.copy(share = DocumentShare(document.name, document.format, canFormat = true, outcome = outcome).shown())
}

private val PreviewOrderState = previewState("order.json", PreviewOrder, Format.Json)
private val PreviewEventsState = previewState("events.ndjson", PreviewEvents, Format.Ndjson)

/**
 * The share's cases: the Share boards' order, whose link every app takes; a link that some apps cut; the order in
 * the link formatted; plain text, which cannot be formatted; the dialog while its first link is being made. Then the
 * encrypted link: chosen, being made, its server not reached, taking no share now, or no more from this network
 * today, made, and not deleted; a document too large for a link that goes only as an encrypted one, as Share-834 draws
 * it, that link made, and this network's links of the day made; one too large for that as well.
 */
internal class ShareCases :
    PreviewCases<WorkspaceState>(
        "short" to shared(PreviewOrderState),
        "long" to shared(previewState("events.ndjson", PreviewLongEvents, Format.Ndjson)),
        "formatted" to shared(PreviewOrderState) { copy(formatted = true) },
        "plain text" to shared(previewState("notes.txt", "Things to check before the release.\n", null)) { copy(canFormat = false) },
        "making" to shared(PreviewOrderState, outcome = null),
        "encrypted" to shared(PreviewOrderState) { copy(way = ShareWay.Encrypted) },
        "creating" to shared(PreviewOrderState) { copy(way = ShareWay.Encrypted, encrypted = EncryptedShare.Creating) },
        "unreachable" to shared(PreviewOrderState) { copy(way = ShareWay.Encrypted, encrypted = EncryptedShare.Failed()) },
        "busy" to shared(PreviewOrderState) { copy(way = ShareWay.Encrypted, encrypted = EncryptedShare.Failed(ShareFailure.Busy)) },
        "daily limit" to shared(PreviewOrderState) {
            copy(way = ShareWay.Encrypted, encrypted = EncryptedShare.Failed(ShareFailure.DailyLimit(DAILY_LIMIT)))
        },
        "created" to shared(PreviewOrderState) { copy(way = ShareWay.Encrypted, encrypted = EncryptedShare.Created(PreviewEncryptedLink)) },
        "not deleted" to shared(PreviewOrderState) {
            copy(way = ShareWay.Encrypted, encrypted = EncryptedShare.Created(PreviewEncryptedLink, deleteFailed = true))
        },
        "encrypted only" to shared(PreviewEventsState, outcome = ShareOutcome.TooLarge(EVENTS_BYTES)),
        "encrypted only created" to shared(PreviewEventsState, ShareOutcome.TooLarge(EVENTS_BYTES)) {
            copy(encrypted = EncryptedShare.Created(PreviewEncryptedLink))
        },
        "encrypted only daily limit" to shared(PreviewEventsState, ShareOutcome.TooLarge(EVENTS_BYTES)) {
            copy(encrypted = EncryptedShare.Failed(ShareFailure.DailyLimit(DAILY_LIMIT)))
        },
        "too large" to shared(previewState("access.ndjson", PreviewEvents, Format.Ndjson), outcome = ShareOutcome.TooLarge(ACCESS_BYTES))
    )

/**
 * A case of [ShareCases] over its workspace, as the Share boards draw it; on a phone with a way of sending the link to
 * another app, as Share-390 has one.
 */
@BoardPreviews
@Composable
internal fun SharePreview(@PreviewParameter(ShareCases::class) case: PreviewCase<WorkspaceState>) {
    FormaticoPreview(case) { state ->
        val commands = rememberDocumentCommands(remember { Workbench() })
        WorkspaceScreen(state = state.copy(share = null), onIntent = {}, commands = commands)
        ShareDocument(state = state, layout = workspaceLayout(), onIntent = {}, commands = commands, sending = {})
    }
}
