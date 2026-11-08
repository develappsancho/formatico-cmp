package com.developersancho.formatico.core.format.yaml

import com.developersancho.formatico.core.format.text.TextRange

/**
 * Where a value starts, and what is written before it.
 *
 * @property start where the value starts: its anchor or its tag where it has one, `&labels` of `&labels {a: 1}`.
 * @property anchor the anchor the value is given, `labels` for `&labels`, or null for none.
 * @property tag the tag the value is given, as it is written, `!!str`, or null for none.
 */
class YamlProperties(val start: Int, val anchor: String?, val tag: String?)

/** A value of a YAML document, where it is written. */
sealed class YamlNode {
    /** Where the value starts, and its anchor and its tag. */
    abstract val properties: YamlProperties

    /**
     * The value itself, from its first character to its last, its anchor and its tag left out, and trailing spaces and
     * line breaks too.
     */
    abstract val range: TextRange

    /** Where the value starts: its anchor or its tag where it has one. */
    val start: Int get() = properties.start

    /** The anchor the value is given, `labels` for `&labels`, or null for none. */
    val anchor: String? get() = properties.anchor

    /** The tag the value is given, as it is written, `!!str`, or null for none. */
    val tag: String? get() = properties.tag
}

/**
 * A mapping: keys and their values.
 *
 * @property entries its entries in the order they are written, a key given twice included.
 * @property flow whether it is written in braces, `{a: 1}`, rather than a key to a line.
 */
class YamlMapping(
    override val properties: YamlProperties,
    override val range: TextRange,
    val entries: List<YamlEntry>,
    val flow: Boolean
) : YamlNode()

/** An entry of a mapping: its key, a scalar mostly, and its value. */
class YamlEntry(val key: YamlNode, val value: YamlNode)

/**
 * A sequence.
 *
 * @property items its values in order.
 * @property flow whether it is written in brackets, `[1, 2]`, rather than an item to a line.
 */
class YamlSequence(
    override val properties: YamlProperties,
    override val range: TextRange,
    val items: List<YamlNode>,
    val flow: Boolean
) : YamlNode()

/**
 * A scalar, kept as it is written.
 *
 * @property raw the scalar as it is written: its quotes, or a block scalar's indicator and lines, included.
 * @property value its value: the quotes taken off, the escapes read, a block scalar's lines joined.
 * @property kind what YAML 1.2's core schema reads it as, or its tag says it is.
 * @property style how it is written.
 */
class YamlScalar(
    override val properties: YamlProperties,
    override val range: TextRange,
    val raw: String,
    val value: String,
    val kind: YamlScalarKind,
    val style: YamlScalarStyle
) : YamlNode()

/**
 * An alias, `*labels`: the value given the anchor [name] before it.
 *
 * @property target the value the anchor was given last before the alias, or null where there is none.
 */
class YamlAlias(override val range: TextRange, val name: String, private val slot: AnchorSlot?) : YamlNode() {
    override val properties: YamlProperties = YamlProperties(range.start, anchor = null, tag = null)

    val target: YamlNode? get() = slot?.node
}

/** Where an anchor's value is kept, filled once the value is read: an alias inside it may point at it before. */
class AnchorSlot internal constructor() {
    /** The anchored value, once it is read. */
    var node: YamlNode? = null
        internal set
}

/**
 * How a document of a stream starts, and how it ends.
 *
 * @property explicit whether it starts with `---`.
 * @property directives its directives, `%YAML 1.2`, as they are written.
 * @property explicitEnd whether it ends with `...`.
 */
data class YamlDocumentStart(val explicit: Boolean, val directives: List<String>, val explicitEnd: Boolean = false)

/** What YAML 1.2's core schema reads a scalar as. */
enum class YamlScalarKind {
    /** `null`, `~`, or nothing at all. */
    Null,

    /** `true` or `false`, in any of the core schema's spellings. */
    Boolean,

    /** A whole number, in decimal, octal (`0o17`) or hexadecimal (`0x1F`). */
    Integer,

    /** A number with a fraction or an exponent, `.inf` or `.nan`. */
    Float,

    /** Any other text, a quoted one among them. */
    String
}

/** How a scalar is written. */
enum class YamlScalarStyle {
    /** With no quotes. */
    Plain,

    /** In single quotes. */
    SingleQuoted,

    /** In double quotes, with escapes. */
    DoubleQuoted,

    /** A block whose line breaks are kept: `|`. */
    Literal,

    /** A block whose line breaks are folded into spaces: `>`. */
    Folded
}
