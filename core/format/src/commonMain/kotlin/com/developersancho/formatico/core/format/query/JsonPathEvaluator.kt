package com.developersancho.formatico.core.format.query

/**
 * Runs JSONPath queries on the document whose root is [root], whose values [values] reads. Nothing here recurses on
 * the document's depth: a descendant segment and the equality of two values walk it with a stack of their own.
 */
internal class JsonPathEvaluator<N>(private val root: N, private val values: JsonValues<N>) {
    private val comparisons = JsonPathComparisons(values)
    private val functions = JsonPathFunctions(values, this)

    /** The values [query] selects, from the root, or from [current], the value a filter tests, where it is relative. */
    fun select(query: PathQuery, current: N): List<N> {
        var nodes = listOf(if (query.relative) current else root)
        for (segment in query.segments) {
            if (nodes.isEmpty()) break
            nodes = apply(segment, nodes)
        }
        return nodes
    }

    /** Whether [logical] is true of [current]. */
    fun test(logical: Logical, current: N): Boolean = when (logical) {
        is Logical.AnyOf -> logical.operands.any { test(it, current) }
        is Logical.AllOf -> logical.operands.all { test(it, current) }
        is Logical.Not -> !test(logical.operand, current)
        is Logical.Comparison -> comparisons.compare(value(logical.left, current), logical.operator, value(logical.right, current))
        is Logical.Exists -> select(logical.query, current).isNotEmpty()
        is Logical.Test -> functions.test(logical.call, current)
    }

    /** The value [comparand] gives where the filter tests [current]: a value, or nothing. */
    fun value(comparand: Comparand, current: N): FilterValue<N> = when (comparand) {
        is Comparand.Literal -> FilterValue.Literal(comparand.value)
        is Comparand.Query -> select(comparand.query, current).singleOrNull()?.let { FilterValue.Node(it) } ?: FilterValue.Absent
        is Comparand.Call -> functions.value(comparand.call, current)
    }

    /** What [segment] selects of each of [nodes], in turn. */
    private fun apply(segment: PathSegment, nodes: List<N>): List<N> {
        val selected = ArrayList<N>()
        for (node in nodes) {
            if (segment.descendant) {
                visit(node) { visited -> segment.selectors.forEach { select(visited, it, selected) } }
            } else {
                segment.selectors.forEach { select(node, it, selected) }
            }
        }
        return selected
    }

    /** Gives [action] [node] and every value inside it, each before the values inside it, in the order they are written. */
    private inline fun visit(node: N, action: (N) -> Unit) {
        val pending = ArrayList<N>()
        pending += node
        while (pending.isNotEmpty()) {
            val next = pending.removeAt(pending.lastIndex)
            action(next)
            val children = values.childrenOf(next)
            for (index in children.indices.reversed()) pending += children[index]
        }
    }

    /** Adds what [selector] selects of [node] to [into]. */
    private fun select(node: N, selector: Selector, into: MutableList<N>) {
        val kind = values.kindOf(node)
        when (selector) {
            is Selector.Name -> if (kind == JsonKind.Object) values.childrenOf(node).filterTo(into) { values.nameOf(it) == selector.name }

            Selector.Wildcard -> into += values.childrenOf(node)

            is Selector.Index -> if (kind == JsonKind.Array) {
                val items = values.childrenOf(node)
                val index = if (selector.index < 0) items.size + selector.index else selector.index
                if (index >= 0 && index < items.size) into += items[index.toInt()]
            }

            is Selector.Slice -> if (kind == JsonKind.Array) {
                val items = values.childrenOf(node)
                sliceIndexes(items.size, selector).mapTo(into) { items[it] }
            }

            is Selector.Filter -> if (kind == JsonKind.Object || kind == JsonKind.Array) {
                values.childrenOf(node).filterTo(into) { test(selector.test, it) }
            }
        }
    }
}

/** What a comparison compares: a value of the document, a literal, or nothing, where a query selects none. */
internal sealed interface FilterValue<out N> {
    /** Nothing: what a query that selects no value, or a function that gives none, gives. */
    data object Absent : FilterValue<Nothing>

    /** A value of the document. */
    class Node<N>(val node: N) : FilterValue<N>

    /** A literal, or a number a function gives. */
    class Literal(val value: LiteralValue) : FilterValue<Nothing>
}

/** The indexes [slice] selects of an array of [length] items, in the order it selects them, as RFC 9535 counts them. */
internal fun sliceIndexes(length: Int, slice: Selector.Slice): List<Int> {
    val step = slice.step ?: 1
    if (step == 0L) return emptyList()
    val size = length.toLong()
    val indexes = ArrayList<Int>()
    if (step > 0) {
        val lower = normalized(slice.start ?: 0, size).coerceIn(0, size)
        val upper = normalized(slice.end ?: size, size).coerceIn(0, size)
        var index = lower
        while (index < upper) {
            indexes += index.toInt()
            index += step
        }
    } else {
        val upper = normalized(slice.start ?: (size - 1), size).coerceIn(-1, size - 1)
        val lower = normalized(slice.end ?: (-size - 1), size).coerceIn(-1, size - 1)
        var index = upper
        while (lower < index) {
            indexes += index.toInt()
            index += step
        }
    }
    return indexes
}

/** [index] counted from the start of an array of [length] items, where it is counted from its end. */
private fun normalized(index: Long, length: Long): Long = if (index >= 0) index else length + index
