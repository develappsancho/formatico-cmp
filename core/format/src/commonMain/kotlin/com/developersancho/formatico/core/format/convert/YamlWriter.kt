package com.developersancho.formatico.core.format.convert

import com.developersancho.formatico.core.format.text.wholeRegex

/**
 * Writes values as a YAML document in block style, each level indented by [indent], an array's items under its key
 * indented as well. A string YAML would read as another value is quoted; what it cannot write as it is goes to [log].
 */
internal class YamlWriter(private val indent: String, private val log: ConvertLog) {
    private val out = StringBuilder()

    /** [value] as a YAML document. */
    fun document(value: DataValue): String {
        when {
            value is DataObject && value.members.isNotEmpty() -> members(value, path = "", column = 0)
            value is DataArray && value.items.isNotEmpty() -> items(value, path = "", column = 0)
            else -> out.append(scalar(value, path = ""))
        }
        if (!out.endsWith('\n')) out.append('\n')
        return out.toString()
    }

    /** The members of [value], each on a line of its own at [column]. */
    private fun members(value: DataObject, path: String, column: Int) {
        value.members.forEachIndexed { index, member ->
            if (index > 0) pad(column)
            member(member, memberPath(path, member.name), column)
        }
    }

    /** [member] from where the line already stands: its key, and its value after it or under it. */
    private fun member(member: DataMember, path: String, column: Int) {
        out.append(key(member.name)).append(':')
        val value = member.value
        when {
            value is DataObject && value.members.isNotEmpty() -> {
                out.append('\n')
                pad(column + indent.length)
                members(value, path, column + indent.length)
            }

            value is DataArray && value.items.isNotEmpty() -> {
                out.append('\n')
                pad(column + indent.length)
                items(value, path, column + indent.length)
            }

            else -> out.append(' ').append(scalar(value, path)).append('\n')
        }
    }

    /** The items of [value], each after a `- ` at [column]; what an item holds goes on after its dash. */
    private fun items(value: DataArray, path: String, column: Int) {
        value.items.forEachIndexed { index, item ->
            if (index > 0) pad(column)
            out.append("- ")
            val itemPath = itemPath(path, index)
            when {
                item is DataObject && item.members.isNotEmpty() -> members(item, itemPath, column + DASH)
                item is DataArray && item.items.isNotEmpty() -> items(item, itemPath, column + DASH)
                else -> out.append(scalar(item, itemPath)).append('\n')
            }
        }
    }

    private fun pad(column: Int) {
        repeat(column) { out.append(' ') }
    }

    /** A value that takes no lines of its own: a scalar, or an empty object or array. */
    private fun scalar(value: DataValue, path: String): String = when (value) {
        is DataObject -> "{}"

        is DataArray -> "[]"

        is DataString -> string(value.value)

        is DataNumber -> number(value.written, path)

        is DataBoolean -> "${value.value}"

        DataNull -> "null"

        is DataDate -> {
            log.lose(Lost.Dates, path)
            string(value.written)
        }
    }

    private fun string(value: String): String {
        val reading = yamlReading(value) ?: return value
        if (reading != YamlReading.Unsafe) log.quoted(asDate = reading == YamlReading.Date)
        return yamlQuoted(value)
    }

    private fun key(name: String): String = if (yamlReading(name) == null) name else yamlQuoted(name)

    private fun number(written: String, path: String): String = when (val spelled = yamlSpelling(written)) {
        null -> {
            log.lose(Lost.Unwritable, path)
            yamlQuoted(written)
        }

        written -> {
            log.kept(written)
            written
        }

        else -> {
            log.lose(Lost.Respelled, path, spelled)
            spelled
        }
    }

    private companion object {
        /** How far an item's value stands past its dash: `- `. */
        const val DASH = 2
    }
}

/** Why a string cannot be written plain in YAML. */
internal enum class YamlReading {
    /** It would be read as a date. */
    Date,

    /** It would be read as another value: a number, `true`, `null`. */
    Value,

    /** It would not be read back as it is: an indicator, a `: `, a space at an end, a line break. */
    Unsafe
}

/** Why [text] cannot be a plain scalar in YAML, its 1.1 readers' as well, or null where it can be. */
internal fun yamlReading(text: String): YamlReading? = when {
    text.isEmpty() -> YamlReading.Unsafe
    YamlDate.containsMatchIn(text) -> YamlReading.Date
    YamlValue.containsMatchIn(text) -> YamlReading.Value
    isUnsafeYaml(text) -> YamlReading.Unsafe
    else -> null
}

private fun isUnsafeYaml(text: String): Boolean =
    text.first() in YAML_INDICATORS ||
        text.first().isWhitespace() ||
        text.last().isWhitespace() ||
        text.endsWith(':') ||
        ": " in text ||
        " #" in text ||
        text.any { it < ' ' || it == '\u007F' || it == '\uFEFF' }

/** What a YAML plain scalar cannot start with. */
private const val YAML_INDICATORS = "-?:,[]{}#&*!|>'\"%@`"

/** A date or a time stamp, as YAML 1.1 reads one: a date, and a time after it with its zone. */
private val YamlDate = wholeRegex(
    "[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}" +
        "([Tt ]+[0-9]{1,2}:[0-9]{2}(:[0-9]{2}(\\.[0-9]*)?)?\\s*(Z|[-+][0-9]{1,2}(:[0-9]{2})?)?)?"
)

/** What YAML 1.2's core schema or 1.1 reads as a null, a truth or a number, and the keys 1.1 gives a meaning. */
private val YamlValue = wholeRegex(
    listOf(
        "~|null|Null|NULL",
        "true|True|TRUE|false|False|FALSE|y|Y|yes|Yes|YES|n|N|no|No|NO|on|On|ON|off|Off|OFF",
        "[-+]?(0b[0-1_]+|0o?[0-7_]+|0x[0-9a-fA-F_]+|[0-9][0-9_]*(:[0-5]?[0-9])*)",
        "[-+]?([0-9][0-9_]*)?\\.[0-9_]*([eE][-+]?[0-9]+)?|[-+]?[0-9][0-9_]*[eE][-+]?[0-9]+",
        "[-+]?\\.(inf|Inf|INF)|\\.(nan|NaN|NAN)",
        "<<|="
    ).joinToString("|")
)

/** [text] as a double-quoted YAML scalar, with what YAML escapes escaped. */
internal fun yamlQuoted(text: String): String = buildString {
    append('"')
    text.forEach { char ->
        when (char) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            in '\u0000'..'\u001F', '\u007F', '\uFEFF' -> append("\\u").append(char.code.toString(HEX).padStart(UNICODE, '0'))
            else -> append(char)
        }
    }
    append('"')
}

private const val HEX = 16
private const val UNICODE = 4
