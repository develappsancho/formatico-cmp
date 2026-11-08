package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.designsystem.BoardPreviews
import com.developersancho.formatico.core.designsystem.FormaticoPreview
import com.developersancho.formatico.core.designsystem.PreviewCase
import com.developersancho.formatico.core.designsystem.PreviewCases
import com.developersancho.formatico.core.format.Format

/** An order: a date, a colour, links, a picture, a null and an array of items, which makes a table. */
private val PreviewOrder =
    """
    {
      "id": "ord_8842",
      "status": "shipped",
      "createdAt": "2026-10-28T14:32:00Z",
      "total": 129.90,
      "currency": "EUR",
      "paid": true,
      "coupon": null,
      "customer": {
        "name": "Elif Yılmaz",
        "email": "elif@example.com",
        "avatar": "https://images.example.com/u/elif.png",
        "website": "https://elif.dev"
      },
      "theme": { "accent": "#0F766E" },
      "items": [
        { "sku": "KB-01", "name": "Keyboard", "price": 89.90, "quantity": 1, "shippedOn": "2026-10-29" },
        { "sku": "MS-02", "name": "Mouse", "price": 25.00, "quantity": 1, "shippedOn": "2026-10-30" },
        { "sku": "PD-03", "name": "Desk pad", "price": 15.00, "quantity": 2, "shippedOn": null }
      ]
    }
    """.trimIndent() + "\n"

/** A deployment: comments, an anchor and its aliases. */
private val PreviewDeployment =
    """
    # A web service and its worker share their labels through an anchor.
    apiVersion: apps/v1
    kind: Deployment
    metadata:
      name: formatico-web
      labels: &labels
        app: formatico
        tier: web
    spec:
      replicas: 3
      selector:
        matchLabels: *labels
      template:
        metadata:
          labels: *labels
        spec:
          containers:
            - name: web
              image: registry.example.com/formatico/web:1.4.2
              env:
                - name: LOG_LEVEL
                  value: "info"
    """.trimIndent() + "\n"

/** The first countries of ISO 3166-1: a column of codes with leading zeros among them. */
private val PreviewCountries =
    """
    code,code3,numeric,name,region,subregion
    AF,AFG,004,Afghanistan,Asia,Southern Asia
    AX,ALA,248,Åland Islands,Europe,Northern Europe
    AL,ALB,008,Albania,Europe,Southern Europe
    DZ,DZA,012,Algeria,Africa,Northern Africa
    AS,ASM,016,American Samoa,Oceania,Polynesia
    AD,AND,020,Andorra,Europe,Southern Europe
    AO,AGO,024,Angola,Africa,Sub-Saharan Africa
    AI,AIA,660,Anguilla,Americas,Latin America and the Caribbean
    """.trimIndent() + "\n"

/** An order with two mistakes: a missing comma and a value cut short. */
private val PreviewInvalid =
    """
    {
      "id": "ord_8842"
      "status": "shipped",
      "total": 129.
    }
    """.trimIndent() + "\n"

/** A feed in XML, a format Formatico does not read yet. */
private const val PREVIEW_FEED = "<feed>\n  <entry id=\"1\">Formatico</entry>\n</feed>\n"

/** The workspace of a document named [name], with [text] read as [format] and its tree left as [view] says. */
private fun previewState(name: String?, text: String, format: Format?, view: DocumentView = DocumentView()) =
    readState(OpenDocument(id = 1, name = name, text = text, format = format, view = view))

private val Order = previewState("order.json", PreviewOrder, Format.Json, DocumentView(selected = "$.customer.avatar"))
private val OrderUnchosen = previewState("order.json", PreviewOrder, Format.Json)
private val Deployment = previewState(
    "deployment.yaml",
    PreviewDeployment,
    Format.Yaml,
    DocumentView(selected = "$.spec.template.spec.containers[0].env[0].name")
)
private val Countries = previewState("countries.csv", PreviewCountries, Format.Csv, DocumentView(pane = DocumentPane.Table))
private val OrderItems = previewState(
    "order.json",
    PreviewOrder,
    Format.Json,
    DocumentView(selected = "$.items[1].price", pane = DocumentPane.Table, tableSort = TableSort("price", descending = true))
)
private val Invalid = previewState("order.json", PreviewInvalid, Format.Json)
private val Feed = previewState("feed.xml", PREVIEW_FEED, Format.Xml)
private val BeingRead = WorkspaceState(document = OpenDocument(id = 1, name = "order.json", text = PreviewOrder, format = Format.Json))
private val Empty = previewState(null, "", null)
private val PlainText = previewState("notes.txt", "Things to check before the release.\n", null)

/**
 * The workspace's cases: a JSON document with its value previews, a YAML one with anchors, a CSV and a JSON array as
 * tables, a document with mistakes, one in a format not read yet, one being read, and a new, empty one.
 */
internal class WorkspaceScreenCases :
    PreviewCases<WorkspaceState>(
        "json" to Order,
        "yaml" to Deployment,
        "csv table" to Countries,
        "json table" to OrderItems,
        "invalid" to Invalid,
        "not read yet" to Feed,
        "reading" to BeingRead,
        "empty" to Empty
    )

@BoardPreviews
@Composable
internal fun WorkspaceScreenPreview(@PreviewParameter(WorkspaceScreenCases::class) case: PreviewCase<WorkspaceState>) {
    FormaticoPreview(case) { state ->
        WorkspaceScreen(state = state, onIntent = {}, commands = rememberDocumentCommands(remember { Workbench() }))
    }
}

/** What a component of the workspace is drawn with in a case: [state], laid out for [layout]. */
internal class LaidOutState(val state: WorkspaceState, val layout: WorkspaceLayout) {
    /** The board of [layout]: the window, the platform and the width the component stands in. */
    val board: Board
        get() = when (layout) {
            WorkspaceLayout.Phone -> Board.Phone
            WorkspaceLayout.Tablet -> Board.Tablet
            WorkspaceLayout.Wide -> Board.Desktop
        }
}

/** The status bar's cases: what it says of a document read, with mistakes, being read and not read, on each board. */
internal class StatusBarCases :
    PreviewCases<LaidOutState>(
        "json" to LaidOutState(Order, WorkspaceLayout.Wide),
        "invalid" to LaidOutState(Invalid, WorkspaceLayout.Wide),
        "reading" to LaidOutState(BeingRead, WorkspaceLayout.Wide),
        "not read yet" to LaidOutState(Feed, WorkspaceLayout.Wide),
        "plain text" to LaidOutState(PlainText, WorkspaceLayout.Wide),
        "csv on a tablet" to LaidOutState(Countries, WorkspaceLayout.Tablet),
        "yaml on a phone" to LaidOutState(Deployment, WorkspaceLayout.Phone)
    )

@Preview
@Composable
internal fun StatusBarPreview(@PreviewParameter(StatusBarCases::class) case: PreviewCase<LaidOutState>) {
    FormaticoPreview(case, Modifier.width(case.value.board.width.dp)) { StatusBar(state = it.state, layout = it.layout) }
}

/** The trail's cases: a value chosen in an object, none chosen, and a value deep in a list on a phone. */
internal class PathTrailCases :
    PreviewCases<LaidOutState>(
        "chosen" to LaidOutState(Order, WorkspaceLayout.Wide),
        "nothing chosen" to LaidOutState(OrderUnchosen, WorkspaceLayout.Wide),
        "deep on a phone" to LaidOutState(Deployment, WorkspaceLayout.Phone)
    )

@Preview
@Composable
internal fun PathTrailPreview(@PreviewParameter(PathTrailCases::class) case: PreviewCase<LaidOutState>) {
    FormaticoPreview(case, Modifier.width(case.value.board.width.dp)) { PathTrail(state = it.state, onIntent = {}) }
}

/** A record of a phone's table: [row] showing the columns [shown], [chosen] or not, its strings [asWritten] or not. */
internal class RecordCase(val row: TableRow, val shown: List<String>, val chosen: Boolean, val asWritten: Boolean)

private val CountryTable = requireNotNull(Countries.table)
private val ItemTable = requireNotNull(OrderItems.table)

/** The record's cases: a country, the country chosen, and an item of an order, whose strings have their quotes. */
internal class TableRecordCases :
    PreviewCases<RecordCase>(
        "country" to RecordCase(CountryTable.rows[1], CountryTable.columns, chosen = false, asWritten = false),
        "chosen" to RecordCase(CountryTable.rows[1], CountryTable.columns, chosen = true, asWritten = false),
        "order item" to RecordCase(ItemTable.rows[0], ItemTable.columns, chosen = false, asWritten = true)
    )

@Preview
@Composable
internal fun TableRecordPreview(@PreviewParameter(TableRecordCases::class) case: PreviewCase<RecordCase>) {
    FormaticoPreview(case, Modifier.width(Board.Phone.width.dp)) {
        TableRecord(row = it.row, shown = it.shown, chosen = it.chosen, asWritten = it.asWritten)
    }
}

/** A column of [table] by its name, [column], or the order of [table]'s rows by [sort]. */
internal class TableColumnCase(val table: TableModel, val column: String, val sort: TableSort? = null)

/** What a column's values come to: numbers, text, and dates with an empty one among them. */
internal class ColumnSummaryCases :
    PreviewCases<TableColumnCase>(
        "numbers" to TableColumnCase(ItemTable, "price"),
        "text" to TableColumnCase(CountryTable, "region"),
        "dates" to TableColumnCase(ItemTable, "shippedOn")
    )

@Preview
@Composable
internal fun ColumnSummaryPreview(@PreviewParameter(ColumnSummaryCases::class) case: PreviewCase<TableColumnCase>) {
    FormaticoPreview(case) { ColumnSummary(column = it.column, stats = statsOf(it.table.rows, it.column), table = it.table) }
}

/** How the rows are ordered: numbers from the largest, dates from the oldest, and text from A. */
internal class SortSummaryCases :
    PreviewCases<TableColumnCase>(
        "numbers, largest first" to TableColumnCase(ItemTable, "price", TableSort("price", descending = true)),
        "dates, oldest first" to TableColumnCase(ItemTable, "shippedOn", TableSort("shippedOn", descending = false)),
        "text" to TableColumnCase(CountryTable, "name", TableSort("name", descending = false))
    )

@Preview
@Composable
internal fun SortSummaryPreview(@PreviewParameter(SortSummaryCases::class) case: PreviewCase<TableColumnCase>) {
    FormaticoPreview(case) { SortSummary(table = it.table, sort = requireNotNull(it.sort)) }
}

/** What the tree's place says where there is no tree: plain text, a format not read yet, an empty document. */
internal class TreePaneMessageCases :
    PreviewCases<WorkspaceState>(
        "plain text" to PlainText,
        "not read yet" to Feed,
        "empty" to Empty
    )

@Preview
@Composable
internal fun TreePaneMessagePreview(@PreviewParameter(TreePaneMessageCases::class) case: PreviewCase<WorkspaceState>) {
    FormaticoPreview(case) { TreePaneMessage(state = it, modifier = Modifier.size(width = 480.dp, height = 320.dp)) }
}

/** What the editor's place says until the editor comes, by what can be done to the document meanwhile. */
internal class EditorComingCases :
    PreviewCases<WorkspaceState>(
        "json" to Order,
        "csv" to Countries,
        "invalid" to Invalid,
        "not read yet" to Feed
    )

@Preview
@Composable
internal fun EditorComingPreview(@PreviewParameter(EditorComingCases::class) case: PreviewCase<WorkspaceState>) {
    FormaticoPreview(case) { EditorComing(state = it, modifier = Modifier.size(width = 480.dp, height = 320.dp)) }
}

/** The views' choice: [selected] among them, with the text's or without it, on [board]. */
internal class ViewChoiceCase(val selected: DocumentPane, val withText: Boolean, val board: Board)

/** The choice of a phone, which has the text among its views, and of a wider window, which shows the text beside. */
internal class ViewChoiceCases :
    PreviewCases<ViewChoiceCase>(
        "tree on a phone" to ViewChoiceCase(DocumentPane.Tree, withText = true, board = Board.Phone),
        "table on a phone" to ViewChoiceCase(DocumentPane.Table, withText = true, board = Board.Phone),
        "tree beside the text" to ViewChoiceCase(DocumentPane.Tree, withText = false, board = Board.Desktop)
    )

@Preview
@Composable
internal fun ViewChoicePreview(@PreviewParameter(ViewChoiceCases::class) case: PreviewCase<ViewChoiceCase>) {
    FormaticoPreview(case, Modifier.width(Board.Phone.width.dp)) {
        ViewChoice(selected = it.selected, withText = it.withText, onSelect = {})
    }
}
