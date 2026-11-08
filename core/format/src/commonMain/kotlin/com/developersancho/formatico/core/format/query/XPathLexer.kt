package com.developersancho.formatico.core.format.query

import com.developersancho.formatico.core.format.xml.isNameChar
import com.developersancho.formatico.core.format.xml.isNameStart
import com.developersancho.formatico.core.format.xml.whitespaceEnd

/** The node types a node test names, before its `(`. */
private val NODE_TYPES = setOf("comment", "text", "processing-instruction", "node")

/** The operators written as names, where an operator is expected. */
private val OPERATOR_NAMES = mapOf(
    "and" to XPathTokenKind.And,
    "or" to XPathTokenKind.Or,
    "mod" to XPathTokenKind.Mod,
    "div" to XPathTokenKind.Div
)

/** The tokens written in two characters, which are read before those in one. */
private val TWO_CHARACTER_TOKENS = mapOf(
    ".." to XPathTokenKind.DotDot,
    "//" to XPathTokenKind.DoubleSlash,
    "::" to XPathTokenKind.ColonColon,
    "!=" to XPathTokenKind.NotEqual,
    "<=" to XPathTokenKind.LessOrEqual,
    ">=" to XPathTokenKind.GreaterOrEqual
)

/** The tokens written in one character. */
private val ONE_CHARACTER_TOKENS = mapOf(
    '(' to XPathTokenKind.LeftParen,
    ')' to XPathTokenKind.RightParen,
    '[' to XPathTokenKind.LeftBracket,
    ']' to XPathTokenKind.RightBracket,
    '.' to XPathTokenKind.Dot,
    '@' to XPathTokenKind.At,
    ',' to XPathTokenKind.Comma,
    '/' to XPathTokenKind.Slash,
    '|' to XPathTokenKind.Pipe,
    '+' to XPathTokenKind.Plus,
    '-' to XPathTokenKind.Minus,
    '=' to XPathTokenKind.Equal,
    '<' to XPathTokenKind.Less,
    '>' to XPathTokenKind.Greater
)

/** The tokens after which a `*` or a name is no operator, but a name test, a function, a node type or an axis. */
private val BEFORE_OPERANDS = setOf(
    XPathTokenKind.At,
    XPathTokenKind.ColonColon,
    XPathTokenKind.LeftParen,
    XPathTokenKind.LeftBracket,
    XPathTokenKind.Comma
)

/**
 * Splits an XPath into its tokens, and tells a name and a `*` apart as XPath 1.0's lexical structure does: after an
 * operand, they are operators, `and`, `or`, `mod`, `div` and `*`; before a `(`, a name is a function or a node type;
 * before a `::`, an axis; anywhere else, a name test.
 */
internal class XPathLexer(private val text: String) {
    private var at = 0
    private val tokens = ArrayList<XPathToken>()

    /** The tokens of the expression, the last of which is [XPathTokenKind.End]. */
    fun tokens(): List<XPathToken> {
        at = text.whitespaceEnd(0)
        if (at == text.length) queryError(QueryErrorCode.Empty, 0)
        while (at < text.length) {
            tokens += next()
            at = text.whitespaceEnd(at)
        }
        tokens += XPathToken(XPathTokenKind.End, "", text.length)
        return tokens
    }

    private fun next(): XPathToken {
        val char = text[at]
        return when {
            char == '"' || char == '\'' -> literal(char)
            char in '0'..'9' || (char == '.' && text.getOrNull(at + 1) in '0'..'9') -> number()
            char == '$' -> queryError(QueryErrorCode.Unsupported, at)
            char == '*' -> token(if (operatorExpected()) XPathTokenKind.Multiply else XPathTokenKind.NameTest, length = 1)
            char != ':' && isNameStart(char) -> name()
            else -> symbol()
        }
    }

    /** Whether an operator is expected next: there is a token before, and it is an operand's last. */
    private fun operatorExpected(): Boolean {
        val previous = tokens.lastOrNull() ?: return false
        return !previous.kind.isOperator && previous.kind !in BEFORE_OPERANDS
    }

    /** The name next: a name test, `prefix:*` among them, a function, a node type, an axis or an operator. */
    private fun name(): XPathToken {
        val start = at
        at = nameEnd(at)
        var wildcard = false
        val next = text.getOrNull(at + 1)
        if (text.getOrNull(at) == ':' && next != null && next != ':') {
            if (next == '*') {
                at += 2
                wildcard = true
            } else if (isNameStart(next)) {
                at = nameEnd(at + 1)
            }
        }
        val name = text.substring(start, at)
        return XPathToken(kindOfName(name, wildcard, start), name, start)
    }

    /** What the name [name], read from [start] up to the place next, is, where `prefix:*` is [wildcard]. */
    private fun kindOfName(name: String, wildcard: Boolean, start: Int): XPathTokenKind {
        if (operatorExpected()) return OPERATOR_NAMES[name] ?: queryError(QueryErrorCode.UnexpectedCharacter, start)
        val after = text.whitespaceEnd(at)
        return when {
            wildcard -> XPathTokenKind.NameTest
            text.startsWith("::", after) -> XPathTokenKind.AxisName
            text.getOrNull(after) == '(' -> if (name in NODE_TYPES) XPathTokenKind.NodeType else XPathTokenKind.FunctionName
            else -> XPathTokenKind.NameTest
        }
    }

    /** Where the name without a colon that starts at [from] ends. */
    private fun nameEnd(from: Int): Int {
        var end = from
        while (end < text.length && text[end] != ':' && isNameChar(text[end])) end++
        return end
    }

    /** The number next: digits, a `.` and digits after it, either part left out but not both. */
    private fun number(): XPathToken {
        val start = at
        while (text.getOrNull(at) in '0'..'9') at++
        if (text.getOrNull(at) == '.') {
            at++
            while (text.getOrNull(at) in '0'..'9') at++
        }
        return XPathToken(XPathTokenKind.Number, text.substring(start, at), start)
    }

    /** The string between the [quote] next and the next one; XPath 1.0 has no escapes in it. */
    private fun literal(quote: Char): XPathToken {
        val close = text.indexOf(quote, at + 1)
        if (close < 0) queryError(QueryErrorCode.UnclosedString, at)
        val token = XPathToken(XPathTokenKind.Literal, text.substring(at + 1, close), at)
        at = close + 1
        return token
    }

    /** The punctuation, or the operator written in symbols, next. */
    private fun symbol(): XPathToken {
        TWO_CHARACTER_TOKENS[text.substring(at, minOf(at + 2, text.length))]?.let { return token(it, length = 2) }
        val kind = ONE_CHARACTER_TOKENS[text[at]] ?: queryError(QueryErrorCode.UnexpectedCharacter, at)
        return token(kind, length = 1)
    }

    private fun token(kind: XPathTokenKind, length: Int): XPathToken {
        val token = XPathToken(kind, text.substring(at, at + length), at)
        at += length
        return token
    }
}
