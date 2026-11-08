package com.developersancho.formatico.core.format.convert

/**
 * Writes values as JSON, a value to a line indented by [indent], or on one line where it is null; as JSON5 writes its
 * numbers where [json5] says so. What it cannot write as it is goes to [log].
 */
internal class JsonWriter(private val indent: String?, private val json5: Boolean, private val log: ConvertLog) {
    private val out = StringBuilder()

    /** [value] as a JSON document. */
    fun document(value: DataValue): String {
        write(value, path = "", depth = 0)
        return out.append('\n').toString()
    }

    /** [value], at [path], on one line. */
    fun line(value: DataValue, path: String): String {
        write(value, path, depth = 0)
        return out.toString()
    }

    private fun write(value: DataValue, path: String, depth: Int) {
        when (value) {
            is DataObject -> container('{', '}', value.members, depth) { member ->
                out.appendJsonString(member.name).append(if (indent == null) ":" else ": ")
                write(member.value, memberPath(path, member.name), depth + 1)
            }

            is DataArray -> {
                var index = 0
                container('[', ']', value.items, depth) { item -> write(item, itemPath(path, index++), depth + 1) }
            }

            is DataString -> out.appendJsonString(value.value)

            is DataNumber -> number(value.written, path)

            is DataBoolean -> out.append(value.value)

            DataNull -> out.append("null")

            is DataDate -> {
                log.lose(Lost.Dates, path)
                out.appendJsonString(value.written)
            }
        }
    }

    private fun <T> container(open: Char, close: Char, entries: List<T>, depth: Int, entry: (T) -> Unit) {
        out.append(open)
        if (entries.isEmpty()) {
            out.append(close)
            return
        }
        entries.forEachIndexed { index, item ->
            if (index > 0) out.append(',')
            newline(depth + 1)
            entry(item)
        }
        newline(depth)
        out.append(close)
    }

    private fun newline(depth: Int) {
        val unit = indent ?: return
        out.append('\n')
        repeat(depth) { out.append(unit) }
    }

    private fun number(written: String, path: String) {
        val spelled = if (json5) json5Spelling(written) else jsonSpelling(written)
        when (spelled) {
            null -> {
                log.lose(Lost.Unwritable, path)
                out.appendJsonString(written)
            }

            written -> {
                log.kept(written)
                out.append(written)
            }

            else -> {
                log.lose(Lost.Respelled, path, spelled)
                out.append(spelled)
            }
        }
    }
}

/** [text] as a JSON string: in double quotes, with what JSON escapes escaped. */
internal fun StringBuilder.appendJsonString(text: String): StringBuilder {
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
            in '\u0000'..'\u001F' -> append("\\u").append(char.code.toString(HEX_RADIX).padStart(UNICODE_DIGITS, '0'))
            else -> append(char)
        }
    }
    return append('"')
}

private const val HEX_RADIX = 16
private const val UNICODE_DIGITS = 4
