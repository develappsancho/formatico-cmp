package com.developersancho.formatico.core.format.diff

import com.developersancho.formatico.core.format.convert.DataArray
import com.developersancho.formatico.core.format.convert.DataMember
import com.developersancho.formatico.core.format.convert.DataNumber
import com.developersancho.formatico.core.format.convert.DataObject
import com.developersancho.formatico.core.format.convert.DataValue
import com.developersancho.formatico.core.format.json.PathStep

/**
 * How the values of two documents, [a] and [b], differ by what they mean: whatever format each is written in, the
 * order of an object's keys is no difference, and neither is the way a number is written, `129.90` against `129.9`.
 *
 * An object's members are matched by name, a name given twice by its place among those of its name. An array's items
 * are matched as the longest run of them the two have in common, in order; the others between two matched ones are
 * compared a place at a time, and those left over are added or removed. The differences come in the order of B, a
 * value removed after the value of A it followed.
 */
fun compareValues(a: DataValue, b: DataValue): ValueComparison = ValueComparer().run {
    compare(a, b, emptyList(), emptyList())
    result()
}

private class ValueComparer {
    private val differences = ArrayList<ValueDifference>()
    private var moved: KeysMoved? = null
    private var movedCount = 0
    private var respelled: Pair<String, String>? = null
    private var respelledCount = 0

    fun result() = ValueComparison(
        differences = differences,
        moved = moved?.copy(count = movedCount),
        respelled = respelled?.let { (first, second) -> NumbersRespelled(first, second, respelledCount) }
    )

    fun compare(a: DataValue, b: DataValue, aPath: List<PathStep>, bPath: List<PathStep>) {
        when {
            a is DataObject && b is DataObject -> objects(a, b, aPath, bPath)
            a is DataArray && b is DataArray -> arrays(a, b, aPath, bPath)
            sameScalar(a, b) -> if (a is DataNumber && b is DataNumber && a.written != b.written) respell(a.written, b.written)
            else -> differences += ValueDifference(Change.Changed, aPath, bPath, a, b)
        }
    }

    private fun respell(first: String, second: String) {
        if (respelled == null) respelled = first to second
        respelledCount++
    }

    private fun objects(a: DataObject, b: DataObject, aPath: List<PathStep>, bPath: List<PathStep>) {
        val aKeys = keyed(a.members)
        val bKeys = keyed(b.members)
        val inB = bKeys.withIndex().associate { (index, key) -> key to index }
        val inA = aKeys.withIndex().associate { (index, key) -> key to index }
        noteMoves(aKeys.filter { it in inB }, bKeys.filter { it in inA })
        // A member of A alone is told after the member of both that comes before it in A.
        val removedAfter = HashMap<Key?, MutableList<Int>>()
        var before: Key? = null
        aKeys.forEachIndexed { index, key ->
            if (key in inB) before = key else removedAfter.getOrPut(before) { ArrayList() } += index
        }

        fun removed(after: Key?) = removedAfter[after]?.forEach { index ->
            val member = a.members[index]
            differences += ValueDifference(Change.Removed, aPath + PathStep.Key(member.name), null, member.value, null)
        }
        removed(null)
        bKeys.forEachIndexed { index, key ->
            val member = b.members[index]
            val aIndex = inA[key]
            if (aIndex == null) {
                differences += ValueDifference(Change.Added, null, bPath + PathStep.Key(member.name), null, member.value)
            } else {
                compare(a.members[aIndex].value, member.value, aPath + PathStep.Key(member.name), bPath + PathStep.Key(member.name))
                removed(key)
            }
        }
    }

    /** Tells which keys of both moved: those out of the longest run the two orders have in common. */
    private fun noteMoves(aOrder: List<Key>, bOrder: List<Key>) {
        if (aOrder == bOrder) return
        val kept = commonRun(aOrder.size, bOrder.size) { i, j -> aOrder[i] == bOrder[j] }.mapTo(HashSet()) { aOrder[it.first] }
        val shifted = aOrder.filter { it !in kept }
        if (shifted.isEmpty()) return
        movedCount += shifted.size
        if (moved != null) return
        val key = shifted.first()
        // A key that moved past one other alone changed places with it; the two are named in B's order.
        val passed = between(aOrder.indexOf(key), bOrder.indexOf(key), aOrder, bOrder)
        moved = when (val other = passed.singleOrNull().takeIf { shifted.size == 1 }) {
            null -> KeysMoved(key.name, null, 0)

            else ->
                if (bOrder.indexOf(other) <
                    bOrder.indexOf(key)
                ) {
                    KeysMoved(other.name, key.name, 0)
                } else {
                    KeysMoved(key.name, other.name, 0)
                }
        }
    }

    /** The keys [key] moved past: those on one side of it in A and on the other in B. */
    private fun between(aIndex: Int, bIndex: Int, aOrder: List<Key>, bOrder: List<Key>): List<Key> {
        val key = aOrder[aIndex]
        val beforeInA = aOrder.subList(0, aIndex).toSet()
        val beforeInB = bOrder.subList(0, bIndex).toSet()
        return aOrder.filter { it != key && (it in beforeInA) != (it in beforeInB) }
    }

    private fun arrays(a: DataArray, b: DataArray, aPath: List<PathStep>, bPath: List<PathStep>) {
        val matched = commonRun(a.items.size, b.items.size) { i, j -> sameMeaning(a.items[i], b.items[j]) }
        var i = 0
        var j = 0
        (matched + (a.items.size to b.items.size)).forEach { (aNext, bNext) ->
            // Between two matched items, the others are compared a place at a time, and the rest added or removed.
            while (i < aNext && j < bNext) compare(a.items[i], b.items[j], aPath + PathStep.Index(i++), bPath + PathStep.Index(j++))
            while (i < aNext) differences += ValueDifference(Change.Removed, aPath + PathStep.Index(i), null, a.items[i++], null)
            while (j < bNext) differences += ValueDifference(Change.Added, null, bPath + PathStep.Index(j), null, b.items[j++])
            if (aNext < a.items.size) compare(a.items[i], b.items[j], aPath + PathStep.Index(i++), bPath + PathStep.Index(j++))
        }
    }
}

/** A member's name and its place among the members of that name: what matches it with a member of the other object. */
private data class Key(val name: String, val place: Int)

private fun keyed(members: List<DataMember>): List<Key> {
    val seen = HashMap<String, Int>()
    return members.map { member ->
        val place = seen[member.name] ?: 0
        seen[member.name] = place + 1
        Key(member.name, place)
    }
}
