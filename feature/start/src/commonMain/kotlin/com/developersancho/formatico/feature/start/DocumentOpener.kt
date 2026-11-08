package com.developersancho.formatico.feature.start

import com.developersancho.formatico.core.format.FormatDetector
import com.developersancho.formatico.core.share.LinkReading
import com.developersancho.formatico.core.share.ShareLinks
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

    /** A link of Formatico's whose document cannot be read back: cut short, or changed on its way. */
    data object BrokenLink : Opening
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
    /**
     * [text], pasted, as a document: the one it carries where it is a link of Formatico's, and else itself. Nothing to
     * open where it is null or blank.
     */
    suspend fun paste(text: String?): Opening {
        if (text.isNullOrBlank()) return Opening.NothingToPaste
        return link(text) ?: Opening.Opened(record(OpenedDocument(name = null, text = text, format = detect(text, null))))
    }

    /**
     * The document a link of Formatico's carries, as a file of its name is opened: [address] is a whole link, or the
     * part of one from its `#`, as a browser's address gives it. A broken link where it cannot be read back; null
     * where [address] is no link of Formatico's.
     */
    suspend fun link(address: String): Opening? = when (val reading = withContext(reader) { ShareLinks.read(address) }) {
        LinkReading.NotALink -> null

        LinkReading.Broken -> Opening.BrokenLink

        is LinkReading.Read -> {
            val (text, name) = reading.document
            Opening.Opened(record(OpenedDocument(name = name, text = text, format = detect(text, name))))
        }
    }

    private suspend fun detect(text: String, name: String?) = withContext(reader) { FormatDetector.detect(text, name) }

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
