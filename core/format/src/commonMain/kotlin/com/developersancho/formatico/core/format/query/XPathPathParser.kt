package com.developersancho.formatico.core.format.query

/** The tokens a step starts with. */
private val STEP_STARTS = setOf(
    XPathTokenKind.Dot,
    XPathTokenKind.DotDot,
    XPathTokenKind.At,
    XPathTokenKind.AxisName,
    XPathTokenKind.NameTest,
    XPathTokenKind.NodeType
)

/** `//`: `/descendant-or-self::node()/`. */
private val DESCENDANT_OR_SELF = XPathStep(XPathAxis.DescendantOrSelf, NodeTest.AnyNode, emptyList())

/** `.`: `self::node()`. */
private val SELF = XPathStep(XPathAxis.Self, NodeTest.AnyNode, emptyList())

/** `..`: `parent::node()`. */
private val PARENT = XPathStep(XPathAxis.Parent, NodeTest.AnyNode, emptyList())

/** Reads an XPath's paths: location paths, absolute and relative, and the filter expressions steps may follow. */
internal class XPathPathParser(private val cursor: XPathTokenCursor, private val parser: XPathParser) {
    /** The path next: a location path, or a primary expression, its predicates, and the steps after it. */
    fun path(): XPathExpression {
        val token = cursor.current
        if (token.kind in STEP_STARTS || token.kind == XPathTokenKind.Slash || token.kind == XPathTokenKind.DoubleSlash) {
            return locationPath()
        }
        val primary = parser.primary()
        val predicates = parser.predicates()
        val steps = relative(ArrayList())
        if (predicates.isEmpty() && steps.isEmpty()) return primary
        return XPathExpression.FilterPath(parser.nodeSet(primary, token.start), predicates, steps)
    }

    /** The location path next: `/` alone, `/` or `//` and a relative one, or a relative one. */
    private fun locationPath(): XPathExpression = when {
        cursor.accept(XPathTokenKind.Slash) -> {
            val steps = if (cursor.current.kind in STEP_STARTS) relative(arrayListOf(step())) else emptyList()
            XPathExpression.LocationPath(absolute = true, steps)
        }

        cursor.accept(
            XPathTokenKind.DoubleSlash
        ) -> XPathExpression.LocationPath(absolute = true, relative(arrayListOf(DESCENDANT_OR_SELF, step())))

        else -> XPathExpression.LocationPath(absolute = false, relative(arrayListOf(step())))
    }

    /** [steps], and a step after each `/` and each `//` next, as many as follow. */
    private fun relative(steps: MutableList<XPathStep>): List<XPathStep> {
        while (true) {
            when (cursor.current.kind) {
                XPathTokenKind.Slash -> cursor.advance()

                XPathTokenKind.DoubleSlash -> {
                    cursor.advance()
                    steps += DESCENDANT_OR_SELF
                }

                else -> return steps
            }
            steps += step()
        }
    }

    /** The step next: `.`, `..`, or an axis, `child::` where none is written, a node test and its predicates. */
    private fun step(): XPathStep = when {
        cursor.accept(XPathTokenKind.Dot) -> SELF
        cursor.accept(XPathTokenKind.DotDot) -> PARENT
        else -> XPathStep(axis(), nodeTest(), parser.predicates())
    }

    /** The axis next: `@`, a name and `::`, or the child axis where neither is written. */
    private fun axis(): XPathAxis {
        val token = cursor.current
        return when (token.kind) {
            XPathTokenKind.At -> {
                cursor.advance()
                XPathAxis.Attribute
            }

            XPathTokenKind.AxisName -> {
                cursor.advance()
                val axis = XPathAxis.entries.firstOrNull { it.axisName == token.text }
                    ?: queryError(if (token.text == "namespace") QueryErrorCode.Unsupported else QueryErrorCode.UnknownAxis, token.start)
                cursor.expect(XPathTokenKind.ColonColon)
                axis
            }

            else -> XPathAxis.Child
        }
    }

    /** The node test next: a name test, or a node type and its parentheses. */
    private fun nodeTest(): NodeTest {
        val token = cursor.current
        return when (token.kind) {
            XPathTokenKind.NameTest -> {
                cursor.advance()
                when {
                    token.text == "*" -> NodeTest.AnyName
                    token.text.endsWith(":*") -> NodeTest.Prefixed(token.text.dropLast(2))
                    else -> NodeTest.Named(token.text)
                }
            }

            XPathTokenKind.NodeType -> {
                cursor.advance()
                cursor.expect(XPathTokenKind.LeftParen)
                val test = when (token.text) {
                    "node" -> NodeTest.AnyNode
                    "text" -> NodeTest.TextNode
                    "comment" -> NodeTest.CommentNode
                    else -> NodeTest.Instruction(if (cursor.current.kind == XPathTokenKind.Literal) cursor.advance().text else null)
                }
                cursor.expect(XPathTokenKind.RightParen)
                test
            }

            else -> cursor.unexpected()
        }
    }
}
