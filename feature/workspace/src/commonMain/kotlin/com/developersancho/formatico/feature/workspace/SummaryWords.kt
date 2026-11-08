package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import com.developersancho.formatico.core.format.json.jsonPath
import com.developersancho.formatico.feature.workspace.strings.summaryStrings
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.chart.metergroup.MeterItem
import com.pantrixui.foundation.theme.PantrixTheme

/** A tile of the Summary: what it counts, how much, and a word on it, [mono] where that word is a path. */
internal data class SummaryTile(val label: String, val value: String, val detail: String, val mono: Boolean = false)

/** A row of what the values were found to hold: how many of a kind, and their paths. */
internal data class FoundRow(val found: Found, val label: String, val paths: String)

/** A note on the document, and the array it says reads as a table, where it says that. */
internal data class SummaryNote(val text: String, val table: TableNote? = null)

/** The most notes on spellings a note names before it counts the rest. */
private const val NAMED_SPELLINGS = 3

/** The three tiles of the text's size: as it is, minified, and gzipped. */
@Composable
internal fun sizeTiles(stats: TextStats, sizes: SizeSummary): List<SummaryTile> {
    val strings = PantrixTheme.strings
    val layout = listOfNotNull(
        workspaceStrings.lines(stats.lines, stats.lines),
        workspaceStrings.encoding,
        stats.layout.lineEnding?.name?.uppercase()
    ).joinToString(" · ")
    val minified = sizes.minified
    val compressed = sizes.compressed
    return listOf(
        SummaryTile(
            summaryStrings.text,
            strings.fileSize(sizes.text),
            summaryStrings.textDetail(layout)
        ),
        if (minified == null) {
            SummaryTile(
                summaryStrings.minified,
                summaryStrings.none,
                summaryStrings.notMinified
            )
        } else {
            val share = strings.percent(if (sizes.text > 0) minified.toFloat() / sizes.text else 1f)
            SummaryTile(
                summaryStrings.minified,
                strings.fileSize(minified),
                summaryStrings.minifiedShare(share)
            )
        },
        SummaryTile(
            label = summaryStrings.compressed,
            value = compressed?.let(strings::fileSize) ?: "…",
            detail = when {
                compressed == null -> summaryStrings.measuring
                minified != null -> summaryStrings.compressedMinified
                else -> summaryStrings.compressedText
            }
        )
    )
}

/** The tiles of the values: six of them, or on a [phone] the first three in fewer words. */
@Composable
internal fun valueTiles(summary: DocumentSummary, phone: Boolean): List<SummaryTile> {
    val objects = summaryStrings.objects(summary.objects, summary.objects)
    val arrays = summaryStrings.arrays(summary.arrays, summary.arrays)
    val repeated = summary.repeatedKeys
    val keys = when {
        repeated == 0 && phone -> summaryStrings.keysNoneShort
        repeated == 0 -> summaryStrings.keysNone
        phone -> summaryStrings.keysRepeatedShort(repeated, repeated)
        else -> summaryStrings.keysRepeated(repeated, repeated)
    }
    val depth = summary.deepest?.amount ?: 0
    val values = SummaryTile(
        summaryStrings.values,
        "${summary.values}",
        if (phone) objects else summaryStrings.valuesIn(objects, arrays)
    )
    val keyTile = SummaryTile(summaryStrings.keys, "${summary.keys}", keys)
    if (phone) {
        return listOf(
            values,
            keyTile,
            SummaryTile(summaryStrings.deepest, "$depth", summaryStrings.levelsWord)
        )
    }
    val none = summaryStrings.none
    return listOf(
        values,
        keyTile,
        // A document of one value is no step deep: its value is the root.
        summary.deepest?.let { extremeTile(summaryStrings.deepest, it, summaryStrings::levels, none, none) }
            ?: SummaryTile(
                summaryStrings.deepest,
                summaryStrings.levels(0, 0),
                "$",
                mono = true
            ),
        extremeTile(
            summaryStrings.longestArray,
            summary.longestArray,
            summaryStrings::items,
            none,
            summaryStrings.noArray
        ),
        extremeTile(
            summaryStrings.longestText,
            summary.longestText,
            summaryStrings::characters,
            none,
            summaryStrings.noText
        ),
        SummaryTile(summaryStrings.numbers, "${summary.numbers}", summaryStrings.numbersKept)
    )
}

/** A tile of the most of something, with the path of the first value that holds it, or [none] and [missing]. */
@Composable
private fun extremeTile(
    label: String,
    extreme: Extreme?,
    amount: (count: Int, first: Int) -> String,
    none: String,
    missing: String
): SummaryTile = if (extreme == null) {
    SummaryTile(label, none, missing)
} else {
    SummaryTile(
        label,
        amount(extreme.amount, extreme.amount),
        jsonPath(extreme.node.path),
        mono = true
    )
}

/** How many values there are of each kind, as the meter of kinds shows them, the most first. */
@Composable
internal fun kindItems(summary: DocumentSummary): List<MeterItem> = summary.kinds.map { (kind, count) ->
    val name = when (kind) {
        ValueKind.String -> summaryStrings.kindText
        ValueKind.Object -> summaryStrings.kindObject
        ValueKind.Number -> summaryStrings.kindNumber
        ValueKind.Array -> summaryStrings.kindArray
        ValueKind.Boolean -> summaryStrings.kindBoolean
        ValueKind.Null -> summaryStrings.kindNull
        ValueKind.Alias -> summaryStrings.kindAlias
    }
    MeterItem(key = kind.name.lowercase(), name = name, value = count)
}

/** What the meter of kinds is called: "The 32 values by kind". */
@Composable
internal fun kindsDescription(summary: DocumentSummary): String =
    summaryStrings.kinds(summary.values, summary.values)

/** The rows of what the values were found to hold: "2 dates", and where they are. */
@Composable
internal fun foundRows(summary: DocumentSummary): List<FoundRow> = summary.found.map { found ->
    val count = found.nodes.size
    val label = when (found.kind) {
        FoundKind.Dates -> summaryStrings.dates(count, count)
        FoundKind.Pictures -> summaryStrings.pictures(count, count)
        FoundKind.Links -> summaryStrings.links(count, count)
        FoundKind.Colours -> summaryStrings.colours(count, count)
    }
    FoundRow(found, label, found.nodes.joinToString(", ") { jsonPath(it.path) })
}

/** The notes on the document: the numbers whose spelling a tool would change, and each array that reads as a table. */
@Composable
internal fun summaryNotes(summary: DocumentSummary): List<SummaryNote> {
    val and = summaryStrings.and
    val respelled = summary.respelled
    val spelling = if (respelled.isEmpty()) {
        null
    } else {
        val more = respelled.size - NAMED_SPELLINGS
        val moreWords = if (more > 0) summaryStrings.more(more, more) else null

        fun list(words: List<String>) = listed(words.take(NAMED_SPELLINGS) + listOfNotNull(moreWords), and)
        SummaryNote(
            summaryStrings.respelled(
                respelled.size,
                list(respelled.map { it.written }),
                list(respelled.map { it.read })
            )
        )
    }
    val tables = summary.tables.map { table ->
        SummaryNote(
            summaryStrings.tableNote(
                jsonPath(table.node.path),
                summaryStrings.objects(table.rows, table.rows),
                summaryStrings.keysCount(table.keys, table.keys)
            ),
            table
        )
    }
    return listOfNotNull(spelling) + tables
}

/** [words] as a sentence lists them: "a", "a and b", "a, b and c". */
internal fun listed(words: List<String>, and: String): String = when (words.size) {
    0 -> ""
    1 -> words.single()
    else -> words.dropLast(1).joinToString(", ") + and + words.last()
}

/** The summary as text, a section to a paragraph, as "Copy the summary as text" copies it; null until it is worked out. */
@Composable
internal fun summaryText(state: WorkspaceState): String? {
    val summary = state.summary ?: return null
    val sizes = state.sizes ?: return null
    val stats = state.stats ?: return null

    fun line(tile: SummaryTile) = "${tile.label}: ${tile.value} (${tile.detail})"
    val sections = listOf(
        summaryStrings.titleSize to sizeTiles(stats, sizes).map(::line),
        summaryStrings.titleValues to valueTiles(summary, phone = false).map(::line) +
            kindItems(summary).joinToString(" · ") { "${it.name} ${it.value}" },
        summaryStrings.titleFound to foundRows(summary).map { "${it.label}: ${it.paths}" },
        summaryStrings.titleNotes to summaryNotes(summary).map { it.text }
    )
    return sections.filter {
        it.second.isNotEmpty()
    }.joinToString("\n\n") { (title, lines) -> (listOf(title) + lines).joinToString("\n") } +
        "\n"
}
