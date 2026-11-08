package com.developersancho.formatico.core.format.diff

import com.developersancho.formatico.core.format.convert.DataValue
import com.developersancho.formatico.core.format.json.PathStep

/** How a value or a run of lines differs between two documents, A and B. */
enum class Change {
    /** It is in both, and not the same. */
    Changed,

    /** It is in B alone. */
    Added,

    /** It is in A alone. */
    Removed
}

/**
 * A difference between the values of two documents.
 *
 * @property change how the value differs.
 * @property aPath where the value is in A, or null for one added.
 * @property bPath where it is in B, or null for one removed.
 * @property before its value in A, or null for one added.
 * @property after its value in B, or null for one removed.
 */
data class ValueDifference(
    val change: Change,
    val aPath: List<PathStep>?,
    val bPath: List<PathStep>?,
    val before: DataValue?,
    val after: DataValue?
) {
    /** Where the value is: in A, or in B for one added. */
    val path: List<PathStep> get() = aPath ?: bPath.orEmpty()
}

/**
 * Keys an object holds in both documents, in another order in B: [first] and, where it changed places with one key
 * alone, that key, [other]; [count], how many keys moved, in every object.
 */
data class KeysMoved(val first: String, val other: String?, val count: Int)

/**
 * Numbers that are the same number in both documents, written another way: [first] as A writes it and [second] as B
 * does, `129.90` and `129.9`; [count], how many there are.
 */
data class NumbersRespelled(val first: String, val second: String, val count: Int)

/**
 * What comparing two documents' values came to: their [differences], in the order of the documents, and what was not
 * counted as one: the keys that [moved] and the numbers [respelled].
 */
data class ValueComparison(val differences: List<ValueDifference>, val moved: KeysMoved?, val respelled: NumbersRespelled?)

/**
 * A run of lines that differs between two texts: from [aStart] in A, [aCount] lines of it, and from [bStart] in B,
 * [bCount] lines, both counted from nought. A run added has no line in A, and one removed none in B; its start is then
 * where the run would stand.
 */
data class LineDifference(val change: Change, val aStart: Int, val aCount: Int, val bStart: Int, val bCount: Int)
