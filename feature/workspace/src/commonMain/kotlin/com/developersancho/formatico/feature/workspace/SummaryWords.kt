package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import com.developersancho.formatico.core.format.json.jsonPath
import com.pantrixui.components.chart.metergroup.MeterItem
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.summary_and
import formatico.feature.workspace.generated.resources.summary_arrays
import formatico.feature.workspace.generated.resources.summary_characters
import formatico.feature.workspace.generated.resources.summary_colours
import formatico.feature.workspace.generated.resources.summary_compressed
import formatico.feature.workspace.generated.resources.summary_compressed_minified
import formatico.feature.workspace.generated.resources.summary_compressed_text
import formatico.feature.workspace.generated.resources.summary_dates
import formatico.feature.workspace.generated.resources.summary_deepest
import formatico.feature.workspace.generated.resources.summary_items
import formatico.feature.workspace.generated.resources.summary_keys
import formatico.feature.workspace.generated.resources.summary_keys_count
import formatico.feature.workspace.generated.resources.summary_keys_none
import formatico.feature.workspace.generated.resources.summary_keys_none_short
import formatico.feature.workspace.generated.resources.summary_keys_repeated
import formatico.feature.workspace.generated.resources.summary_keys_repeated_short
import formatico.feature.workspace.generated.resources.summary_kind_alias
import formatico.feature.workspace.generated.resources.summary_kind_array
import formatico.feature.workspace.generated.resources.summary_kind_boolean
import formatico.feature.workspace.generated.resources.summary_kind_null
import formatico.feature.workspace.generated.resources.summary_kind_number
import formatico.feature.workspace.generated.resources.summary_kind_object
import formatico.feature.workspace.generated.resources.summary_kind_text
import formatico.feature.workspace.generated.resources.summary_kinds
import formatico.feature.workspace.generated.resources.summary_levels
import formatico.feature.workspace.generated.resources.summary_levels_word
import formatico.feature.workspace.generated.resources.summary_links
import formatico.feature.workspace.generated.resources.summary_longest_array
import formatico.feature.workspace.generated.resources.summary_longest_text
import formatico.feature.workspace.generated.resources.summary_measuring
import formatico.feature.workspace.generated.resources.summary_minified
import formatico.feature.workspace.generated.resources.summary_minified_share
import formatico.feature.workspace.generated.resources.summary_more
import formatico.feature.workspace.generated.resources.summary_no_array
import formatico.feature.workspace.generated.resources.summary_no_text
import formatico.feature.workspace.generated.resources.summary_none
import formatico.feature.workspace.generated.resources.summary_not_minified
import formatico.feature.workspace.generated.resources.summary_numbers
import formatico.feature.workspace.generated.resources.summary_numbers_kept
import formatico.feature.workspace.generated.resources.summary_objects
import formatico.feature.workspace.generated.resources.summary_pictures
import formatico.feature.workspace.generated.resources.summary_respelled
import formatico.feature.workspace.generated.resources.summary_table_note
import formatico.feature.workspace.generated.resources.summary_text
import formatico.feature.workspace.generated.resources.summary_text_detail
import formatico.feature.workspace.generated.resources.summary_title_found
import formatico.feature.workspace.generated.resources.summary_title_notes
import formatico.feature.workspace.generated.resources.summary_title_size
import formatico.feature.workspace.generated.resources.summary_title_values
import formatico.feature.workspace.generated.resources.summary_values
import formatico.feature.workspace.generated.resources.summary_values_in
import formatico.feature.workspace.generated.resources.workspace_encoding
import formatico.feature.workspace.generated.resources.workspace_lines
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

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
        pluralStringResource(Res.plurals.workspace_lines, stats.lines, stats.lines),
        stringResource(Res.string.workspace_encoding),
        stats.layout.lineEnding?.name?.uppercase()
    ).joinToString(" · ")
    val minified = sizes.minified
    val compressed = sizes.compressed
    return listOf(
        SummaryTile(
            stringResource(Res.string.summary_text),
            strings.fileSize(sizes.text),
            stringResource(Res.string.summary_text_detail, layout)
        ),
        if (minified == null) {
            SummaryTile(
                stringResource(Res.string.summary_minified),
                stringResource(Res.string.summary_none),
                stringResource(Res.string.summary_not_minified)
            )
        } else {
            val share = strings.percent(if (sizes.text > 0) minified.toFloat() / sizes.text else 1f)
            SummaryTile(
                stringResource(Res.string.summary_minified),
                strings.fileSize(minified),
                stringResource(Res.string.summary_minified_share, share)
            )
        },
        SummaryTile(
            label = stringResource(Res.string.summary_compressed),
            value = compressed?.let(strings::fileSize) ?: "…",
            detail = when {
                compressed == null -> stringResource(Res.string.summary_measuring)
                minified != null -> stringResource(Res.string.summary_compressed_minified)
                else -> stringResource(Res.string.summary_compressed_text)
            }
        )
    )
}

/** The tiles of the values: six of them, or on a [phone] the first three in fewer words. */
@Composable
internal fun valueTiles(summary: DocumentSummary, phone: Boolean): List<SummaryTile> {
    val objects = pluralStringResource(Res.plurals.summary_objects, summary.objects, summary.objects)
    val arrays = pluralStringResource(Res.plurals.summary_arrays, summary.arrays, summary.arrays)
    val repeated = summary.repeatedKeys
    val keys = when {
        repeated == 0 && phone -> stringResource(Res.string.summary_keys_none_short)
        repeated == 0 -> stringResource(Res.string.summary_keys_none)
        phone -> pluralStringResource(Res.plurals.summary_keys_repeated_short, repeated, repeated)
        else -> pluralStringResource(Res.plurals.summary_keys_repeated, repeated, repeated)
    }
    val depth = summary.deepest?.amount ?: 0
    val values = SummaryTile(
        stringResource(Res.string.summary_values),
        "${summary.values}",
        if (phone) objects else stringResource(Res.string.summary_values_in, objects, arrays)
    )
    val keyTile = SummaryTile(stringResource(Res.string.summary_keys), "${summary.keys}", keys)
    if (phone) {
        return listOf(
            values,
            keyTile,
            SummaryTile(stringResource(Res.string.summary_deepest), "$depth", stringResource(Res.string.summary_levels_word))
        )
    }
    val none = stringResource(Res.string.summary_none)
    return listOf(
        values,
        keyTile,
        // A document of one value is no step deep: its value is the root.
        summary.deepest?.let { extremeTile(Res.string.summary_deepest, it, Res.plurals.summary_levels, none, none) }
            ?: SummaryTile(
                stringResource(Res.string.summary_deepest),
                pluralStringResource(Res.plurals.summary_levels, 0, 0),
                "$",
                mono = true
            ),
        extremeTile(
            Res.string.summary_longest_array,
            summary.longestArray,
            Res.plurals.summary_items,
            none,
            stringResource(Res.string.summary_no_array)
        ),
        extremeTile(
            Res.string.summary_longest_text,
            summary.longestText,
            Res.plurals.summary_characters,
            none,
            stringResource(Res.string.summary_no_text)
        ),
        SummaryTile(stringResource(Res.string.summary_numbers), "${summary.numbers}", stringResource(Res.string.summary_numbers_kept))
    )
}

/** A tile of the most of something, with the path of the first value that holds it, or [none] and [missing]. */
@Composable
private fun extremeTile(
    label: StringResource,
    extreme: Extreme?,
    amount: PluralStringResource,
    none: String,
    missing: String
): SummaryTile = if (extreme == null) {
    SummaryTile(stringResource(label), none, missing)
} else {
    SummaryTile(
        stringResource(label),
        pluralStringResource(amount, extreme.amount, extreme.amount),
        jsonPath(extreme.node.path),
        mono = true
    )
}

/** How many values there are of each kind, as the meter of kinds shows them, the most first. */
@Composable
internal fun kindItems(summary: DocumentSummary): List<MeterItem> = summary.kinds.map { (kind, count) ->
    val name = when (kind) {
        ValueKind.String -> Res.string.summary_kind_text
        ValueKind.Object -> Res.string.summary_kind_object
        ValueKind.Number -> Res.string.summary_kind_number
        ValueKind.Array -> Res.string.summary_kind_array
        ValueKind.Boolean -> Res.string.summary_kind_boolean
        ValueKind.Null -> Res.string.summary_kind_null
        ValueKind.Alias -> Res.string.summary_kind_alias
    }
    MeterItem(key = kind.name.lowercase(), name = stringResource(name), value = count)
}

/** What the meter of kinds is called: "The 32 values by kind". */
@Composable
internal fun kindsDescription(summary: DocumentSummary): String =
    pluralStringResource(Res.plurals.summary_kinds, summary.values, summary.values)

/** The rows of what the values were found to hold: "2 dates", and where they are. */
@Composable
internal fun foundRows(summary: DocumentSummary): List<FoundRow> = summary.found.map { found ->
    val count = found.nodes.size
    val label = when (found.kind) {
        FoundKind.Dates -> pluralStringResource(Res.plurals.summary_dates, count, count)
        FoundKind.Pictures -> pluralStringResource(Res.plurals.summary_pictures, count, count)
        FoundKind.Links -> pluralStringResource(Res.plurals.summary_links, count, count)
        FoundKind.Colours -> pluralStringResource(Res.plurals.summary_colours, count, count)
    }
    FoundRow(found, label, found.nodes.joinToString(", ") { jsonPath(it.path) })
}

/** The notes on the document: the numbers whose spelling a tool would change, and each array that reads as a table. */
@Composable
internal fun summaryNotes(summary: DocumentSummary): List<SummaryNote> {
    val and = stringResource(Res.string.summary_and)
    val respelled = summary.respelled
    val spelling = if (respelled.isEmpty()) {
        null
    } else {
        val more = respelled.size - NAMED_SPELLINGS
        val moreWords = if (more > 0) pluralStringResource(Res.plurals.summary_more, more, more) else null

        fun list(words: List<String>) = listed(words.take(NAMED_SPELLINGS) + listOfNotNull(moreWords), and)
        SummaryNote(
            pluralStringResource(
                Res.plurals.summary_respelled,
                respelled.size,
                list(respelled.map { it.written }),
                list(respelled.map { it.read })
            )
        )
    }
    val tables = summary.tables.map { table ->
        SummaryNote(
            stringResource(
                Res.string.summary_table_note,
                jsonPath(table.node.path),
                pluralStringResource(Res.plurals.summary_objects, table.rows, table.rows),
                pluralStringResource(Res.plurals.summary_keys_count, table.keys, table.keys)
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
        stringResource(Res.string.summary_title_size) to sizeTiles(stats, sizes).map(::line),
        stringResource(Res.string.summary_title_values) to valueTiles(summary, phone = false).map(::line) +
            kindItems(summary).joinToString(" · ") { "${it.name} ${it.value}" },
        stringResource(Res.string.summary_title_found) to foundRows(summary).map { "${it.label}: ${it.paths}" },
        stringResource(Res.string.summary_title_notes) to summaryNotes(summary).map { it.text }
    )
    return sections.filter {
        it.second.isNotEmpty()
    }.joinToString("\n\n") { (title, lines) -> (listOf(title) + lines).joinToString("\n") } +
        "\n"
}
