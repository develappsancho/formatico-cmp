package com.developersancho.formatico.core.format.yaml

/**
 * Where the line that ends a node written in [placed]'s place is ended from: an anchored node written as its alias ends
 * its key's line, [headerEnd]; anything else, the end of what stands in [placed]'s place in the text.
 */
internal fun lineEndOf(node: YamlNode, placed: YamlNode, headerEnd: Int): Int =
    if (node is YamlAlias && node !== placed) headerEnd else placed.range.end

/** The name a key is sorted by: a scalar's value, anything else as it is written. */
internal fun sortName(node: YamlNode, text: String): String =
    (node as? YamlScalar)?.value ?: text.substring(node.range.start, node.range.end)

/**
 * The order a block mapping's entries are written in: their own, or sorted by their keys' names where [sortKeys] says
 * so. A flow mapping is written as it is.
 */
internal fun entryOrder(node: YamlMapping, text: String, sortKeys: Boolean): List<Int> =
    if (sortKeys && !node.flow) node.entries.indices.sortedBy { sortName(node.entries[it].key, text) } else node.entries.indices.toList()

/**
 * Where writing [documents]' keys sorted would put an alias before the anchor it points at, which YAML does not allow:
 * the node written in each one's place instead, so that every value stays the same. The anchored node is written where
 * its first alias comes, with its anchor, and that alias where the anchored node stood. An alias or an anchored node in
 * a flow collection, which is written as it is, is not moved; whoever sorts still reads the result again.
 */
internal fun anchorSwaps(documents: List<YamlNode?>, text: String): Map<YamlNode, YamlNode> {
    val walk = AnchorWalk(text)
    documents.filterNotNull().forEach(walk::inFlowOf)
    documents.filterNotNull().forEach(walk::walk)
    return walk.swaps
}

/** Walks the documents in the order their sorted keys are written, moving an anchor up to its first alias. */
private class AnchorWalk(private val text: String) {
    val swaps = HashMap<YamlNode, YamlNode>()
    private val seen = HashSet<String>()
    private val inFlow = HashSet<YamlNode>()

    /** Notes every node inside a flow collection under [node]. */
    fun inFlowOf(node: YamlNode, flow: Boolean = false) {
        if (flow) inFlow += node
        val childrenFlow = flow || (node as? YamlMapping)?.flow == true || (node as? YamlSequence)?.flow == true
        childrenOf(node).forEach { inFlowOf(it, childrenFlow) }
    }

    /** Walks [placed], where it is written: an anchored node written as its alias has nothing under it written there. */
    fun walk(placed: YamlNode) {
        val shown = swaps[placed] ?: placed
        when {
            shown !== placed -> Unit
            placed is YamlAlias -> moveAnchor(placed)
            else -> under(placed)
        }
    }

    /** The node [alias] points at written in its place, where its anchor is not written yet, and walked there. */
    private fun moveAnchor(alias: YamlAlias) {
        val target = alias.target ?: return
        val inside = alias.range.start >= target.range.start && alias.range.end <= target.range.end
        if (alias.name in seen || inside) return
        // A flow collection is written as it is: neither its alias nor its anchored node can take the other's place.
        if (alias in inFlow || target in inFlow) return
        swaps[alias] = target
        swaps[target] = alias
        under(target)
    }

    /** What is written of [node]: its anchor, and what stands under it, in the order it is written. */
    private fun under(node: YamlNode) {
        node.anchor?.let(seen::add)
        when {
            node is YamlMapping && !node.flow -> entryOrder(node, text, sortKeys = true).forEach { walk(node.entries[it].value) }
            node is YamlSequence && !node.flow -> node.items.forEach(::walk)
            else -> anchorsIn(node)
        }
    }

    /** The anchors of a flow collection, written as it is, and of what is in it. */
    private fun anchorsIn(node: YamlNode) {
        node.anchor?.let(seen::add)
        childrenOf(node).forEach(::anchorsIn)
    }

    private fun childrenOf(node: YamlNode): List<YamlNode> = when (node) {
        is YamlMapping -> node.entries.map { it.value }
        is YamlSequence -> node.items
        else -> emptyList()
    }
}
