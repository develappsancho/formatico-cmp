package com.developersancho.formatico.core.format.convert

import com.developersancho.formatico.core.format.text.wholeRegex

/**
 * Writes values as a TOML document: a table's own keys first, then its tables, `[a.b]`, and its arrays of tables,
 * `[[items]]`; an array of other values, and an object inside one, on its key's line. TOML has no null: one is left
 * out, a comment marking where it was, or written as an empty string, as [options] say. What it cannot write as it is
 * goes to [log].
 */
internal class TomlWriter(private val options: ConvertOptions, private val log: ConvertLog) {
    private val out = StringBuilder()
    private var lines = 0
    private val leavesNulls = options.nulls == NullHandling.LeaveOut

    /** [value] as a TOML document, put under a key of its own where it is no table. */
    fun document(value: DataValue): String {
        val root = when (value) {
            is DataObject -> value
            is DataArray -> wrapped(ARRAY_NAME, value)
            else -> wrapped(VALUE_NAME, value)
        }
        table(root, keys = emptyList(), path = "", header = null)
        return out.toString()
    }

    private fun wrapped(name: String, value: DataValue): DataObject {
        log.wrap(name)
        return DataObject(listOf(DataMember(name, value)))
    }

    /** [value], under [header] where it is given one: its own keys, then its tables. */
    private fun table(value: DataObject, keys: List<String>, path: String, header: String?) {
        val (sections, own) = value.members.partition { isSection(it.value) }
        var afterSection = false
        value.members.forEach { member ->
            if (isSection(member.value)) {
                afterSection = true
            } else if (afterSection) {
                log.lose(Lost.Moved, memberPath(path, member.name))
            }
        }
        // A table of tables alone needs no header of its own: theirs make it.
        if (header != null && (own.isNotEmpty() || sections.isEmpty())) line(header, blankBefore = true)
        own.forEach { ownKey(it, path) }
        sections.forEach { member ->
            val inner = keys + member.name
            val innerPath = memberPath(path, member.name)
            when (val section = member.value) {
                is DataObject -> table(section, inner, innerPath, "[${tomlKeys(inner)}]")

                is DataArray -> section.items.forEachIndexed { index, item ->
                    val itemPath = itemPath(innerPath, index)
                    if (item is DataObject) table(item, inner, itemPath, "[[${tomlKeys(inner)}]]") else log.lose(Lost.Nulls, itemPath)
                }

                else -> Unit
            }
        }
    }

    /** Whether [value] is written under a header of its own: a table, or an array of tables. */
    private fun isSection(value: DataValue): Boolean = when (value) {
        is DataObject -> value.members.isNotEmpty()
        is DataArray -> value.items.any { it is DataObject } && value.items.all { it is DataObject || (leavesNulls && it == DataNull) }
        else -> false
    }

    /** A key of a table's own and its value; a null left out is a comment where it was. */
    private fun ownKey(member: DataMember, path: String) {
        val memberPath = memberPath(path, member.name)
        if (member.value == DataNull && leavesNulls) {
            log.leaveOut(memberPath, lines)
            line("# ${options.commentFor(member.name)}")
            return
        }
        line("${tomlKey(member.name)} = ${inline(member.value, memberPath)}")
    }

    private fun line(text: String, blankBefore: Boolean = false) {
        if (blankBefore && out.isNotEmpty()) {
            out.append('\n')
            lines++
        }
        out.append(text).append('\n')
        lines++
    }

    /** [value] on one line: an array in brackets, an object in braces. */
    private fun inline(value: DataValue, path: String): String = when (value) {
        is DataObject -> inlineTable(value, path)

        is DataArray -> value.items.withIndex()
            .filter { (index, item) -> kept(item, itemPath(path, index)) }
            .joinToString(", ", "[", "]") { (index, item) -> inline(item, itemPath(path, index)) }

        is DataString -> tomlString(value.value)

        is DataNumber -> number(value.written, path)

        is DataBoolean -> "${value.value}"

        is DataDate -> value.written

        DataNull -> {
            log.lose(Lost.Nulls, path)
            "\"\""
        }
    }

    private fun inlineTable(value: DataObject, path: String): String {
        if (value.members.isEmpty()) return "{}"
        return value.members
            .filter { kept(it.value, memberPath(path, it.name)) }
            .joinToString(", ", "{ ", " }") { "${tomlKey(it.name)} = ${inline(it.value, memberPath(path, it.name))}" }
    }

    /** Whether [value], at [path], is written on its line: a null left out is not, and is told to [log]. */
    private fun kept(value: DataValue, path: String): Boolean {
        if (value != DataNull || !leavesNulls) return true
        log.lose(Lost.Nulls, path)
        return false
    }

    private fun number(written: String, path: String): String = when (val spelled = tomlSpelling(written)) {
        null -> {
            log.lose(Lost.Unwritable, path)
            tomlString(written)
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
        /** The key an array is put under, as TOML's document is a table. */
        const val ARRAY_NAME = "items"

        /** The key any other value is put under. */
        const val VALUE_NAME = "value"
    }
}

private val BareKey = wholeRegex("[A-Za-z0-9_-]+")

/** [name] as a TOML key: bare where it can be, quoted otherwise. */
internal fun tomlKey(name: String): String = if (BareKey.containsMatchIn(name)) name else tomlString(name)

/** [names] as the dotted key of a header. */
private fun tomlKeys(names: List<String>): String = names.joinToString(".") { tomlKey(it) }

/** [text] as a TOML basic string, with what TOML escapes escaped. */
internal fun tomlString(text: String): String = buildString {
    append('"')
    text.forEach { char ->
        when (char) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            in '\u0000'..'\u001F', '\u007F' -> append("\\u").append(char.code.toString(HEX).padStart(UNICODE, '0'))
            else -> append(char)
        }
    }
    append('"')
}

private const val HEX = 16
private const val UNICODE = 4
