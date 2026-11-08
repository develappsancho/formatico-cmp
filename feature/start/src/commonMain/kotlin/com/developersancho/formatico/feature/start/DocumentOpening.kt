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
import com.pantrixui.components.overlay.snackbar.LocalPantrixSnackbars
import com.pantrixui.foundation.files.FilePickRequest
import com.pantrixui.foundation.files.FilePicker
import com.pantrixui.foundation.files.rememberFilePicker
import com.pantrixui.foundation.platform.PantrixClipboard
import com.pantrixui.foundation.platform.rememberPantrixClipboard
import formatico.feature.start.generated.resources.Res
import formatico.feature.start.generated.resources.start_clipboard_empty
import formatico.feature.start.generated.resources.start_clipboard_unreadable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Paste and open a file away from the Start screen, as the command palette does: what is opened goes to the history
 * and on to the screen's `onOpen`, and what could not be opened is said as the Start screen says it, in a snackbar.
 */
@Stable
class DocumentOpening internal constructor(
    private val scope: CoroutineScope,
    private val opener: DocumentOpener,
    private val clipboard: PantrixClipboard,
    private val picker: FilePicker,
    private val said: MutableState<OpeningMessage?>,
    private val onOpen: State<(OpenedDocument) -> Unit>
) {
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
        }
    }
}

/** What opening away from the Start screen says in a snackbar. */
internal sealed interface OpeningMessage {
    data object NothingToPaste : OpeningMessage

    data object ClipboardUnreadable : OpeningMessage

    data class Refused(val refusal: FileRefusal) : OpeningMessage
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
    OpeningMessages(said, scope)
    return remember(scope, opener, clipboard, picker) { DocumentOpening(scope, opener, clipboard, picker, said, open) }
}

/**
 * Says the last thing [said] holds in a snackbar, as the Start screen says it, and forgets it: the snackbar is shown in
 * [scope], which forgetting it does not end.
 */
@Composable
private fun OpeningMessages(said: MutableState<OpeningMessage?>, scope: CoroutineScope) {
    val snackbars = LocalPantrixSnackbars.current
    val message = when (val shown = said.value) {
        OpeningMessage.NothingToPaste -> stringResource(Res.string.start_clipboard_empty) to "nothing_to_paste"
        OpeningMessage.ClipboardUnreadable -> stringResource(Res.string.start_clipboard_unreadable) to "clipboard_unreadable"
        is OpeningMessage.Refused -> refusalText(shown.refusal) to "file_refused"
        null -> null
    }
    LaunchedEffect(message) {
        val (text, tag) = message ?: return@LaunchedEffect
        said.value = null
        scope.launch { snackbars?.show(message = text, tagKey = tag) }
    }
}
