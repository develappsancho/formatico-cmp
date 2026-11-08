package com.developersancho.formatico.feature.start

import com.developersancho.formatico.core.format.FormatDetector
import com.developersancho.formatico.core.network.AddressReading
import com.developersancho.formatico.core.network.Online
import com.developersancho.formatico.core.network.SharedOpening
import com.developersancho.formatico.core.network.linkSite
import com.developersancho.formatico.core.share.LinkReading
import com.developersancho.formatico.core.share.ShareLinks
import com.developersancho.formatico.core.share.SharedDocument
import com.pantrixui.foundation.files.PantrixFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/** What came of opening a text or a file: the document, nothing to open, or why the file was not opened. */
sealed interface Opening {
    /** [document] is to be shown, with its place in the history where it holds text. */
    data class Opened(val document: OpenedDocument) : Opening

    /** The clipboard held no text to paste. */
    data object NothingToPaste : Opening

    /** The text pasted has more bytes than the platform's most, [maxSize], as a file's may have. */
    data class PasteTooLarge(val maxSize: Long) : Opening

    /** The file was not opened, for [refusal]. */
    data class Refused(val refusal: FileRefusal) : Opening

    /** A link of Formatico's whose document cannot be read back: cut short, or changed on its way. */
    data object BrokenLink : Opening

    /** An encrypted link whose share is gone: it expired, or whoever shared it deleted it. */
    data object SharedLinkGone : Opening

    /** An encrypted link at [address] whose share Formatico's server, not reached, could not give. */
    data class SharedLinkUnreachable(val address: String) : Opening

    /** The document at a web address was not opened, for what [reading] says. */
    data class AddressRefused(val reading: AddressReading) : Opening
}

/**
 * Turns what the user pastes or opens into a document: its format told off the screen's thread, and its place at the
 * top of [history], as the Start screen and the command palette both do it.
 *
 * @param history the documents opened before.
 * @param clock what a document is recorded as opened by.
 * @param maxFileSize the most bytes a file, or the document at a web address, may have, by the platform.
 * @param reader where files are read and their formats told.
 * @param keeps whether the history is kept, as Settings has it: where it is not, nothing goes to it.
 * @param online where an encrypted link's share and the document at a web address are fetched from: none in a test
 *   that opens neither.
 */
class DocumentOpener(
    private val history: DocumentHistory,
    private val clock: Clock,
    val maxFileSize: Long,
    private val reader: CoroutineDispatcher = Dispatchers.Default,
    private val keeps: () -> Boolean = { true },
    private val online: Online? = null
) {
    private var fetchingJob: Job? = null
    private val fetchingNow = MutableStateFlow(false)

    /** Whether an encrypted link's share is being fetched, which the app waits for over whatever it shows. */
    val fetching: StateFlow<Boolean> = fetchingNow.asStateFlow()

    /** Stops fetching the encrypted link's share: nothing is opened. */
    fun cancelFetching() {
        fetchingJob?.cancel()
    }

    /**
     * [text], pasted, as a document: the one it carries where it is a link of Formatico's, and else itself. Nothing to
     * open where it is null or blank; too large where it has more bytes than a file may, [maxFileSize].
     */
    suspend fun paste(text: String?): Opening {
        if (text.isNullOrBlank()) return Opening.NothingToPaste
        if (utf8Size(text) > maxFileSize) return Opening.PasteTooLarge(maxFileSize)
        return link(text) ?: Opening.Opened(record(OpenedDocument(name = null, text = text, format = detect(text, null))))
    }

    /**
     * The document a link of Formatico's carries, as a file of its name is opened: [address] is a whole link, at
     * Formatico's site or where links are made here ([linkSite]), or the part of one from its `#`, as a browser's
     * address gives it. A broken link where it cannot be read back; null where [address] is no link of Formatico's.
     */
    suspend fun link(address: String): Opening? = when (val reading = withContext(reader) { ShareLinks.read(address, linkSite()) }) {
        LinkReading.NotALink -> null
        LinkReading.Broken -> Opening.BrokenLink
        is LinkReading.Read -> opened(reading.document)
        is LinkReading.Encrypted -> encrypted(reading, address)
    }

    /** The document an encrypted link shares, fetched from Formatico's server and decrypted off the screen's thread. */
    private suspend fun encrypted(link: LinkReading.Encrypted, address: String): Opening {
        val shares = online?.shares ?: return Opening.SharedLinkUnreachable(address)
        fetchingJob = currentCoroutineContext().job
        fetchingNow.value = true
        val opening = try {
            withContext(reader) { shares.open(link) }
        } finally {
            fetchingJob = null
            fetchingNow.value = false
        }
        return when (opening) {
            is SharedOpening.Opened -> opened(opening.document)
            SharedOpening.Gone -> Opening.SharedLinkGone
            SharedOpening.Unreachable -> Opening.SharedLinkUnreachable(address)
            SharedOpening.Broken -> Opening.BrokenLink
        }
    }

    /**
     * The document at the web [address], as a file of the name its address gives it is opened; refused where it is
     * not a public web address, is not there, is larger than [maxFileSize], is no text, or cannot be reached.
     */
    suspend fun address(address: String): Opening {
        val addresses = online?.addresses ?: return Opening.AddressRefused(AddressReading.Unreachable)
        return when (val reading = addresses.open(address, maxFileSize)) {
            is AddressReading.Read -> opened(SharedDocument(reading.text, reading.name))
            else -> Opening.AddressRefused(reading)
        }
    }

    private suspend fun opened(document: SharedDocument): Opening.Opened {
        val (text, name) = document
        return Opening.Opened(record(OpenedDocument(name = name, text = text, format = detect(text, name))))
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

/** The first character codes that UTF-8 writes in two bytes and in three. */
private const val TWO_BYTES_FROM = 0x80
private const val THREE_BYTES_FROM = 0x800

/** The bytes UTF-8 writes a character in from [THREE_BYTES_FROM], and a surrogate pair, a character past 0xFFFF, in. */
private const val THREE_BYTES = 3
private const val PAIR_BYTES = 4

/** How many bytes [text] has in UTF-8, as a file of it would, counted without writing it out. */
internal fun utf8Size(text: String): Long {
    var size = 0L
    var index = 0
    while (index < text.length) {
        val char = text[index]
        size += when {
            char.code < TWO_BYTES_FROM -> 1

            char.code < THREE_BYTES_FROM -> 2

            char.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate() -> {
                index++
                PAIR_BYTES
            }

            else -> THREE_BYTES
        }
        index++
    }
    return size
}
