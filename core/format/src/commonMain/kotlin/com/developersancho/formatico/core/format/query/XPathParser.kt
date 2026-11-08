package com.developersancho.formatico.core.format.query

/** The binary operators below `or` and `and`, by precedence, from the loosest to the tightest. */
private val PRECEDENCE = listOf(
    mapOf(XPathTokenKind.Equal to XPathOperator.Equal, XPathTokenKind.NotEqual to XPathOperator.NotEqual),
    mapOf(
        XPathTokenKind.Less to XPathOperator.Less,
        XPathTokenKind.LessOrEqual to XPathOperator.LessOrEqual,
        XPathTokenKind.Greater to XPathOperator.Greater,
        XPathTokenKind.GreaterOrEqual to XPathOperator.GreaterOrEqual
    ),
    mapOf(XPathTokenKind.Plus to XPathOperator.Plus, XPathTokenKind.Minus to XPathOperator.Minus),
    mapOf(
        XPathTokenKind.Multiply to XPathOperator.Multiply,
        XPathTokenKind.Div to XPathOperator.Div,
        XPathTokenKind.Mod to XPathOperator.Mod
    )
)

/**
 * Reads an XPath 1.0 expression from its tokens, as its grammar writes it, and checks its types as it goes: since no
 * variable is read, each expression's type is known, and one that is no node-set where a node-set is needed is an
 * error, as XPath 1.0 has it. [XPathPathParser] reads its location paths.
 */
internal class XPathParser(private val cursor: XPathTokenCursor) {
    private val paths = XPathPathParser(cursor, this)

    /** The whole expression. */
    fun parse(): XPathExpression {
        val expression = expression()
        if (cursor.current.kind != XPathTokenKind.End) cursor.unexpected()
        return expression
    }

    /** The expression next. */
    fun expression(): XPathExpression = logical(any = true)

    /** The `or` of `and`s next, where [any], or the `and` of comparisons. */
    private fun logical(any: Boolean): XPathExpression {
        val separator = if (any) XPathTokenKind.Or else XPathTokenKind.And
        val operands = arrayListOf(if (any) logical(any = false) else operations(level = 0))
        while (cursor.accept(separator)) operands += if (any) logical(any = false) else operations(level = 0)
        return operands.singleOrNull() ?: XPathExpression.Logical(any, operands)
    }

    /** The operations of the precedence [level] in [PRECEDENCE] next, each operand of the levels above it. */
    private fun operations(level: Int): XPathExpression {
        val first = if (level == PRECEDENCE.lastIndex) unary() else operations(level + 1)
        val operations = ArrayList<Operation>()
        while (true) {
            val operator = PRECEDENCE[level][cursor.current.kind] ?: break
            cursor.advance()
            operations += Operation(operator, if (level == PRECEDENCE.lastIndex) unary() else operations(level + 1))
        }
        return if (operations.isEmpty()) first else XPathExpression.Operations(first, operations)
    }

    /** The union next, after as many `-` as stand before it. */
    private fun unary(): XPathExpression {
        var minuses = 0
        while (cursor.accept(XPathTokenKind.Minus)) minuses++
        val operand = union()
        return if (minuses == 0) operand else XPathExpression.Negation(operand, negated = minuses % 2 == 1)
    }

    /** The path next, or the union of the paths next, each of which must be a node-set. */
    private fun union(): XPathExpression {
        var start = cursor.current.start
        val first = paths.path()
        if (cursor.current.kind != XPathTokenKind.Pipe) return first
        val operands = arrayListOf(nodeSet(first, start))
        while (cursor.accept(XPathTokenKind.Pipe)) {
            start = cursor.current.start
            operands += nodeSet(paths.path(), start)
        }
        return XPathExpression.Union(operands)
    }

    /** [expression], which starts at [start], where it is a node-set. */
    fun nodeSet(expression: XPathExpression, start: Int): XPathExpression =
        if (expression.type == XPathType.NodeSet) expression else queryError(QueryErrorCode.WrongType, start)

    /** The primary expression next: an expression in parentheses, a string, a number or a function's call. */
    fun primary(): XPathExpression {
        val token = cursor.current
        return when (token.kind) {
            XPathTokenKind.LeftParen -> {
                cursor.enter()
                cursor.advance()
                val inner = expression()
                cursor.expect(XPathTokenKind.RightParen)
                cursor.leave()
                inner
            }

            XPathTokenKind.Literal -> XPathExpression.Literal(cursor.advance().text)

            XPathTokenKind.Number -> XPathExpression.NumberLiteral(xpathNumber(cursor.advance().text))

            XPathTokenKind.FunctionName -> call()

            else -> cursor.unexpected()
        }
    }

    /** The call of the function whose name is next. */
    private fun call(): XPathExpression {
        val name = cursor.advance()
        val function = XPathFunction.entries.firstOrNull { it.functionName == name.text }
            ?: queryError(QueryErrorCode.UnknownFunction, name.start)
        cursor.enter()
        cursor.expect(XPathTokenKind.LeftParen)
        val arguments = ArrayList<Pair<Int, XPathExpression>>()
        if (cursor.current.kind != XPathTokenKind.RightParen) {
            do {
                arguments += cursor.current.start to expression()
            } while (cursor.accept(XPathTokenKind.Comma))
        }
        cursor.expect(XPathTokenKind.RightParen)
        cursor.leave()
        if (arguments.size !in function.minArguments..function.maxArguments) queryError(QueryErrorCode.WrongArgumentCount, name.start)
        return XPathExpression.Call(
            function,
            arguments.map { (start, argument) -> if (function.takesNodeSets) nodeSet(argument, start) else argument }
        )
    }

    /** The predicates next, each between brackets, as many as follow. */
    fun predicates(): List<XPathExpression> {
        val predicates = ArrayList<XPathExpression>()
        while (cursor.current.kind == XPathTokenKind.LeftBracket) {
            cursor.enter()
            cursor.advance()
            predicates += expression()
            cursor.expect(XPathTokenKind.RightBracket)
            cursor.leave()
        }
        return predicates
    }
}
