package com.developersancho.formatico.core.format.yaml

import com.developersancho.formatico.core.format.Problem
import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.Severity
import com.developersancho.formatico.core.format.text.LineMap
import com.developersancho.formatico.core.format.text.TextRange
import it.krzeminski.snakeyaml.engine.kmp.api.LoadSettings
import it.krzeminski.snakeyaml.engine.kmp.api.lowlevel.Parse
import it.krzeminski.snakeyaml.engine.kmp.common.FlowStyle
import it.krzeminski.snakeyaml.engine.kmp.common.ScalarStyle
import it.krzeminski.snakeyaml.engine.kmp.events.AliasEvent
import it.krzeminski.snakeyaml.engine.kmp.events.DocumentEndEvent
import it.krzeminski.snakeyaml.engine.kmp.events.DocumentStartEvent
import it.krzeminski.snakeyaml.engine.kmp.events.Event
import it.krzeminski.snakeyaml.engine.kmp.events.MappingEndEvent
import it.krzeminski.snakeyaml.engine.kmp.events.MappingStartEvent
import it.krzeminski.snakeyaml.engine.kmp.events.ScalarEvent
import it.krzeminski.snakeyaml.engine.kmp.events.SequenceEndEvent
import it.krzeminski.snakeyaml.engine.kmp.events.SequenceStartEvent
import it.krzeminski.snakeyaml.engine.kmp.exceptions.Mark
import it.krzeminski.snakeyaml.engine.kmp.exceptions.MarkedYamlEngineException
import it.krzeminski.snakeyaml.engine.kmp.exceptions.ReaderException
import it.krzeminski.snakeyaml.engine.kmp.exceptions.YamlEngineException
import it.krzeminski.snakeyaml.engine.kmp.nodes.Tag
import it.krzeminski.snakeyaml.engine.kmp.resolver.CoreScalarResolver

/**
 * A YAML text as it was read by snakeyaml-engine-kmp: its documents, each a value or nothing, its comments, and its
 * problems, each with its line and column. Every value keeps where it is written, its anchor and its tag, and a scalar
 * its spelling.
 *
 * The reader of YAML stops at its first syntax error: what was read before it is kept, and the error is the last
 * problem. A key given twice and an alias with no anchor before it are found as well.
 *
 * @property text the text read.
 * @property documents the documents of the stream, in order: a document with no value is null.
 * @property starts how each document of [documents] starts.
 * @property comments where each comment is, from its `#` to the end of its line, in order.
 * @property problems what is wrong in it, in the order of the text.
 */
class YamlDocument private constructor(
    val text: String,
    val documents: List<YamlNode?>,
    val starts: List<YamlDocumentStart>,
    val comments: List<TextRange>,
    val problems: List<Problem>
) {
    /** Whether the document has no error. */
    val isValid: Boolean get() = problems.none { it.severity == Severity.Error }

    companion object {
        /** Reads [text] as a stream of YAML documents. */
        fun parse(text: String): YamlDocument {
            val reader = YamlReader(text)
            reader.read()
            val lines = LineMap(text)
            val problems = reader.found.sortedBy { it.start }.map {
                Problem(
                    code = it.code,
                    severity = Severity.Error,
                    range = TextRange(it.start, it.end),
                    start = lines.position(it.start),
                    end = lines.position(it.end)
                )
            }
            return YamlDocument(text, reader.documents, reader.starts, commentsOf(text, reader.scalars), problems)
        }
    }
}

/** A problem found while reading, by its offsets in UTF-16 units. */
private class FoundProblem(val code: ProblemCode, val start: Int, val end: Int)

/** Builds the documents of [text] from snakeyaml-engine-kmp's events, a collection at a time. */
private class YamlReader(private val text: String) {
    val documents = ArrayList<YamlNode?>()
    val starts = ArrayList<YamlDocumentStart>()
    val found = ArrayList<FoundProblem>()

    /** Where every scalar is, keys among them: no comment starts inside one. */
    val scalars = ArrayList<TextRange>()

    private val offsets = CodePointOffsets(text)
    private val resolver = CoreScalarResolver(supportMerge = true)
    private val anchors = HashMap<String, AnchorSlot>()
    private val open = ArrayList<OpenCollection>()
    private var root: YamlNode? = null
    private var inDocument = false
    private var lastDocumentEnd = 0

    /** A mapping or a sequence whose end is still to come. */
    private class OpenCollection(
        val start: Int,
        val properties: Properties,
        val slot: AnchorSlot?,
        val mapping: Boolean,
        val flow: Boolean
    ) {
        val items = ArrayList<YamlNode>()
    }

    fun read() {
        try {
            Parse(Settings).parse(text).forEach(::accept)
        } catch (error: MarkedYamlEngineException) {
            val mark = error.problemMark ?: error.contextMark
            report(ProblemCode.YamlSyntax, mark?.let { offsets.of(it) } ?: text.length)
            closeAll()
        } catch (error: ReaderException) {
            report(ProblemCode.YamlSyntax, offsets.of(error.position))
            closeAll()
        } catch (@Suppress("SwallowedException") error: YamlEngineException) {
            // An error with no place in the text: it is said where the text ends.
            report(ProblemCode.YamlSyntax, text.length)
            closeAll()
        }
    }

    private fun accept(event: Event) {
        when (event) {
            is DocumentStartEvent -> {
                root = null
                inDocument = true
                val start = event.startMark?.let(offsets::of) ?: lastDocumentEnd
                val directives = text.substring(lastDocumentEnd, maxOf(lastDocumentEnd, start))
                    .lines()
                    .filter { it.startsWith("%") }
                    .map { it.trimEnd() }
                starts += YamlDocumentStart(explicit = event.explicit, directives = directives)
            }

            is DocumentEndEvent -> {
                documents += root
                inDocument = false
                if (event.isExplicit &&
                    starts.isNotEmpty()
                ) {
                    starts[starts.lastIndex] = starts.last().copy(explicitEnd = true)
                }
                lastDocumentEnd = event.endMark?.let(offsets::of) ?: lastDocumentEnd
            }

            is ScalarEvent -> {
                add(scalar(event))
            }

            is AliasEvent -> {
                add(alias(event))
            }

            is MappingStartEvent -> {
                open(event.startMark, mapping = true, flow = event.flowStyle == FlowStyle.FLOW)
            }

            is SequenceStartEvent -> {
                open(event.startMark, mapping = false, flow = event.flowStyle == FlowStyle.FLOW)
            }

            is MappingEndEvent, is SequenceEndEvent -> {
                close(end = event.endMark?.let(offsets::of))
            }

            else -> {}
        }
    }

    private fun scalar(event: ScalarEvent): YamlScalar {
        val start = event.startMark?.let(offsets::of) ?: 0
        val end = event.endMark?.let(offsets::of) ?: start
        val properties = text.propertiesAt(start)
        val contentStart = minOf(properties.contentStart, end)
        val range = TextRange(contentStart, maxOf(contentStart, text.trimmedEnd(contentStart, end)))
        scalars += range
        val style = when (event.scalarStyle) {
            ScalarStyle.SINGLE_QUOTED -> YamlScalarStyle.SingleQuoted
            ScalarStyle.DOUBLE_QUOTED, ScalarStyle.JSON_SCALAR_STYLE -> YamlScalarStyle.DoubleQuoted
            ScalarStyle.LITERAL -> YamlScalarStyle.Literal
            ScalarStyle.FOLDED -> YamlScalarStyle.Folded
            ScalarStyle.PLAIN -> YamlScalarStyle.Plain
        }
        val tag = event.tag?.takeIf { it != NON_SPECIFIC }?.let(::Tag)
            ?: resolver.resolve(event.value, implicit = event.implicit.canOmitTagInPlainScalar())
        val scalar = YamlScalar(
            properties = YamlProperties(start, event.anchor?.value, properties.tag),
            range = range,
            raw = text.substring(range.start, range.end),
            value = event.value,
            kind = tag.kind,
            style = style
        )
        event.anchor?.value?.let { anchors[it] = AnchorSlot().apply { node = scalar } }
        return scalar
    }

    private fun alias(event: AliasEvent): YamlAlias {
        val from = event.startMark?.let(offsets::of) ?: 0
        val to = event.endMark?.let(offsets::of) ?: from
        val range = TextRange(from, maxOf(from, text.trimmedEnd(from, to)))
        val name = event.alias.value
        val slot = anchors[name]
        if (slot == null) found += FoundProblem(ProblemCode.UndefinedAlias, range.start, range.end)
        return YamlAlias(range, name, slot)
    }

    private fun open(start: Mark?, mapping: Boolean, flow: Boolean) {
        val from = start?.let(offsets::of) ?: 0
        val properties = text.propertiesAt(from)
        val slot = properties.anchor?.let { AnchorSlot().also { slot -> anchors[it] = slot } }
        open += OpenCollection(from, properties, slot, mapping, flow)
    }

    /** Ends the innermost open collection; a block one ends where its last value does. */
    private fun close(end: Int?) {
        val collection = open.removeAt(open.lastIndex)
        val items = collection.items
        val contentStart = collection.properties.contentStart
        val lastEnd = items.lastOrNull()?.range?.end ?: contentStart
        val until = text.trimmedEnd(contentStart, if (collection.flow && end != null) end else lastEnd)
        val range = TextRange(contentStart, maxOf(contentStart, until))
        val properties = YamlProperties(collection.start, collection.properties.anchor, collection.properties.tag)
        val node = if (collection.mapping) {
            val entries = items.chunked(2).filter { it.size == 2 }.map { YamlEntry(it[0], it[1]) }
            duplicateKeys(entries).forEach {
                found +=
                    FoundProblem(ProblemCode.DuplicateKey, it.range.start, it.range.end)
            }
            YamlMapping(properties, range, entries, collection.flow)
        } else {
            YamlSequence(properties, range, items.toList(), collection.flow)
        }
        collection.slot?.node = node
        add(node)
    }

    /** Ends every collection still open after an error, with what was read of each, and the document they are in. */
    private fun closeAll() {
        while (open.isNotEmpty()) close(end = null)
        if (inDocument) documents += root
    }

    private fun add(node: YamlNode) {
        val parent = open.lastOrNull()
        if (parent == null) root = node else parent.items += node
    }

    private fun report(code: ProblemCode, at: Int) {
        val start = at.coerceIn(0, text.length)
        found += FoundProblem(code, start, minOf(text.length, start + 1))
    }

    companion object {
        /** The tag `!`, which asks for no type: the scalar is a string. */
        private const val NON_SPECIFIC = "!"

        /** Formatico reads documents larger than snakeyaml's own limit of 3 MB, and shows aliases as they are. */
        private val Settings = LoadSettings(codePointLimit = Int.MAX_VALUE)
    }
}

/**
 * Where each comment of [text] is: a `#` at the start of a line or after a space, outside every scalar of [scalars], to
 * the end of its line.
 */
private fun commentsOf(text: String, scalars: List<TextRange>): List<TextRange> {
    val inside = scalars.sortedBy { it.start }
    val comments = ArrayList<TextRange>()
    var next = 0
    var index = 0
    while (index < text.length) {
        while (next < inside.size && inside[next].end <= index) next++
        val scalar = inside.getOrNull(next)?.takeIf { it.start <= index && index < it.end }
        when {
            scalar != null -> {
                index = scalar.end
            }

            text[index] == '#' && (index == 0 || text[index - 1].isWhitespace()) -> {
                var end = index
                while (end < text.length && text[end] != '\n' && text[end] != '\r') end++
                comments += TextRange(index, end)
                index = end
            }

            else -> {
                index++
            }
        }
    }
    return comments
}

/** A value's anchor and tag as they are written, and where the value itself starts after them. */
private class Properties(val anchor: String?, val tag: String?, val contentStart: Int)

/** What ends an anchor or a tag in a flow collection, where no space need follow it. */
private const val FLOW_ENDS = ",]}"

/**
 * The anchor and the tag written at [from], `&labels !!map`, and where the value starts after them and after the
 * spaces, line breaks and comments that follow them.
 */
private fun String.propertiesAt(from: Int): Properties {
    var index = from
    var anchor: String? = null
    var tag: String? = null
    while (index < length && (this[index] == '&' || this[index] == '!')) {
        val start = index
        while (index < length && !this[index].isWhitespace() && this[index] !in FLOW_ENDS) index++
        if (this[start] == '&') anchor = substring(start + 1, index) else tag = substring(start, index)
        index = skipSpace(index)
    }
    return Properties(anchor, tag, if (anchor == null && tag == null) from else index)
}

/** [from] moved past spaces, line breaks and comments. */
private fun String.skipSpace(from: Int): Int {
    var index = from
    while (index < length) {
        when {
            this[index].isWhitespace() -> index++
            this[index] == '#' && this[index - 1].isWhitespace() -> index = lineEnd(index)
            else -> return index
        }
    }
    return index
}

private fun String.lineEnd(from: Int): Int {
    var index = from
    while (index < length && this[index] != '\n' && this[index] != '\r') index++
    return index
}

/** [end] moved back over the spaces and line breaks before it, not past [start]. */
private fun String.trimmedEnd(start: Int, end: Int): Int {
    var until = end.coerceAtMost(length)
    while (until > start && this[until - 1].isWhitespace()) until--
    return until
}

/** What the core schema's [Tag] reads a scalar as. */
private val Tag.kind: YamlScalarKind
    get() = when (this) {
        Tag.NULL -> YamlScalarKind.Null
        Tag.BOOL -> YamlScalarKind.Boolean
        Tag.INT -> YamlScalarKind.Integer
        Tag.FLOAT -> YamlScalarKind.Float
        else -> YamlScalarKind.String
    }

/** The scalar keys of [entries] given a second time or more, which YAML does not allow. */
private fun duplicateKeys(entries: List<YamlEntry>): List<YamlScalar> {
    val seen = HashSet<Pair<YamlScalarKind, String>>()
    return entries.mapNotNull { it.key as? YamlScalar }.filterNot { seen.add(it.kind to it.value) }
}

/**
 * Turns snakeyaml's places, which count code points, into offsets in UTF-16 units, as Kotlin's strings count. A text
 * with no character beyond the Basic Multilingual Plane counts the same both ways.
 */
private class CodePointOffsets(text: String) {
    private val length = text.length
    private val utf16: IntArray? = if (text.none { it.isHighSurrogate() }) {
        null
    } else {
        val offsets = ArrayList<Int>(text.length + 1)
        var index = 0
        while (index < text.length) {
            offsets += index
            val pair = text[index].isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()
            index += if (pair) 2 else 1
        }
        offsets += text.length
        offsets.toIntArray()
    }

    /** The offset of [mark]. */
    fun of(mark: Mark): Int = of(mark.index)

    fun of(codePoint: Int): Int =
        utf16?.let { it[codePoint.coerceIn(0, it.lastIndex)] } ?: codePoint.coerceIn(0, length)
}
