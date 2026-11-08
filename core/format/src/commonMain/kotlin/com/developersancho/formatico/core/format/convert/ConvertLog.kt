package com.developersancho.formatico.core.format.convert

/** The kinds of [Loss] a writer finds, in the order a reader is told of them. */
internal enum class Lost {
    Nulls,
    Untyped,
    Flattened,
    Emptied,
    Duplicates,
    Renamed,
    ItemsNamed,
    Moved,
    Dates,
    Unwritable,
    Respelled,
    OneLine
}

/** What a writer finds as it writes: each kind of loss with its first value and how many, and what it did. */
internal class ConvertLog(private val nulls: NullHandling) {
    private class Tally(val first: String, val written: String) {
        var count = 0
    }

    private val tallies = HashMap<Lost, Tally>()
    private var wrapped: String? = null
    private var quotedDates = 0
    private var quotedOthers = 0
    private val spelled = ArrayList<String>(2)
    private var spelledCount = 0
    private val nullLines = ArrayList<Int>()

    /** The lines, counted from nought, of the comments that mark where a null was left out. */
    val markedLines: List<Int> get() = nullLines

    /** A null was left out at [path], and the comment on the line [line], counted from nought, marks where. */
    fun leaveOut(path: String, line: Int) {
        lose(Lost.Nulls, path)
        nullLines += line
    }

    /** [kind] of loss at the value whose path is [path]; [written], what it was written as, where that is said. */
    fun lose(kind: Lost, path: String, written: String = "") {
        tallies.getOrPut(kind) { Tally(path.ifEmpty { ROOT_PATH }, written) }.count++
    }

    /** The document's value is put under [name]. */
    fun wrap(name: String) {
        wrapped = name
    }

    /** A string is written in quotes, as it would be read as another value: a date where [asDate] says so. */
    fun quoted(asDate: Boolean) {
        if (asDate) quotedDates++ else quotedOthers++
    }

    /** A number is written as it was, [written], where that is its own way of writing it. */
    fun kept(written: String) {
        if (!isSpelledOwnWay(written)) return
        if (spelled.size < 2) spelled += written
        spelledCount++
    }

    /** What was lost, each kind once, in the order a reader is told of them. */
    fun losses(): List<Loss> = buildList {
        wrapped?.let { add(Loss.Wrapped(it)) }
        Lost.entries.forEach { kind ->
            val tally = tallies[kind] ?: return@forEach
            add(tally.loss(kind))
        }
    }

    private fun Tally.loss(kind: Lost): Loss = when (kind) {
        Lost.Nulls -> Loss.Nulls(first, count, nulls)
        Lost.Untyped -> Loss.Untyped(first, count)
        Lost.Flattened -> Loss.Flattened(first, count)
        Lost.Emptied -> Loss.Emptied(first, count)
        Lost.Duplicates -> Loss.Duplicates(first, count)
        Lost.Renamed -> Loss.Renamed(first, written, count)
        Lost.ItemsNamed -> Loss.ItemsNamed(first, count)
        Lost.Moved -> Loss.Moved(first, count)
        Lost.Dates -> Loss.Dates(first, count)
        Lost.Unwritable -> Loss.Unwritable(first, count)
        Lost.Respelled -> Loss.Respelled(first, written, count)
        Lost.OneLine -> Loss.OneLine(first, count)
    }

    /** What was done to carry the rest. */
    fun notes(): List<Note> = buildList {
        val quoted = quotedDates + quotedOthers
        if (quoted > 0) add(Note.Quoted(dates = quotedOthers == 0, count = quoted))
        if (spelledCount > 0) add(Note.Spelling(spelled[0], spelled.getOrNull(1), spelledCount))
    }
}

/** How a path names the document itself. */
internal const val ROOT_PATH = "$"

/** The path of the member named [name] of the value at [parent], as a flattened name writes it. */
internal fun memberPath(parent: String, name: String): String = if (parent.isEmpty()) name else "$parent.$name"

/** The path of the item at [index] of the array at [parent]. */
internal fun itemPath(parent: String, index: Int): String = "$parent[$index]"
