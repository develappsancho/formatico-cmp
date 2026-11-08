package com.developersancho.formatico.core.format.query

/**
 * Evaluates XPath expressions on [index]'s model of a document. It recurses on the expression, whose depth reading
 * bounds, and never on the document's.
 */
internal class XPathEvaluator(private val index: XPathIndex) {
    private val functions = XPathFunctions(index, this)

    /** What [expression] evaluates to in [context]. */
    fun evaluate(expression: XPathExpression, context: XPathContext): XPathValue = when (expression) {
        is XPathExpression.Logical -> XPathValue.Truth(
            if (expression.any) {
                expression.operands.any { boolean(evaluate(it, context)) }
            } else {
                expression.operands.all { boolean(evaluate(it, context)) }
            }
        )

        is XPathExpression.Operations -> operations(expression, context)

        is XPathExpression.Negation -> negation(expression, context)

        is XPathExpression.Union -> XPathValue.Nodes(inDocumentOrder(expression.operands.flatMap { nodes(it, context) }))

        is XPathExpression.LocationPath -> XPathValue.Nodes(
            steps(listOf(if (expression.absolute) index.root else context.node), expression.steps)
        )

        is XPathExpression.FilterPath -> filterPath(expression, context)

        is XPathExpression.Literal -> XPathValue.Text(expression.value)

        is XPathExpression.NumberLiteral -> XPathValue.Number(expression.value)

        is XPathExpression.Call -> functions.call(expression, context)
    }

    /** The nodes [expression], a node-set's, selects in [context]. */
    fun nodes(expression: XPathExpression, context: XPathContext): List<XPathTreeNode> =
        (evaluate(expression, context) as? XPathValue.Nodes)?.nodes.orEmpty()

    private fun operations(expression: XPathExpression.Operations, context: XPathContext): XPathValue {
        var value = evaluate(expression.first, context)
        expression.operations.forEach { operation ->
            val operand = evaluate(operation.operand, context)
            value = if (operation.operator.compares) {
                XPathValue.Truth(index.compare(value, operation.operator, operand))
            } else {
                XPathValue.Number(arithmetic(index.number(value), operation.operator, index.number(operand)))
            }
        }
        return value
    }

    private fun negation(expression: XPathExpression.Negation, context: XPathContext): XPathValue {
        val number = index.number(evaluate(expression.operand, context))
        return XPathValue.Number(if (expression.negated) -number else number)
    }

    private fun filterPath(expression: XPathExpression.FilterPath, context: XPathContext): XPathValue {
        // A filter's predicates count positions along the child axis: in document order.
        val filtered = expression.predicates.fold(nodes(expression.primary, context)) { nodes, predicate -> filter(nodes, predicate) }
        return XPathValue.Nodes(steps(filtered, expression.steps))
    }

    /** What [steps] select from each of [start] in turn, in document order. */
    private fun steps(start: List<XPathTreeNode>, steps: List<XPathStep>): List<XPathTreeNode> =
        steps.fold(start) { nodes, step -> if (nodes.isEmpty()) nodes else step(nodes, step) }

    /** What [step] selects from each of [contexts], in document order, each node once. */
    private fun step(contexts: List<XPathTreeNode>, step: XPathStep): List<XPathTreeNode> {
        val selected = ArrayList<XPathTreeNode>()
        contexts.forEach { context ->
            val along = index.axis(context, step.axis).filter { step.test.matches(it, step.axis) }
            selected += step.predicates.fold(along) { nodes, predicate -> filter(nodes, predicate) }
        }
        val ordered = contexts.size == 1 && !step.axis.reverse
        return if (ordered) selected else inDocumentOrder(selected)
    }

    /** The nodes of [nodes] [predicate] is true of, each at its position among them; a number is a position. */
    private fun filter(nodes: List<XPathTreeNode>, predicate: XPathExpression): List<XPathTreeNode> {
        if (predicate is XPathExpression.NumberLiteral) {
            val wanted = predicate.value.toInt()
            return if (wanted.toDouble() == predicate.value && wanted in 1..nodes.size) listOf(nodes[wanted - 1]) else emptyList()
        }
        return nodes.filterIndexed { at, node ->
            val value = evaluate(predicate, XPathContext(node, at + 1, nodes.size))
            if (predicate.type == XPathType.Number) index.number(value) == (at + 1).toDouble() else boolean(value)
        }
    }
}

/** [left] [operator] [right], [operator] one of arithmetic, in IEEE 754's doubles; `mod` keeps the sign of [left]. */
private fun arithmetic(left: Double, operator: XPathOperator, right: Double): Double = when (operator) {
    XPathOperator.Plus -> left + right
    XPathOperator.Minus -> left - right
    XPathOperator.Multiply -> left * right
    XPathOperator.Div -> left / right
    XPathOperator.Mod -> left % right
    else -> Double.NaN
}
