package com.developersancho.formatico.core.format.query

/**
 * The nodes along [axis] from [node], in the axis's order: document order for a forward axis, the nearest first for
 * a reverse one, as a step's positions count them. An attribute has no children and no siblings; the nodes after it
 * are its owner's children and what follows, and those before it, what precedes its owner.
 */
internal fun XPathIndex.axis(node: XPathTreeNode, axis: XPathAxis): List<XPathTreeNode> {
    val attribute = node.kind == XPathKind.Attribute
    return when (axis) {
        XPathAxis.Child -> node.children
        XPathAxis.Descendant -> if (attribute) emptyList() else nodes.subList(node.flat + 1, node.subtreeEnd)
        XPathAxis.DescendantOrSelf -> if (attribute) listOf(node) else nodes.subList(node.flat, node.subtreeEnd)
        XPathAxis.Parent -> listOfNotNull(node.parent)
        XPathAxis.Ancestor -> ancestors(node)
        XPathAxis.AncestorOrSelf -> listOf(node) + ancestors(node)
        XPathAxis.FollowingSibling -> siblings(node)?.let { it.subList(node.siblingIndex + 1, it.size) }.orEmpty()
        XPathAxis.PrecedingSibling -> siblings(node)?.subList(0, node.siblingIndex)?.asReversed().orEmpty()
        XPathAxis.Following -> nodes.subList(if (attribute) node.flat + 1 else node.subtreeEnd, nodes.size)
        XPathAxis.Preceding -> preceding(if (attribute) requireNotNull(node.parent) else node)
        XPathAxis.Attribute -> node.attributes
        XPathAxis.Self -> listOf(node)
    }
}

/** The siblings of [node] where it has any: its parent's children, unless it is an attribute or the root. */
private fun siblings(node: XPathTreeNode): List<XPathTreeNode>? =
    if (node.kind == XPathKind.Attribute) null else node.parent?.children

/** The ancestors of [node], the nearest first. */
private fun ancestors(node: XPathTreeNode): List<XPathTreeNode> {
    val ancestors = ArrayList<XPathTreeNode>()
    var parent = node.parent
    while (parent != null) {
        ancestors += parent
        parent = parent.parent
    }
    return ancestors
}

/** The nodes before [node] in document order but its ancestors, the nearest first. */
private fun XPathIndex.preceding(node: XPathTreeNode): List<XPathTreeNode> =
    nodes.subList(0, node.flat).filter { it.subtreeEnd <= node.flat }.asReversed()

/**
 * Whether [node] passes this test on [axis], whose principal node type is the attribute on the attribute axis, and the
 * element on any other.
 */
internal fun NodeTest.matches(node: XPathTreeNode, axis: XPathAxis): Boolean {
    val principal = if (axis == XPathAxis.Attribute) XPathKind.Attribute else XPathKind.Element
    return when (this) {
        NodeTest.AnyName -> node.kind == principal
        is NodeTest.Prefixed -> node.kind == principal && node.name.startsWith("$prefix:")
        is NodeTest.Named -> node.kind == principal && node.name == name
        NodeTest.AnyNode -> true
        NodeTest.TextNode -> node.kind == XPathKind.Text
        NodeTest.CommentNode -> node.kind == XPathKind.Comment
        is NodeTest.Instruction -> node.kind == XPathKind.Instruction && (target == null || node.name == target)
    }
}

/** [nodes] in document order, each once. */
internal fun inDocumentOrder(nodes: List<XPathTreeNode>): List<XPathTreeNode> {
    if (nodes.size < 2) return nodes
    val sorted = nodes.sortedBy { it.order }
    val distinct = ArrayList<XPathTreeNode>(sorted.size)
    sorted.forEach { if (distinct.lastOrNull() !== it) distinct += it }
    return distinct
}
