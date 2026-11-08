package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.designsystem.FormaticoPreview
import com.developersancho.formatico.core.designsystem.PreviewCase
import com.developersancho.formatico.core.designsystem.PreviewCases
import com.developersancho.formatico.core.format.Format

/** [text], named [name] and read as [format], shown as its graph, [selected] chosen. */
internal fun graphed(name: String, text: String, format: Format?, selected: String? = null, expanded: Set<String>? = null) =
    previewState(name, text, format, DocumentView(pane = DocumentPane.Graph, selected = selected, expanded = expanded))

/** The order of the Graph boards, its customer chosen, every card open: Graph-1440's, Graph-834's and Graph-390's state. */
internal val OrderGraph = graphed("order.json", PreviewBoardOrder, Format.Json, selected = "$.customer")

/** How many postcodes the people of [peopleOf] live in. */
private const val POSTCODES = 100

/** How many people the "large" case lists: more cards than a graph opens all of. */
private const val MANY_PEOPLE = 150

/** How many totals the "long" case's card holds: more values than a card shows. */
private const val MANY_TOTALS = 60

/** A list of [count] people, each an object with an address in it: more cards than a graph opens all of. */
internal fun peopleOf(count: Int): String = (0 until count).joinToString(",\n", "[\n", "\n]\n") { index ->
    "  { \"id\": $index, \"name\": \"Person $index\", \"address\": { \"city\": \"İzmir\", \"zip\": \"35${index % POSTCODES}\" } }"
}

/** A case of the graph: [state], laid out for [layout], in a pane as tall as [height]. */
internal class GraphCase(val state: WorkspaceState, val layout: WorkspaceLayout, val height: Int) {
    /** The board the pane stands in, and the width it has there: half a wide window, or the whole. */
    val board get() = LaidOutState(state, layout).board

    val width: Int
        get() = if (layout == WorkspaceLayout.Wide) board.width / 2 else board.width
}

/**
 * The graph's cases: the order, its customer chosen, as the three boards draw it; its customer closed; a deployment,
 * YAML, from the top down; a list of 150 people, which opens to its second level; a card of more values than it shows;
 * and plain text, which has no graph.
 */
internal class GraphCases :
    PreviewCases<GraphCase>(
        "order" to GraphCase(OrderGraph, WorkspaceLayout.Wide, height = 760),
        "order on a tablet" to GraphCase(OrderGraph, WorkspaceLayout.Tablet, height = 760),
        "order on a phone" to GraphCase(OrderGraph, WorkspaceLayout.Phone, height = 560),
        "closed" to GraphCase(
            graphed("order.json", PreviewBoardOrder, Format.Json, selected = "$.shipping", expanded = setOf("$", "$.items")),
            WorkspaceLayout.Wide,
            height = 560
        ),
        "yaml" to GraphCase(graphed("deployment.yaml", PreviewDeployment, Format.Yaml), WorkspaceLayout.Phone, height = 560),
        "large" to GraphCase(graphed("people.json", peopleOf(MANY_PEOPLE), Format.Json), WorkspaceLayout.Wide, height = 560),
        "long" to GraphCase(
            graphed("totals.json", (1..MANY_TOTALS).joinToString(", ", "{ \"totals\": [", "] }\n") { "$it" }, Format.Json),
            WorkspaceLayout.Wide,
            height = 560
        ),
        "plain text" to GraphCase(graphed("notes.txt", "Things to check before the release.\n", null), WorkspaceLayout.Phone, height = 400)
    )

@Preview
@Composable
internal fun GraphPreview(@PreviewParameter(GraphCases::class) case: PreviewCase<GraphCase>) {
    FormaticoPreview(case, Modifier.width(case.value.width.dp).height(case.value.height.dp)) { graph ->
        val commands = rememberDocumentCommands(remember { Workbench() })
        if (graph.layout == WorkspaceLayout.Phone) {
            if (graph.state.root == null) {
                TreePaneMessage(state = graph.state)
            } else {
                DocumentGraphView(state = graph.state, layout = graph.layout, onIntent = {})
            }
        } else {
            GraphPane(state = graph.state, layout = graph.layout, onIntent = {}, commands = commands)
        }
    }
}
