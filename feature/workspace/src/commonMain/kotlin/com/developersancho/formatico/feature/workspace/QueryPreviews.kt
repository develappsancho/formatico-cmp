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

/** The queries the Query boards offer again: the order's, and the feed's. */
internal val PreviewRecentQueries = RecentQueries(
    jsonPath = listOf("$.items[*].name", "$..price", "$.customer.email", "$.items[?(@.qty > 1)]"),
    xPath = listOf("//item[category='release']/title", "//item/pubDate", "count(//item)")
)

/** The order queried, as a view leaves it: its [query] asked, and its value keyed [selected] chosen. */
private fun orderQueried(query: String, selected: String? = null) =
    previewState("order.json", PreviewOrder, Format.Json, DocumentView(selected = selected, pane = DocumentPane.Query, query = query))
        .copy(recentQueries = PreviewRecentQueries)

/** The feed queried with [query], its value keyed [selected] chosen. */
private fun feedQueried(query: String, selected: String? = null) =
    previewState("feed.xml", PreviewFeed, Format.Xml, DocumentView(selected = selected, pane = DocumentPane.Query, query = query))
        .copy(recentQueries = PreviewRecentQueries)

/** The order's items' names, the first chosen: Query-1440's state. */
internal val OrderNames = orderQueried("$.items[*].name", selected = "$.items[0].name")

/** The feed's releases' titles, the first chosen: Query-834's state. */
internal val FeedReleases = feedQueried("//item[category='release']/title", selected = "$.rss.channel.item[0].title")

/** The order's prices, the first chosen: Query-390's state. */
internal val OrderPrices = orderQueried("$..price", selected = "$.items[0].price")

/** A case of the query's pane: [state], laid out for [layout], in a pane as tall as [height]. */
internal class QueryCase(val state: WorkspaceState, val layout: WorkspaceLayout, val height: Int) {
    /** The board the pane stands in, and the width it has there: a wide window's share beside the text, or the whole. */
    val board get() = LaidOutState(state, layout).board

    val width: Int
        get() = if (layout == WorkspaceLayout.Wide) WIDE_QUERY_WIDTH else board.width
}

/**
 * The query's cases: what it found in the order, in the feed and on a phone; nothing asked yet; a query that is none,
 * one that finds nothing, an XPath that comes to a number, and a document with nothing read to query.
 */
internal class QueryCases :
    PreviewCases<QueryCase>(
        "json" to QueryCase(OrderNames, WorkspaceLayout.Wide, height = 640),
        "xml on a tablet" to QueryCase(FeedReleases, WorkspaceLayout.Tablet, height = 600),
        "json on a phone" to QueryCase(OrderPrices, WorkspaceLayout.Phone, height = 640),
        "nothing asked" to QueryCase(orderQueried(""), WorkspaceLayout.Wide, height = 400),
        "no query" to QueryCase(orderQueried("$.items["), WorkspaceLayout.Wide, height = 400),
        "no matches" to QueryCase(orderQueried("$.discount"), WorkspaceLayout.Wide, height = 400),
        "a value" to QueryCase(feedQueried("count(//item)"), WorkspaceLayout.Tablet, height = 480),
        "nothing read" to QueryCase(
            previewState("broken.json", "", Format.Json, DocumentView(pane = DocumentPane.Query, query = "$.id")),
            WorkspaceLayout.Wide,
            height = 400
        ),
        "no query on a phone" to QueryCase(orderQueried("$..["), WorkspaceLayout.Phone, height = 480)
    )

@Preview
@Composable
internal fun QueryPreview(@PreviewParameter(QueryCases::class) case: PreviewCase<QueryCase>) {
    FormaticoPreview(case, Modifier.width(case.value.width.dp).height(case.value.height.dp)) { query ->
        val commands = rememberDocumentCommands(remember { Workbench() })
        if (query.layout == WorkspaceLayout.Phone) {
            PhoneQuery(state = query.state, onIntent = {}, commands = commands)
        } else {
            QueryPane(state = query.state, layout = query.layout, onIntent = {}, commands = commands)
        }
    }
}

/** How wide the query stands beside the text in a wide window: 1440's share of 1 to the text's 1.15. */
private const val WIDE_QUERY_WIDTH = 670
