package com.developersancho.formatico.feature.workspace

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.designsystem.PreviewCase
import com.developersancho.formatico.core.testing.awaitTexts
import com.developersancho.formatico.core.testing.checkPreviewCases
import com.pantrixui.testing.onPantrixNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Every preview case of the workspace, drawn as its preview draws it at the boards its screenshots are taken at: what
 * each shows, and that Pantrix's diagnostics find nothing in it. What pressing does is in each screen's own tests.
 */
@OptIn(ExperimentalTestApi::class)
class PreviewCasesTest {
    /** The texts each case is known by, at every board it is drawn at. */
    private fun <T> textsOf(case: PreviewCase<T>, texts: Map<String, List<String>>): List<String> =
        texts[case.name] ?: fail("No texts are given for the case \"${case.name}\"")

    @Test
    fun theScreenShowsEachDocumentAndPane() = checkPreviewCases(
        WorkspaceScreenCases(),
        boardsOf = { Board.entries },
        content = { WorkspaceScreenPreview(it) }
    ) { case, _ ->
        awaitTexts(*textsOf(case, ScreenTexts).toTypedArray())
    }

    @Test
    fun theShareDialogSaysWhereEachShareStands() = checkPreviewCases(
        ShareCases(),
        boardsOf = { Board.entries },
        content = { SharePreview(it) }
    ) { case, _ ->
        awaitTexts(*textsOf(case, ShareTexts).toTypedArray())
    }

    @Test
    fun theStatusBarSaysWhatTheDocumentIs() = checkPreviewCases(
        StatusBarCases(),
        boardsOf = { listOf(it.board) },
        content = { StatusBarPreview(it) }
    ) { case, _ ->
        awaitTexts(*textsOf(case, StatusTexts).toTypedArray())
    }

    @Test
    fun theTrailGoesToTheChosenValue() = checkPreviewCases(
        PathTrailCases(),
        boardsOf = { listOf(it.board) },
        content = { PathTrailPreview(it) }
    ) { case, _ ->
        awaitTexts(*textsOf(case, TrailTexts).toTypedArray())
    }

    @Test
    fun aRecordShowsItsFirstValuesAndTheRest() = checkPreviewCases(
        TableRecordCases(),
        boardsOf = { listOf(Board.Phone) },
        content = { TableRecordPreview(it) }
    ) { case, _ ->
        awaitTexts(*textsOf(case, RecordTexts).toTypedArray())
    }

    @Test
    fun aColumnsSummaryCountsItsValues() = checkPreviewCases(
        ColumnSummaryCases(),
        boardsOf = { listOf(Board.Desktop) },
        content = { ColumnSummaryPreview(it) }
    ) { case, _ ->
        awaitTexts(*textsOf(case, ColumnTexts).toTypedArray())
    }

    @Test
    fun theSortIsSaidInTheColumnsOwnTerms() = checkPreviewCases(
        SortSummaryCases(),
        boardsOf = { listOf(Board.Desktop) },
        content = { SortSummaryPreview(it) }
    ) { case, _ ->
        awaitTexts(*textsOf(case, SortTexts).toTypedArray())
    }

    @Test
    fun aPaneWithNoTreeSaysWhy() = checkPreviewCases(
        TreePaneMessageCases(),
        boardsOf = { listOf(Board.Desktop) },
        content = { TreePaneMessagePreview(it) }
    ) { case, _ ->
        awaitTexts(*textsOf(case, PaneMessageTexts).toTypedArray())
    }

    @Test
    fun theViewsChoiceShowsTheViewChosen() = checkPreviewCases(
        ViewChoiceCases(),
        boardsOf = { listOf(it.board) },
        content = { ViewChoicePreview(it) }
    ) { case, _ ->
        awaitTexts("Tree", "Table", "Graph")
        val text = onAllNodes(hasText("Text")).fetchSemanticsNodes().isNotEmpty()
        assertEquals(case.value.withText, text, "whether the text is a view")
        onPantrixNode("Segment", case.value.selected.name.lowercase(), scope = "SegmentButton_view").assertIsSelected()
    }

    @Test
    fun theTreeShowsTheDocumentsValues() = checkPreviewCases(
        DocumentTreeCases(),
        boardsOf = { listOf(it.board) },
        content = { DocumentTreePreview(it) }
    ) { case, _ ->
        awaitTexts(*textsOf(case, TreeTexts).toTypedArray())
    }

    @Test
    fun aQuerySaysWhatItFound() = checkPreviewCases(
        QueryCases(),
        boardsOf = { listOf(it.board) },
        content = { QueryPreview(it) }
    ) { case, _ ->
        awaitTexts(*textsOf(case, QueryTexts).toTypedArray())
    }

    @Test
    fun aConversionSaysWhatIsLost() = checkPreviewCases(
        ConvertCases(),
        boardsOf = { listOf(it.board) },
        content = { ConvertPreview(it) }
    ) { case, _ ->
        awaitTexts(*textsOf(case, ConvertTexts).toTypedArray())
    }

    @Test
    fun theTypesAreNamedAndSaved() = checkPreviewCases(
        TypesCases(),
        boardsOf = { listOf(it.board) },
        content = { TypesPreview(it) }
    ) { case, _ ->
        awaitTexts(*textsOf(case, TypesTexts).toTypedArray())
    }

    @Test
    fun theGraphShowsItsCardsOrWhyItHasNone() = checkPreviewCases(
        GraphCases(),
        boardsOf = { listOf(it.board) },
        content = { GraphPreview(it) }
    ) { case, _ ->
        awaitTexts(*textsOf(case, GraphTexts).toTypedArray())
    }

    @Test
    fun aSchemaSaysWhatItAsksFor() = checkPreviewCases(
        SchemaCases(),
        boardsOf = { listOf(it.board) },
        content = { SchemaPreview(it) }
    ) { case, _ ->
        awaitTexts(*textsOf(case, SchemaTexts).toTypedArray())
    }

    @Test
    fun aComparisonSaysHowTheTwoDiffer() = checkPreviewCases(
        CompareCases(),
        boardsOf = { listOf(it.board) },
        content = { ComparePreview(it) }
    ) { case, _ ->
        awaitTexts(*textsOf(case, CompareTexts).toTypedArray())
    }

    @Test
    fun theSummaryCountsWhatTheDocumentHolds() = checkPreviewCases(
        SummaryPaneCases(),
        boardsOf = { listOf(it.board) },
        content = { SummaryPanePreview(it) }
    ) { case, _ ->
        awaitTexts(*textsOf(case, SummaryTexts).toTypedArray())
    }

    @Test
    fun theProblemsListSaysWhatIsWrongAndHowToRepairIt() = checkPreviewCases(
        ProblemsPaneCases(),
        boardsOf = { listOf(it.board) },
        content = { ProblemsPanePreview(it) }
    ) { case, _ ->
        awaitTexts(*textsOf(case, ProblemsTexts).toTypedArray())
    }

    @Test
    fun theTableShowsItsRowsAsTheyAreFilteredAndSorted() = checkPreviewCases(
        TablePaneCases(),
        boardsOf = { listOf(it.board) },
        content = { TablePanePreview(it) }
    ) { case, _ ->
        awaitTexts(*textsOf(case, TableTexts).toTypedArray())
    }

    @Test
    fun theTextPaneHoldsTheDocumentsText() = checkPreviewCases(
        TextPaneCases(),
        boardsOf = { listOf(it.board) },
        content = { TextPanePreview(it) }
    ) { case, _ ->
        // The editor's text is one field: each case is known by a piece of it.
        awaitTexts(*textsOf(case, EditorTexts).toTypedArray(), substring = true)
    }

    @Test
    fun aPhonesCardSaysWhichProblemTheCaretIsAt() = checkPreviewCases(
        ProblemCardCases(),
        boardsOf = { listOf(it.board) },
        content = { ProblemCardPreview(it) }
    ) { case, _ ->
        awaitTexts(*textsOf(case, CardTexts).toTypedArray())
    }

    private companion object {
        val SummaryTexts = mapOf(
            "order" to listOf("342 B", "The minified text, gzip", "3 levels"),
            "measuring" to listOf("Being measured", "3 levels"),
            "countries on a tablet" to listOf("407 B", "269 B"),
            "order on a phone" to listOf("342 B", "6 objects"),
            "yaml" to listOf("473 B", "22 lines · UTF-8 · LF")
        )

        val ProblemsTexts = mapOf(
            "json" to listOf("Problems (4)", "This is not valid JSON", "Add a comma after the value on line 9."),
            "json on a tablet" to listOf("Problems (4)", "This is not valid JSON", "Add the comma"),
            "json on a phone" to listOf("This is not valid JSON", "Add a comma after the value on line 9."),
            "yaml" to listOf("This is not valid YAML", "These brackets are never closed.", "Close it"),
            "one mistake" to listOf("Problems (1)", "Add a comma after the value on line 2.", "Line 3, column 3")
        )

        val TableTexts = mapOf(
            "csv" to listOf("8 rows · 6 columns", "AFG"),
            "sorted" to listOf("3 rows · 5 columns", "price"),
            "filtered" to listOf("3 of 8 rows · 6 columns", "AND"),
            "nothing found" to listOf("0 of 8 rows · 6 columns", "No row holds “Atlantis”"),
            "columns hidden" to listOf("American Samoa", "Polynesia"),
            "tsv on a tablet" to listOf("4 rows", "AFG"),
            "csv on a phone" to listOf("8 rows", "code3: AFG  numeric: 004  region: Asia  subregion: Southern Asia"),
            "no records" to listOf("No list to show as a table", "The table shows an array, and this document has none.")
        )

        val EditorTexts = mapOf(
            "json" to listOf("\"id\": \"ord_8842\""),
            "invalid" to listOf("// turn off before the release"),
            "query matches" to listOf("\"price\": 89.90"),
            "invalid yaml" to listOf("ports: [80, 443"),
            "plain text" to listOf("Things to check before the release."),
            "json on a phone" to listOf("\"id\": \"ord_8842\"")
        )

        val CardTexts = mapOf(
            "second of four" to listOf("Problem 2 of 4 · line 10, column 5", "Add a comma after the value on line 9."),
            "first of four" to listOf("Problem 1 of 4 · line 6, column 3", "Remove the comment"),
            "yaml" to listOf("Not valid YAML: 1 problem", "Close it"),
            "one mistake" to listOf("Not valid JSON: 1 problem", "Problem 1 of 1 · line 3, column 3")
        )

        val ScreenTexts = mapOf(
            "json" to listOf("customer", "avatar"),
            "yaml" to listOf("\$ {4}", "apiVersion: apps/v1"),
            "csv table" to listOf("code3", "numeric"),
            "json table" to listOf("sku", "quantity"),
            "invalid" to listOf("Repair"),
            "problems" to listOf("This is not valid JSON", "Repair all"),
            "invalid yaml" to listOf("Repair"),
            "xml" to listOf("rss", "\$ {1}"),
            "toml" to listOf("serde", "\$ {6}"),
            "ndjson" to listOf("\$ [5]", "event: \"open\""),
            "properties" to listOf("server.port: 8080"),
            "ini" to listOf("theme: teal", "font: Geist Mono"),
            "jsonc" to listOf("\$ {4}", "editor.tabSize: 2"),
            "json5" to listOf("\$ {5}", "port: 0x1F90"),
            "tsv table" to listOf("code3", "numeric"),
            "summary" to listOf("Summary", "678 B", "342 B"),
            "summary measuring" to listOf("Summary", "678 B", "Being measured"),
            "csv summary" to listOf("Summary", "407 B", "9 lines · UTF-8 · LF"),
            "query" to listOf("JSONPath", "\$.customer.email"),
            "query xml" to listOf("XPath", "count(//item)", "2 matches"),
            "query prices" to listOf("JSONPath", "\$.items[*].name"),
            "convert" to listOf("Open as a document", "Nothing is lost"),
            "convert toml" to listOf("TOML has no null", "Leave it out"),
            "schema" to listOf("2 problems against order.schema.json", "coupon must be a string, and is null."),
            "graph" to listOf("\$.customer", "Valid JSON"),
            "compare" to listOf("Meaning", "\$.status"),
            "compare text" to listOf("Meaning", "Line 2"),
            "long lines" to listOf("title: \"Release checklist\""),
            "long lines wrapped" to listOf("title: \"Release checklist\""),
            "reading" to listOf("Reading…"),
            "empty" to listOf("The document is empty", "Plain text"),
            "plain" to listOf("Plain text")
        )

        val ShareTexts = mapOf(
            "short" to listOf("Share order.json", "Format the document in the link", "Copy the link"),
            "long" to listOf("In the link", "As an encrypted link"),
            "formatted" to listOf("Share order.json", "Format the document in the link", "Copy the link"),
            "plain text" to listOf("Share notes.txt", "Format the document in the link"),
            "making" to listOf("Share order.json", "Format the document in the link", "Copy the link"),
            "encrypted" to listOf("Keep it for", "Format the document before it is encrypted", "Create the link"),
            "creating" to listOf("Keep it for", "Format the document before it is encrypted"),
            "unreachable" to listOf("Formatico’s server could not be reached", "Try again"),
            "busy" to listOf("Formatico’s server is not taking shares right now", "Try again"),
            "daily limit" to listOf("No more encrypted links from this network today"),
            "created" to listOf("Link", "Delete the link"),
            "not deleted" to listOf("Formatico’s server could not be reached"),
            "encrypted only" to listOf("Share events.ndjson", "Keep it for"),
            "encrypted only created" to listOf("Share events.ndjson", "Link"),
            "encrypted only daily limit" to listOf("Share events.ndjson", "No more encrypted links from this network today"),
            "too large" to listOf("access.ndjson is too large to share", "Save as a file")
        )

        val StatusTexts = mapOf(
            "json" to listOf("Valid JSON", "Ln 12, Col 15", "\$.customer.avatar"),
            "invalid" to listOf("4 problems"),
            "reading" to listOf("Reading…"),
            "toml" to listOf("Valid TOML", "Ln 9, Col 21"),
            "plain text" to listOf("Plain text"),
            "csv on a tablet" to listOf("Valid CSV"),
            "yaml on a phone" to listOf("Valid YAML", "Ln 21, Col 21")
        )

        val TrailTexts = mapOf(
            "chosen" to listOf("\$", "customer", "avatar"),
            "nothing chosen" to listOf("\$"),
            "deep on a phone" to listOf("containers", "[0]", "env", "name")
        )

        val RecordTexts = mapOf(
            "country" to listOf("Åland Islands", "AX"),
            "chosen" to listOf("Åland Islands", "AX"),
            "order item" to listOf("Keyboard", "KB-01")
        )

        val ColumnTexts = mapOf(
            "numbers" to listOf("price", "3 numbers", "15.00"),
            "text" to listOf("region", "8 values"),
            "dates" to listOf("shippedOn", "3 values")
        )

        val SortTexts = mapOf(
            "numbers, largest first" to listOf("Sorted by price, largest first"),
            "dates, oldest first" to listOf("Sorted by shippedOn, oldest first"),
            "text" to listOf("Sorted by name, A to Z")
        )

        val PaneMessageTexts = mapOf(
            "plain text" to listOf("Plain text has no tree", "Choose the format it is written in, and it is read as one."),
            "empty" to listOf("The document is empty", "There is nothing in it to read yet.")
        )

        val TreeTexts = mapOf(
            "json" to listOf("\$ {10}", "customer: {4}"),
            "nothing chosen" to listOf("\$ {10}", "customer: {4}"),
            "searched" to listOf("\$ {10}", "name: \"Elif Yılmaz\""),
            "yaml" to listOf("\$ {4}", "kind: Deployment"),
            "json on a tablet" to listOf("\$ {10}", "customer: {4}"),
            "searched on a phone" to listOf("\$ {10}", "name: \"Elif Yılmaz\""),
            "yaml on a phone" to listOf("kind: Deployment", "labels: {2}")
        )

        val QueryTexts = mapOf(
            "json" to listOf("3 matches", "Copy the results", "\$.items[0].name"),
            "xml on a tablet" to listOf("2 matches", "/rss/channel/item[1]/title"),
            "json on a phone" to listOf("3 matches", "Choose one to see it in the text"),
            "nothing asked" to listOf("Write a JSONPath, as \$.items[*].name. XPath is for XML documents."),
            "no query" to listOf("The query ends too soon (character 9)"),
            "no matches" to listOf("No matches"),
            "a value" to listOf("Value", "count(//item)"),
            "nothing read" to listOf("The document is empty"),
            "no query on a phone" to listOf("The query ends too soon (character 5)")
        )

        val ConvertTexts = mapOf(
            "yaml" to listOf("Save as order.yaml", "Nothing is lost"),
            "toml on a tablet" to listOf("From JSON to", "TOML has no null", "Leave it out"),
            "yaml on a phone" to listOf("Convert order.json", "Nothing is lost"),
            "toml on a phone" to listOf("Convert order.json", "TOML has no null"),
            "xml" to listOf("Save as order.xml", "XML has no null"),
            "csv" to listOf("From CSV to", "Save as countries.json"),
            "yaml to json" to listOf("Save as deployment.json", "The comments are left behind", "Aliases are written out"),
            "invalid" to listOf("The JSON has 4 errors. Repair them to convert the document."),
            "plain text" to listOf("Convert notes.txt", "Plain text has no tree")
        )

        val TypesTexts = mapOf(
            "kotlin" to listOf("Save as Order.kt"),
            "typescript on a tablet" to listOf("From JSON, in", "Save as order.ts"),
            "swift on a phone" to listOf("Types of order.json", "Save as Order.swift"),
            "go" to listOf("Save as order.go"),
            "json schema" to listOf("Save as order.schema.json"),
            "named root" to listOf("Save as Purchase.kt"),
            "list" to listOf("Save as Events.kt", "1 type from events.ndjson"),
            "yaml" to listOf("10 types from deployment.yaml", "Save as deployment.go"),
            "no object" to listOf("No types to write"),
            "invalid" to listOf("The JSON has 4 errors. Repair them to write its types."),
            "plain text" to listOf("Types of notes.txt", "Plain text has no tree")
        )

        val GraphTexts = mapOf(
            "order" to listOf("customer"),
            "order on a tablet" to listOf("100%", "customer"),
            "order on a phone" to listOf("70%"),
            "closed" to listOf("shipping"),
            "yaml" to listOf("70%"),
            "large" to listOf("85%"),
            "long" to listOf("85%"),
            "plain text" to listOf("Plain text has no tree")
        )

        val SchemaTexts = mapOf(
            "problems" to listOf("2 problems against order.schema.json", "coupon must be a string, and is null."),
            "problems on a tablet" to listOf("Validate order.json against", "2 problems against order.schema.json"),
            "valid on a tablet" to listOf("Valid against order.schema.json"),
            "none on a phone" to listOf("Validate order.json", "Choose a schema"),
            "problems on a phone" to listOf("2 problems against order.schema.json", "Line 8, column 13 · \$.coupon"),
            "unusable" to listOf("order.schema.json cannot be used as a schema"),
            "named" to listOf("Validate order.json against", "Choose a schema"),
            "yaml" to listOf("4 problems against order.schema.json", "total must be at least 0, and is -1."),
            "invalid" to listOf("4 problems"),
            "csv" to listOf("No schema for CSV"),
            "plain text" to listOf("Validate notes.txt", "Plain text has no tree")
        )

        val CompareTexts = mapOf(
            "meaning" to listOf("4 differences", "\"shipped\" → \"delivered\"", "Not counted as differences"),
            "meaning on a tablet" to listOf("A · order.json", "B · order-v2.json", "1 → 2"),
            "meaning on a phone" to listOf("In A", "\"shipped\"", "In B", "\"delivered\""),
            "last on a phone" to listOf("In A", "In B"),
            "shown on a phone" to listOf("A · order.json → B · order-v2.json"),
            "text" to listOf("4 differences", "Line 2", "\"status\": \"delivered\","),
            "far down" to listOf("1 changed", "\$.items[59].qty"),
            "the same" to listOf("No differences", "A and B mean the same."),
            "alone" to listOf("Nothing to compare with"),
            "invalid" to listOf("settings.json has 4 errors", "Repair it to compare by meaning, or compare by text."),
            "plain text" to listOf("notes.txt has nothing to compare by meaning")
        )
    }
}
