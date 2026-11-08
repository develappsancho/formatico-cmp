package com.developersancho.formatico.core.format.query

/**
 * Compares the values of a filter as RFC 9535 does: nothing equals nothing alone; numbers, strings, `true`, `false` and
 * `null` equal a value of their kind that is the same, and arrays and objects one whose items, or members, are equal;
 * and only two numbers, or two strings, in the order of their code points, are less than one another.
 */
internal class JsonPathComparisons<N>(private val values: JsonValues<N>) {
    /** Whether [left] [operator] [right] is true. */
    fun compare(left: FilterValue<N>, operator: ComparisonOperator, right: FilterValue<N>): Boolean = when (operator) {
        ComparisonOperator.Equal -> equal(left, right)
        ComparisonOperator.NotEqual -> !equal(left, right)
        ComparisonOperator.Less -> less(left, right)
        ComparisonOperator.LessOrEqual -> less(left, right) || equal(left, right)
        ComparisonOperator.Greater -> less(right, left)
        ComparisonOperator.GreaterOrEqual -> less(right, left) || equal(left, right)
    }

    private fun equal(left: FilterValue<N>, right: FilterValue<N>): Boolean = when {
        left is FilterValue.Absent || right is FilterValue.Absent -> left is FilterValue.Absent && right is FilterValue.Absent
        left is FilterValue.Node && right is FilterValue.Node -> nodesEqual(left.node, right.node)
        else -> literalsEqual(scalar(left), scalar(right))
    }

    private fun less(left: FilterValue<N>, right: FilterValue<N>): Boolean {
        val first = scalar(left)
        val second = scalar(right)
        return when {
            first is LiteralValue.Number && second is LiteralValue.Number -> first.value < second.value
            first is LiteralValue.Text && second is LiteralValue.Text -> compareCodePoints(first.value, second.value) < 0
            else -> false
        }
    }

    /** [value] as a literal: null for nothing, an object and an array. */
    private fun scalar(value: FilterValue<N>): LiteralValue? = when (value) {
        FilterValue.Absent -> null
        is FilterValue.Literal -> value.value
        is FilterValue.Node -> scalarOf(value.node)
    }

    private fun scalarOf(node: N): LiteralValue? = when (values.kindOf(node)) {
        JsonKind.String -> LiteralValue.Text(values.stringOf(node))
        JsonKind.Number -> LiteralValue.Number(values.numberOf(node))
        JsonKind.Boolean -> LiteralValue.Truth(values.booleanOf(node))
        JsonKind.Null -> LiteralValue.Null
        JsonKind.Object, JsonKind.Array -> null
    }

    /** Whether [first] and [second] are equal, inside and all, walked with a stack rather than by recursion. */
    private fun nodesEqual(first: N, second: N): Boolean {
        val pending = ArrayList<Pair<N, N>>()
        pending += first to second
        var equal = true
        while (equal && pending.isNotEmpty()) {
            val (one, other) = pending.removeAt(pending.lastIndex)
            val kind = values.kindOf(one)
            equal = kind == values.kindOf(other) &&
                when (kind) {
                    JsonKind.Array -> pairItems(one, other, pending)
                    JsonKind.Object -> pairMembers(one, other, pending)
                    else -> literalsEqual(scalarOf(one), scalarOf(other))
                }
        }
        return equal
    }

    /** Whether two arrays have as many items, each pair of which is added to [pending] to be compared. */
    private fun pairItems(one: N, other: N, pending: MutableList<Pair<N, N>>): Boolean {
        val items = values.childrenOf(one)
        val others = values.childrenOf(other)
        if (items.size != others.size) return false
        pending += items.zip(others)
        return true
    }

    /** Whether two objects have as many members of each name, each pair of which is added to [pending] to be compared. */
    private fun pairMembers(one: N, other: N, pending: MutableList<Pair<N, N>>): Boolean {
        val members = values.childrenOf(one).groupBy(values::nameOf)
        val others = values.childrenOf(other).groupBy(values::nameOf)
        if (members.keys != others.keys || members.any { (name, named) -> named.size != others.getValue(name).size }) return false
        members.forEach { (name, named) -> pending += named.zip(others.getValue(name)) }
        return true
    }
}

/** Whether [first] and [second] are the same literal: both nothing is not that. */
private fun literalsEqual(first: LiteralValue?, second: LiteralValue?): Boolean = when (first) {
    is LiteralValue.Text -> second is LiteralValue.Text && first.value == second.value
    is LiteralValue.Number -> second is LiteralValue.Number && first.value == second.value
    is LiteralValue.Truth -> second is LiteralValue.Truth && first.value == second.value
    LiteralValue.Null -> second == LiteralValue.Null
    null -> false
}
