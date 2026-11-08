package com.developersancho.formatico.core.format.yaml

import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.text.Indentation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class YamlPrinterTest {
    private val deployment =
        """
        # A service
        apiVersion: apps/v1
        metadata:
            name: formatico-web   # its name
            labels: &labels
                app: formatico

        spec:
            replicas: 3
            selector:
                matchLabels: *labels
            containers:
            -   name: web
                args: ["--queue", 'conversions', 4]
                env: &env
                -   name: LOG_LEVEL
                    value: "50"
            -   name: worker
                env: *env
        """.trimIndent() + "\n"

    /** The values of [text] in a form that says nothing of how they are written: what two equal documents share. */
    private fun valuesOf(text: String): String {
        val document = YamlDocument.parse(text)
        assertTrue(document.isValid, "Not valid: <$text> ${document.problems}")
        return document.documents.joinToString(" --- ") { it?.let(::canonical) ?: "~" }
    }

    private fun canonical(node: YamlNode): String {
        val properties = listOfNotNull(node.anchor?.let { "&$it" }, node.tag).joinToString("") { "$it " }
        return properties + when (node) {
            is YamlMapping -> node.entries.joinToString(", ", "{", "}") {
                canonical(it.key) + ": " + canonical(it.value)
            }

            is YamlSequence -> node.items.joinToString(", ", "[", "]") { canonical(it) }

            // `a:`, `a: ~` and `a: null` hold the same null.
            is YamlScalar -> if (node.kind == YamlScalarKind.Null) "Null" else "${node.kind}(${node.value})"

            is YamlAlias -> "*${node.name}"
        }
    }

    private fun format(text: String, sortKeys: Boolean = false): String =
        YamlDocument.parse(text).formatted(Indentation.Spaces(2), sortKeys)

    @Test
    fun aDocumentIsIndentedAgainAndKeepsWhatIsWrittenInIt() {
        val expected =
            """
            # A service
            apiVersion: apps/v1
            metadata:
              name: formatico-web # its name
              labels: &labels
                app: formatico

            spec:
              replicas: 3
              selector:
                matchLabels: *labels
              containers:
                - name: web
                  args: ["--queue", 'conversions', 4]
                  env: &env
                    - name: LOG_LEVEL
                      value: "50"
                - name: worker
                  env: *env
            """.trimIndent() + "\n"

        assertEquals(expected, format(deployment))
        assertEquals(valuesOf(deployment), valuesOf(expected))
    }

    @Test
    fun formattingWhatIsFormattedChangesNothing() {
        val once = format(deployment)
        assertEquals(once, format(once))
    }

    @Test
    fun aWiderIndentationLinesTheItemsUpUnderTheirDash() {
        val formatted = YamlDocument.parse("a:\n- b: 1\n  c: 2\n").formatted(Indentation.Spaces(4))

        assertEquals("a:\n    -   b: 1\n        c: 2\n", formatted)
        assertEquals(valuesOf("a:\n- b: 1\n  c: 2\n"), valuesOf(formatted))
    }

    @Test
    fun aBlockScalarMovesWithItsKeyAndKeepsItsLines() {
        val text = "notes:\n      script: |\n          echo one\n            indented\n\n          echo two\n" +
            "      after: 1\n"
        val formatted = format(text)

        assertEquals("notes:\n  script: |\n    echo one\n      indented\n\n    echo two\n  after: 1\n", formatted)
        assertEquals(valuesOf(text), valuesOf(formatted))
    }

    @Test
    fun aKeptBlockScalarKeepsItsLastBlankLines() {
        val text = "a: |+\n  line\n\n\nb: 1\n"
        assertEquals(valuesOf(text), valuesOf(format(text)))
    }

    @Test
    fun aScalarOverSeveralLinesIsWrittenOnOne() {
        val text = "a: one\n  two\nb: \"x\n  y\"\n"
        val formatted = format(text)

        assertEquals("a: one two\nb: \"x y\"\n", formatted)
        assertEquals(valuesOf(text), valuesOf(formatted))
    }

    @Test
    fun anchorsTagsAndEmptyValuesAreKept() {
        val text = "a: &x !!str 12\nb: !!map\n  c:\nd: []\ne: {}\nf: &y\n- *x\n"
        val formatted = format(text)

        assertEquals("a: &x !!str 12\nb: !!map\n  c:\nd: []\ne: {}\nf: &y\n  - *x\n", formatted)
        assertEquals(valuesOf(text), valuesOf(formatted))
    }

    @Test
    fun everyDocumentAndItsMarkersAreKept() {
        val text = "%YAML 1.2\n---\na: 1\n---\n- 2\n...\n"

        assertEquals("%YAML 1.2\n---\na: 1\n---\n- 2\n...\n", format(text))
    }

    @Test
    fun sortingTheKeysTakesEachEntrysCommentsAlong() {
        val text =
            """
            # zebra's
            zebra: 1 # stripes
            apple:
              y: 2
              x: 1
            """.trimIndent() + "\n"
        val expected =
            """
            apple:
              x: 1
              y: 2
            # zebra's
            zebra: 1 # stripes
            """.trimIndent() + "\n"

        assertEquals(expected, format(text, sortKeys = true))
    }

    @Test
    fun sortingCanPutAnAliasBeforeItsAnchor() {
        val sorted = format("b: &x 1\na: *x\n", sortKeys = true)

        assertTrue(YamlDocument.parse(sorted).problems.any { it.code == ProblemCode.UndefinedAlias })
    }

    @Test
    fun minifyingWritesEachDocumentOnOneLineWithoutComments() {
        val minified = YamlDocument.parse(deployment).minified()

        assertEquals(1, minified.trimEnd().lines().size)
        assertTrue("#" !in minified.replace("\"", ""))
        assertEquals(valuesOf(deployment), valuesOf(minified))
    }

    @Test
    fun minifyingQuotesWhatCannotStandInAFlowCollection() {
        val text = "a: one, two\nb: |\n  line\nc: plain\nd:\ne: http://x\n"
        val minified = YamlDocument.parse(text).minified()

        assertEquals("{a: \"one, two\",b: \"line\\n\",c: plain,d: null,e: \"http://x\"}\n", minified)
        assertEquals(valuesOf(text), valuesOf(minified))
    }

    @Test
    fun aStreamIsMinifiedADocumentToALine() {
        assertEquals("a: 1\n---\n[2]\n".let { YamlDocument.parse(it).minified() }, "{a: 1}\n---\n[2]\n")
    }

    @Test
    fun aDocumentThatIsNotValidIsNotWrittenOut() {
        val document = YamlDocument.parse("a: [1\n")
        assertFailsWith<IllegalArgumentException> { document.formatted() }
        assertFailsWith<IllegalArgumentException> { document.minified() }
    }

    @Test
    fun whatIsWrittenOutReadsAsTheSameValuesAndFormatsToItself() {
        val corpus = listOf(
            "- - a\n  - b\n- - c\n",
            "- a:\n  - 1\n  - 2\n  b: x\n",
            "a: 1 # one\n# before b\nb:\n  # inside\n  c: 2 # two\n# end\n",
            "a: >-\n  folded\n  text\nb: 1\n",
            "'a b': 1\n\"c\": [1, {d: 2}]\n? complex\n: value\n",
            "a: [1,\n  2,\n  3]\nb: {x: 1,\n  y: 2}\n",
            "a: [1, # one\n  2]\nb: 3\n",
            "- &a\n  k: v\n- *a\n",
            "hello\n",
            "|\n  text\n",
            "a: 1\r\nb:\r\n  c: 2\r\n",
            "ş: 'ç'\nemoji: \uD83D\uDE00\n",
            "a: # note\nb: 1\n",
            "a:\n  - b: 1\n    c:\n      - d\n      - e: |\n          deep\n",
            deployment
        )
        corpus.forEach { text ->
            val formatted = format(text)
            assertEquals(valuesOf(text), valuesOf(formatted), "Format of <$text> gave <$formatted>")
            assertEquals(formatted, format(formatted), "Format of <$formatted> is not itself")
            val minified = YamlDocument.parse(text).minified()
            assertEquals(valuesOf(text), valuesOf(minified), "Minify of <$text> gave <$minified>")
        }
    }

    @Test
    fun theCommentsAtTheHeadOfADocumentStayThereWhenTheKeysAreSorted() {
        val text = "# Settings of the web service\n\n# how many\nzebra: 1\napple: 2\n"

        val sorted = YamlDocument.parse(text).formatted(sortKeys = true)

        assertEquals("# Settings of the web service\n\napple: 2\n# how many\nzebra: 1\n", sorted)
    }

    @Test
    fun aCommentAfterADashStaysOnTheDashsLine() {
        val text = "items:\n  - # the first\n    a: 1 # one\n    b: 2\n  - c: 3\n"

        val formatted = YamlDocument.parse(text).formatted()

        assertEquals("items:\n  - # the first\n    a: 1 # one\n    b: 2\n  - c: 3\n", formatted)
        assertEquals(valuesOf(text), valuesOf(formatted))
    }

    @Test
    fun aDocumentThatEndsWithItsMarkerKeepsIt() {
        val text = "---\na: 1\n...\n---\nb: 2\n"

        val formatted = YamlDocument.parse(text).formatted()

        assertEquals("---\na: 1\n...\n---\nb: 2\n", formatted)
        assertEquals(listOf(true, false), YamlDocument.parse(text).starts.map { it.explicitEnd })
    }
}
