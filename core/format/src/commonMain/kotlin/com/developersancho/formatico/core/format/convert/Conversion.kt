package com.developersancho.formatico.core.format.convert

import com.developersancho.formatico.core.format.text.Indentation

/** What a null becomes in a format that has none: TOML, XML, properties and INI. CSV's is always an empty field. */
enum class NullHandling {
    /** It is left out, and a comment marks where it was, where the format has comments and the place allows one. */
    LeaveOut,

    /** It is written as an empty string. */
    EmptyString
}

/**
 * How a conversion writes.
 *
 * @property indentation one level of a format written in levels: JSON, YAML and XML.
 * @property sortKeys whether every object's members are written in the order of their names.
 * @property nulls what a null becomes where the format has none.
 * @property nullComment the comment that marks where a null was left out, `{name}` standing for its name: the words
 *   are the caller's, in its language.
 */
data class ConvertOptions(
    val indentation: Indentation = Indentation.Spaces(2),
    val sortKeys: Boolean = false,
    val nulls: NullHandling = NullHandling.LeaveOut,
    val nullComment: String = "$NAME = null"
) {
    /** The comment for the null named [name]. */
    internal fun commentFor(name: String): String = nullComment.replace(NAME, name)

    companion object {
        /** What stands for a null's name in [nullComment]. */
        const val NAME = "{name}"
    }
}

/**
 * What a conversion came to: the [text] of the new document, what of the old one it does not carry as it is
 * ([losses]), in the order a reader is told of them, what it did to carry the rest ([notes]), and the lines, counted
 * from nought, of the comments that mark where a null was left out ([nullLines]).
 */
data class Conversion(val text: String, val losses: List<Loss>, val notes: List<Note>, val nullLines: List<Int> = emptyList())

/**
 * What a conversion does not carry as it is: each kind once, with the first value it is about, by its path, and how
 * many there are. A path is written as a flattened name is, `customer.name`, `items[0].sku`, and the document itself is
 * `$`.
 */
sealed interface Loss {
    /** How many values, keys or comments it is about. */
    val count: Int

    /** The format has no null: [first] and the rest were null, and are left out or written as empty strings. */
    data class Nulls(val first: String, override val count: Int, val handling: NullHandling) : Loss

    /** The document has comments, and a conversion carries values: the caller, which read the document, says so. */
    data class Comments(override val count: Int) : Loss

    /** A YAML alias stands for the value it points at, and is written as a copy of it: the caller says so. */
    data class Aliases(val first: String, override val count: Int) : Loss

    /** The format has only text: [first] and the rest, numbers, `true` and `false`, are written as text. */
    data class Untyped(val first: String, override val count: Int) : Loss

    /** The format has one level: [first] and the rest, inside other values, are names of their own. */
    data class Flattened(val first: String, override val count: Int) : Loss

    /** The document's value cannot be the format's document: it is put under [name]. */
    data class Wrapped(val name: String) : Loss {
        override val count: Int get() = 1
    }

    /** A key is no name in the format: [first] is written [written], and the rest likewise. */
    data class Renamed(val first: String, val written: String, override val count: Int) : Loss

    /** A key is given twice in an object, and the format keeps a key once: the last of each is kept. */
    data class Duplicates(val first: String, override val count: Int) : Loss

    /** A number is written in the format's own spelling: [first] as [written], and the rest likewise. */
    data class Respelled(val first: String, val written: String, override val count: Int) : Loss

    /** A number has no value in the format, an infinity or an integer too large: [first] and the rest are strings. */
    data class Unwritable(val first: String, override val count: Int) : Loss

    /** A TOML date is a string in the format: [first] and the rest. */
    data class Dates(val first: String, override val count: Int) : Loss

    /** The format writes a level's own values before its tables or sections: [first] and the rest move up. */
    data class Moved(val first: String, override val count: Int) : Loss

    /** An empty object or array has nothing to be written as in the format: [first] and the rest. */
    data class Emptied(val first: String, override val count: Int) : Loss

    /** The format writes a value on one line: the line breaks of [first] and the rest are written as `\n`. */
    data class OneLine(val first: String, override val count: Int) : Loss

    /** An array in an array has no name in XML: the items of [first] and the rest are written as `item`. */
    data class ItemsNamed(val first: String, override val count: Int) : Loss
}

/** What a conversion did so that the new document converts back as it was. */
sealed interface Note {
    /**
     * A string the format would read as another value is written in quotes: [dates] says whether every one of them
     * would be read as a date.
     */
    data class Quoted(val dates: Boolean, val count: Int) : Note

    /**
     * A number keeps a spelling another converter would not: `129.90`, `1e3`. [first] and [second] are the first two,
     * and [count] how many there are.
     */
    data class Spelling(val first: String, val second: String?, val count: Int) : Note
}
