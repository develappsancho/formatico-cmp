package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.developersancho.formatico.core.designsystem.BoardPreviews
import com.developersancho.formatico.core.designsystem.FormaticoPreview
import com.developersancho.formatico.core.designsystem.PreviewCase
import com.developersancho.formatico.core.designsystem.PreviewCases
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.share.ShareLinks
import com.developersancho.formatico.core.share.ShareOutcome
import com.developersancho.formatico.core.share.SharedDocument

/** The size of the Share-834 board's events.ndjson, too large for a link: 4.2 MB. */
private const val EVENTS_BYTES = 4_200_000

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

/** [state] with its document shared as [share] says, its link made as Formatico makes it unless [outcome] is given. */
private fun shared(
    state: WorkspaceState,
    formatted: Boolean = false,
    canFormat: Boolean = true,
    outcome: ShareOutcome? = state.document?.let { ShareLinks.share(SharedDocument(it.text, it.name)) }
): WorkspaceState {
    val document = requireNotNull(state.document)
    return state.copy(share = DocumentShare(document.name, document.format, formatted, canFormat, outcome))
}

/**
 * The share's cases: the Share boards' order, whose link every app takes; a link that some apps cut; the order in
 * the link formatted; plain text, which cannot be formatted; a document too large for a link, as Share-834 draws it;
 * and the dialog while its first link is being made.
 */
internal class ShareCases :
    PreviewCases<WorkspaceState>(
        "short" to shared(previewState("order.json", PreviewOrder, Format.Json)),
        "long" to shared(previewState("events.ndjson", PreviewLongEvents, Format.Ndjson)),
        "formatted" to shared(previewState("order.json", PreviewOrder, Format.Json), formatted = true),
        "plain text" to shared(previewState("notes.txt", "Things to check before the release.\n", null), canFormat = false),
        "too large" to shared(
            previewState("events.ndjson", PreviewEvents, Format.Ndjson),
            outcome = ShareOutcome.TooLarge(EVENTS_BYTES)
        ),
        "making" to shared(previewState("order.json", PreviewOrder, Format.Json), outcome = null)
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
