package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.designsystem.BoardPreviews
import com.developersancho.formatico.core.designsystem.FormaticoPreview
import com.developersancho.formatico.core.designsystem.PreviewCase
import com.developersancho.formatico.core.designsystem.PreviewCases
import com.developersancho.formatico.core.format.Format
import com.pantrixui.components.data.codeeditor.CodeEditorState

/** An order: a date, a colour, links, a picture, a null and an array of items, which makes a table. */
internal val PreviewOrder =
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

/** The Invalid boards' settings: a comment, a missing comma, a key in no quotes and a trailing comma. */
private val PreviewInvalid =
    """
    {
      "name": "formatico-web",
      "version": "1.4.0",
      "port": 8080,
      "debug": true,
      // turn off before the release
      "hosts": [
        "formatico.pantrixui.com",
        "localhost"
        "127.0.0.1"
      ],
      timeout: 30,
      "retries": 3,
      "features": {
        "share": false,
        "ai": false,
      },
      "theme": "teal"
    }
    """.trimIndent() + "\n"

/** A service whose list of ports is never closed: a YAML flow left open, which its line's end closes. */
private val PreviewInvalidYaml =
    """
    # The web service and its ports.
    apiVersion: v1
    kind: Service
    metadata:
      name: formatico-web
    spec:
      ports: [80, 443
      selector:
        app: formatico
    """.trimIndent() + "\n"

/** Formatico's notes as an RSS feed, as the Query board shows it: three items, two of them releases. */
internal val PreviewFeed =
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <rss version="2.0">
      <channel>
        <title>Formatico notes</title>
        <link>https://formatico.pantrixui.com/notes</link>
        <language>en</language>
        <item>
          <title>Tabs that remember</title>
          <pubDate>Mon, 26 Oct 2026 09:00:00 GMT</pubDate>
          <category>release</category>
        </item>
        <item>
          <title>YAML anchors, explained</title>
          <pubDate>Wed, 21 Oct 2026 09:00:00 GMT</pubDate>
          <category>guide</category>
        </item>
        <item>
          <title>A tree that previews colours</title>
          <pubDate>Thu, 15 Oct 2026 09:00:00 GMT</pubDate>
          <category>release</category>
        </item>
      </channel>
    </rss>
    """.trimIndent() + "\n"

/** Start's Rust manifest: tables, inline tables and arrays of tables. */
private val PreviewCargo =
    """
    [package]
    name = "formatico-cli"
    version = "0.3.0"
    edition = "2021"
    description = "Format and convert structured text from the command line"
    license = "MIT"

    [dependencies]
    serde = { version = "1.0", features = ["derive"] }
    serde_json = "1.0"
    toml = "0.8"
    clap = { version = "4.5", features = ["derive"] }

    [dev-dependencies]
    pretty_assertions = "1.4"

    [[bin]]
    name = "fmt"
    path = "src/main.rs"

    [[bench]]
    name = "parse"
    harness = false

    [profile.release]
    lto = true
    codegen-units = 1
    """.trimIndent() + "\n"

/** What was done in Formatico, an event to a line, as the Share board's events.ndjson has it. */
private val PreviewEvents = listOf("open", "search", "format", "convert", "copy").mapIndexed { index, event ->
    """{"at": "2026-10-29T09:0$index:12Z", "event": "$event", "bytes": ${1200 + 37 * index}}"""
}.joinToString("\n", postfix = "\n")

/** A Spring service's settings: dotted keys, a value carried on over two lines, an escape. */
private val PreviewApplication =
    """
    # The web service, as Spring reads it.
    server.port=8080
    server.servlet.context-path=/api
    spring.application.name=formatico
    spring.datasource.url=jdbc:postgresql://localhost:5432/formatico
    app.greeting=Merhaba, d\u00fcnya
    app.allowed-origins=https://formatico.pantrixui.com, \
        http://localhost:8080
    """.trimIndent() + "\n"

/** An INI file of Formatico's own: keys before the first section, sections, comments. */
private val PreviewSettings =
    """
    ; Formatico's settings
    version = 3

    [editor]
    theme = teal
    font = Geist Mono
    tab size = 2

    [files]
    ; How many documents Recent lists
    recent = 12
    max size = 50 MB
    """.trimIndent() + "\n"

/** The workspace of a document named [name], with [text] read as [format] and its tree left as [view] says. */
internal fun previewState(name: String?, text: String, format: Format?, view: DocumentView = DocumentView()) =
    readState(OpenDocument(id = 1, name = name, text = text, format = format, view = view))

internal val Order = previewState("order.json", PreviewOrder, Format.Json, DocumentView(selected = "$.customer.avatar"))
internal val OrderUnchosen = previewState("order.json", PreviewOrder, Format.Json)

/** The order searched for "elif", as the Workspace boards' search state has it: its first match chosen. */
internal val OrderSearched =
    previewState("order.json", PreviewOrder, Format.Json, DocumentView(selected = "$.customer.name", searching = true))

/** The deployment with an alias chosen, its anchor and the alias open, as the Workspace boards' YAML state has it. */
internal val DeploymentAlias = previewState(
    "deployment.yaml",
    PreviewDeployment,
    Format.Yaml,
    DocumentView(
        selected = "$.spec.selector.matchLabels",
        expanded = setOf("$", "$.metadata", "$.metadata.labels", "$.spec", "$.spec.selector")
    )
)
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

// The text is the phone's view, as Invalid-390 draws it; a wider window shows the tree beside it.
private val Invalid = previewState("settings.json", PreviewInvalid, Format.Json, DocumentView(pane = DocumentPane.Text))
private val InvalidProblems = previewState("settings.json", PreviewInvalid, Format.Json, DocumentView(pane = DocumentPane.Problems))
private val InvalidYaml = previewState("service.yaml", PreviewInvalidYaml, Format.Yaml, DocumentView(pane = DocumentPane.Text))

/** Where the Invalid boards' caret stands: at the second problem, the missing comma on line 10. */
private val InvalidCaret = PreviewInvalid.indexOf("\"127.0.0.1\"")
private val Feed = previewState("feed.xml", PreviewFeed, Format.Xml, DocumentView(selected = "$.rss.channel.title"))
private val Cargo = previewState("Cargo.toml", PreviewCargo, Format.Toml, DocumentView(selected = "$.dependencies.serde.version"))
private val Events = previewState("events.ndjson", PreviewEvents, Format.Ndjson)
private val Application = previewState("application.properties", PreviewApplication, Format.Properties)
private val Settings = previewState("settings.ini", PreviewSettings, Format.Ini)
private val BeingRead = WorkspaceState(document = OpenDocument(id = 1, name = "order.json", text = PreviewOrder, format = Format.Json))
private val Empty = previewState(null, "", null)

/** The order's Summary while the platform is still gzipping it. */
private val OrderSummaryMeasuring = previewState("order.json", PreviewOrder, Format.Json, DocumentView(pane = DocumentPane.Summary))

/** How many bytes the minified order and the countries' text come to, gzipped by the JVM, as the desktop measures them. */
private const val ORDER_GZIPPED = 342L
private const val COUNTRIES_GZIPPED = 269L

private val OrderSummary = OrderSummaryMeasuring.copy(sizes = OrderSummaryMeasuring.sizes?.copy(compressed = ORDER_GZIPPED))
private val CountriesSummary = previewState("countries.csv", PreviewCountries, Format.Csv, DocumentView(pane = DocumentPane.Summary))
    .let { it.copy(sizes = it.sizes?.copy(compressed = COUNTRIES_GZIPPED)) }
private val PlainText = previewState("notes.txt", "Things to check before the release.\n", null)

/** A case of the screen: [state], with its editor's caret at [caret]. */
internal class ScreenCase(val state: WorkspaceState, val caret: Int = 0)

/**
 * The workspace's cases: a JSON document with its value previews, a YAML one with anchors, a CSV and a JSON array as
 * tables, a document with mistakes and its problems, a YAML one with a flow left open, an XML feed, a TOML manifest,
 * newline-delimited JSON, a properties file and an INI one, the order's Summary measured and being measured and a CSV's,
 * the order and the feed queried, as the Query boards draw them, one being read, and a new, empty one.
 */
internal class WorkspaceScreenCases :
    PreviewCases<ScreenCase>(
        "json" to ScreenCase(Order),
        "yaml" to ScreenCase(Deployment),
        "csv table" to ScreenCase(Countries),
        "json table" to ScreenCase(OrderItems),
        "invalid" to ScreenCase(Invalid, caret = InvalidCaret),
        "problems" to ScreenCase(InvalidProblems, caret = InvalidCaret),
        "invalid yaml" to ScreenCase(InvalidYaml),
        "xml" to ScreenCase(Feed),
        "toml" to ScreenCase(Cargo),
        "ndjson" to ScreenCase(Events),
        "properties" to ScreenCase(Application),
        "ini" to ScreenCase(Settings),
        "summary" to ScreenCase(OrderSummary),
        "summary measuring" to ScreenCase(OrderSummaryMeasuring),
        "csv summary" to ScreenCase(CountriesSummary),
        "query" to ScreenCase(OrderNames),
        "query xml" to ScreenCase(FeedReleases),
        "query prices" to ScreenCase(OrderPrices),
        "reading" to ScreenCase(BeingRead),
        "empty" to ScreenCase(Empty)
    )

@BoardPreviews
@Composable
internal fun WorkspaceScreenPreview(@PreviewParameter(WorkspaceScreenCases::class) case: PreviewCase<ScreenCase>) {
    FormaticoPreview(case) { screen ->
        val editor = remember(screen) {
            screen.state.document?.let { CodeEditorState(it.text, initialSelection = TextRange(screen.caret)) }
        }
        WorkspaceScreen(state = screen.state, onIntent = {}, commands = rememberDocumentCommands(remember { Workbench() }), editor = editor)
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

/** The status bar's cases: what it says of a document read, with mistakes, being read and in plain text, on each board. */
internal class StatusBarCases :
    PreviewCases<LaidOutState>(
        "json" to LaidOutState(Order, WorkspaceLayout.Wide),
        "invalid" to LaidOutState(Invalid, WorkspaceLayout.Wide),
        "reading" to LaidOutState(BeingRead, WorkspaceLayout.Wide),
        "toml" to LaidOutState(Cargo, WorkspaceLayout.Wide),
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

/** What the tree's place says where there is no tree: plain text, an empty document. */
internal class TreePaneMessageCases :
    PreviewCases<WorkspaceState>(
        "plain text" to PlainText,
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
        "xml" to Feed
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
