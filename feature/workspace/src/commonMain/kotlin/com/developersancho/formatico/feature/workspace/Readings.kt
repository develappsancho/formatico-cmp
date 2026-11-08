package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.pantrixui.foundation.platform.PlatformType

/**
 * What the open documents of [workbench] were read as, kept while they are open: a workspace made again for a
 * document, as one shown again after Start, finds it read and reads it again only where its text or its format has
 * changed. A large document is read once, not each time its tab comes back.
 *
 * What is kept comes to no more than [budget] characters of text: a document's reading weighs some fifteen to thirty
 * times its text, and the one used longest ago is let go first, to be read again when it is shown again.
 */
internal class Readings(private val workbench: Workbench, private val budget: Long = Long.MAX_VALUE) {
    private class Kept(val text: String, val format: Format?, val header: CsvHeader, val reading: Reading, val stats: TextStats)

    // In the order they were last used, the one used longest ago first.
    private val kept = LinkedHashMap<Long, Kept>()

    /** What the document whose id is [id] was read as, where it was read with [text] in [format], its columns named as [header] says. */
    fun of(id: Long, text: String, format: Format?, header: CsvHeader = CsvHeader.FirstRow): Pair<Reading, TextStats>? {
        forgetClosed()
        val found = kept[id] ?: return null
        // The same text is mostly the same string: comparing it whole is for the rest.
        val sameText = found.text === text || found.text == text
        if (found.format != format || found.header != header || !sameText) return null
        kept.remove(id)
        kept[id] = found
        return found.reading to found.stats
    }

    /** Keeps what the document whose id is [id] was read as, with [text] in [format], its columns named as [header] says. */
    fun keep(id: Long, text: String, format: Format?, read: Pair<Reading, TextStats>, header: CsvHeader = CsvHeader.FirstRow) {
        forgetClosed()
        kept.remove(id)
        kept[id] = Kept(text, format, header, read.first, read.second)
        var total = kept.values.sumOf { it.text.length.toLong() }
        val oldest = kept.entries.iterator()
        while (total > budget && oldest.hasNext()) {
            val (other, reading) = oldest.next()
            if (other == id) continue
            total -= reading.text.length
            oldest.remove()
        }
    }

    private fun forgetClosed() {
        val open = workbench.state.value.documents.mapTo(HashSet()) { it.id }
        kept.keys.retainAll(open)
    }
}

/**
 * How many characters of text the readings [Readings] keeps may come to on [platform]: one large document's on a phone,
 * a tablet and the web, whose files go to 10 MB, and on a desktop one of 50 MB, besides the small ones.
 */
internal fun readingsBudget(platform: PlatformType): Long = if (platform == PlatformType.Desktop) DESKTOP_READINGS else READINGS

// A 50 MB document's text and some small ones; a 10 MB document's text and some small ones.
private const val DESKTOP_READINGS = 60_000_000L
private const val READINGS = 12_000_000L
