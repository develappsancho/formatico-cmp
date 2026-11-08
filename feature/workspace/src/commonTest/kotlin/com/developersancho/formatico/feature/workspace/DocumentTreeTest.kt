package com.developersancho.formatico.feature.workspace

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.developersancho.formatico.core.designsystem.SyntaxColors
import com.developersancho.formatico.core.format.Format
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DocumentTreeTest {
    private val order =
        """
        {
          "id": "ord_8842",
          "createdAt": "2026-10-28T14:32:00Z",
          "total": 129.90,
          "paid": true,
          "coupon": null,
          "customer": {"name": "Elif Yılmaz", "email": "elif@example.com", "website": "https://elif.dev"},
          "theme": {"accent": "#0F766E"},
          "items": [1, 2]
        }
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

    private val inks = RowInks(
        syntax = SyntaxColors.Light,
        size = Color(0xFF475569),
        match = Color(0xFFFEF3C7),
        current = Color(0xFFD97706),
        onCurrent = Color(0xFF000000)
    )

    private fun root(text: String = order, format: Format = Format.Json): ValueNode =
        assertIs<Reading.Read>(readDocument(text, format)).root!!

    private fun ValueNode.find(key: String): ValueNode = trail(this, key).last()

    /** The parts of [label] in a style, as the text they cover and the style. */
    private fun partsOf(label: AnnotatedString): List<Pair<String, SpanStyle>> =
        label.spanStyles.map { label.text.substring(it.start, it.end) to it.item }

    @Test
    fun aRowIsTheNameAndTheValueTheRootsDollarWithNoColon() {
        val root = root()
        assertEquals("\${8}", rowLabel(root))
        assertEquals("id: \"ord_8842\"", rowLabel(root.find("$.id")))
        assertEquals("customer: {3}", rowLabel(root.find("$.customer")))
        assertEquals("items: [2]", rowLabel(root.find("$.items")))
        assertEquals("0: 1", rowLabel(root.find("$.items[0]")))
    }

    @Test
    fun theKeyTheColonAndTheValueAreEachInTheirKindsColour() {
        val root = root()
        val syntax = SyntaxColors.Light
        val id = styledLabelOf(root.find("$.id"), inks, chosen = false, marks = null)
        assertEquals("id: \"ord_8842\"", id.text)
        assertEquals(
            listOf("id" to syntax.key, ": " to syntax.punctuation, "\"ord_8842\"" to syntax.string),
            partsOf(id).map { it.first to it.second.color }
        )
        val inkOfValue = { key: String -> partsOf(styledLabelOf(root.find(key), inks, false, null)).last().second.color }
        assertEquals(syntax.number, inkOfValue("$.total"))
        assertEquals(syntax.literal, inkOfValue("$.paid"))
        assertEquals(syntax.literal, inkOfValue("$.coupon"))
        assertEquals(inks.size, inkOfValue("$.customer"))
        assertEquals(
            syntax.literal,
            partsOf(styledLabelOf(root(deployment, Format.Yaml).find("$.spec.selector.matchLabels"), inks, false, null)).last().second.color
        )
    }

    @Test
    fun theChosenRowIsWrittenInItsOwnColour() {
        val label = styledLabelOf(root().find("$.id"), inks, chosen = true, marks = null)
        assertEquals("id: \"ord_8842\"", label.text)
        assertTrue(label.spanStyles.isEmpty())
    }

    @Test
    fun aSearchMarksWhatItFoundInTheKeyAndInTheValueTheCurrentMatchStronger() {
        val root = root()
        val marks = TreeMarks(sought = "elif", language = "en", current = "$.customer.name")
        val name = styledLabelOf(root.find("$.customer.name"), inks, chosen = true, marks = marks)
        assertEquals(
            listOf("Elif" to SpanStyle(color = inks.onCurrent, background = inks.current)),
            partsOf(name)
        )
        val email = styledLabelOf(root.find("$.customer.email"), inks, chosen = false, marks = marks)
        assertEquals(
            listOf("elif" to SpanStyle(background = inks.match)),
            partsOf(email).filter {
                it.second.background != Color.Unspecified
            }
        )
        // A key is searched too, a value's quotes are not.
        val key = styledLabelOf(root.find("$.customer.website"), inks, false, TreeMarks("web", "en", current = null))
        assertEquals(listOf("web"), partsOf(key).filter { it.second.background != Color.Unspecified }.map { it.first })
        val quote = styledLabelOf(root.find("$.id"), inks, false, TreeMarks("\"", "en", current = null))
        assertTrue(partsOf(quote).none { it.second.background != Color.Unspecified })
    }

    @Test
    fun placesAreFoundInTheTextsOwnLettersInTheReadersLanguage() {
        assertEquals(listOf(0..3, 7..10), placesOf("elif", "Elif & elif", "en"))
        assertEquals(emptyList(), placesOf("", "Elif", "en"))
        // In Turkish "i" is the small "İ"; elsewhere "İ" grows to two small letters, and the place is still its own.
        assertEquals(listOf(0..4), placesOf("izmir", "İzmir", "tr"))
        assertEquals(listOf(0..0), placesOf("i̇", "İzmir", "en"))
        assertEquals(listOf(1..1), placesOf("z", "İzmir", "en"))
    }

    @Test
    fun aRowEndsInWhatItsStringLooksLikeItsAnchorAndWhereItsAliasPoints() {
        val rows = documentRows(root(), inks, selected = "$.id", marks = null)
        val top = rows.nodes.single()
        assertEquals("$", top.key)
        assertEquals("root", top.tagKey)
        assertTrue(top.children.any { it.key == "$.createdAt" && it.tagKey == "createdAt" })
        assertIs<ValuePreview.Moment>(rows.ends.getValue("$.createdAt").preview)
        assertEquals("#0F766E", rows.ends.getValue("$.theme.accent").written)
        assertIs<ValuePreview.Link>(rows.ends.getValue("$.customer.website").preview)
        assertNull(rows.ends["$.id"])
        // The chosen row's label has no colours of its own.
        assertTrue(top.children.first { it.key == "$.id" }.styledLabel!!.spanStyles.isEmpty())

        val yaml = documentRows(root(deployment, Format.Yaml), inks, selected = null, marks = null)
        assertEquals("&labels", yaml.ends.getValue("$.metadata.labels").anchor)
        assertEquals("$.metadata.labels", yaml.ends.getValue("$.spec.selector.matchLabels").target)
    }
}
