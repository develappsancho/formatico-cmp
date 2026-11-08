package com.developersancho.formatico.core.format.query

/** The kinds of value JSONPath tells apart. */
enum class JsonKind {
    /** An object: members, each with a name. */
    Object,

    /** An array: items, in order. */
    Array,

    /** A string. */
    String,

    /** A number. */
    Number,

    /** `true` or `false`. */
    Boolean,

    /** `null`. */
    Null
}

/** A document's values as JSONPath reads them, whatever the format: [N] is the document's own node. */
interface JsonValues<N> {
    /** The kind of [node]. */
    fun kindOf(node: N): JsonKind

    /** An object's members' values, or an array's items, in order; nothing for any other value. */
    fun childrenOf(node: N): List<N>

    /** The name of [member], a value of an object. */
    fun nameOf(member: N): String

    /** The value of [node], a string. */
    fun stringOf(node: N): String

    /** The value of [node], a number. */
    fun numberOf(node: N): Double

    /** The value of [node], `true` or `false`. */
    fun booleanOf(node: N): Boolean
}

/**
 * A JSONPath query, RFC 9535 as a whole: the child and descendant segments; the name, wildcard, index, slice and filter
 * selectors; and in filters, comparisons, `&&`, `||`, `!`, parentheses, literals, `@` and `$` queries, and the
 * functions `length()`, `count()`, `match()`, `search()` and `value()`, whose regular expressions are I-Regexps (RFC
 * 9485). It is read as strictly as the RFC writes it: no whitespace before the `$` or after the query's end, and the
 * rules on singular queries and on the types of functions are checked as it is read.
 *
 * Two places the RFC leaves to the document are settled so: an object's members are visited in the order they are
 * written; and where an object gives a name twice, a name selects every member of that name, a comparison takes a
 * query that selects more than one of them for no value, as `value()` does, and two objects are equal where their
 * members of each name are equal in order.
 */
class JsonPath private constructor(private val query: PathQuery) {
    /** The values of the document whose root is [root] the query selects, in the RFC's order, duplicates kept. */
    fun <N> select(root: N, values: JsonValues<N>): List<N> = JsonPathEvaluator(root, values).select(query, root)

    companion object {
        /** Reads [query] as a JSONPath. */
        fun parse(query: String): QueryParse<JsonPath> = readQuery { JsonPath(JsonPathParser(JsonPathCursor(query)).query()) }
    }
}
