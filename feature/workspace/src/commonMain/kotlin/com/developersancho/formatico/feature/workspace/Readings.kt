package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format

/**
 * What the open documents of [workbench] were read as, kept while they are open: a workspace made again for a
 * document, as one shown again after Start, finds it read and reads it again only where its text or its format has
 * changed. A large document is read once, not each time its tab comes back.
 */
internal class Readings(private val workbench: Workbench) {
    private class Kept(val text: String, val format: Format?, val reading: Reading, val stats: TextStats)

    private val kept = mutableMapOf<Long, Kept>()

    /** What the document whose id is [id] was read as, where it was read with [text] in [format]. */
    fun of(id: Long, text: String, format: Format?): Pair<Reading, TextStats>? {
        forgetClosed()
        val found = kept[id] ?: return null
        // The same text is mostly the same string: comparing it whole is for the rest.
        return if (found.format == format && (found.text === text || found.text == text)) found.reading to found.stats else null
    }

    /** Keeps what the document whose id is [id] was read as, with [text] in [format]. */
    fun keep(id: Long, text: String, format: Format?, read: Pair<Reading, TextStats>) {
        forgetClosed()
        kept[id] = Kept(text, format, read.first, read.second)
    }

    private fun forgetClosed() {
        val open = workbench.state.value.documents.mapTo(HashSet()) { it.id }
        kept.keys.retainAll(open)
    }
}
