package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Immutable
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.convert.ConvertOptions
import com.developersancho.formatico.core.format.convert.DataArray
import com.developersancho.formatico.core.format.convert.DataBoolean
import com.developersancho.formatico.core.format.convert.DataDate
import com.developersancho.formatico.core.format.convert.DataMember
import com.developersancho.formatico.core.format.convert.DataNull
import com.developersancho.formatico.core.format.convert.DataNumber
import com.developersancho.formatico.core.format.convert.DataObject
import com.developersancho.formatico.core.format.convert.DataString
import com.developersancho.formatico.core.format.convert.DataValue
import com.developersancho.formatico.core.format.convert.Loss
import com.developersancho.formatico.core.format.convert.Note
import com.developersancho.formatico.core.format.convert.NullHandling
import com.developersancho.formatico.core.format.convert.convert
import com.developersancho.formatico.core.format.json.PathStep
import com.developersancho.formatico.core.format.text.Indentation

/**
 * How a document is converted, as its view keeps it.
 *
 * @property to the format it is converted to, or null for the one it is converted to first.
 * @property indent how many spaces a level of a format written in levels is indented by: 2 or 4.
 * @property sortKeys whether every object's members are written in the order of their names.
 * @property nulls what a null becomes in a format that has none.
 */
data class ConvertChoice(
    val to: Format? = null,
    val indent: Int = 2,
    val sortKeys: Boolean = false,
    val nulls: NullHandling = NullHandling.LeaveOut
)

/** The format a document of this format is converted to first: YAML for JSON and its kin, JSON for the rest. */
internal val Format.firstTarget: Format
    get() = when (this) {
        Format.Json, Format.Jsonc, Format.Json5, Format.Ndjson -> Format.Yaml
        else -> Format.Json
    }

/** The formats a document of this format converts to: every other, in the order the format menu lists them. */
internal val Format.targets: List<Format> get() = Format.entries - this

/** Whether a document of this format is written in levels, each indented: JSON and its kin but NDJSON, YAML, XML. */
internal val Format.indents: Boolean
    get() = this == Format.Json || this == Format.Jsonc || this == Format.Json5 || this == Format.Yaml || this == Format.Xml

/** Whether this format, which has no null, offers a choice of what one becomes: CSV's is always an empty field. */
internal val Format.choosesNulls: Boolean
    get() = this == Format.Toml || this == Format.Xml || this == Format.Properties || this == Format.Ini

/** The format a document of [from] is converted to by [choice]: its own choice, or the first, but never [from]. */
internal fun targetOf(from: Format, choice: ConvertChoice): Format = choice.to?.takeIf { it != from } ?: from.firstTarget

/** What converting a document came to. */
internal sealed interface ConvertOutcome {
    /** It could not be read: it is plain text, or nothing of it could be read. */
    data object Unread : ConvertOutcome

    /** It has [errors]: an invalid document is repaired before it is converted. */
    data class Invalid(val format: Format, val errors: Int) : ConvertOutcome

    /**
     * It was converted from [from] to [to]: the new document, [name] and [text], [bytes] long in UTF-8; what it does
     * not carry as it is, [losses], the old document's comments and aliases first; what was done to carry the rest; and
     * the lines, from nought, of the comments that mark where a null was left out. Its name is null where the old
     * document has none.
     */
    @Immutable
    data class Converted(
        val from: Format,
        val to: Format,
        val name: String?,
        val text: String,
        val bytes: Long,
        val losses: List<Loss>,
        val notes: List<Note>,
        val nullLines: List<Int> = emptyList()
    ) : ConvertOutcome
}

/** The converted document, where [outcome] is one. */
internal val ConvertOutcome?.converted: ConvertOutcome.Converted? get() = this as? ConvertOutcome.Converted

/**
 * [document], read as [reading], converted as [choice] says; a null left out is marked by [nullComment], `{name}`
 * standing for its name, in the new document.
 */
internal fun conversionOf(document: OpenDocument, reading: Reading, choice: ConvertChoice, nullComment: String): ConvertOutcome {
    val read = reading as? Reading.Read ?: return ConvertOutcome.Unread
    val root = read.root ?: return ConvertOutcome.Unread
    if (read.errors > 0) return ConvertOutcome.Invalid(read.format, read.errors)
    val to = targetOf(read.format, choice)
    val data = DocumentData(root, read.format)
    val options = ConvertOptions(
        indentation = Indentation.Spaces(choice.indent.coerceIn(MIN_INDENT, MAX_INDENT)),
        sortKeys = choice.sortKeys,
        nulls = choice.nulls,
        nullComment = nullComment
    )
    val conversion = convert(data.value, to, options)
    val losses = buildList {
        if (read.comments > 0) add(Loss.Comments(read.comments))
        data.aliases()?.let(::add)
        addAll(conversion.losses)
    }
    return ConvertOutcome.Converted(
        from = read.format,
        to = to,
        name = convertedName(document.name, to),
        text = conversion.text,
        bytes = conversion.text.encodeToByteArray().size.toLong(),
        losses = losses,
        notes = conversion.notes,
        nullLines = conversion.nullLines
    )
}

/** The name of a document named [name] converted to [format]: its stem and the format's extension; null for none. */
internal fun convertedName(name: String?, format: Format): String? {
    val stem = name?.substringBeforeLast('.')?.takeIf { it.isNotEmpty() } ?: return null
    return "$stem.${format.extensions.first()}"
}

/** The value of the tree whose root is [root], read as [format], as a conversion carries it and a comparison weighs it. */
internal fun dataValueOf(root: ValueNode, format: Format): DataValue = DocumentData(root, format).value

/**
 * The values of [nodes] of the tree whose root is [root], read as [format], as a conversion carries them: an alias in
 * one is the value it points at anywhere in the document; a node that is null is a null.
 */
internal fun dataValuesOf(root: ValueNode, format: Format, nodes: List<ValueNode?>): List<DataValue> {
    val data = DocumentData(root, format)
    return nodes.map { node -> node?.let(data::of) ?: DataNull }
}

/**
 * The value of the tree whose root is [root], read as [format], as a conversion carries it. A YAML alias is the value
 * it points at, as YAML reads it, unless that value holds the alias itself, or the document has more aliases than
 * [MAX_ALIASES] written out: then it is the name it is written with. A TOML date, the one string TOML writes with no
 * quotes, is a date.
 */
private class DocumentData(private val root: ValueNode, private val format: Format) {
    private val byKey: Map<String, ValueNode> by lazy {
        buildMap {
            val left = ArrayDeque(listOf(root))
            while (left.isNotEmpty()) {
                val node = left.removeLast()
                put(node.key, node)
                left.addAll(node.children)
            }
        }
    }
    private var aliasCount = 0
    private var firstAlias: String? = null

    val value: DataValue by lazy { valueOf(root, inside = emptySet()) }

    /** The value of [node], a node of the tree. */
    fun of(node: ValueNode): DataValue = valueOf(node, inside = emptySet())

    /** That the aliases were written out as copies, where there were any. */
    fun aliases(): Loss.Aliases? = firstAlias?.let { Loss.Aliases(it, aliasCount) }

    private fun valueOf(node: ValueNode, inside: Set<String>): DataValue = when (node.kind) {
        ValueKind.Object -> DataObject(node.children.map { DataMember(it.name, valueOf(it, inside)) })
        ValueKind.Array -> DataArray(node.children.map { valueOf(it, inside) })
        ValueKind.String -> stringOf(node)
        ValueKind.Number -> DataNumber(node.text)
        ValueKind.Boolean -> DataBoolean(node.text.equals("true", ignoreCase = true))
        ValueKind.Null -> DataNull
        ValueKind.Alias -> alias(node, inside)
    }

    /** A TOML date, the one string TOML writes with no quotes, is a date; any other string is its value. */
    private fun stringOf(node: ValueNode): DataValue {
        val date = format == Format.Toml && node.text.firstOrNull() !in TomlQuotes
        return if (date) DataDate(node.text) else DataString(node.value ?: node.text)
    }

    private fun alias(node: ValueNode, inside: Set<String>): DataValue {
        val target = node.target?.let { byKey[it.key] }
        if (target == null || target.key in inside || aliasCount == MAX_ALIASES) return DataString(node.text)
        aliasCount++
        if (firstAlias == null) firstAlias = flatPath(node.path)
        return valueOf(target, inside + target.key)
    }
}

/** [path] as a loss names a value: `customer.name`, `items[0].sku`, and `$` for the document. */
internal fun flatPath(path: List<PathStep>): String {
    if (path.isEmpty()) return "$"
    return buildString {
        path.forEach { step ->
            when (step) {
                is PathStep.Key -> {
                    if (isNotEmpty()) append('.')
                    append(step.name)
                }

                is PathStep.Index -> append('[').append(step.index).append(']')
            }
        }
    }
}

/** What a TOML string is written in: a quote, double or single. A date is written with none. */
private val TomlQuotes = setOf('"', '\'')

/** The most aliases a document is written out with, so that one of aliases on aliases does not grow without end. */
private const val MAX_ALIASES = 1_000

private const val MIN_INDENT = 1
private const val MAX_INDENT = 8
