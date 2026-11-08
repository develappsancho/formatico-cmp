package com.developersancho.formatico.core.format.toml

import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.text.TextRange

/** How a table came to be, which says what may be done with it later. */
private enum class Origin(val kind: TomlTableKind) {
    /** The document's own table. */
    Root(TomlTableKind.Root),

    /** A header, `[name]`: nothing defines it again. */
    Header(TomlTableKind.Header),

    /** A header for a table under it, `[a.b]` for `a`: a header of its own may define it later, once. */
    HeaderPath(TomlTableKind.Implicit),

    /** A dotted key, `a.b = 1` for `a`: only dotted keys add to it, and headers define tables under it. */
    Dotted(TomlTableKind.Implicit),

    /** A header of an array of tables, `[[name]]`. */
    ArrayItem(TomlTableKind.ArrayItem),

    /** An inline table: nothing adds to it once it is written. */
    Inline(TomlTableKind.Inline)
}

/** A value of the document while it is built: a table and an array of tables still grow as headers come. */
private sealed class Node {
    /** Whether it is a table, or an array of them: what a key holding it cannot be given again. */
    abstract val isTable: Boolean

    /** The value it has become, once the whole document is read. */
    abstract fun freeze(): TomlValue
}

/**
 * A table being built.
 *
 * @property start where the text that went into it starts, or [Int.MAX_VALUE] while none has.
 * @property end where that text ends, or -1 while none has.
 */
private class TableNode(var origin: Origin) : Node() {
    val entries = LinkedHashMap<String, EntryNode>()
    var start = Int.MAX_VALUE
    var end = -1

    override val isTable: Boolean get() = true

    /** Adds [node] under [name], made a table by a header or a dotted key, and gives it back. */
    fun <T : Node> put(name: String, keyRange: TextRange?, node: T): T {
        entries[name] = EntryNode(name, keyRange, node, assigned = false)
        return node
    }

    /** Makes the table run over [range] too. */
    fun cover(range: TextRange) {
        start = minOf(start, range.start)
        end = maxOf(end, range.end)
    }

    override fun freeze(): TomlTable {
        val frozen = entries.values.map { TomlEntry(it.key, it.keyRange, it.node.freeze()) }
        val kind = origin.kind
        // An implicit table runs over everything that went into it, what is under it included.
        if (kind == TomlTableKind.Implicit) frozen.forEach { cover(it.value.range) }
        return TomlTable(if (end < 0) TextRange(0, 0) else TextRange(start, end), frozen, kind)
    }
}

/** An array of tables being built, a `[[name]]` header at a time. */
private class ArrayNode : Node() {
    val items = ArrayList<TableNode>()

    override val isTable: Boolean get() = true

    override fun freeze(): TomlArray {
        val frozen = items.map { it.freeze() }
        return TomlArray(TextRange(frozen.first().range.start, frozen.last().range.end), frozen, ofTables = true)
    }
}

/** A value given by a key/value: nothing is added to it. */
private class ValueNode(val value: TomlValue) : Node() {
    override val isTable: Boolean get() = value is TomlTable

    override fun freeze(): TomlValue = value
}

/**
 * A key of a table being built.
 *
 * @property assigned whether a key/value gave it its value, rather than a header or a dotted key making it a table.
 */
private class EntryNode(val key: String, var keyRange: TextRange?, val node: Node, val assigned: Boolean)

/**
 * Builds a document's tables from its statements, as TOML 1.0 defines them, and reports a key given twice and a table
 * defined twice.
 *
 * A header whose table cannot be defined has its key/values read into a table of their own, which is not part of the
 * document: they are checked, and not reported again as given twice.
 */
internal class TomlTreeBuilder(private val length: Int, private val report: (code: ProblemCode, range: TextRange) -> Unit) {
    private val root = TableNode(Origin.Root)
    private var section = root

    /** The document's root table, built from [statements] in their order. */
    fun build(statements: List<TomlStatement>): TomlTable {
        statements.forEach { statement ->
            when (statement) {
                is TomlHeaderStatement -> section = header(statement) ?: TableNode(Origin.Header)
                is TomlKeyValueStatement -> keyValue(statement.pair)
            }
        }
        root.cover(TextRange(0, length))
        return root.freeze()
    }

    /** Gives [pair]'s key its value in the table of the header above it, or in the root. */
    private fun keyValue(pair: TomlKeyValue) {
        assign(section, pair)
        // A header's table runs to the end of its last key/value.
        if (section.origin == Origin.Header || section.origin == Origin.ArrayItem) {
            section.end = maxOf(section.end, pair.range.end)
        }
    }

    /** Gives [pair]'s key its value in [table], through the tables its dotted key makes. */
    private fun assign(table: TableNode, pair: TomlKeyValue) {
        // The value is built first, so that what is wrong inside it is found even where its key cannot have it.
        val value = valueOf(pair.value)
        val parts = pair.key.parts
        var target = table
        for (index in 0 until parts.lastIndex) {
            target = dotted(target, parts[index], pair.range) ?: return
        }
        val last = parts.last()
        val existing = target.entries[last.name]
        if (existing == null) {
            target.entries[last.name] = EntryNode(last.name, last.range, ValueNode(value), assigned = true)
        } else {
            report(if (existing.assigned) ProblemCode.DuplicateKey else ProblemCode.TomlTableRedefined, last.range)
        }
    }

    /** The table [part] of a dotted key names in [table], made where there is none, running over [range] too. */
    private fun dotted(table: TableNode, part: TomlKeyPart, range: TextRange): TableNode? {
        val existing = table.entries[part.name]
        val node = existing?.node
        val child = when {
            existing == null -> table.put(part.name, part.range, TableNode(Origin.Dotted))
            node is TableNode && node.origin == Origin.Dotted -> node
            else -> return clash(existing, part)
        }
        child.cover(range)
        return child
    }

    /** The table a header's statement defines, or null where it cannot be defined, which is reported. */
    private fun header(statement: TomlHeaderStatement): TableNode? {
        val parts = statement.key?.parts ?: return null
        var table = root
        for (index in 0 until parts.lastIndex) {
            table = above(table, parts[index]) ?: return null
        }
        val last = parts.last()
        return if (statement.ofArray) arrayItem(table, last, statement.range) else defined(table, last, statement.range)
    }

    /** The table [part] of a header names in [table], on the way to the header's own: the last item of an array of tables. */
    private fun above(table: TableNode, part: TomlKeyPart): TableNode? {
        val existing = table.entries[part.name] ?: return table.put(part.name, keyRange = null, TableNode(Origin.HeaderPath))
        return when (val node = existing.node) {
            is TableNode -> node
            is ArrayNode -> node.items.last()
            is ValueNode -> clash(existing, part)
        }
    }

    /** The table `[name]` defines, [part] in [table], from [range]: a header's or a key's above another header's. */
    private fun defined(table: TableNode, part: TomlKeyPart, range: TextRange): TableNode? {
        val existing = table.entries[part.name]
        val node = existing?.node
        val defined = when {
            existing == null -> table.put(part.name, part.range, TableNode(Origin.Header))

            node is TableNode && node.origin == Origin.HeaderPath -> {
                existing.keyRange = part.range
                node.also { it.origin = Origin.Header }
            }

            else -> return clash(existing, part)
        }
        defined.start = range.start
        defined.end = range.end
        return defined
    }

    /** A new item of the array of tables `[[name]]` adds to, [part] in [table], from [range]. */
    private fun arrayItem(table: TableNode, part: TomlKeyPart, range: TextRange): TableNode? {
        val existing = table.entries[part.name]
        val node = existing?.node
        val array = when {
            existing == null -> table.put(part.name, part.range, ArrayNode())
            node is ArrayNode -> node
            else -> return clash(existing, part)
        }
        return TableNode(Origin.ArrayItem).also {
            it.cover(range)
            array.items += it
        }
    }

    /** Reports that [part] names what is there already, [existing]: a table defined again, or a key given twice. */
    private fun clash(existing: EntryNode, part: TomlKeyPart): TableNode? {
        report(if (existing.node.isTable) ProblemCode.TomlTableRedefined else ProblemCode.DuplicateKey, part.range)
        return null
    }

    /** The value [syntax] is written as; an inline table's keys are checked as a table's are. */
    private fun valueOf(syntax: TomlValueSyntax): TomlValue = when (syntax) {
        is TomlScalarSyntax -> {
            syntax.value
        }

        is TomlArraySyntax -> {
            TomlArray(syntax.range, syntax.items.map(::valueOf), ofTables = false)
        }

        is TomlInlineTableSyntax -> {
            val table = TableNode(Origin.Inline).also { it.cover(syntax.range) }
            syntax.pairs.forEach { assign(table, it) }
            table.freeze()
        }
    }
}
