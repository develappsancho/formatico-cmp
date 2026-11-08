package com.developersancho.formatico.feature.start

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Instant

/** The documents opened on this device, the newest first. */
interface DocumentHistory {
    /** What Recent lists. */
    val documents: StateFlow<List<RecentDocument>>

    /** Puts [document] at the top, opened [at] that moment: a document already in the history moves there. */
    fun record(document: OpenedDocument, at: Instant)

    /** The document whose id is [id], or null where the history has no such document. */
    fun document(id: Long): OpenedDocument?

    /** Forgets every document. */
    fun clear()
}

/**
 * A history kept while the app runs: until a store on the device is written, nothing outlives the app.
 *
 * @param limit how many documents it keeps; the oldest goes first.
 */
class SessionDocumentHistory(private val limit: Int = DEFAULT_LIMIT) : DocumentHistory {
    private class Entry(val recent: RecentDocument, val document: OpenedDocument)

    private val entries = MutableStateFlow<List<Entry>>(emptyList())
    private val listed = MutableStateFlow<List<RecentDocument>>(emptyList())
    private var nextId = 1L

    override val documents: StateFlow<List<RecentDocument>> = listed.asStateFlow()

    override fun record(document: OpenedDocument, at: Instant) {
        entries.update { current ->
            val same = current.firstOrNull { it.document == document }
            val id = same?.recent?.id ?: nextId++
            val recent = RecentDocument(
                id = id,
                name = document.name,
                format = document.format,
                size = document.text.encodeToByteArray().size.toLong(),
                editedAt = at
            )
            (listOf(Entry(recent, document)) + current.filter { it !== same }).take(limit)
        }
        listed.value = entries.value.map { it.recent }
    }

    override fun document(id: Long): OpenedDocument? = entries.value.firstOrNull { it.recent.id == id }?.document

    override fun clear() {
        entries.value = emptyList()
        listed.value = emptyList()
    }

    private companion object {
        const val DEFAULT_LIMIT = 20
    }
}
