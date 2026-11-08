package com.developersancho.formatico.core.format.types

/**
 * The notes on the document whose root objects are merged into [root]: [TypeNote.RootList] first where the document
 * is a list ([rootList]), then each other kind there are names for, in the order [TypeNote] lists them.
 */
internal fun notesOf(root: Shape, rootList: Boolean): List<TypeNote> {
    val names = NoteNames().apply { members(root) }
    return buildList {
        if (rootList) add(TypeNote.RootList)
        if (names.onlyNull.isNotEmpty()) add(TypeNote.OnlyNull(names.onlyNull.toList()))
        if (names.mixed.isNotEmpty()) add(TypeNote.Mixed(names.mixed.toList()))
        if (names.emptyLists.isNotEmpty()) add(TypeNote.EmptyList(names.emptyLists.toList()))
        if (names.dates.isNotEmpty()) add(TypeNote.Dates(names.dates.toList()))
    }
}

/**
 * The member names each note is about, gathered depth first, a member's values looked into right after it: the
 * order a reader of the document meets them in.
 */
private class NoteNames {
    val onlyNull = LinkedHashSet<String>()
    val mixed = LinkedHashSet<String>()
    val emptyLists = LinkedHashSet<String>()
    val dates = LinkedHashSet<String>()

    /** Gathers the names of the members of the objects merged into [shape]. */
    fun members(shape: Shape) {
        shape.members.forEach { (name, member) -> values(name, member) }
    }

    /** Gathers [name] where what its values, merged into [shape], are calls for a note; an array's items are its. */
    private fun values(name: String, shape: Shape) {
        when {
            shape.kinds > 1 -> mixed += name
            shape.kinds == 0 && shape.nulls -> onlyNull += name
            shape.kinds == 0 -> emptyLists += name
            shape.strings > 0 -> if (shape.dates == shape.strings) dates += name
            shape.objects > 0 -> members(shape)
            shape.arrays > 0 -> values(name, shape.items)
        }
    }
}
