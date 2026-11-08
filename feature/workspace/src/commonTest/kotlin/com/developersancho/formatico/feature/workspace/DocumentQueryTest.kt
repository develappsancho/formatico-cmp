package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.storage.MemorySmallStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentQueryTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val order =
        """
        {
          "id": "ord_8842",
          "total": 129.90,
          "items": [
            {"sku": "TS-001", "name": "T-shirt", "qty": 2, "price": 39.95},
            {"sku": "MG-014", "name": "Mug", "qty": 1, "price": 50.00}
          ]
        }
        """.trimIndent()

    private val feed =
        """
        <rss version="2.0">
          <channel>
            <item><title>Tabs</title><category>release</category></item>
            <item><title>Anchors</title><category>guide</category></item>
            <item><title>Colours</title><category>release</category></item>
          </channel>
        </rss>
        """.trimIndent()

    private val deployment =
        """
        metadata:
          labels: &labels
            app: formatico
        spec:
          selector:
            matchLabels: *labels
        """.trimIndent()

    private fun outcome(text: String, format: Format, query: String): QueryOutcome {
        val document = OpenDocument(id = 1, name = "doc", text = text, format = format)
        return queryOutcome(document, readDocument(text, format), query)
    }

    private fun found(text: String, format: Format, query: String): List<QueryMatch> =
        assertIs<QueryOutcome.Found>(outcome(text, format, query)).matches

    @Test
    fun aJsonPathFindsTheTreesValuesWithTheirPathsAsTheyAreWritten() {
        val matches = found(order, Format.Json, "$.items[*].name")
        assertEquals(listOf("$.items[0].name", "$.items[1].name"), matches.map { it.path })
        assertEquals(listOf("\"T-shirt\"", "\"Mug\""), matches.map { it.value })
        assertEquals(listOf("$.items[0].name", "$.items[1].name"), matches.map { it.key })
        assertTrue(matches.all { it.kind == ValueKind.String })
        // The spelling of a number is its own; a filter compares its value.
        assertEquals(listOf("50.00"), found(order, Format.Json, "  $.items[?@.price > 40].price  ").map { it.value })
        assertEquals(listOf("$.total"), found(order, Format.Json, "$.total").map { it.key })
        assertEquals(emptyList(), found(order, Format.Json, "$.discount"))
    }

    @Test
    fun aJsonPathGoesThroughAYamlAliasToTheValueItPointsAt() {
        val matches = found(deployment, Format.Yaml, "$.spec.selector.matchLabels.app")
        assertEquals(listOf("$.metadata.labels.app"), matches.map { it.key })
        assertEquals(listOf("formatico"), matches.map { it.value })
    }

    @Test
    fun anXPathFindsAnXmlDocumentsNodesAndChoosesTheTreesValueWrittenWhereTheyAre() {
        val matches = found(feed, Format.Xml, "//item[category='release']/title")
        assertEquals(listOf("/rss/channel/item[1]/title", "/rss/channel/item[3]/title"), matches.map { it.path })
        assertEquals(listOf("Tabs", "Colours"), matches.map { it.value })
        assertEquals(listOf("$.rss.channel.item[0].title", "$.rss.channel.item[2].title"), matches.map { it.key })
        assertEquals(listOf("$.rss['@version']"), found(feed, Format.Xml, "/rss/@version").map { it.key })
        assertEquals(QueryOutcome.Computed("3"), outcome(feed, Format.Xml, "count(//item)"))
    }

    @Test
    fun aBlankQueryAsksNothingAndAQueryThatIsNoneSaysWhereInItsOwnText() {
        assertEquals(QueryOutcome.Unasked, outcome(order, Format.Json, "  "))
        assertEquals(QueryOutcome.Unread, outcome("", Format.Json, "$.id"))
        val failed = assertIs<QueryOutcome.Failed>(outcome(order, Format.Json, "  $.items["))
        // The place is the query's as it was written, its spaces before it counted.
        assertTrue(failed.error.offset >= "  $.items".length, "At ${failed.error.offset}")
        assertIs<QueryOutcome.Failed>(outcome(feed, Format.Xml, "//item["))
    }

    @Test
    fun numbersAreReadAsEachFormatWritesThem() {
        assertEquals(129.9, numberWritten("129.90"))
        assertEquals(1000.0, numberWritten("1_000"))
        assertEquals(31.0, numberWritten("0x1F"))
        assertEquals(-8.0, numberWritten("-0o10"))
        assertEquals(5.0, numberWritten("+0b101"))
        assertEquals(Double.POSITIVE_INFINITY, numberWritten(".inf"))
        assertEquals(Double.NEGATIVE_INFINITY, numberWritten("-Infinity"))
        assertTrue(numberWritten("nan").isNaN())
        assertTrue(numberWritten("twelve").isNaN())
    }

    @Test
    fun theResultsAreAJsonArrayOrAnXmlDocumentNamedAfterTheDocument() {
        val document = OpenDocument(id = 1, name = "order.json", text = order, format = Format.Json)
        val root = assertIs<Reading.Read>(readDocument(order, Format.Json)).root
        val results = queryResults(document, root, outcome(order, Format.Json, "$.items[*]['name','price']"))!!
        assertEquals("order-query.json", results.name)
        assertEquals(Format.Json, results.format)
        val written = assertIs<Reading.Read>(readDocument(results.text, Format.Json))
        assertEquals(0, written.errors)
        assertEquals(listOf("\"T-shirt\"", "39.95", "\"Mug\"", "50.00"), written.root?.children?.map { it.text })

        val yaml = OpenDocument(id = 2, name = "deployment.yaml", text = deployment, format = Format.Yaml)
        val yamlRoot = assertIs<Reading.Read>(readDocument(deployment, Format.Yaml)).root
        val labels = queryResults(yaml, yamlRoot, outcome(deployment, Format.Yaml, "$.spec.selector"))!!
        assertEquals("deployment-query.json", labels.name)
        assertTrue("\"app\": \"formatico\"" in labels.text, labels.text)

        val xml = OpenDocument(id = 3, name = "feed.xml", text = feed, format = Format.Xml)
        val xmlRoot = assertIs<Reading.Read>(readDocument(feed, Format.Xml)).root
        val titles = queryResults(xml, xmlRoot, outcome(feed, Format.Xml, "//item[1]/title | /rss/@version"))!!
        assertEquals("feed-query.xml", titles.name)
        assertTrue("<result>2.0</result>" in titles.text && "<title>Tabs</title>" in titles.text, titles.text)
        val count = queryResults(xml, xmlRoot, outcome(feed, Format.Xml, "count(//item)"))!!
        assertTrue("<result>3</result>" in count.text, count.text)

        assertNull(queryResults(document, root, outcome(order, Format.Json, "$.discount")))
        assertNull(queryResults(document, root, QueryOutcome.Unasked))
    }

    @Test
    fun theRecentQueriesAreTheLatestFirstEachOnceAndAFewOfEachLanguage() {
        val recent = (1..7).fold(RecentQueries()) { queries, n -> queries.with(QueryLanguage.JsonPath, "$.a$n") }
            .with(QueryLanguage.JsonPath, "$.a5")
            .with(QueryLanguage.XPath, "//a")
        assertEquals(listOf("$.a5", "$.a7", "$.a6", "$.a4", "$.a3"), recent.of(QueryLanguage.JsonPath))
        assertEquals(listOf("//a"), recent.of(QueryLanguage.XPath))
        // Those used since the kept ones were read stand before them.
        val kept = RecentQueries(jsonPath = listOf("$.old", "$.a5"))
        assertEquals(listOf("$.new", "$.old", "$.a5"), kept.before(RecentQueries(jsonPath = listOf("$.new"))).of(QueryLanguage.JsonPath))
    }

    @Test
    fun theQueriesUsedAreKeptOnTheDeviceAndReadBackUnderThoseUsedSince() = runTest(dispatcher) {
        val small = MemorySmallStore()
        val first = QueryHistory(small, this)
        first.remember(QueryLanguage.JsonPath, "  $.items  ")
        // What is used before what was kept is read waits for it, and is written with it.
        first.start().join()
        first.remember(QueryLanguage.JsonPath, " ")
        assertEquals(listOf("$.items"), first.queries.value.of(QueryLanguage.JsonPath))

        val second = QueryHistory(small, this)
        second.remember(QueryLanguage.XPath, "//item")
        second.start().join()
        assertEquals(listOf("$.items"), second.queries.value.of(QueryLanguage.JsonPath))
        assertEquals(listOf("//item"), second.queries.value.of(QueryLanguage.XPath))
    }

    @Test
    fun theViewModelAsksTheQueryWhileItIsShownAndChoosesItsFirstMatch() = runTest(dispatcher) {
        val workbench = Workbench(worker = dispatcher)
        val history = QueryHistory(MemorySmallStore(), TestScope(dispatcher))
        val id = workbench.open("order.json", order, Format.Json)
        val model = viewModel(workbench, id, history, dispatcher)

        model.dispatch(WorkspaceIntent.Query("$.items[*].name"))
        assertNull(model.state.value.query, "Asked only while it is shown")
        model.dispatch(WorkspaceIntent.ShowPane(DocumentPane.Query))
        val state = model.state.value
        assertEquals(listOf("$.items[0].name", "$.items[1].name"), state.query.matches.map { it.key })
        assertEquals("$.items[0].name", state.selected?.key)
        assertEquals(0, state.currentMatch)

        // A match chosen stays chosen as long as it is one.
        model.dispatch(WorkspaceIntent.Select("$.items[1].name"))
        model.dispatch(WorkspaceIntent.Query("$.items[*].name "))
        assertEquals(1, model.state.value.currentMatch)

        model.dispatch(WorkspaceIntent.UseQuery)
        assertEquals(listOf("$.items[*].name"), model.state.value.recentQueries.of(QueryLanguage.JsonPath))
        assertEquals(emptyList(), model.state.value.otherQueries)

        model.dispatch(WorkspaceIntent.ShowPane(DocumentPane.Tree))
        assertNull(model.state.value.query)
        assertEquals("$.items[*].name ", workbench.state.value.selected?.view?.query)
    }

    private fun viewModel(workbench: Workbench, id: Long, history: QueryHistory, reader: CoroutineDispatcher) =
        WorkspaceViewModel(documentId = id, workbench = workbench, readings = Readings(workbench), reader = reader, queries = history)
}
