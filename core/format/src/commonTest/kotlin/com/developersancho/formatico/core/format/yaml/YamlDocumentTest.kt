package com.developersancho.formatico.core.format.yaml

import com.developersancho.formatico.core.format.Problem
import com.developersancho.formatico.core.format.ProblemCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class YamlDocumentTest {
    private val deployment =
        """
        # A service
        metadata:
          name: formatico-web
          labels: &labels
            app: formatico
        spec:
          replicas: 3
          selector:
            matchLabels: *labels
          args: ["--queue", 'conversions', 4]
        """.trimIndent() + "\n"

    private fun YamlNode?.asMapping(): YamlMapping = assertIs<YamlMapping>(this)

    private fun YamlMapping.value(key: String): YamlNode = entries.first { (it.key as YamlScalar).value == key }.value

    private fun YamlDocument.codes(): List<ProblemCode> = problems.map(Problem::code)

    @Test
    fun aMappingKeepsItsKeysInOrderAndItsAnchors() {
        val document = YamlDocument.parse(deployment)

        assertTrue(document.isValid)
        val root = document.documents.single().asMapping()
        assertEquals(listOf("metadata", "spec"), root.entries.map { (it.key as YamlScalar).value })
        val labels = root.value("metadata").asMapping().value("labels").asMapping()
        assertEquals("labels", labels.anchor)
        val alias =
            assertIs<YamlAlias>(root.value("spec").asMapping().value("selector").asMapping().value("matchLabels"))
        assertEquals("labels", alias.name)
        assertSame(labels, alias.target)
        assertEquals("*labels", deployment.substring(alias.range.start, alias.range.end))
    }

    @Test
    fun scalarsAreReadByTheCoreSchemaAndKeepTheirSpelling() {
        val text = "a: 3\nb: \"50\"\nc: 250m\nd: true\ne: ~\nf: 1.50\ng: 0x1F\nh: 'x'\ni: !!str 12\nj:\n"
        val root = YamlDocument.parse(text).documents.single().asMapping()
        val scalars = root.entries.map { assertIs<YamlScalar>(it.value) }

        assertEquals(
            listOf(
                YamlScalarKind.Integer,
                YamlScalarKind.String,
                YamlScalarKind.String,
                YamlScalarKind.Boolean,
                YamlScalarKind.Null,
                YamlScalarKind.Float,
                YamlScalarKind.Integer,
                YamlScalarKind.String,
                YamlScalarKind.String,
                YamlScalarKind.Null
            ),
            scalars.map { it.kind }
        )
        assertEquals("\"50\"", scalars[1].raw)
        assertEquals("50", scalars[1].value)
        assertEquals(YamlScalarStyle.DoubleQuoted, scalars[1].style)
        assertEquals("1.50", scalars[5].raw)
    }

    @Test
    fun aFlowSequenceIsReadWithItsItems() {
        val root = YamlDocument.parse(deployment).documents.single().asMapping()
        val args = assertIs<YamlSequence>(root.value("spec").asMapping().value("args"))

        assertTrue(args.flow)
        assertEquals(listOf("--queue", "conversions", "4"), args.items.map { (it as YamlScalar).value })
        assertEquals("""["--queue", 'conversions', 4]""", deployment.substring(args.range.start, args.range.end))
    }

    @Test
    fun aBlockCollectionEndsWhereItsLastValueDoes() {
        val text = "a:\n  b: 1\n  c: 2\n\n# after\nd: 3\n"
        val a = YamlDocument.parse(text).documents.single().asMapping().value("a")

        assertEquals("b: 1\n  c: 2", text.substring(a.range.start, a.range.end))
    }

    @Test
    fun aBlockScalarKeepsItsLines() {
        val text = "notes: |\n  first\n  second\nnext: 1\n"
        val notes = assertIs<YamlScalar>(YamlDocument.parse(text).documents.single().asMapping().value("notes"))

        assertEquals("first\nsecond\n", notes.value)
        assertEquals(YamlScalarStyle.Literal, notes.style)
        assertEquals("|\n  first\n  second", notes.raw)
    }

    @Test
    fun everyDocumentOfAStreamIsRead() {
        val documents = YamlDocument.parse("a: 1\n---\n- 2\n---\n").documents

        assertEquals(3, documents.size)
        assertIs<YamlMapping>(documents[0])
        assertIs<YamlSequence>(documents[1])
        // A document with nothing in it holds null, as YAML reads it.
        assertEquals(YamlScalarKind.Null, assertIs<YamlScalar>(documents[2]).kind)
        assertTrue(YamlDocument.parse("# only a comment\n").documents.isEmpty())
    }

    @Test
    fun aSyntaxErrorSaysWhereItIsAndKeepsWhatWasReadBefore() {
        val document = YamlDocument.parse("first: 1\nsecond: [1, 2\nthird: 3\n")

        assertFalse(document.isValid)
        assertEquals(listOf(ProblemCode.YamlSyntax), document.codes())
        assertEquals(3, document.problems.single().start.line)
        assertEquals("first", (document.documents.single().asMapping().entries.first().key as YamlScalar).value)
    }

    @Test
    fun aKeyGivenTwiceIsAnError() {
        val document = YamlDocument.parse("a: 1\nb: 2\na: 3\n")

        assertEquals(listOf(ProblemCode.DuplicateKey), document.codes())
        assertEquals(3, document.problems.single().start.line)
        assertFalse(document.isValid)
    }

    @Test
    fun anAliasWithNoAnchorBeforeItIsAnError() {
        val document = YamlDocument.parse("a: *missing\nb: &later 1\n")

        assertEquals(listOf(ProblemCode.UndefinedAlias), document.codes())
        assertNull(assertIs<YamlAlias>(document.documents.single().asMapping().value("a")).target)
    }

    @Test
    fun anAliasInsideItsAnchorPointsAtIt() {
        val root = YamlDocument.parse("node: &self\n  next: *self\n").documents.single().asMapping()
        val node = root.value("node").asMapping()

        assertSame(node, assertIs<YamlAlias>(node.value("next")).target)
    }

    @Test
    fun placesAreCountedInUtf16UnitsBeyondTheBasicPlane() {
        val text = "emoji: \"\uD83D\uDE00\"\nafter: yes\n"
        val after = YamlDocument.parse(text).documents.single().asMapping().value("after")

        assertEquals("yes", text.substring(after.range.start, after.range.end))
    }

    @Test
    fun aValueKeepsItsAnchorAndItsTagApartFromItself() {
        val text = "a: &x !!str 12\nb: !!map &y\n  c: 1\n"
        val root = YamlDocument.parse(text).documents.single().asMapping()
        val a = assertIs<YamlScalar>(root.value("a"))
        val b = root.value("b").asMapping()

        assertEquals("12", a.raw)
        assertEquals("x", a.anchor)
        assertEquals("!!str", a.tag)
        assertEquals("&x !!str 12", text.substring(a.start, a.range.end))
        assertEquals("y", b.anchor)
        assertEquals("!!map", b.tag)
        assertEquals("c: 1", text.substring(b.range.start, b.range.end))
    }

    @Test
    fun commentsAreFoundOutsideTheScalars() {
        val text = "# top\na: \"no # here\" # after a\nb: |\n  # kept in b\nc: 1 #c\nd: x#y\n"
        val comments = YamlDocument.parse(text).comments.map { text.substring(it.start, it.end) }

        assertEquals(listOf("# top", "# after a", "#c"), comments)
    }

    @Test
    fun aDocumentSaysHowItStarts() {
        val document = YamlDocument.parse("%YAML 1.2\n---\na: 1\n---\nb: 2\n")

        assertEquals(
            listOf(YamlDocumentStart(true, listOf("%YAML 1.2")), YamlDocumentStart(true, emptyList())),
            document.starts
        )
        assertEquals(listOf(YamlDocumentStart(false, emptyList())), YamlDocument.parse("a: 1\n").starts)
    }
}
