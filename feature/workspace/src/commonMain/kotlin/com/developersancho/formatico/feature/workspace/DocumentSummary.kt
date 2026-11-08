package com.developersancho.formatico.feature.workspace

/** The most of something a document holds, and the first value that holds it: "3 levels", at `$.items[0].sku`. */
internal data class Extreme(val amount: Int, val node: ValueNode)

/** The kinds of value a document's previews find. */
internal enum class FoundKind { Dates, Pictures, Links, Colours }

/** The values of a document whose preview is of [kind], in the order the tree shows them. */
internal data class Found(val kind: FoundKind, val nodes: List<ValueNode>)

/** A number [written] as the document writes it, which a tool that reads it as a number writes as [read]. */
internal data class Respelled(val written: String, val read: String)

/** An array of [rows] objects that all have the same [keys]: it reads as a table. */
internal data class TableNote(val node: ValueNode, val rows: Int, val keys: Int)

/**
 * What a document's values come to, as its Summary shows them.
 *
 * @property values how many values it holds, the objects and the arrays among them, the root too.
 * @property keys how many members its objects have, and [repeatedKeys] of them a key their object has before.
 * @property deepest the value the most steps from the root, or null for a document of one value.
 * @property kinds how many values there are of each kind, the most first.
 */
internal data class DocumentSummary(
    val values: Int,
    val objects: Int,
    val arrays: Int,
    val keys: Int,
    val repeatedKeys: Int,
    val deepest: Extreme?,
    val longestArray: Extreme?,
    val longestText: Extreme?,
    val numbers: Int,
    val respelled: List<Respelled>,
    val kinds: List<Pair<ValueKind, Int>>,
    val found: List<Found>,
    val tables: List<TableNote>
)

/** How many bytes a document's text is, minified where its format has a minified form, and either of them gzipped. */
internal data class SizeSummary(val text: Long, val minified: Long?, val compressed: Long?)

/** The order kinds of the same count are listed in: as the Summary board lists them. */
private val KindOrder = listOf(
    ValueKind.String,
    ValueKind.Object,
    ValueKind.Number,
    ValueKind.Array,
    ValueKind.Boolean,
    ValueKind.Null,
    ValueKind.Alias
)

/** The largest integer a double holds exactly: past it, a tool that reads a number as one loses digits. */
private const val EXACT_INTEGER = 9_007_199_254_740_992.0

/** What [root] and every value in it come to. */
internal fun summaryOf(root: ValueNode): DocumentSummary {
    val all = mutableListOf<ValueNode>()

    fun walk(node: ValueNode) {
        all += node
        node.children.forEach(::walk)
    }
    walk(root)
    val objects = all.filter { it.kind == ValueKind.Object }
    val arrays = all.filter { it.kind == ValueKind.Array }
    val numbers = all.filter { it.kind == ValueKind.Number }
    return DocumentSummary(
        values = all.size,
        objects = objects.size,
        arrays = arrays.size,
        keys = objects.sumOf { it.children.size },
        repeatedKeys = objects.sumOf { node -> node.children.size - node.children.map { it.name }.distinct().size },
        deepest = all.maxByOrNull { it.path.size }?.takeIf { it.path.isNotEmpty() }?.let { Extreme(it.path.size, it) },
        longestArray = arrays.maxByOrNull { it.children.size }?.let { Extreme(it.children.size, it) },
        longestText = all.filter { it.kind == ValueKind.String }
            .maxByOrNull { (it.value ?: it.text).length }
            ?.let { Extreme((it.value ?: it.text).length, it) },
        numbers = numbers.size,
        respelled = numbers.mapNotNull { node -> readAsNumber(node.text)?.let { Respelled(node.text, it) } },
        kinds = all.groupingBy { it.kind }.eachCount().toList()
            .sortedWith(compareByDescending<Pair<ValueKind, Int>> { it.second }.thenBy { KindOrder.indexOf(it.first) }),
        found = FoundKind.entries.mapNotNull { kind ->
            all.filter { it.preview?.let(::foundKindOf) == kind }.takeIf { it.isNotEmpty() }?.let { Found(kind, it) }
        },
        tables = arrays.mapNotNull(::tableNoteOf)
    )
}

/** The kind of value [preview] finds. */
private fun foundKindOf(preview: ValuePreview): FoundKind = when (preview) {
    is ValuePreview.Moment, is ValuePreview.Day -> FoundKind.Dates
    is ValuePreview.Picture -> FoundKind.Pictures
    is ValuePreview.Link -> FoundKind.Links
    is ValuePreview.Colour -> FoundKind.Colours
}

/** [array] as a table where it holds two objects or more that all have the same keys, or null. */
private fun tableNoteOf(array: ValueNode): TableNote? {
    val rows = array.children
    if (rows.size < 2 || rows.any { it.kind != ValueKind.Object }) return null
    val keys = rows.first().children.map { it.name }.toSet()
    if (keys.isEmpty() || rows.any { row -> row.children.map { it.name }.toSet() != keys }) return null
    return TableNote(array, rows.size, keys.size)
}

/**
 * How a tool that reads [written] as a double writes it back, as JavaScript writes a number: `129.90` as `129.9`, `1E3`
 * as `1000`; or null where it writes it as [written] does, or does not read it as a number at all.
 */
internal fun readAsNumber(written: String): String? {
    val number = written.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
    val read = if (number == kotlin.math.floor(number) && kotlin.math.abs(number) < EXACT_INTEGER) {
        number.toLong().toString()
    } else {
        number.toString().replace("E", "e").replace(".0e", "e")
    }
    return read.takeIf { it != written }
}

/** [document]'s text minified, where its format has a minified form and it can be made: what is gzipped in its place. */
internal fun minifiedOf(document: OpenDocument): String? = (edited(document, DocumentEdit.Minify) as? Edited.Text)?.text

/** How many bytes [document]'s text and its [minified] text are, in UTF-8; their gzipped size is measured apart. */
internal fun sizesOf(document: OpenDocument, minified: String?): SizeSummary =
    SizeSummary(
        text = document.text.encodeToByteArray().size.toLong(),
        minified = minified?.encodeToByteArray()?.size?.toLong(),
        compressed = null
    )
