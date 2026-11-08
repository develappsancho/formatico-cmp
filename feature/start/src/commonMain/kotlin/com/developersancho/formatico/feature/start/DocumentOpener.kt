package com.developersancho.formatico.feature.start

import com.developersancho.formatico.core.format.FormatDetector
import com.pantrixui.foundation.files.PantrixFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/** What came of opening a text or a file: the document, nothing to open, or why the file was not opened. */
sealed interface Opening {
    /** [document] is to be shown, with its place in the history where it holds text. */
    data class Opened(val document: OpenedDocument) : Opening

    /** The clipboard held no text to paste. */
    data object NothingToPaste : Opening

    /** The file was not opened, for [refusal]. */
    data class Refused(val refusal: FileRefusal) : Opening
}

/**
 * Turns what the user pastes or opens into a document: its format told off the screen's thread, and its place at the
 * top of [history], as the Start screen and the command palette both do it.
 *
 * @param history the documents opened before.
 * @param clock what a document is recorded as opened by.
 * @param maxFileSize the most bytes a file may have, by the platform.
 * @param reader where files are read and their formats told.
 * @param keeps whether the history is kept, as Settings has it: where it is not, nothing goes to it.
 */
class DocumentOpener(
    private val history: DocumentHistory,
    private val clock: Clock,
    private val maxFileSize: Long,
    private val reader: CoroutineDispatcher = Dispatchers.Default,
    private val keeps: () -> Boolean = { true }
) {
    /** [text], pasted, as a document; nothing to open where it is null or blank. */
    suspend fun paste(text: String?): Opening {
        if (text.isNullOrBlank()) return Opening.NothingToPaste
        val format = withContext(reader) { FormatDetector.detect(text) }
        return Opening.Opened(record(OpenedDocument(name = null, text = text, format = format)))
    }

    /** [file] read as a document; refused where it is larger than the platform's most, or cannot be read. */
    suspend fun open(file: PantrixFile): Opening {
        val tooLarge = Opening.Refused(FileRefusal.TooLarge(file.name, maxFileSize))
        val size = file.size
        val bytes = if (size != null && size > maxFileSize) null else runCatching { file.readBytes() }.getOrNull()
        return when {
            size != null && size > maxFileSize -> tooLarge

            bytes == null -> Opening.Refused(FileRefusal.Unreadable(file.name))

            bytes.size > maxFileSize -> tooLarge

            else -> {
                val document = withContext(reader) {
                    val text = bytes.decodeToString()
                    OpenedDocument(name = file.name, text = text, format = FormatDetector.detect(text, file.name))
                }
                Opening.Opened(record(document))
            }
        }
    }

    /**
     * [document] at the top of the history where it holds text and a history is kept, with its place there; an empty
     * one, or any where none is kept, as it is.
     */
    suspend fun record(document: OpenedDocument): OpenedDocument {
        val historyId = if (document.text.isNotEmpty() && keeps()) history.record(document, clock.now()) else null
        return document.copy(historyId = historyId)
    }
}
