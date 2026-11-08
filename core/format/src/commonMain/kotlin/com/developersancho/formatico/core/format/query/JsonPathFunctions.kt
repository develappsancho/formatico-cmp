package com.developersancho.formatico.core.format.query

/**
 * The functions of RFC 9535: `length()`, `count()` and `value()`, which give a value, and `match()` and `search()`,
 * which test a string against an I-Regexp. A pattern is compiled once for each run of a query.
 */
internal class JsonPathFunctions<N>(private val values: JsonValues<N>, private val evaluator: JsonPathEvaluator<N>) {
    private val patterns = HashMap<String, Regex?>()

    /** The value [call] gives where the filter tests [current]. */
    fun value(call: FunctionCall, current: N): FilterValue<N> = when (call.function) {
        FilterFunction.Length -> length(single(call.arguments[0], current))

        FilterFunction.Count -> number(nodes(call.arguments[0], current).size)

        FilterFunction.Value -> nodes(call.arguments[0], current).singleOrNull()?.let { FilterValue.Node(it) } ?: FilterValue.Absent

        // The parser compares no function whose result is logical.
        FilterFunction.Match, FilterFunction.Search -> FilterValue.Absent
    }

    /** Whether [call], `match()` or `search()`, is true where the filter tests [current]. */
    fun test(call: FunctionCall, current: N): Boolean = when (call.function) {
        FilterFunction.Match -> matches(call, current, whole = true)

        FilterFunction.Search -> matches(call, current, whole = false)

        // The parser tests no function that gives a value.
        FilterFunction.Length, FilterFunction.Count, FilterFunction.Value -> false
    }

    /** `length()`: a string's characters, an array's items or an object's members; nothing for any other value. */
    private fun length(value: FilterValue<N>): FilterValue<N> = when (value) {
        is FilterValue.Literal -> (value.value as? LiteralValue.Text)?.let { number(codePointCount(it.value)) } ?: FilterValue.Absent
        is FilterValue.Node -> lengthOf(value.node)
        FilterValue.Absent -> FilterValue.Absent
    }

    private fun lengthOf(node: N): FilterValue<N> = when (values.kindOf(node)) {
        JsonKind.String -> number(codePointCount(values.stringOf(node)))
        JsonKind.Array, JsonKind.Object -> number(values.childrenOf(node).size)
        JsonKind.Number, JsonKind.Boolean, JsonKind.Null -> FilterValue.Absent
    }

    /**
     * `match()`, where [whole], or `search()`: whether the first argument is a string the second, an I-Regexp, matches
     * as a whole, or in a part of it. Any other argument, a pattern that is no I-Regexp among them, is false.
     */
    private fun matches(call: FunctionCall, current: N, whole: Boolean): Boolean {
        val text = string(single(call.arguments[0], current))
        val pattern = string(single(call.arguments[1], current))
        val regex = if (text == null || pattern == null) null else regex(pattern)
        return when {
            text == null || regex == null -> false
            whole -> regex.matchEntire(text) != null
            else -> regex.containsMatchIn(text)
        }
    }

    private fun regex(pattern: String): Regex? {
        if (!patterns.containsKey(pattern)) patterns[pattern] = iRegexp(pattern)
        return patterns[pattern]
    }

    /** [value]'s string, where it is one. */
    private fun string(value: FilterValue<N>): String? = when (value) {
        is FilterValue.Literal -> (value.value as? LiteralValue.Text)?.value
        is FilterValue.Node -> if (values.kindOf(value.node) == JsonKind.String) values.stringOf(value.node) else null
        FilterValue.Absent -> null
    }

    /** The value of an argument of [ParameterType.ValueType], or the single value a query selects. */
    private fun single(argument: Argument, current: N): FilterValue<N> = when (argument) {
        is Argument.Single -> evaluator.value(argument.value, current)
        is Argument.Nodes -> evaluator.select(argument.query, current).singleOrNull()?.let { FilterValue.Node(it) } ?: FilterValue.Absent
    }

    /** The values an argument of [ParameterType.NodesType] selects. */
    private fun nodes(argument: Argument, current: N): List<N> = when (argument) {
        is Argument.Nodes -> evaluator.select(argument.query, current)
        is Argument.Single -> emptyList()
    }

    private fun number(count: Int): FilterValue<N> = FilterValue.Literal(LiteralValue.Number(count.toDouble()))
}
