package com.developersancho.formatico.feature.start

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.developersancho.formatico.core.network.AddressReading
import com.developersancho.formatico.feature.start.strings.startStrings
import com.pantrixui.components.overlay.snackbar.LocalPantrixSnackbars
import com.pantrixui.components.overlay.snackbar.PantrixSnackbarState
import com.pantrixui.components.overlay.snackbar.SnackbarDuration
import com.pantrixui.components.overlay.snackbar.SnackbarResult
import com.pantrixui.components.overlay.snackbar.SnackbarTone
import com.pantrixui.foundation.files.FilePickRequest
import com.pantrixui.foundation.files.FilePicker
import com.pantrixui.foundation.files.rememberFilePicker
import com.pantrixui.foundation.platform.PantrixClipboard
import com.pantrixui.foundation.platform.rememberPantrixClipboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** The web address asked for while its dialog is shown: its [text], whether it is [opening], and why it was not. */
internal data class AddressAsk(val text: String = "", val opening: Boolean = false, val refusal: AddressReading? = null)

/**
 * Paste, open a file or a web address away from the Start screen, as the command palette does: what is opened goes to
 * the history and on to the screen's `onOpen`, and what could not be opened is said as the Start screen says it, in a
 * snackbar; a web address that was not opened, in its dialog.
 */
@Stable
class DocumentOpening internal constructor(
    private val scope: CoroutineScope,
    internal val opener: DocumentOpener,
    private val clipboard: PantrixClipboard,
    private val picker: FilePicker,
    private val said: MutableState<OpeningMessage?>,
    private val onOpen: State<(OpenedDocument) -> Unit>
) {
    private val asked = mutableStateOf<AddressAsk?>(null)
    private var addressJob: Job? = null

    /** The web address asked for, while its dialog is shown. */
    internal val address: AddressAsk? get() = asked.value

    /** Asks for a web address to open the document at, in a dialog. */
    fun askAddress() {
        asked.value = AddressAsk()
    }

    internal fun changeAddress(text: String) {
        asked.value = asked.value?.copy(text = text, refusal = null)
    }

    /** Opens the document at the address asked for: the dialog leaves once it is open, and says why where it is not. */
    internal fun openAddress() {
        val ask = asked.value?.takeUnless { it.opening || it.text.isBlank() } ?: return
        asked.value = ask.copy(opening = true, refusal = null)
        addressJob = scope.launch {
            when (val opening = opener.address(ask.text)) {
                is Opening.Opened -> {
                    asked.value = null
                    onOpen.value(opening.document)
                }

                is Opening.AddressRefused -> asked.value = asked.value?.copy(opening = false, refusal = opening.reading)

                else -> asked.value = asked.value?.copy(opening = false)
            }
        }
    }

    /** Leaves the dialog, and the address being opened. */
    internal fun dismissAddress() {
        addressJob?.cancel()
        asked.value = null
    }

    /** Reads the clipboard's text and opens it as a new document; says so where there is none, or no clipboard. */
    fun paste() {
        scope.launch {
            if (clipboard.traits.readable) {
                show(opener.paste(clipboard.readText()))
            } else {
                said.value = OpeningMessage.ClipboardUnreadable
            }
        }
    }

    /**
     * Opens the document a link of Formatico's carries: [address] is the part of a browser's address from its `#`.
     * One that is no link of Formatico's opens nothing; a broken one is said so.
     */
    fun openLink(address: String) {
        scope.launch { opener.link(address)?.let(::show) }
    }

    /** Opens the platform's picker, and the file chosen in it. */
    fun pickFile() {
        picker.pick(FilePickRequest(accept = DocumentFileTypes)) { files ->
            val file = files.firstOrNull() ?: return@pick
            scope.launch { show(opener.open(file)) }
        }
    }

    private fun show(opening: Opening) {
        when (opening) {
            is Opening.Opened -> onOpen.value(opening.document)
            Opening.NothingToPaste -> said.value = OpeningMessage.NothingToPaste
            is Opening.Refused -> said.value = OpeningMessage.Refused(opening.refusal)
            Opening.BrokenLink -> said.value = OpeningMessage.BrokenLink
            Opening.SharedLinkGone -> said.value = OpeningMessage.SharedLinkGone
            is Opening.SharedLinkUnreachable -> said.value = OpeningMessage.SharedLinkUnreachable(opening.address)
            is Opening.AddressRefused -> asked.value = asked.value?.copy(opening = false, refusal = opening.reading)
        }
    }
}

/** What opening away from the Start screen says in a snackbar. */
internal sealed interface OpeningMessage {
    data object NothingToPaste : OpeningMessage

    data object ClipboardUnreadable : OpeningMessage

    data class Refused(val refusal: FileRefusal) : OpeningMessage

    data object BrokenLink : OpeningMessage

    data object SharedLinkGone : OpeningMessage

    data class SharedLinkUnreachable(val address: String) : OpeningMessage
}

/** A way to paste and open a file from anywhere in the app; what is opened is handed to [onOpen]. */
@Composable
fun rememberDocumentOpening(onOpen: (OpenedDocument) -> Unit): DocumentOpening {
    val scope = rememberCoroutineScope()
    val opener = koinInject<DocumentOpener>()
    val clipboard = rememberPantrixClipboard()
    val picker = rememberFilePicker()
    val said = remember { mutableStateOf<OpeningMessage?>(null) }
    val open = rememberUpdatedState(onOpen)
    val opening = remember(scope, opener, clipboard, picker) { DocumentOpening(scope, opener, clipboard, picker, said, open) }
    OpeningMessages(said, scope, onRetry = opening::openLink)
    WebAddressDialog(opening)
    SharedLinkWait(opener)
    return opening
}

/**
 * Says the last thing [said] holds in a snackbar, as the Start screen says it, and forgets it: the snackbar is shown in
 * [scope], which forgetting it does not end.
 */
@Composable
private fun OpeningMessages(said: MutableState<OpeningMessage?>, scope: CoroutineScope, onRetry: (String) -> Unit) {
    val snackbars = LocalPantrixSnackbars.current
    val shown = said.value
    val tryAgain = startStrings.sharedLinkTryAgain
    val message = when (shown) {
        OpeningMessage.NothingToPaste -> startStrings.clipboardEmpty to "nothing_to_paste"
        OpeningMessage.ClipboardUnreadable -> startStrings.clipboardUnreadable to "clipboard_unreadable"
        is OpeningMessage.Refused -> refusalText(shown.refusal) to "file_refused"
        OpeningMessage.BrokenLink -> startStrings.linkBroken to "link_broken"
        OpeningMessage.SharedLinkGone -> startStrings.sharedLinkGone to "shared_link_gone"
        is OpeningMessage.SharedLinkUnreachable -> startStrings.sharedLinkUnreachable to "shared_link_unreachable"
        null -> null
    }
    LaunchedEffect(message) {
        val (text, tag) = message ?: return@LaunchedEffect
        said.value = null
        scope.launch {
            when (shown) {
                OpeningMessage.BrokenLink -> snackbars?.showBrokenLink(text)

                OpeningMessage.SharedLinkGone -> snackbars?.showSharedLinkGone(text)

                is OpeningMessage.SharedLinkUnreachable ->
                    if (snackbars?.showSharedLinkUnreachable(text, tryAgain) == true) onRetry(shown.address)

                else -> snackbars?.show(message = text, tagKey = tag)
            }
        }
    }
}

/**
 * Says a link of Formatico's holds no document it can read, as the Start board does: in the danger's tone, for longer
 * than a word, until it is closed or its time is up.
 */
internal suspend fun PantrixSnackbarState.showBrokenLink(message: String) {
    show(message = message, duration = SnackbarDuration.Long, closeButton = true, tone = SnackbarTone.Danger, tagKey = "link_broken")
}

/** Says an encrypted link's share is gone, as the Start board does: in the danger's tone, until it is closed. */
internal suspend fun PantrixSnackbarState.showSharedLinkGone(message: String) {
    show(message = message, duration = SnackbarDuration.Long, closeButton = true, tone = SnackbarTone.Danger, tagKey = "shared_link_gone")
}

/** Says an encrypted link could not be opened, with [tryAgain] to open it again: true where it was pressed. */
internal suspend fun PantrixSnackbarState.showSharedLinkUnreachable(message: String, tryAgain: String): Boolean =
    show(message = message, actionLabel = tryAgain, tone = SnackbarTone.Danger, tagKey = "shared_link_unreachable") ==
        SnackbarResult.ActionPerformed
