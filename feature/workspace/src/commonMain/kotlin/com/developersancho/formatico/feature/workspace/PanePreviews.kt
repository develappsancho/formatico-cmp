package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.designsystem.FormaticoPreview
import com.developersancho.formatico.core.designsystem.PreviewCase
import com.developersancho.formatico.core.designsystem.PreviewCases
import com.developersancho.formatico.core.format.Format
import com.pantrixui.components.data.codeeditor.CodeEditorState

/**
 * A case of one of the workspace's panes: [state], laid out for [layout], in a pane as tall as [height], its editor's
 * caret at [caret] where the pane has one.
 */
internal class PaneCase(val state: WorkspaceState, val layout: WorkspaceLayout, val height: Int, val caret: Int = 0) {
    /** The board the pane stands in. */
    val board get() = LaidOutState(state, layout).board

    /** How wide the pane is there: half a wide window, beside the text, or the whole. */
    val width: Int
        get() = if (layout == WorkspaceLayout.Wide) board.width / 2 else board.width
}

/** [case]'s pane, drawn at its width and height with [content], which is given the case and an editor of its text. */
@Composable
private fun PanePreview(case: PreviewCase<PaneCase>, content: @Composable (PaneCase, CodeEditorState) -> Unit) {
    FormaticoPreview(case, Modifier.width(case.value.width.dp).height(case.value.height.dp)) { pane ->
        val editor = remember(pane) { CodeEditorState(pane.state.document?.text.orEmpty(), initialSelection = TextRange(pane.caret)) }
        content(pane, editor)
    }
}

/** A JSON object with one mistake: a comma left out between its two values. */
private val OneMistake =
    previewState(
        "prices.json",
        "{\n  \"total\": 129.90\n  \"currency\": \"EUR\"\n}\n",
        Format.Json,
        DocumentView(pane = DocumentPane.Problems)
    )

/**
 * The Summary's cases: the order measured, as the Summary boards draw it, and while it is still being measured; the
 * countries on a tablet; the order on a phone; and the deployment, YAML with anchors, being measured.
 */
internal class SummaryPaneCases :
    PreviewCases<PaneCase>(
        "order" to PaneCase(OrderSummary, WorkspaceLayout.Wide, height = 760),
        "measuring" to PaneCase(OrderSummaryMeasuring, WorkspaceLayout.Wide, height = 760),
        "countries on a tablet" to PaneCase(CountriesSummary, WorkspaceLayout.Tablet, height = 900),
        "order on a phone" to PaneCase(OrderSummary, WorkspaceLayout.Phone, height = 760),
        "yaml" to PaneCase(
            previewState("deployment.yaml", PreviewDeployment, Format.Yaml, DocumentView(pane = DocumentPane.Summary)),
            WorkspaceLayout.Wide,
            height = 760
        )
    )

@Preview
@Composable
internal fun SummaryPanePreview(@PreviewParameter(SummaryPaneCases::class) case: PreviewCase<PaneCase>) {
    PanePreview(case) { pane, _ ->
        SummaryPane(state = pane.state, layout = pane.layout, onIntent = {}, commands = rememberDocumentCommands(remember { Workbench() }))
    }
}

/**
 * The Problems list's cases: the Invalid boards' four mistakes, the caret at the second, in a wide window and on a
 * tablet and a phone; a YAML flow left open; and a document with one mistake.
 */
internal class ProblemsPaneCases :
    PreviewCases<PaneCase>(
        "json" to PaneCase(InvalidProblems, WorkspaceLayout.Wide, height = 560, caret = InvalidCaret),
        "json on a tablet" to PaneCase(InvalidProblems, WorkspaceLayout.Tablet, height = 560, caret = InvalidCaret),
        "json on a phone" to PaneCase(InvalidProblems, WorkspaceLayout.Phone, height = 640, caret = InvalidCaret),
        "yaml" to PaneCase(InvalidYaml, WorkspaceLayout.Wide, height = 400),
        "one mistake" to PaneCase(OneMistake, WorkspaceLayout.Wide, height = 400)
    )

@Preview
@Composable
internal fun ProblemsPanePreview(@PreviewParameter(ProblemsPaneCases::class) case: PreviewCase<PaneCase>) {
    PanePreview(case) { pane, editor ->
        // A phone shows the list alone, under its own bar's choice of views.
        if (pane.layout == WorkspaceLayout.Phone) {
            ProblemsList(state = pane.state, editor = editor, layout = pane.layout, onIntent = {})
        } else {
            ProblemsPane(state = pane.state, editor = editor, layout = pane.layout, onIntent = {})
        }
    }
}

/** The countries, as a table, with [view]'s filter, hidden columns or order. */
private fun countriesTable(view: DocumentView) = previewState(
    "countries.csv",
    PreviewCountries,
    Format.Csv,
    view.copy(pane = DocumentPane.Table)
)

/**
 * The table's cases: the countries, as the Table boards draw them; the order's items sorted by price; the countries
 * filtered to Europe, filtered to nothing, and with two columns hidden; a TSV on a tablet; the countries on a phone;
 * the countries read with no header, beside the text and on a phone; and a document with no list of records to make a
 * table of.
 */
internal class TablePaneCases :
    PreviewCases<PaneCase>(
        "csv" to PaneCase(Countries, WorkspaceLayout.Wide, height = 560),
        "sorted" to PaneCase(OrderItems, WorkspaceLayout.Wide, height = 400),
        "filtered" to PaneCase(countriesTable(DocumentView(tableFilter = "Europe")), WorkspaceLayout.Wide, height = 560),
        "nothing found" to PaneCase(countriesTable(DocumentView(tableFilter = "Atlantis")), WorkspaceLayout.Wide, height = 400),
        "columns hidden" to PaneCase(
            countriesTable(DocumentView(hiddenColumns = setOf("code3", "numeric"))),
            WorkspaceLayout.Wide,
            height = 560
        ),
        "tsv on a tablet" to PaneCase(
            previewState("countries.tsv", PreviewTsv, Format.Tsv, DocumentView(pane = DocumentPane.Table)),
            WorkspaceLayout.Tablet,
            height = 560
        ),
        "csv on a phone" to PaneCase(Countries, WorkspaceLayout.Phone, height = 760),
        "csv no header" to PaneCase(CountriesWithoutHeader, WorkspaceLayout.Wide, height = 560),
        "csv no header on a phone" to PaneCase(CountriesWithoutHeader, WorkspaceLayout.Phone, height = 760),
        "no records" to PaneCase(
            previewState("theme.json", "{ \"accent\": \"#0F766E\" }\n", Format.Json, DocumentView(pane = DocumentPane.Table)),
            WorkspaceLayout.Wide,
            height = 400
        )
    )

@Preview
@Composable
internal fun TablePanePreview(@PreviewParameter(TablePaneCases::class) case: PreviewCase<PaneCase>) {
    PanePreview(case) { pane, _ ->
        TablePane(state = pane.state, layout = pane.layout, onIntent = {}, commands = rememberDocumentCommands(remember { Workbench() }))
    }
}

/**
 * The text's cases: the order, its avatar chosen in the tree; the Invalid boards' settings, their mistakes marked and
 * the caret at the second; the order's prices found by a query; a YAML flow left open; plain text; and the order on a
 * phone.
 */
internal class TextPaneCases :
    PreviewCases<PaneCase>(
        "json" to PaneCase(Order, WorkspaceLayout.Wide, height = 560),
        "invalid" to PaneCase(Invalid, WorkspaceLayout.Wide, height = 560, caret = InvalidCaret),
        "query matches" to PaneCase(OrderPrices, WorkspaceLayout.Wide, height = 560),
        "invalid yaml" to PaneCase(InvalidYaml, WorkspaceLayout.Wide, height = 400),
        "plain text" to PaneCase(PlainText, WorkspaceLayout.Wide, height = 400),
        "json on a phone" to PaneCase(Order, WorkspaceLayout.Phone, height = 640)
    )

@Preview
@Composable
internal fun TextPanePreview(@PreviewParameter(TextPaneCases::class) case: PreviewCase<PaneCase>) {
    PanePreview(case) { pane, editor ->
        TextPane(state = pane.state, editor = editor, onIntent = {})
    }
}

/**
 * A phone's strip over an invalid document's text and its problem card, as Invalid-390 draws them: the caret at the
 * second of four mistakes, at the start before the first, a YAML flow left open, and a document with one mistake.
 */
internal class ProblemCardCases :
    PreviewCases<PaneCase>(
        "second of four" to PaneCase(Invalid, WorkspaceLayout.Phone, height = 320, caret = InvalidCaret),
        "first of four" to PaneCase(Invalid, WorkspaceLayout.Phone, height = 320),
        "yaml" to PaneCase(InvalidYaml, WorkspaceLayout.Phone, height = 320),
        "one mistake" to PaneCase(OneMistake, WorkspaceLayout.Phone, height = 320)
    )

@Preview
@Composable
internal fun ProblemCardPreview(@PreviewParameter(ProblemCardCases::class) case: PreviewCase<PaneCase>) {
    PanePreview(case) { pane, editor ->
        Column {
            InvalidStrip(state = pane.state, onIntent = {})
            ProblemCard(state = pane.state, editor = editor, onIntent = {})
        }
    }
}
