package com.developersancho.formatico.core.format.query

/**
 * Reads a filter's logical expression, as RFC 9535's grammar writes it, and checks its types as it goes: a comparison
 * compares values, which a singular query, a literal and `length()`, `count()` and `value()` give; a test stands
 * alone, as a query or `match()` and `search()` do; and each function is given arguments of its parameters' types.
 */
internal class JsonPathFilterParser(private val cursor: JsonPathCursor, private val paths: JsonPathParser) {
    /** The logical expression after a filter's `?`. */
    fun filter(): Logical = logical(OR_LEVEL, first = null)

    /**
     * The `||` of `&&`s, at [OR_LEVEL], or the `&&` of comparisons and tests, at the level below it, whose first
     * operand is [first] where it has been read already.
     */
    private fun logical(level: Int, first: Operand?): Logical {
        val operands = arrayListOf(if (level == OR_LEVEL) logical(AND_LEVEL, first) else basic(first))
        val symbol = if (level == OR_LEVEL) "||" else "&&"
        while (cursor.acceptSymbol(symbol)) {
            cursor.skipWhitespace()
            operands += if (level == OR_LEVEL) logical(AND_LEVEL, first = null) else basic(first = null)
        }
        return operands.singleOrNull() ?: if (level == OR_LEVEL) Logical.AnyOf(operands) else Logical.AllOf(operands)
    }

    /** A comparison, a test, or an expression in parentheses, `!` before the last two or not. */
    private fun basic(first: Operand?): Logical {
        if (first == null) cursor.skipWhitespace()
        return when {
            first == null && cursor.peek() == '!' && cursor.peek(1) != '=' -> {
                cursor.at++
                cursor.skipWhitespace()
                Logical.Not(negated())
            }

            first == null && cursor.peek() == '(' -> parenthesized()

            else -> comparisonOrTest(first ?: operand())
        }
    }

    /** [left] compared to the operand after the operator next, or [left] as a test where no operator is next. */
    private fun comparisonOrTest(left: Operand): Logical {
        val operator = cursor.comparisonOperator() ?: return left.test()
        cursor.skipWhitespace()
        return Logical.Comparison(left.comparand(), operator, operand().comparand())
    }

    /** What a `!` negates: an expression in parentheses, or a test; never a comparison. */
    private fun negated(): Logical = if (cursor.peek() == '(') parenthesized() else operand().test()

    /** The expression between the parentheses next. */
    private fun parenthesized(): Logical {
        cursor.enter()
        cursor.expect('(')
        cursor.skipWhitespace()
        val inner = filter()
        cursor.skipWhitespace()
        cursor.expect(')')
        cursor.leave()
        return inner
    }

    /** The literal, the query or the function next. */
    private fun operand(): Operand {
        val start = cursor.at
        return when (val char = cursor.peek()) {
            '$', '@' -> {
                cursor.at++
                Operand.Query(PathQuery(relative = char == '@', paths.segments()), start)
            }

            '\'', '"' -> Operand.Literal(LiteralValue.Text(cursor.readString()), start)

            else -> when {
                char == '-' || char in '0'..'9' -> Operand.Literal(LiteralValue.Number(cursor.readNumber()), start)
                char in 'a'..'z' -> word(start)
                else -> cursor.unexpected()
            }
        }
    }

    /** The function whose name starts at [start], or the literal `true`, `false` or `null` there. */
    private fun word(start: Int): Operand {
        val word = cursor.readWord()
        if (cursor.peek() == '(') return Operand.Call(call(word, start), start)
        val literal = when (word) {
            "true" -> LiteralValue.Truth(true)
            "false" -> LiteralValue.Truth(false)
            "null" -> LiteralValue.Null
            else -> queryError(QueryErrorCode.UnexpectedCharacter, start)
        }
        return Operand.Literal(literal, start)
    }

    /** The call of the function [name], whose `(` is next and whose name starts at [start]. */
    private fun call(name: String, start: Int): FunctionCall {
        val function = FilterFunction.entries.firstOrNull { it.functionName == name } ?: queryError(QueryErrorCode.UnknownFunction, start)
        cursor.enter()
        cursor.expect('(')
        cursor.skipWhitespace()
        val arguments = ArrayList<RawArgument>()
        if (cursor.peek() != ')') {
            do {
                cursor.skipWhitespace()
                arguments += argument()
                cursor.skipWhitespace()
            } while (cursor.accept(','))
        }
        cursor.expect(')')
        cursor.leave()
        if (arguments.size != function.parameters.size) queryError(QueryErrorCode.WrongArgumentCount, start)
        return FunctionCall(function, arguments.zip(function.parameters) { argument, type -> argument.typed(type) })
    }

    /** A function's argument: a literal, a query or a function as it is, or a logical expression. */
    private fun argument(): RawArgument {
        val start = cursor.at
        val operand = if (cursor.peek() == '!' || cursor.peek() == '(') null else operand()
        val after = cursor.at
        cursor.skipWhitespace()
        if (operand != null && (cursor.peek() == ',' || cursor.peek() == ')')) return RawArgument(start, operand)
        // A logical expression, which no function takes: it is read all the same, so that a mistake in it is found first.
        cursor.at = after
        logical(OR_LEVEL, operand)
        return RawArgument(start, operand = null)
    }

    private companion object {
        const val OR_LEVEL = 0
        const val AND_LEVEL = 1
    }
}

/** What stands in a filter before it is known to be compared or tested, and [start], where it is written. */
internal sealed interface Operand {
    /** Where it starts in the query. */
    val start: Int

    /** A literal. */
    class Literal(val value: LiteralValue, override val start: Int) : Operand

    /** A query. */
    class Query(val query: PathQuery, override val start: Int) : Operand

    /** A function's call. */
    class Call(val call: FunctionCall, override val start: Int) : Operand
}

/**
 * A function's argument as it is read, at [start]: [operand] where it is a literal, a query or a function, and null
 * where it is a logical expression, which no function of RFC 9535 takes.
 */
internal class RawArgument(val start: Int, val operand: Operand?)

/** The operand as a comparison compares it: a literal, a singular query, or a function that gives a value. */
private fun Operand.comparand(): Comparand = when (this) {
    is Operand.Literal -> Comparand.Literal(value)
    is Operand.Query -> if (query.isSingular) Comparand.Query(query) else queryError(QueryErrorCode.NotSingular, start)
    is Operand.Call -> if (call.function.givesValue) Comparand.Call(call) else queryError(QueryErrorCode.WrongType, start)
}

/** The operand as a test that stands alone: a query, which passes where it selects anything, or a logical function. */
private fun Operand.test(): Logical = when (this) {
    is Operand.Literal -> queryError(QueryErrorCode.WrongType, start)
    is Operand.Query -> Logical.Exists(query)
    is Operand.Call -> if (call.function.givesValue) queryError(QueryErrorCode.WrongType, start) else Logical.Test(call)
}

/** The argument as a parameter of [type] takes it. */
private fun RawArgument.typed(type: ParameterType): Argument = when (type) {
    ParameterType.ValueType -> Argument.Single(operand?.comparand() ?: queryError(QueryErrorCode.WrongType, start))
    ParameterType.NodesType -> (operand as? Operand.Query)?.let { Argument.Nodes(it.query) } ?: queryError(QueryErrorCode.WrongType, start)
}

/** Reads [symbol] where it stands next, whitespace before it or not, and says whether it does. */
private fun JsonPathCursor.acceptSymbol(symbol: String): Boolean {
    val before = at
    skipWhitespace()
    if (text.startsWith(symbol, at)) {
        at += symbol.length
        return true
    }
    at = before
    return false
}

/** Reads the comparison's operator next, whitespace before it or not, or nothing where none is next. */
private fun JsonPathCursor.comparisonOperator(): ComparisonOperator? =
    ComparisonOperator.entries.firstOrNull { acceptSymbol(it.symbol) }
