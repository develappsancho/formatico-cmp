package com.developersancho.formatico.core.format.query

/**
 * A JSONPath query as it is read: from the document's root, `$`, or, in a filter, from the value it tests, `@`.
 *
 * @property relative whether it starts at `@`.
 * @property segments its segments, in order.
 */
internal class PathQuery(val relative: Boolean, val segments: List<PathSegment>) {
    /** Whether it selects one value at most: each of its segments a child segment with one name or one index. */
    val isSingular: Boolean
        get() = segments.all { segment ->
            !segment.descendant && segment.selectors.singleOrNull().let { it is Selector.Name || it is Selector.Index }
        }
}

/**
 * A segment: its selectors, applied to each value it is given in turn.
 *
 * @property descendant whether it is a descendant segment, `..`, which applies them to the value and to every value
 *   inside it as well.
 */
internal class PathSegment(val selectors: List<Selector>, val descendant: Boolean)

/** What a segment selects of a value. */
internal sealed interface Selector {
    /** The members of an object whose name is [name]. */
    class Name(val name: String) : Selector

    /** Every member of an object, or every item of an array. */
    data object Wildcard : Selector

    /** The item of an array at [index], counted from its end where it is negative. */
    class Index(val index: Long) : Selector

    /** The items of an array from [start] up to [end] by [step], each where it is given. */
    class Slice(val start: Long?, val end: Long?, val step: Long?) : Selector

    /** The members of an object, or the items of an array, that pass [test]. */
    class Filter(val test: Logical) : Selector
}

/** A filter's logical expression. */
internal sealed interface Logical {
    /** `||`: whether any of [operands] is true. */
    class AnyOf(val operands: List<Logical>) : Logical

    /** `&&`: whether every one of [operands] is true. */
    class AllOf(val operands: List<Logical>) : Logical

    /** `!`: whether [operand] is false. */
    class Not(val operand: Logical) : Logical

    /** [left] compared to [right]. */
    class Comparison(val left: Comparand, val operator: ComparisonOperator, val right: Comparand) : Logical

    /** Whether [query] selects anything. */
    class Exists(val query: PathQuery) : Logical

    /** A function whose result is logical: `match()` or `search()`. */
    class Test(val call: FunctionCall) : Logical
}

/** What a comparison compares: a value, or nothing at all. */
internal sealed interface Comparand {
    /** A literal. */
    class Literal(val value: LiteralValue) : Comparand

    /** The value a singular [query] selects, or nothing where it selects none. */
    class Query(val query: PathQuery) : Comparand

    /** The value a function gives: `length()`, `count()` or `value()`. */
    class Call(val call: FunctionCall) : Comparand
}

/** A literal of a filter. */
internal sealed interface LiteralValue {
    /** A string. */
    class Text(val value: String) : LiteralValue

    /** A number. */
    class Number(val value: Double) : LiteralValue

    /** `true` or `false`. */
    class Truth(val value: Boolean) : LiteralValue

    /** `null`. */
    data object Null : LiteralValue
}

/** A comparison's operator, as it is written. */
internal enum class ComparisonOperator(val symbol: String) {
    Equal("=="),
    NotEqual("!="),
    LessOrEqual("<="),
    GreaterOrEqual(">="),
    Less("<"),
    Greater(">")
}

/** The type of a function's parameter, as RFC 9535 names it. */
internal enum class ParameterType {
    /** A value, or nothing: a literal, a singular query or a function that gives a value. */
    ValueType,

    /** The values a query selects. */
    NodesType
}

/**
 * A function of RFC 9535.
 *
 * @property functionName its name, as it is written.
 * @property parameters its parameters' types.
 * @property givesValue whether it gives a value, which a comparison compares, rather than a logical result, which a
 *   filter tests.
 */
internal enum class FilterFunction(val functionName: String, val parameters: List<ParameterType>, val givesValue: Boolean) {
    Length("length", listOf(ParameterType.ValueType), givesValue = true),
    Count("count", listOf(ParameterType.NodesType), givesValue = true),
    Match("match", listOf(ParameterType.ValueType, ParameterType.ValueType), givesValue = false),
    Search("search", listOf(ParameterType.ValueType, ParameterType.ValueType), givesValue = false),
    Value("value", listOf(ParameterType.NodesType), givesValue = true)
}

/** A call of [function] with [arguments], one for each of its parameters. */
internal class FunctionCall(val function: FilterFunction, val arguments: List<Argument>)

/** An argument of a function. */
internal sealed interface Argument {
    /** A value, or nothing, for a parameter of [ParameterType.ValueType]. */
    class Single(val value: Comparand) : Argument

    /** The values [query] selects, for a parameter of [ParameterType.NodesType]. */
    class Nodes(val query: PathQuery) : Argument
}
