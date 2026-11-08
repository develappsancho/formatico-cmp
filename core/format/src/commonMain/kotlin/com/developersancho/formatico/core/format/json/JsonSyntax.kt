package com.developersancho.formatico.core.format.json

import com.developersancho.formatico.core.format.text.TextRange

/**
 * Which JSON a document is read as.
 *
 * @property allowsComments whether `//` and `/* */` comments are part of it.
 * @property allowsTrailingCommas whether a comma may follow an object's or an array's last item.
 * @property isJson5 whether JSON5's own syntax is part of it: single quotes, unquoted keys, its numbers and escapes.
 */
enum class JsonDialect(val allowsComments: Boolean, val allowsTrailingCommas: Boolean, val isJson5: Boolean) {
    /** RFC 8259: no comments, no trailing commas. */
    Json(allowsComments = false, allowsTrailingCommas = false, isJson5 = false),

    /** JSON with comments and trailing commas. */
    Jsonc(allowsComments = true, allowsTrailingCommas = true, isJson5 = false),

    /** JSON5. */
    Json5(allowsComments = true, allowsTrailingCommas = true, isJson5 = true)
}

/** What a document uses beyond RFC 8259's JSON, whatever dialect it was read as. */
enum class JsonFeature {
    /** A comment. */
    Comments,

    /** A comma after an object's or an array's last item. */
    TrailingCommas,

    /** Syntax only JSON5 has: a single-quoted string, an unquoted key, `0x1F`, `.5`, `+1`, `Infinity`, `\x41`. */
    Json5Syntax
}

/** The kind of a [JsonToken]. */
enum class JsonTokenKind(val isTrivia: Boolean = false) {
    /** `{` */
    LeftBrace,

    /** `}` */
    RightBrace,

    /** `[` */
    LeftBracket,

    /** `]` */
    RightBracket,

    /** `:` */
    Colon,

    /** `,` */
    Comma,

    /** A string, with its quotes, as it is written. */
    String,

    /** A number, as it is written: `1.50` stays `1.50`. */
    Number,

    /** `true` */
    True,

    /** `false` */
    False,

    /** `null` */
    Null,

    /** A bare word that is no keyword: a JSON5 key, or a mistake. */
    Identifier,

    /** Spaces, tabs and line breaks. */
    Whitespace(isTrivia = true),

    /** A `//` comment, to the end of its line. */
    LineComment(isTrivia = true),

    /** A `/* */` comment. */
    BlockComment(isTrivia = true),

    /** Characters no token starts with. */
    Error,

    /** The end of the text: an empty token after the last character. */
    EndOfFile
}

/**
 * A token of a JSON text: its kind and where it is. The tokens of a document follow each other with no gap, from
 * the first character to the last, so that they are the text itself.
 */
data class JsonToken(val kind: JsonTokenKind, val start: Int, val end: Int) {
    /** Where it is, as a range. */
    val range: TextRange get() = TextRange(start, end)
}

/** A value of a JSON document, where it is written. */
sealed class JsonValue {
    /** The text of the value, from its first character to its last. */
    abstract val range: TextRange
}

/**
 * An object.
 *
 * @property members its members in the order they are written, a key given twice included.
 */
class JsonObject(override val range: TextRange, val members: List<JsonMember>) : JsonValue()

/**
 * A member of an object: its key and its value.
 *
 * @property value the value, or null when the text has none that can be read.
 */
class JsonMember(val range: TextRange, val key: JsonKey, val value: JsonValue?)

/**
 * A member's key.
 *
 * @property raw the key as it is written, its quotes included.
 * @property quoted whether it is written in quotes; JSON5 allows a key without.
 */
class JsonKey(val range: TextRange, val raw: String, val quoted: Boolean) {
    /** The key's name: [raw] without its quotes, its escapes read. */
    val name: String by lazy { if (quoted) decodeJsonString(raw) else raw }
}

/**
 * An array.
 *
 * @property items its values in order.
 */
class JsonArray(override val range: TextRange, val items: List<JsonValue>) : JsonValue()

/**
 * A string.
 *
 * @property raw the string as it is written, its quotes and escapes included.
 */
class JsonString(override val range: TextRange, val raw: String) : JsonValue() {
    /** The string's value: [raw] without its quotes, its escapes read. */
    val value: String by lazy { decodeJsonString(raw) }
}

/**
 * A number, kept as it is written: `1.0`, `1e3` and `0x1F` are not turned into another spelling.
 *
 * @property raw the number as it is written.
 */
class JsonNumber(override val range: TextRange, val raw: String) : JsonValue()

/** `true` or `false`. */
class JsonBoolean(override val range: TextRange, val value: Boolean) : JsonValue()

/** `null`. */
class JsonNull(override val range: TextRange) : JsonValue()
