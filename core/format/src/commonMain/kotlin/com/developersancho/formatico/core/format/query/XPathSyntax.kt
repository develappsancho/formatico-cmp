package com.developersancho.formatico.core.format.query

/** How many arguments `substring()` takes at most: a string, where to start and how many characters. */
private const val SUBSTRING_MAX_ARGUMENTS = 3

/** How many arguments `translate()` takes: a string, the characters to translate, and what they are translated into. */
private const val TRANSLATE_ARGUMENTS = 3

/** The four types of XPath 1.0's values; an expression's is known as it is read, since no variable is read. */
internal enum class XPathType { NodeSet, Number, Text, Truth }

/** An XPath expression, read. */
internal sealed interface XPathExpression {
    /** The type of what it evaluates to. */
    val type: XPathType

    /** `or`, where [any], or `and`: of [operands], each read as a boolean, in turn until one decides. */
    class Logical(val any: Boolean, val operands: List<XPathExpression>) : XPathExpression {
        override val type: XPathType get() = XPathType.Truth
    }

    /**
     * [first], then each of [operations] in turn on what came before, of one precedence: `a = b != c`, `a + b - c`.
     * A comparison gives a boolean, the arithmetic operators a number.
     */
    class Operations(val first: XPathExpression, val operations: List<Operation>) : XPathExpression {
        override val type: XPathType get() = if (operations.first().operator.compares) XPathType.Truth else XPathType.Number
    }

    /** [operand] as a number, negated where [negated]: `-x`, `--x`. */
    class Negation(val operand: XPathExpression, val negated: Boolean) : XPathExpression {
        override val type: XPathType get() = XPathType.Number
    }

    /** The union of the node-sets of [operands]: `a | b`. */
    class Union(val operands: List<XPathExpression>) : XPathExpression {
        override val type: XPathType get() = XPathType.NodeSet
    }

    /** A location path: [steps], from the root where it is [absolute], from the context node where it is not. */
    class LocationPath(val absolute: Boolean, val steps: List<XPathStep>) : XPathExpression {
        override val type: XPathType get() = XPathType.NodeSet
    }

    /** The node-set of [primary], filtered by [predicates] in document order, then followed by [steps]. */
    class FilterPath(val primary: XPathExpression, val predicates: List<XPathExpression>, val steps: List<XPathStep>) : XPathExpression {
        override val type: XPathType get() = XPathType.NodeSet
    }

    /** A string. */
    class Literal(val value: String) : XPathExpression {
        override val type: XPathType get() = XPathType.Text
    }

    /** A number. */
    class NumberLiteral(val value: Double) : XPathExpression {
        override val type: XPathType get() = XPathType.Number
    }

    /** A call of [function] with [arguments]. */
    class Call(val function: XPathFunction, val arguments: List<XPathExpression>) : XPathExpression {
        override val type: XPathType get() = function.result
    }
}

/** An [operator] and its right-hand [operand]. */
internal class Operation(val operator: XPathOperator, val operand: XPathExpression)

/** A binary operator of XPath but `or`, `and` and `|`. */
internal enum class XPathOperator(val compares: Boolean) {
    Equal(compares = true),
    NotEqual(compares = true),
    Less(compares = true),
    LessOrEqual(compares = true),
    Greater(compares = true),
    GreaterOrEqual(compares = true),
    Plus(compares = false),
    Minus(compares = false),
    Multiply(compares = false),
    Div(compares = false),
    Mod(compares = false);

    /** The operator that compares the other way round: `a < b` is `b > a`. */
    val mirrored: XPathOperator
        get() = when (this) {
            Less -> Greater
            LessOrEqual -> GreaterOrEqual
            Greater -> Less
            GreaterOrEqual -> LessOrEqual
            else -> this
        }
}

/** A step of a location path: the nodes along [axis] that pass [test], filtered by [predicates] in the axis's order. */
internal class XPathStep(val axis: XPathAxis, val test: NodeTest, val predicates: List<XPathExpression>)

/**
 * An axis of XPath 1.0, but the namespace axis.
 *
 * @property axisName its name, as it is written before `::`.
 * @property reverse whether it runs against document order, so that its nearest node is its first.
 */
internal enum class XPathAxis(val axisName: String, val reverse: Boolean = false) {
    Ancestor("ancestor", reverse = true),
    AncestorOrSelf("ancestor-or-self", reverse = true),
    Attribute("attribute"),
    Child("child"),
    Descendant("descendant"),
    DescendantOrSelf("descendant-or-self"),
    Following("following"),
    FollowingSibling("following-sibling"),
    Parent("parent", reverse = true),
    Preceding("preceding", reverse = true),
    PrecedingSibling("preceding-sibling", reverse = true),
    Self("self")
}

/** What a step's nodes must be. */
internal sealed interface NodeTest {
    /** `*`: any node of the axis's principal type, an attribute on the attribute axis and an element on the others. */
    data object AnyName : NodeTest

    /** `prefix:*`: a node of the principal type whose name is written with [prefix]. */
    class Prefixed(val prefix: String) : NodeTest

    /** A node of the principal type whose name is written [name]. */
    class Named(val name: String) : NodeTest

    /** `node()`: any node. */
    data object AnyNode : NodeTest

    /** `text()`. */
    data object TextNode : NodeTest

    /** `comment()`. */
    data object CommentNode : NodeTest

    /** `processing-instruction()`, or `processing-instruction('target')`, where [target] is given. */
    class Instruction(val target: String?) : NodeTest
}

/**
 * A function of XPath 1.0's core library.
 *
 * @property functionName its name, as it is written.
 * @property result the type of what it gives.
 * @property minArguments how many arguments it takes at least.
 * @property maxArguments how many it takes at most.
 * @property takesNodeSets whether its arguments must be node-sets, which no other type is turned into.
 */
internal enum class XPathFunction(
    val functionName: String,
    val result: XPathType,
    val minArguments: Int,
    val maxArguments: Int,
    val takesNodeSets: Boolean = false
) {
    Last("last", XPathType.Number, 0, 0),
    Position("position", XPathType.Number, 0, 0),
    Count("count", XPathType.Number, 1, 1, takesNodeSets = true),
    Id("id", XPathType.NodeSet, 1, 1),
    LocalName("local-name", XPathType.Text, 0, 1, takesNodeSets = true),
    NamespaceUri("namespace-uri", XPathType.Text, 0, 1, takesNodeSets = true),
    Name("name", XPathType.Text, 0, 1, takesNodeSets = true),
    StringOf("string", XPathType.Text, 0, 1),
    Concat("concat", XPathType.Text, 2, Int.MAX_VALUE),
    StartsWith("starts-with", XPathType.Truth, 2, 2),
    Contains("contains", XPathType.Truth, 2, 2),
    SubstringBefore("substring-before", XPathType.Text, 2, 2),
    SubstringAfter("substring-after", XPathType.Text, 2, 2),
    Substring("substring", XPathType.Text, 2, SUBSTRING_MAX_ARGUMENTS),
    StringLength("string-length", XPathType.Number, 0, 1),
    NormalizeSpace("normalize-space", XPathType.Text, 0, 1),
    Translate("translate", XPathType.Text, TRANSLATE_ARGUMENTS, TRANSLATE_ARGUMENTS),
    BooleanOf("boolean", XPathType.Truth, 1, 1),
    Not("not", XPathType.Truth, 1, 1),
    True("true", XPathType.Truth, 0, 0),
    False("false", XPathType.Truth, 0, 0),
    Lang("lang", XPathType.Truth, 1, 1),
    NumberOf("number", XPathType.Number, 0, 1),
    Sum("sum", XPathType.Number, 1, 1, takesNodeSets = true),
    Floor("floor", XPathType.Number, 1, 1),
    Ceiling("ceiling", XPathType.Number, 1, 1),
    Round("round", XPathType.Number, 1, 1)
}
