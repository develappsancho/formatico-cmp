package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.json.PathStep
import com.pantrixui.components.data.graph.GraphLink
import com.pantrixui.components.data.graph.GraphNode

/** How tall a card's head is, as the Graph boards draw it. */
internal val GraphHead = 32.dp

/** How tall a row of a card is. */
internal val GraphRow = 24.dp

/** The room under a card's last row. */
internal val GraphFoot = 6.dp

/** How many rows a card shows at most: past them, a row says how many more there are. */
internal const val MOST_CARD_ROWS = 50

/** How many cards a document has at most for its graph to open with all of them: past them, it opens to [LARGE_LEVEL]. */
internal const val MOST_OPEN_CARDS = 200

/** The level a large document's graph opens to. */
internal const val LARGE_LEVEL = 2

/**
 * A card of a document's graph: an object or an array, [node], with a value in it, as its head says: [title], the
 * document's name for the root, a member's key, an item by its array's name and its place (`items[0]`); its single
 * values as [rows], the first [MOST_CARD_ROWS], and how many [more] there are.
 */
@Immutable
internal class GraphCard(val node: ValueNode, val title: String, val rows: List<ValueNode>, val more: Int) {
    /** How many rows the card draws: its values', and one for the [more] where there are some. */
    val shownRows: Int get() = rows.size + if (more > 0) 1 else 0
}

/**
 * A document's graph as [com.pantrixui.components.data.graph.PantrixGraph] draws it: the [nodes] and [links] of the
 * cards shown, and those of each closed card's children, which its chevron opens; each card by its key, [cards].
 */
@Immutable
internal class DocumentGraph(val nodes: List<GraphNode>, val links: List<GraphLink>, val cards: Map<String, GraphCard>)

/** Whether [node] is a card of the graph: an object or an array that holds something. */
internal fun isCard(node: ValueNode): Boolean =
    (node.kind == ValueKind.Object || node.kind == ValueKind.Array) && node.children.isNotEmpty()

/**
 * The graph of the document whose tree is [root], named [name], its [open] cards showing what is in them: each card
 * [width] wide and as tall as its head and its rows, so that the graph lays it out without drawing it. [label] says a
 * card for a screen reader. A closed card's children are given too, unopened, for the graph to know it has some.
 */
internal fun documentGraph(root: ValueNode, name: String, open: Set<String>, width: Dp, label: (GraphCard) -> String): DocumentGraph {
    val nodes = ArrayList<GraphNode>()
    val links = ArrayList<GraphLink>()
    val cards = LinkedHashMap<String, GraphCard>()

    fun add(node: ValueNode, title: String, place: String): GraphCard {
        val card = cardOf(node, title)
        cards[node.key] = card
        nodes += GraphNode(key = node.key, label = label(card), size = sizeOf(card, width), tagKey = place)
        return card
    }
    val left = ArrayDeque<Triple<ValueNode, String, String>>()
    left.addLast(Triple(root, name, ROOT_TAG))
    while (left.isNotEmpty()) {
        val (node, title, place) = left.removeFirst()
        add(node, title, place)
        val opened = node.key in open
        node.children.forEachIndexed { index, child ->
            if (!isCard(child)) return@forEachIndexed
            links += GraphLink(from = node.key, to = child.key, label = linkLabelOf(child))
            val childPlace = if (place == ROOT_TAG) "$index" else "${place}_$index"
            if (opened) left.addLast(Triple(child, titleOf(child, title), childPlace)) else add(child, titleOf(child, title), childPlace)
        }
    }
    return DocumentGraph(nodes, links, cards)
}

/**
 * A card's automation key is its place in the tree, `0_2_1`, the root's `root`: short and of its own, which a path cut
 * at Pantrix's 32 characters could not be.
 */
private const val ROOT_TAG = "root"

/** The keys of the cards a document's graph opens with: all of them, or for a large document, those to [LARGE_LEVEL]. */
internal fun graphOpening(root: ValueNode): Set<String> {
    var cards = 0
    val left = ArrayDeque(listOf(root))
    while (left.isNotEmpty() && cards <= MOST_OPEN_CARDS) {
        val node = left.removeLast()
        if (!isCard(node)) continue
        cards++
        left.addAll(node.children)
    }
    return expandedTo(root, if (cards > MOST_OPEN_CARDS) LARGE_LEVEL else null)
}

/** The card [selected] stands in: itself where it is one, else the nearest that holds it. */
internal fun cardKeyOf(root: ValueNode?, selected: ValueNode?): String? {
    val chosen = selected ?: return null
    if (isCard(chosen)) return chosen.key
    return trail(root, chosen.key).dropLast(1).lastOrNull(::isCard)?.key
}

private fun cardOf(node: ValueNode, title: String): GraphCard {
    val values = node.children.filterNot(::isCard)
    return GraphCard(node, title, values.take(MOST_CARD_ROWS), (values.size - MOST_CARD_ROWS).coerceAtLeast(0))
}

private fun sizeOf(card: GraphCard, width: Dp): DpSize {
    val rows = card.shownRows
    return DpSize(width, GraphHead + GraphRow * rows + if (rows > 0) GraphFoot else 0.dp)
}

/** A card's title: a member by its key, an item by its array's [parentTitle] and its place. */
private fun titleOf(node: ValueNode, parentTitle: String): String = when (val step = node.path.lastOrNull()) {
    is PathStep.Index -> "$parentTitle[${step.index}]"
    is PathStep.Key -> step.name
    null -> parentTitle
}

/** What the link to [node] is written with: its key, or its place in brackets. */
private fun linkLabelOf(node: ValueNode): String = when (val step = node.path.lastOrNull()) {
    is PathStep.Index -> "[${step.index}]"
    is PathStep.Key -> step.name
    null -> ""
}
