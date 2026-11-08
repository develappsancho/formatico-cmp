package com.developersancho.formatico.core.format.query

/**
 * Whether [left] [operator] [right] is true, [operator] a comparison, as XPath 1.0 compares: a node-set is true where
 * any of its nodes compares true, by its string-value, or that string's number, or by whether it is empty against a
 * boolean; two other values are compared as booleans where either is one, then as numbers where either is one, then as
 * strings, for `=` and `!=`, and always as numbers for `<`, `<=`, `>` and `>=`.
 */
internal fun XPathIndex.compare(left: XPathValue, operator: XPathOperator, right: XPathValue): Boolean = when {
    left is XPathValue.Nodes && right is XPathValue.Nodes -> compareNodeSets(left.nodes, operator, right.nodes)
    left is XPathValue.Nodes -> compareNodeSet(left.nodes, operator, right)
    right is XPathValue.Nodes -> compareNodeSet(right.nodes, operator.mirrored, left)
    else -> compareValues(left, operator, right)
}

/** Whether some node of [nodes] and some of [others] compare true by their string-values. */
private fun XPathIndex.compareNodeSets(nodes: List<XPathTreeNode>, operator: XPathOperator, others: List<XPathTreeNode>): Boolean {
    if (nodes.isEmpty() || others.isEmpty()) return false
    return when (operator) {
        XPathOperator.Equal, XPathOperator.NotEqual -> {
            val strings = nodes.mapTo(HashSet(), ::stringValue)
            val otherStrings = others.mapTo(HashSet(), ::stringValue)
            if (operator == XPathOperator.Equal) {
                strings.any { it in otherStrings }
            } else {
                strings.size > 1 || otherStrings.size > 1 || strings.single() != otherStrings.single()
            }
        }

        else -> {
            // Some pair compares true where the least of one side and the greatest of the other do.
            val numbers = nodes.map { xpathNumber(stringValue(it)) }.filterNot(Double::isNaN)
            val otherNumbers = others.map { xpathNumber(stringValue(it)) }.filterNot(Double::isNaN)
            numbers.isNotEmpty() &&
                otherNumbers.isNotEmpty() &&
                when (operator) {
                    XPathOperator.Less, XPathOperator.LessOrEqual -> compareNumbers(numbers.min(), operator, otherNumbers.max())
                    else -> compareNumbers(numbers.max(), operator, otherNumbers.min())
                }
        }
    }
}

/** Whether some node of [nodes] compares true with [other], which is no node-set, with [operator] between them in that order. */
private fun XPathIndex.compareNodeSet(nodes: List<XPathTreeNode>, operator: XPathOperator, other: XPathValue): Boolean = when (other) {
    is XPathValue.Truth -> compareValues(XPathValue.Truth(nodes.isNotEmpty()), operator, other)
    is XPathValue.Number -> nodes.any { compareNumbers(xpathNumber(stringValue(it)), operator, other.value) }
    is XPathValue.Text -> nodes.any { compareValues(XPathValue.Text(stringValue(it)), operator, other) }
    is XPathValue.Nodes -> compareNodeSets(nodes, operator, other.nodes)
}

/** Whether [left] [operator] [right] is true, neither of them a node-set. */
private fun XPathIndex.compareValues(left: XPathValue, operator: XPathOperator, right: XPathValue): Boolean {
    val equality = operator == XPathOperator.Equal || operator == XPathOperator.NotEqual
    return when {
        equality && (left is XPathValue.Truth || right is XPathValue.Truth) -> (boolean(left) == boolean(right)) ==
            (operator == XPathOperator.Equal)

        equality && left is XPathValue.Text && right is XPathValue.Text -> (left.value == right.value) == (operator == XPathOperator.Equal)

        else -> compareNumbers(number(left), operator, number(right))
    }
}

/** Whether [left] [operator] [right] is true of two numbers, NaN equal to none and unequal to all. */
private fun compareNumbers(left: Double, operator: XPathOperator, right: Double): Boolean = when (operator) {
    XPathOperator.Equal -> left == right
    XPathOperator.NotEqual -> left != right
    XPathOperator.Less -> left < right
    XPathOperator.LessOrEqual -> left <= right
    XPathOperator.Greater -> left > right
    XPathOperator.GreaterOrEqual -> left >= right
    else -> false
}
