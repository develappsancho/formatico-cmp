package com.developersancho.formatico.feature.start

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.storage.SmallStore
import com.developersancho.formatico.core.storage.TextStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlin.time.Instant

/** The documents opened on this device, the newest first. */
interface DocumentHistory {
    /** What Recent lists. */
    val documents: StateFlow<List<RecentDocument>>

    /**
     * Puts [document] at the top, opened [at] that moment: a document already in the history moves there.
     *
     * @return its id in the history.
     */
    suspend fun record(document: OpenedDocument, at: Instant): Long

    /** The document whose id is [id] was changed to [text] [at] that moment: it moves to the top, as edited. */
    suspend fun edit(id: Long, text: String, at: Instant)

    /** The document whose id is [id], or null where the history has no such document. */
    suspend fun document(id: Long): OpenedDocument?

    /** Forgets every document. */
    suspend fun clear()
}

/**
 * The history kept on this device: Recent's list in [small], each document's text in [texts] under `history-<id>`,
 * read again when the document is opened from Recent. What changes is written in [scope], one write after another in
 * the order the changes were made, so that opening a large document does not wait for its text to be written.
 *
 * @param limit how many documents it keeps; the oldest goes first, and its text with it.
 */
class StoredDocumentHistory(
    private val small: SmallStore,
    private val texts: TextStore,
    scope: CoroutineScope,
    private val limit: Int = DEFAULT_LIMIT
) : DocumentHistory {
    private val lock = Mutex()
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private var kept: List<KeptRecent>? = null
    private var nextId = 1L

    // The texts recorded since the app opened: what has not been written yet is read from here.
    private val recorded = mutableMapOf<Long, String>()
    private val listed = MutableStateFlow<List<RecentDocument>>(emptyList())

    override val documents: StateFlow<List<RecentDocument>> = listed.asStateFlow()

    init {
        scope.launch { lock.withLock { loaded() } }
        scope.launch {
            // A write that fails leaves the history as the device had it; the app goes on.
            for (write in writes) runCatching { write() }
        }
    }

    override suspend fun record(document: OpenedDocument, at: Instant): Long = lock.withLock {
        val entries = loaded()
        val size = document.text.encodeToByteArray().size.toLong()
        val hash = document.text.hashCode()
        val same = entries.firstOrNull {
            it.name == document.name && it.format == document.format?.name && it.size == size && it.hash == hash
        }
        val entry = KeptRecent(
            id = same?.id ?: nextId++,
            name = document.name,
            format = document.format?.name,
            size = size,
            hash = hash,
            at = at.toEpochMilliseconds(),
            edited = false
        )
        recorded[entry.id] = document.text
        keep(listOf(entry) + entries.filter { it.id != entry.id }, text = entry.id to document.text)
        entry.id
    }

    override suspend fun edit(id: Long, text: String, at: Instant) {
        lock.withLock {
            val entries = loaded()
            val entry = entries.firstOrNull { it.id == id } ?: return
            val changed = entry.copy(
                size = text.encodeToByteArray().size.toLong(),
                hash = text.hashCode(),
                at = at.toEpochMilliseconds(),
                edited = true
            )
            recorded[id] = text
            keep(listOf(changed) + entries.filter { it.id != id }, text = id to text)
        }
    }

    override suspend fun document(id: Long): OpenedDocument? {
        val (entry, text) = lock.withLock {
            val entry = loaded().firstOrNull { it.id == id } ?: return null
            entry to recorded[id]
        }
        val read = text ?: texts.read(entry.textName) ?: return null
        return OpenedDocument(name = entry.name, text = read, format = entry.format(), historyId = id)
    }

    override suspend fun clear() {
        lock.withLock {
            val forgotten = loaded()
            recorded.clear()
            kept = emptyList()
            listed.value = emptyList()
            writes.send {
                forgotten.forEach { texts.delete(it.textName) }
                small.delete(KEY)
            }
        }
    }

    /** Keeps [entries], at most [limit] of them, and writes them with [text]; what falls off the end is forgotten. */
    private suspend fun keep(entries: List<KeptRecent>, text: Pair<Long, String>) {
        val shown = entries.take(limit)
        val dropped = entries.drop(limit)
        dropped.forEach { recorded.remove(it.id) }
        kept = shown
        listed.value = shown.map(KeptRecent::recent)
        writes.send {
            texts.write(textName(text.first), text.second)
            dropped.forEach { texts.delete(it.textName) }
            small.put(KEY, shown, ListSerializer(KeptRecent.serializer()))
        }
    }

    /** The history as the device has it, read once. */
    private suspend fun loaded(): List<KeptRecent> = kept ?: run {
        val read = small.get(KEY, ListSerializer(KeptRecent.serializer())).orEmpty()
        nextId = (read.maxOfOrNull { it.id } ?: 0L) + 1
        kept = read
        listed.value = read.map(KeptRecent::recent)
        read
    }

    private companion object {
        const val DEFAULT_LIMIT = 20
        const val KEY = "history"
    }
}

/**
 * A document of the history as it is kept: its text is kept apart, by [id], and told from another of the same name and
 * size by its [hash].
 */
@Serializable
private data class KeptRecent(
    val id: Long,
    val name: String?,
    val format: String?,
    val size: Long,
    val hash: Int,
    val at: Long,
    val edited: Boolean
) {
    val textName: String get() = textName(id)

    val recent: RecentDocument
        get() = RecentDocument(
            id = id,
            name = name,
            format = format(),
            size = size,
            at = Instant.fromEpochMilliseconds(at),
            edited = edited
        )

    fun format(): Format? = Format.entries.firstOrNull { it.name == format }
}

private fun textName(id: Long) = "history-$id"
