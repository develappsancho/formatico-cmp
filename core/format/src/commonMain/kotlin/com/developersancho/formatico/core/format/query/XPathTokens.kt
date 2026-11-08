package com.developersancho.formatico.core.format.query

/** The kind of an [XPathToken], as XPath 1.0's lexical structure tells them apart. */
internal enum class XPathTokenKind(val isOperator: Boolean = false) {
    LeftParen,
    RightParen,
    LeftBracket,
    RightBracket,
    Dot,
    DotDot,
    At,
    Comma,
    ColonColon,

    /** A name test: `*`, `prefix:*`, or a name, `prefix:name` or `name`. */
    NameTest,

    /** `comment`, `text`, `processing-instruction` or `node`, before a `(`. */
    NodeType,

    /** A function's name, before a `(`. */
    FunctionName,

    /** An axis's name, before a `::`. */
    AxisName,
    And(isOperator = true),
    Or(isOperator = true),
    Mod(isOperator = true),
    Div(isOperator = true),
    Multiply(isOperator = true),
    Slash(isOperator = true),
    DoubleSlash(isOperator = true),
    Pipe(isOperator = true),
    Plus(isOperator = true),
    Minus(isOperator = true),
    Equal(isOperator = true),
    NotEqual(isOperator = true),
    Less(isOperator = true),
    LessOrEqual(isOperator = true),
    Greater(isOperator = true),
    GreaterOrEqual(isOperator = true),

    /** A string, its quotes taken off. */
    Literal,

    /** A number, as it is written. */
    Number,

    /** The end of the expression. */
    End
}

/** A token of an XPath: [kind], [text], and [start], where it starts in the expression. */
internal class XPathToken(val kind: XPathTokenKind, val text: String, val start: Int)

/**
 * Where the parsers of an XPath are among its [tokens], the last of which is [XPathTokenKind.End]; and how deep its
 * parentheses, predicates and functions are nested there, which is bounded by [MAX_QUERY_DEPTH]. [length] is the
 * expression's.
 */
internal class XPathTokenCursor(private val tokens: List<XPathToken>, private val length: Int) {
    private var index = 0
    private var depth = 0

    /** The token next. */
    val current: XPathToken get() = tokens[index]

    /** Reads the token next. */
    fun advance(): XPathToken = tokens[index].also { if (it.kind != XPathTokenKind.End) index++ }

    /** Reads the token next where it is of [kind], and says whether it was. */
    fun accept(kind: XPathTokenKind): Boolean {
        if (current.kind != kind) return false
        advance()
        return true
    }

    /** Reads the token next, which must be of [kind]. */
    fun expect(kind: XPathTokenKind): XPathToken = if (current.kind == kind) advance() else unexpected()

    /** Stops at the token next, which cannot stand where it does, or at the end where there is none. */
    fun unexpected(): Nothing = if (current.kind == XPathTokenKind.End) {
        queryError(QueryErrorCode.UnexpectedEnd, length)
    } else {
        queryError(QueryErrorCode.UnexpectedCharacter, current.start)
    }

    /** Goes one level deeper, at a parenthesis, a predicate or a function. */
    fun enter() {
        if (++depth > MAX_QUERY_DEPTH) queryError(QueryErrorCode.TooDeep, current.start)
    }

    /** Comes back from the level [enter] went into. */
    fun leave() {
        depth--
    }
}
