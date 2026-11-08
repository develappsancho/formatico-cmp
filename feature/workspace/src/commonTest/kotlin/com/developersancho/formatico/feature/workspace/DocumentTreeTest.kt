package com.developersancho.formatico.feature.workspace

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.developersancho.formatico.core.designsystem.SyntaxColors
import com.developersancho.formatico.core.format.Format
import com.pantrixui.components.data.treeview.TreeNode
import com.pantrixui.foundation.l10n.PantrixLocale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
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

    /** The keys of every branch under this node and its own: a tree with all of them open. */
    private fun ValueNode.branches(): Set<String> =
        children.flatMapTo(if (children.isEmpty()) mutableSetOf() else mutableSetOf(key)) { it.branches() }

    /** The parts of [label] in a style, as the text they cover and the style. */
    private fun partsOf(label: AnnotatedString): List<Pair<String, SpanStyle>> =
        label.spanStyles.map { label.text.substring(it.start, it.end) to it.item }

    @Test
    fun aRowIsTheNameAndTheValueTheRootsDollarWithASpaceAndNoColon() {
        val root = root()
        assertEquals("\$ {8}", rowLabel(root))
        assertEquals("id: \"ord_8842\"", rowLabel(root.find("$.id")))
        assertEquals("customer: {3}", rowLabel(root.find("$.customer")))
        assertEquals("items: [2]", rowLabel(root.find("$.items")))
        assertEquals("0: 1", rowLabel(root.find("$.items[0]")))
    }

    @Test
    fun theKeyTheColonAndTheValueAreEachInTheirKindsColour() {
        val root = root()
        val syntax = SyntaxColors.Light
        val id = styledLabelOf(root.find("$.id"), inks, marks = null)
        assertEquals("id: \"ord_8842\"", id.text)
        assertEquals(
            listOf("id" to syntax.key, ": " to syntax.punctuation, "\"ord_8842\"" to syntax.string),
            partsOf(id).map { it.first to it.second.color }
        )
        val inkOfValue = { key: String -> partsOf(styledLabelOf(root.find(key), inks, null)).last().second.color }
        assertEquals(syntax.number, inkOfValue("$.total"))
        assertEquals(syntax.literal, inkOfValue("$.paid"))
        assertEquals(syntax.literal, inkOfValue("$.coupon"))
        assertEquals(inks.size, inkOfValue("$.customer"))
        assertEquals(
            syntax.literal,
            partsOf(styledLabelOf(root(deployment, Format.Yaml).find("$.spec.selector.matchLabels"), inks, null)).last().second.color
        )
    }

    @Test
    fun aSearchMarksWhatItFoundInTheKeyAndInTheValueTheCurrentMatchStronger() {
        val root = root()
        val marks = TreeMarks(sought = "elif", locale = PantrixLocale.English, current = "$.customer.name")
        val name = styledLabelOf(root.find("$.customer.name"), inks, marks = marks)
        // The tree keeps a mark's own colours on the chosen row, where the current match is.
        assertEquals(
            listOf("Elif" to SpanStyle(color = inks.onCurrent, background = inks.current)),
            partsOf(name).filter { it.second.background != Color.Unspecified }
        )
        val email = styledLabelOf(root.find("$.customer.email"), inks, marks = marks)
        assertEquals(
            listOf("elif" to SpanStyle(background = inks.match)),
            partsOf(email).filter {
                it.second.background != Color.Unspecified
            }
        )
        // A key is searched too, a value's quotes are not.
        val key = styledLabelOf(root.find("$.customer.website"), inks, TreeMarks("web", PantrixLocale.English, current = null))
        assertEquals(listOf("web"), partsOf(key).filter { it.second.background != Color.Unspecified }.map { it.first })
        val quote = styledLabelOf(root.find("$.id"), inks, TreeMarks("\"", PantrixLocale.English, current = null))
        assertTrue(partsOf(quote).none { it.second.background != Color.Unspecified })
    }

    @Test
    fun placesAreFoundInTheTextsOwnLettersInTheReadersLanguage() {
        val english = PantrixLocale.English
        assertEquals(listOf(0..3, 7..10), placesOf("elif", "Elif & elif", english))
        assertEquals(emptyList(), placesOf("", "Elif", english))
        // In Turkish "ı" is the small "I"; in every language "i" is the small "İ", and a place is the text's own.
        assertEquals(listOf(0..4), placesOf("izmir", "İzmir", PantrixLocale.forLanguageTag("tr")))
        assertEquals(listOf(0..0), placesOf("ı", "Isparta", PantrixLocale.forLanguageTag("tr")))
        assertEquals(listOf(0..4), placesOf("izmir", "İzmir", english))
        assertEquals(listOf(1..1), placesOf("z", "İzmir", english))
    }

    /** The row of [key] in [source], asked for as the tree asks: down from the top, a branch at a time. */
    private fun rowIn(source: DocumentSource, key: String): TreeNode {
        var row = source.childAt(null, 0)
        while (row.key != key) {
            val under = (0 until source.childCount(row)).map { source.childAt(row, it) }
            row = under.first { key == it.key || key.startsWith(it.key + ".") || key.startsWith(it.key + "[") }
        }
        return row
    }

    @Test
    fun aRowEndsInWhatItsStringLooksLikeItsAnchorAndWhereItsAliasPoints() {
        val source = DocumentSource(root(), inks, marks = null)
        val top = source.childAt(null, 0)
        assertEquals(1, source.childCount(null))
        assertEquals("$", top.key)
        assertEquals("root", top.tagKey)
        assertEquals("createdAt", rowIn(source, "$.createdAt").tagKey)
        assertIs<ValuePreview.Moment>(source.endOf("$.createdAt")?.preview)
        assertEquals("#0F766E", rowIn(source, "$.theme.accent").let { source.endOf(it.key) }?.written)
        assertIs<ValuePreview.Link>(rowIn(source, "$.customer.website").let { source.endOf(it.key) }?.preview)
        assertNull(rowIn(source, "$.id").let { source.endOf(it.key) })

        val yaml = DocumentSource(root(deployment, Format.Yaml), inks, marks = null)
        assertEquals("&labels", rowIn(yaml, "$.metadata.labels").let { yaml.endOf(it.key) }?.anchor)
        assertEquals("$.metadata.labels", rowIn(yaml, "$.spec.selector.matchLabels").let { yaml.endOf(it.key) }?.target)
    }

    @Test
    fun aBranchsRowsAreMadeOnlyAsTheTreeAsksForThem() {
        val source = DocumentSource(root(), inks, marks = null)
        val customer = rowIn(source, "$.customer")
        assertTrue(customer.hasChildren)
        assertTrue(customer.children.isEmpty())
        // A row not asked for yet has no end made for it.
        assertNull(source.endOf("$.customer.website"))

        assertEquals(3, source.childCount(customer))
        assertEquals(
            listOf("$.customer.name", "$.customer.email", "$.customer.website"),
            (0 until 3).map {
                source.childAt(customer, it).key
            }
        )
        assertIs<ValuePreview.Link>(source.endOf("$.customer.website")?.preview)
        // A leaf is no branch.
        assertTrue((0 until 3).none { source.childAt(customer, it).hasChildren })
    }

    @Test
    fun aKeyIsFoundAmongItsBranchsValuesWithoutMakingTheirRows() {
        val source = DocumentSource(root(), inks, marks = null)
        val top = source.childAt(null, 0)

        assertEquals(0, source.indexOf(null, "$"))
        assertEquals(-1, source.indexOf(null, "$.id"))
        assertEquals(5, source.indexOf(top, "$.customer"))
        assertEquals("$.items", source.childAt(top, source.indexOf(top, "$.items")).key)
        assertEquals(1, source.indexOf(rowIn(source, "$.items"), "$.items[1]"))
        assertEquals(-1, source.indexOf(top, "$.nothing"))
    }

    @Test
    fun aSearchIsAnotherSourceAndAChoiceIsNot() {
        val root = root()
        val marks = TreeMarks("elif", PantrixLocale.English, current = "$.customer.name")
        assertEquals(DocumentSource(root, inks, null), DocumentSource(root, inks, null))
        assertNotEquals(DocumentSource(root, inks, null), DocumentSource(root, inks, marks))
        // The same values read again are another document.
        assertNotEquals(DocumentSource(root, inks, null), DocumentSource(root(), inks, null))
        // What a search marks is in the row it makes.
        val name = rowIn(DocumentSource(root, inks, marks), "$.customer.name")
        assertEquals(styledLabelOf(root.find("$.customer.name"), inks, marks), name.styledLabel)
    }
}
