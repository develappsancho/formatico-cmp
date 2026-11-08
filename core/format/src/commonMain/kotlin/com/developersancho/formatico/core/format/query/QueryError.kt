package com.developersancho.formatico.core.format.query

/** Why a query could not be read: [code], at [offset], the place of a character in the query (its length for its end). */
data class QueryError(val code: QueryErrorCode, val offset: Int)

/** What is wrong with a query, in JSONPath or in XPath; each says where the offset of its [QueryError] points. */
enum class QueryErrorCode {
    /** The query is empty, or nothing but whitespace. The offset is 0. */
    Empty,

    /** The query ends where more of it is needed: `$[0`, `$.`, `/a[`, `concat('a',`. The offset is the query's length. */
    UnexpectedEnd,

    /**
     * A character, or a word, stands where the language does not allow it: `$.a b`, `$[?@.a = 1]`, a space after a
     * JSONPath, `a b` and `1 +)` in XPath. The offset is the character's, or the word's first.
     */
    UnexpectedCharacter,

    /** A JSONPath does not start with `$`, the document's root, at its very first character. The offset is 0. */
    MissingRoot,

    /** A string is never closed. The offset is its opening quote's. */
    UnclosedString,

    /**
     * A JSONPath string holds an escape JSONPath does not have: `\a`, `\u12`, a `\"` between single quotes, or half of
     * a surrogate pair. The offset is the backslash's.
     */
    InvalidEscape,

    /**
     * A JSONPath number is not written as JSONPath writes one: `01`, `1.`, `1e`, or `-0` as an index. The offset is its
     * first character's.
     */
    InvalidNumber,

    /**
     * A JSONPath index, a slice's bound or its step is beyond I-JSON's integers, -(2^53 - 1) to 2^53 - 1, as RFC 9535
     * requires. The offset is its first character's.
     */
    IntegerOutOfRange,

    /**
     * A JSONPath query that can select more than one value stands where at most one is allowed: on either side of a
     * comparison, `$[?@.* == 1]`, or as a value given to a function, `length(@..a)`. The offset is its `@` or `$`.
     */
    NotSingular,

    /**
     * An expression of a type that cannot stand where it does. In JSONPath: a literal as a filter's test, `$[?1]`; a
     * function's result where its type is not allowed, `$[?length(@)]` or `$[?match(@, 'a') == true]`; an argument of
     * the wrong type, `count(1)`. In XPath: a value that is no node-set where one is needed, `count(1)`, `1 | //a`,
     * `'a'[1]` or `(1)/a`. The offset is the expression's first character's.
     */
    WrongType,

    /**
     * A function the language does not have: none of RFC 9535's five in JSONPath, none of XPath 1.0's core library in
     * XPath. The offset is its name's.
     */
    UnknownFunction,

    /** A function is given more or fewer arguments than it takes. The offset is its name's. */
    WrongArgumentCount,

    /** An XPath axis XPath does not have: `sibling::a`. The offset is its name's. */
    UnknownAxis,

    /** A part of XPath 1.0 that is not evaluated: a variable, `$x`, or the namespace axis. The offset is the `$`, or the axis's name. */
    Unsupported,

    /**
     * The query nests brackets, parentheses, filters and functions deeper than is read, [MAX_QUERY_DEPTH] levels. The
     * offset is the bracket's or the parenthesis's that goes one level too deep.
     */
    TooDeep
}

/** What reading a query gave: the query, or why it could not be read. */
sealed interface QueryParse<out Q> {
    /** The query, read. */
    data class Parsed<Q>(val query: Q) : QueryParse<Q>

    /** Why the query could not be read. */
    data class Failed(val error: QueryError) : QueryParse<Nothing>
}

/**
 * How deep brackets, parentheses, filters and functions are nested at most in a query, so that reading it, and running
 * it, stays within the stack.
 */
internal const val MAX_QUERY_DEPTH = 64

/** Ends reading a query, which [readQuery] turns into [QueryParse.Failed]. */
internal class QueryException(val error: QueryError) : RuntimeException("${error.code} at ${error.offset}")

/** Stops reading a query: it is wrong with [code] at [offset]. */
internal fun queryError(code: QueryErrorCode, offset: Int): Nothing = throw QueryException(QueryError(code, offset))

/** What [read] reads, or the error it stops at. */
internal inline fun <Q> readQuery(read: () -> Q): QueryParse<Q> = try {
    QueryParse.Parsed(read())
} catch (stopped: QueryException) {
    QueryParse.Failed(stopped.error)
}
