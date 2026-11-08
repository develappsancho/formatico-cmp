package com.developersancho.formatico.feature.workspace

import androidx.compose.ui.graphics.Color
import com.developersancho.formatico.core.designsystem.SyntaxColors
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.FormatDetector
import com.developersancho.formatico.core.format.types.TypeLanguage
import com.developersancho.formatico.core.format.types.generateTypes
import com.pantrixui.components.data.treeview.TreeNode
import com.pantrixui.foundation.l10n.PantrixLocale
import java.io.File
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.measureTimedValue

/**
 * How long each step of the workspace takes on a large document, and how much it keeps, on the JVM: Faz 14's measure.
 * It runs only when asked for, `./gradlew :feature:workspace:jvmTest --tests '*LargeDocumentsMeasure*'
 * -Pformatico.measure`, with the sizes in MB of `-Pformatico.measure.sizes=10,50`; the table is written to
 * `build/measure/large-documents.md`.
 */
class LargeDocumentsMeasure {
    private val asked = System.getProperty("formatico.measure") != null
    private val sizes = System.getProperty("formatico.measure.sizes")?.split(',')?.map { it.trim().toInt() } ?: listOf(10, 50)
    private val formats = System.getProperty("formatico.measure.formats")?.split(',')?.map { name ->
        Format.entries.first { it.name.equals(name.trim(), ignoreCase = true) }
    } ?: LargeSamples.formats
    private val inks = RowInks(SyntaxColors.Light, Color.Gray, Color.Yellow, Color.Cyan, Color.Black)
    private val rows = mutableListOf<String>()

    /**
     * Writes the samples of [formats] at [sizes] as files into the folder `-Pformatico.measure.samples` names, for the
     * measures of the app's whole screen: in the desktop's window (`AppLargeDocumentsMeasure`) and in a browser
     * (`WebLargeDocumentsMeasure`). Each is `<format>-<size>MB.<extension>`, the same bytes every time.
     */
    @Test
    fun samples() {
        val folder = System.getProperty("formatico.measure.samples")?.let(::File) ?: return
        folder.mkdirs()
        for (size in sizes) {
            for (format in formats) {
                val name = "${format.name.lowercase()}-${size}MB.${format.extensions.first()}"
                File(folder, name).writeText(sampleUnder(format, size * MB))
            }
        }
    }

    @Test
    fun largeDocuments() {
        if (!asked) return
        rows += "| Format | MB | Step | ms | Kept MB | Note |"
        rows += "| :--- | ---: | :--- | ---: | ---: | :--- |"
        File("build/measure").mkdirs()
        // Written after each document, so that what was measured is kept if a step goes on too long to wait for.
        for (size in sizes) {
            for (format in formats) {
                measure(format, size)
                File("build/measure/large-documents.md").writeText(rows.joinToString("\n", postfix = "\n"))
            }
        }
        println(rows.joinToString("\n"))
    }

    private fun measure(format: Format, size: Int) {
        val text = LargeSamples.of(format, size * MB)
        val document = OpenDocument(id = 1, name = "large.${format.name.lowercase()}", text = text, format = format)
        val base = used()
        step(format, size, "detect") { FormatDetector.detect(text) }
        step(format, size, "lines") { TextStats.of(text) }
        val reading = step(format, size, "read", keep = true, base = base) { readDocument(text, format) }
        val root = (reading as? Reading.Read)?.root
        if (root != null) {
            val afterReading = used()
            val opened = defaultExpanded(root)
            // The rows a screen shows, asked of the tree's source as the tree asks for them: made only as they are shown.
            step(format, size, "tree rows", keep = true, base = afterReading, note = { made: Int -> "$made rows made" }) {
                screenOf(DocumentSource(root, inks, null), opened)
            }
            // The branch that holds the most, opened with the ones it stands in: the rows a reader scrolls through.
            val largest = largestBranch(root)
            step(format, size, "tree rows, its largest branch open", note = { made: Int -> "$made rows made" }) {
                screenOfBranch(DocumentSource(root, inks, null), root, largest.key)
            }
            // A search in the tree, its matches looked for in every value, as each letter typed looks again.
            step(format, size, "tree search", note = { found: List<ValueNode> -> "${found.size} matches" }) {
                matchesOf(root, "grace", PantrixLocale.English)
            }
            step(format, size, "table") { tableOf(root, null) }
            step(format, size, "summary") { summaryOf(root) }
            if (format in SchemaFormats) measureSchema(format, size, document, reading, root)
        }
        // What the main thread works out each time the document's view changes: a branch opened, a value chosen.
        step(format, size, "view arranged") { WorkspaceState(document = document, reading = reading).arranged() }
        step(format, size, "format") { edited(document, DocumentEdit.Format) }
        step(format, size, "minify") { edited(document, DocumentEdit.Minify) }
        step(format, size, "query") { queryOutcome(document, reading, if (format == Format.Xml) "//order" else "$..id") }
        val to = if (format == Format.Json) Format.Yaml else Format.Json
        step(format, size, "convert to ${to.displayName}") { conversionOf(document, reading, ConvertChoice(to = to), nullComment = "") }
        // The other document is the same but for one value in its middle, as a change made to it would be.
        val middle = text.indexOf(CHANGED, text.length / 2)
        val other = document.copy(id = 2, text = text.replaceRange(middle, middle + CHANGED.length, CHANGED_TO))
        val otherReading = readDocument(other.text, format)
        step(format, size, "compare by meaning", note = ::differences) {
            comparisonOf(document, reading, other, otherReading, CompareBy.Meaning)
        }
        step(format, size, "compare as text", note = ::differences) {
            comparisonOf(document, reading, other, otherReading, CompareBy.Text)
        }
    }

    /**
     * The document validated against the JSON Schema Types writes of it (Faz 19): the schema written, read as one, and
     * the document validated against it, valid as it must be.
     */
    private fun measureSchema(format: Format, size: Int, document: OpenDocument, reading: Reading, root: ValueNode) {
        val text = step(format, size, "schema written", note = { written: String? -> "${written?.length ?: 0} characters" }) {
            generateTypes(dataValueOf(root, format), TypeLanguage.JsonSchema, "Large")?.text
        } ?: return
        val schema =
            schemaDocumentOf(
                OpenDocument(id = 3, name = "large.schema.json", text = text, format = Format.Json),
                readDocument(text, Format.Json)
            )
                ?: return
        step(format, size, "validated against its schema", note = { validation: SchemaValidation ->
            validation.check::class.simpleName.orEmpty()
        }) {
            schemaCheckOf(document, reading, SchemaChoice(with = schema.id), listOf(schema))
        }
    }

    /**
     * Times [work] and writes its row, with what [note] says of what it made; where [keep] says so, also how much more
     * of the heap is used while what it made is held than [base].
     */
    private fun <T> step(
        format: Format,
        size: Int,
        name: String,
        keep: Boolean = false,
        base: Long = 0,
        note: (T) -> String = { "" },
        work: () -> T
    ): T {
        val (value, took) = measureTimedValue { work() }
        val kept = if (keep) "%.0f".format((used() - base) / MB.toDouble()) else ""
        rows += "| ${format.displayName} | $size | $name | ${took.ms} | $kept | ${note(value)} |"
        return value
    }

    /**
     * Asks [source] for a screen of rows, as the tree does: the [SCREEN_ROWS] first rows of the [expanded] branches,
     * down from the top. It gives how many rows were made.
     */
    private fun screenOf(source: DocumentSource, expanded: Set<String>): Int {
        var made = 0

        fun walk(row: TreeNode) {
            made++
            if (row.key !in expanded) return
            val count = source.childCount(row)
            var index = 0
            while (index < count && made < SCREEN_ROWS) walk(source.childAt(row, index++))
        }
        walk(source.childAt(null, 0))
        return made
    }

    /**
     * Asks [source] for the rows of [branch], as the tree does where it is scrolled to it: the rows it stands in, found
     * by their keys, and the [SCREEN_ROWS] first of its own. It gives how many rows were made.
     */
    private fun screenOfBranch(source: DocumentSource, root: ValueNode, branch: String): Int {
        var row = source.childAt(null, 0)
        var made = 1
        trail(root, branch).drop(1).forEach { value ->
            row = source.childAt(row, source.indexOf(row, value.key))
            made++
        }
        repeat(minOf(source.childCount(row), SCREEN_ROWS)) { source.childAt(row, it).also { made++ } }
        return made
    }

    private fun differences(outcome: CompareOutcome): String =
        (outcome as? CompareOutcome.Compared)?.differences?.let { "${it.size} differences" } ?: outcome.toString()

    private fun largestBranch(root: ValueNode): ValueNode {
        var largest = root

        fun walk(node: ValueNode) {
            if (node.children.size > largest.children.size) largest = node
            node.children.forEach(::walk)
        }
        walk(root)
        return largest
    }

    /**
     * A sample of [format] as large as it can be within [limit] bytes, as a file the app takes at its limit: a sample
     * asked for a size ends its last record past it.
     */
    private fun sampleUnder(format: Format, limit: Int): String {
        var asked = limit
        var text = LargeSamples.of(format, asked)
        while (text.encodeToByteArray().size > limit) {
            asked -= text.encodeToByteArray().size - limit + SAMPLE_MARGIN
            text = LargeSamples.of(format, asked)
        }
        return text
    }

    private val Duration.ms: String get() = inWholeMilliseconds.toString()

    /** How much of the heap is used once what nothing holds is collected. */
    private fun used(): Long {
        repeat(GC_ROUNDS) {
            System.gc()
            Thread.sleep(GC_PAUSE)
        }
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    private companion object {
        // As Formatico counts a file's size, and its limits: 10 MB is 10,000,000 bytes.
        const val MB = 1_000_000
        const val GC_ROUNDS = 3
        const val GC_PAUSE = 50L

        // What a sample is asked for less than its limit, each time it ends past it.
        const val SAMPLE_MARGIN = 1_000

        // A name every sample writes, and another as long.
        const val CHANGED = "Grace Hopper"

        // The rows a tall window shows of a tree.
        const val SCREEN_ROWS = 60
        const val CHANGED_TO = "Grace Murray"
    }
}
