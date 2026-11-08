package com.developersancho.formatico.core.format.convert

/**
 * A value of a format of one level: its [name] there, the path to it, `customer.name`, `items[0].sku`, its [path] in
 * the document, and its [value], which is no object or array.
 */
internal class FlatLeaf(val name: String, val path: String, val value: DataValue)

/**
 * The values of [value], named [name] and at [path], as a format of one level holds them: [value] itself where it is
 * no object or array, and otherwise what it holds, each named by its path from [name]. A value inside another is told
 * to [log] as flattened, and an empty object or array, which leaves nothing, as emptied.
 */
internal fun flatten(value: DataValue, name: String, path: String, log: ConvertLog, nested: Boolean = false): List<FlatLeaf> = buildList {
    fun walk(value: DataValue, name: String, path: String, nested: Boolean) {
        when (value) {
            is DataObject -> {
                if (value.members.isEmpty()) log.lose(Lost.Emptied, path)
                value.members.forEach { walk(it.value, memberPath(name, it.name), memberPath(path, it.name), nested = true) }
            }

            is DataArray -> {
                if (value.items.isEmpty()) log.lose(Lost.Emptied, path)
                value.items.forEachIndexed { index, item -> walk(item, itemPath(name, index), itemPath(path, index), nested = true) }
            }

            else -> {
                if (nested) log.lose(Lost.Flattened, path)
                add(FlatLeaf(name, path, value))
            }
        }
    }
    walk(value, name, path, nested)
}

/** [leaves] with the last of each name alone, as a format of one level keeps a name once: each one dropped told to [log]. */
internal fun uniqueLeaves(leaves: List<FlatLeaf>, log: ConvertLog): List<FlatLeaf> {
    val last = HashMap<String, Int>()
    leaves.forEachIndexed { index, leaf -> last[leaf.name] = index }
    return leaves.filterIndexed { index, leaf ->
        val kept = last[leaf.name] == index
        if (!kept) log.lose(Lost.Duplicates, leaf.path)
        kept
    }
}

/**
 * The text of [leaf] in a format that has only text: a number as it is written and a truth as `true` or `false`, told
 * to [log] as text, a date as a string, and a null as an empty string.
 */
internal fun textOf(leaf: FlatLeaf, log: ConvertLog): String = when (val value = leaf.value) {
    is DataString -> value.value
    is DataNumber -> value.written.also { log.lose(Lost.Untyped, leaf.path) }
    is DataBoolean -> "${value.value}".also { log.lose(Lost.Untyped, leaf.path) }
    is DataDate -> value.written.also { log.lose(Lost.Dates, leaf.path) }
    DataNull -> "".also { log.lose(Lost.Nulls, leaf.path) }
    is DataObject, is DataArray -> ""
}
