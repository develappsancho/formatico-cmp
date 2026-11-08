package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.types.TypeLanguage
import com.developersancho.formatico.core.network.linkSite
import com.developersancho.formatico.core.share.LinkReading
import com.developersancho.formatico.core.share.ShareLinks
import com.developersancho.formatico.core.share.ShareOutcome
import com.developersancho.formatico.core.share.SharedDocument
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val workbench = Workbench(worker = dispatcher)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val readings = Readings(workbench)

    private fun viewModel(id: Long, reader: CoroutineDispatcher = dispatcher) =
        WorkspaceViewModel(documentId = id, workbench = workbench, readings = readings, reader = reader)

    @Test
    fun theShareMakesALinkThatCarriesTheDocumentAndFormatsItWhereAsked() {
        val id = workbench.open("order.json", """{"a":1}""", Format.Json)
        val viewModel = viewModel(id)

        viewModel.dispatch(WorkspaceIntent.Share(open = true))
        val share = assertNotNull(viewModel.state.value.share)
        val link = assertIs<ShareOutcome.Link>(share.outcome)
        assertEquals(LinkReading.Read(SharedDocument("""{"a":1}""", "order.json")), ShareLinks.read(link.link, linkSite()))
        assertTrue(share.canFormat)
        assertFalse(share.formatted)
        assertTrue(workbench.state.value.documents.single().view.sharing)

        viewModel.dispatch(WorkspaceIntent.ShareFormatted(formatted = true))
        val formatted = assertIs<ShareOutcome.Link>(viewModel.state.value.share?.outcome)
        assertEquals(LinkReading.Read(SharedDocument("{\n  \"a\": 1\n}", "order.json")), ShareLinks.read(formatted.link, linkSite()))
        // The document itself is left as it is.
        assertEquals("""{"a":1}""", workbench.state.value.documents.single().text)

        viewModel.dispatch(WorkspaceIntent.Share(open = false))
        assertNull(viewModel.state.value.share)
        // Opened again, the link carries the document as it is.
        viewModel.dispatch(WorkspaceIntent.Share(open = true))
        assertEquals(false, viewModel.state.value.share?.formatted)
    }

    @Test
    fun plainTextAndADocumentWithErrorsAreSharedAsTheyAre() {
        val text = viewModel(workbench.open("notes.txt", "a note\n", null))
        text.dispatch(WorkspaceIntent.Share(open = true))
        assertEquals(false, text.state.value.share?.canFormat)

        val invalid = viewModel(workbench.open("broken.json", """{"a": }""", Format.Json))
        invalid.dispatch(WorkspaceIntent.Share(open = true))
        invalid.dispatch(WorkspaceIntent.ShareFormatted(formatted = true))
        val link = assertIs<ShareOutcome.Link>(invalid.state.value.share?.outcome)
        assertEquals(false, invalid.state.value.share?.canFormat)
        assertEquals(LinkReading.Read(SharedDocument("""{"a": }""", "broken.json")), ShareLinks.read(link.link, linkSite()))
    }

    @Test
    fun theDocumentIsReadAndOpensWithItsObjects() {
        val id = workbench.open("order.json", """{"a": {"b": 1}, "c": [2]}""", Format.Json)

        val state = viewModel(id).state.value

        val reading = assertIs<Reading.Read>(state.reading)
        assertEquals(0, reading.errors)
        assertEquals(setOf("$", "$.a"), state.expanded)
        assertEquals(DocumentEdit.entries.toSet(), state.edits)
        assertEquals(1, state.stats?.lines)
    }

    @Test
    fun theChosenValueAndTheOpenBranchesAreKeptWithTheDocument() {
        val id = workbench.open(null, """{"a": {"b": 1}}""", Format.Json)
        val viewModel = viewModel(id)

        viewModel.dispatch(WorkspaceIntent.Select("$.a.b"))
        viewModel.dispatch(WorkspaceIntent.Expand(setOf("$")))

        assertEquals(listOf("$", "$.a", "$.a.b"), viewModel.state.value.trail.map { it.key })
        assertEquals(setOf("$"), viewModel.state.value.expanded)
        // A workspace made again for the same tab finds it as it was left.
        val again = viewModel(id).state.value
        assertEquals("$.a.b", again.selected?.key)
        assertEquals(setOf("$"), again.expanded)
    }

    @Test
    fun choosingAValueOpensTheBranchesItStandsIn() {
        val id = workbench.open(null, """{"a": [{"b": 1}], "c": 2}""", Format.Json)
        val viewModel = viewModel(id)
        assertEquals(setOf("$"), viewModel.state.value.expanded)

        viewModel.dispatch(WorkspaceIntent.Select("$.a[0].b"))

        assertEquals(setOf("$", "$.a", "$.a[0]"), viewModel.state.value.expanded)
        assertEquals("$.a[0].b", viewModel.state.value.selected?.key)
    }

    @Test
    fun anEditIsMadeAndReportedAndTheDocumentReadAgain() = runTest {
        val id = workbench.open(null, """{"b": 1, "a": 2}""", Format.Json)
        val viewModel = viewModel(id)
        val effects = mutableListOf<WorkspaceEffect>()
        backgroundScope.launch(dispatcher) { viewModel.effects.collect { effects += it } }

        viewModel.dispatch(WorkspaceIntent.Edit(DocumentEdit.SortKeys))

        val edited = assertIs<WorkspaceEffect.Edited>(effects.single())
        assertEquals(DocumentEdit.SortKeys, edited.edit)
        assertIs<EditResult.Changed>(edited.result)
        assertEquals(listOf("a", "b"), viewModel.state.value.root?.children?.map { it.name })
    }

    @Test
    fun plainTextIsNotEditable() {
        val id = workbench.open("notes.txt", "Things to check", format = null)

        val state = viewModel(id).state.value

        assertEquals(Reading.PlainText, state.reading)
        assertTrue(state.edits.isEmpty())
    }

    @Test
    fun aYamlDocumentIsReadAndFormatted() = runTest {
        val id = workbench.open("deployment.yaml", "a:    &x 1\nb:   *x\n", Format.Yaml)
        val viewModel = viewModel(id)

        val reading = assertIs<Reading.Read>(viewModel.state.value.reading)
        assertEquals(0, reading.errors)
        assertEquals(DocumentEdit.entries.toSet(), viewModel.state.value.edits)
        assertIs<EditResult.Changed>(workbench.edit(id, DocumentEdit.Format))
        assertEquals("a: &x 1\nb: *x\n", workbench.state.value.selected!!.text)
    }

    @Test
    fun theTableFollowsTheChoiceAndKeepsItsOrderItsColumnsAndItsFilter() {
        val id = workbench.open(null, """{"rows": [{"a": 2, "b": 1}, {"a": 1}]}""", Format.Json)
        val viewModel = viewModel(id)

        viewModel.dispatch(WorkspaceIntent.Select("$.rows[1].a"))
        viewModel.dispatch(WorkspaceIntent.SortTable(TableSort("a", descending = true)))
        viewModel.dispatch(WorkspaceIntent.HideColumns(setOf("b")))
        viewModel.dispatch(WorkspaceIntent.FilterTable("2"))
        viewModel.dispatch(WorkspaceIntent.ShowPane(DocumentPane.Tree))

        val state = viewModel.state.value
        assertEquals("$.rows", state.table?.source?.key)
        assertEquals(TableChoice(row = 1, column = "a"), state.tableChoice)
        assertEquals(TableSort("a", descending = true), state.document?.view?.tableSort)
        assertEquals(setOf("b"), state.document?.view?.hiddenColumns)
        assertEquals("2", state.document?.view?.tableFilter)
    }

    @Test
    fun aCsvCanBeFormattedOnly() {
        val id = workbench.open("a.csv", "a,b\n1,2\n", Format.Csv)

        assertEquals(setOf(DocumentEdit.Format), viewModel(id).state.value.edits)
    }

    @Test
    fun changingTheFormatReadsTheDocumentAgain() {
        val id = workbench.open(null, "{a: 1}", Format.Json)
        val viewModel = viewModel(id)
        assertEquals(1, assertIs<Reading.Read>(viewModel.state.value.reading).errors)

        viewModel.dispatch(WorkspaceIntent.ChangeFormat(Format.Json5))

        assertEquals(0, assertIs<Reading.Read>(viewModel.state.value.reading).errors)
    }

    @Test
    fun closingTheDocumentLeavesTheWorkspaceWithNone() {
        val id = workbench.open(null, "{}", Format.Json)
        val viewModel = viewModel(id)

        viewModel.dispatch(WorkspaceIntent.Close)

        assertNull(viewModel.state.value.document)
        assertTrue(workbench.state.value.documents.isEmpty())
    }

    @Test
    fun aWorkspaceMadeAgainFindsItsDocumentReadAndReadsItOnlyOnceChanged() {
        val id = workbench.open("order.json", """{"a": 1}""", Format.Json)
        assertIs<Reading.Read>(viewModel(id).state.value.reading)
        val text = workbench.state.value.documents.single().text

        // A reader that never runs: the workspace made again for the same document does not need it.
        assertIs<Reading.Read>(viewModel(id, reader = StandardTestDispatcher()).state.value.reading)

        // Another text, or the same in another format, is read again.
        assertNull(readings.of(id, """{"a": 2}""", Format.Json))
        assertNull(readings.of(id, text, Format.Json5))
    }

    @Test
    fun aClosedDocumentsReadingIsForgotten() {
        val id = workbench.open("order.json", """{"a": 1}""", Format.Json)
        viewModel(id)
        val text = workbench.state.value.documents.single().text

        workbench.close(id)

        assertNull(readings.of(id, text, Format.Json))
    }

    @Test
    fun theDocumentIsConvertedWhileItsConvertIsShownAndAsItsChoiceSays() = runTest(dispatcher) {
        val id = workbench.open("order.json", """{"coupon": null, "b": 1}""", Format.Json)
        val viewModel = viewModel(id)
        assertNull(viewModel.state.value.conversion)

        viewModel.dispatch(WorkspaceIntent.ShowPane(DocumentPane.Convert))
        val yaml = viewModel.state.first { it.conversion != null }.conversion.converted
        assertEquals("coupon: null\nb: 1\n", yaml?.text)

        viewModel.dispatch(WorkspaceIntent.Convert(ConvertChoice(to = Format.Toml)))
        val toml = viewModel.state.first { it.conversion.converted?.to == Format.Toml }.conversion.converted
        // The comment that marks the null left out is in the reader's language, with the formats' names in it.
        val comment = toml?.text?.lines()?.first().orEmpty()
        assertTrue(comment.startsWith("# coupon = null") && "JSON" in comment && "TOML" in comment, comment)
        assertEquals("b = 1", toml?.text?.lines()?.get(1))
        assertEquals(ConvertChoice(to = Format.Toml), workbench.state.value.documents.single().view.convert)

        viewModel.dispatch(WorkspaceIntent.ShowPane(DocumentPane.Tree))
        viewModel.state.first { it.conversion == null }
    }

    @Test
    fun theDocumentsTypesAreWrittenWhileItsTypesIsShownAndAsItsChoiceSays() = runTest(dispatcher) {
        val id = workbench.open("order.json", """{"id": "a", "qty": 2}""", Format.Json)
        val viewModel = viewModel(id)
        assertNull(viewModel.state.value.types)

        viewModel.dispatch(WorkspaceIntent.ShowPane(DocumentPane.Types))
        val kotlin = viewModel.state.first { it.types != null }.types.written
        assertEquals(TypeLanguage.Kotlin, kotlin?.language)
        assertEquals("Order", kotlin?.root)
        assertEquals("Order.kt", kotlin?.fileName)
        assertTrue(kotlin?.text.orEmpty().contains("data class Order("), kotlin?.text)

        viewModel.dispatch(WorkspaceIntent.Types(TypesChoice(language = TypeLanguage.Go, root = "line item")))
        val go = viewModel.state.first { it.types.written?.language == TypeLanguage.Go }.types.written
        assertEquals("LineItem", go?.root)
        assertTrue(go?.text.orEmpty().contains("type LineItem struct {"), go?.text)
        assertEquals(TypesChoice(language = TypeLanguage.Go, root = "line item"), workbench.state.value.documents.single().view.types)

        viewModel.dispatch(WorkspaceIntent.ShowPane(DocumentPane.Tree))
        viewModel.state.first { it.types == null }
    }

    @Test
    fun theDocumentIsValidatedWhileItsSchemaIsShownOrOneIsChosen() = runTest(dispatcher) {
        val schemaId = workbench.open("order.schema.json", PreviewOrderSchema, Format.Json)
        val id = workbench.open("order.json", PreviewBoardOrder, Format.Json)
        val viewModel = viewModel(id)
        assertNull(viewModel.state.value.schema)

        viewModel.dispatch(WorkspaceIntent.ShowPane(DocumentPane.Schema))
        val none = viewModel.state.first { it.schema != null }
        assertEquals(SchemaCheck.NoSchema(named = null), none.schema)
        assertEquals(listOf(OfferedSchema(schemaId, "order.schema.json", null)), none.schemas)

        viewModel.dispatch(WorkspaceIntent.Schema(SchemaChoice(with = schemaId)))
        val failed = assertIs<SchemaCheck.Failed>(viewModel.state.first { it.schema is SchemaCheck.Failed }.schema)
        assertEquals(2, failed.total)

        // Chosen, the schema validates the document in its other views too, again as it is written.
        viewModel.dispatch(WorkspaceIntent.ShowPane(DocumentPane.Tree))
        assertIs<SchemaCheck.Failed>(viewModel.state.value.schema)
        workbench.write(id, PreviewValidOrder)
        viewModel.state.first { it.schema is SchemaCheck.Valid }

        // Its tab closed, the schema is chosen no more.
        workbench.close(schemaId)
        viewModel.state.first { it.schema == null }
        assertEquals(SchemaChoice(), workbench.state.value.documents.single().view.schema)
    }

    @Test
    fun theSchemaTheDocumentsSchemaNamesIsChosenForIt() = runTest(dispatcher) {
        val withId = PreviewOrderSchema.replaceFirst("{\n", "{\n  \"\$id\": \"https://example.com/schemas/order.json\",\n")
        val schemaId = workbench.open("order.schema.json", withId, Format.Json)
        val naming = PreviewBoardOrder.replaceFirst("{\n", "{\n  \"\$schema\": \"https://example.com/schemas/order.json\",\n")
        val id = workbench.open("order.json", naming, Format.Json)
        val viewModel = viewModel(id)

        viewModel.dispatch(WorkspaceIntent.ShowPane(DocumentPane.Schema))
        viewModel.state.first { it.schema is SchemaCheck.Failed }
        assertEquals(SchemaChoice(with = schemaId), workbench.state.value.documents.first { it.id == id }.view.schema)
    }

    @Test
    fun theDocumentIsComparedWithTheTabAfterItWhileItsCompareIsShown() = runTest(dispatcher) {
        val id = workbench.open("order.json", PreviewCompareA, Format.Json)
        val other = workbench.open("order-v2.json", PreviewCompareB, Format.Json)
        workbench.select(id)
        val viewModel = viewModel(id)
        assertEquals(listOf(OtherDocument(other, "order-v2.json")), viewModel.state.value.others)
        assertNull(viewModel.state.value.comparison)

        viewModel.dispatch(WorkspaceIntent.ShowPane(DocumentPane.Compare))
        val meaning = viewModel.state.first { it.comparison != null }.comparison.compared
        assertEquals(4, meaning?.differences?.size)
        assertEquals("order-v2.json", meaning?.b?.name)

        viewModel.dispatch(WorkspaceIntent.Compare(CompareChoice(by = CompareBy.Text)))
        val text = viewModel.state.first { it.comparison.compared?.by == CompareBy.Text }.comparison.compared
        assertTrue(text?.differences?.all { it.path == null } == true)
        assertEquals(CompareChoice(by = CompareBy.Text), workbench.state.value.documents.first { it.id == id }.view.compare)

        viewModel.dispatch(WorkspaceIntent.ShowPane(DocumentPane.Tree))
        viewModel.state.first { it.comparison == null }
    }
}
