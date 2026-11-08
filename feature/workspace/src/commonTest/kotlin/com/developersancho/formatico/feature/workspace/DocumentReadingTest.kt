package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.text.Indentation
import com.developersancho.formatico.core.format.text.LineEnding
import com.pantrixui.foundation.date.PantrixDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Instant

class DocumentReadingTest {
    private val order =
        """
        {
          "id": "ord_8842",
          "createdAt": "2026-10-28T14:32:00Z",
          "total": 129.90,
          "customer": {"avatar": "https://images.example.com/u/elif.png", "website": "https://elif.dev"},
          "theme": {"accent": "#0F766E"},
          "items": [{"sku": "TS-001"}, {"sku": "MG-014"}],
          "eta": "2026-11-04"
        }
        """.trimIndent()

    private fun root(text: String = order): ValueNode = assertIs<Reading.Read>(readDocument(text, Format.Json)).root!!

    private fun ValueNode.find(key: String): ValueNode = trail(this, key).last()

    @Test
    fun theTreeHasAValueToANodeKeyedByItsPath() {
        val root = root()

        assertEquals("$", root.key)
        assertEquals("{7}", root.text)
        assertEquals(
            listOf("id", "createdAt", "total", "customer", "theme", "items", "eta"),
            root.children.map {
                it.name
            }
        )
        assertEquals("129.90", root.find("$.total").text)
        assertEquals("[2]", root.find("$.items").text)
        assertEquals("\"MG-014\"", root.find("$.items[1].sku").text)
        assertEquals("MG-014", root.find("$.items[1].sku").value)
    }

    @Test
    fun stringsArePreviewedByWhatTheyLookLike() {
        val root = root()

        assertEquals(
            ValuePreview.Moment(Instant.parse("2026-10-28T14:32:00Z")),
            root.find("$.createdAt").preview
        )
        assertEquals(ValuePreview.Day(PantrixDate(2026, 11, 4)), root.find("$.eta").preview)
        assertEquals(ValuePreview.Colour(0xFF0F766EL), root.find("$.theme.accent").preview)
        assertIs<ValuePreview.Picture>(root.find("$.customer.avatar").preview)
        assertIs<ValuePreview.Link>(root.find("$.customer.website").preview)
        assertNull(root.find("$.id").preview)
    }

    @Test
    fun previewsReadTheWaysTheyAreWritten() {
        assertEquals(ValuePreview.Colour(0xFFAABBCCL), previewOf("#abc"))
        assertEquals(ValuePreview.Colour(0x80112233L), previewOf("#11223380"))
        assertEquals(ValuePreview.Moment(Instant.parse("2026-10-28T11:32:00Z")), previewOf("2026-10-28 14:32:00+03:00"))
        assertIs<ValuePreview.Picture>(previewOf("https://example.com/a.JPG?size=2"))
        assertNull(previewOf("2026-13-45"))
        assertNull(previewOf("#12345"))
        assertNull(previewOf("example.com"))
    }

    @Test
    fun aKeyGivenTwiceGivesTheSecondANodeKeyOfItsOwn() {
        val root = root("""{"a": {"b": 1}, "a": {"b": 2}}""")

        assertEquals(listOf("$.a", "$.a#1"), root.children.map { it.key })
        assertEquals(listOf("$.a.b", "$.a#1.b"), root.children.map { it.children.single().key })
    }

    @Test
    fun aDocumentOpensWithItsRootAndItsObjectsOpen() {
        assertEquals(setOf("$", "$.customer", "$.theme"), defaultExpanded(root()))
    }

    @Test
    fun theTrailRunsFromTheRootToTheChosenValue() {
        assertEquals(
            listOf("$", "$.items", "$.items[0]", "$.items[0].sku"),
            trail(root(), "$.items[0].sku").map {
                it.key
            }
        )
        assertEquals(emptyList(), trail(root(), "$.missing"))
    }

    @Test
    fun plainTextIsNotRead() {
        assertEquals(Reading.PlainText, readDocument("hello", null))
    }

    @Test
    fun aYamlTreeKeepsItsAnchorsAndItsAliases() {
        val yaml =
            """
            metadata:
              labels: &labels
                app: formatico
            spec:
              replicas: 3
              size: "50"
              selector:
                matchLabels: *labels
            """.trimIndent()
        val root = assertIs<Reading.Read>(readDocument(yaml, Format.Yaml)).root!!

        assertEquals("labels", root.find("$.metadata.labels").anchor)
        val alias = root.find("$.spec.selector.matchLabels")
        assertEquals(ValueKind.Alias, alias.kind)
        assertEquals("*labels", alias.text)
        assertEquals("$.metadata.labels", alias.target?.key)
        assertEquals(ValueKind.Number, root.find("$.spec.replicas").kind)
        assertEquals("\"50\"", root.find("$.spec.size").text)
        assertEquals("50", root.find("$.spec.size").value)
    }

    @Test
    fun aStreamOfYamlDocumentsIsAnArrayOfThem() {
        val root = assertIs<Reading.Read>(readDocument("a: 1\n---\nb: 2\n", Format.Yaml)).root!!

        assertEquals(ValueKind.Array, root.kind)
        assertEquals(listOf("$[0]", "$[1]"), root.children.map { it.key })
        assertEquals("$[1].b", root.find("$[1].b").key)
    }

    @Test
    fun brokenYamlCountsItsErrorAndKeepsWhatWasRead() {
        val reading = assertIs<Reading.Read>(readDocument("a: 1\nb: [1, 2\n", Format.Yaml))

        assertEquals(1, reading.errors)
        assertEquals("a", reading.root?.children?.first()?.name)
    }

    @Test
    fun aBrokenDocumentCountsItsErrors() {
        val reading = assertIs<Reading.Read>(readDocument("""{"a": , "b": [1}""", Format.Json))
        assertEquals(2, reading.errors)
    }

    @Test
    fun theStatsCountLinesBytesAndTheLayout() {
        val stats = TextStats.of("{\r\n  \"ş\": 1\r\n}\r\n")

        assertEquals(3, stats.lines)
        assertEquals(17L, stats.bytes)
        assertEquals(LineEnding.Crlf, stats.layout.lineEnding)
        assertEquals(Indentation.Spaces(2), stats.layout.indentation)
        assertEquals(0, TextStats.of("").lines)
    }
}
