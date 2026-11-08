package com.developersancho.formatico.core.format

import com.developersancho.formatico.core.format.json.JsonDialect
import com.developersancho.formatico.core.format.json.JsonDocument
import com.developersancho.formatico.core.format.json.JsonFeature
import com.developersancho.formatico.core.format.text.wholeRegex

/**
 * Tells which [Format] a text is written in: by its file's extension when it has one Formatico knows, and otherwise by
 * what the text looks like.
 */
object FormatDetector {
    /** How many lines are looked at for a format told by its lines: YAML, TOML, INI, properties, CSV and TSV. */
    private const val LINES_LOOKED_AT = 200

    /** How many lines of a newline-delimited JSON are read, each as a JSON value of its own. */
    private const val NDJSON_LINES_READ = 50

    private val SectionHeader = wholeRegex("""^\[\[?[^\[\]]+\]\]?$""")
    private val KeyEqualsValue = Regex("""^[\w.\-"' ]+?\s*=\s*(.*)$""")
    private val KeyColonValue = wholeRegex("""^\s*[^\s:#\-][^:]*:(\s.*)?$""")
    private val ListItem = wholeRegex("""^\s*-(\s.*)?$""")
    private val TomlValue = wholeRegex(
        """^(".*"|'.*'|-?[0-9][0-9_]*(\.[0-9_]+)?([eE][+-]?[0-9]+)?|true|false|\[.*|\{.*|""" +
            """\d{4}-\d{2}-\d{2}.*|[+-]?(inf|nan))\s*(#.*)?$"""
    )

    /**
     * The format of [text], or null when it looks like none of Formatico's: plain text, or nothing at all.
     *
     * [fileName], when given, is believed first: a `.json` file is JSON even while it is broken.
     */
    fun detect(text: String, fileName: String? = null): Format? {
        fileName?.let(Format::fromFileName)?.let { return it }
        val start = text.indexOfFirst { !it.isWhitespace() && it != '\uFEFF' }
        if (start < 0) return null
        return when (text[start]) {
            '{', '[' -> detectJson(text)
            '<' -> Format.Xml
            else -> detectByLines(text)
        }
    }

    private fun detectJson(text: String): Format? {
        if (isNdjson(text)) return Format.Ndjson
        // Read as the widest dialect, then named by what it uses.
        val document = JsonDocument.parse(text, JsonDialect.Json5)
        if (!document.isValid) {
            // `[server]` alone on the first line opens a TOML table or an INI section, not a JSON array.
            val firstLine = text.lineSequence().first { it.isNotBlank() }.trim().removePrefix("\uFEFF")
            return if (SectionHeader.containsMatchIn(firstLine)) detectByLines(text) else Format.Json
        }
        return when {
            JsonFeature.Json5Syntax in document.features -> Format.Json5
            JsonFeature.Comments in document.features || JsonFeature.TrailingCommas in document.features -> Format.Jsonc
            else -> Format.Json
        }
    }

    private fun isNdjson(text: String): Boolean {
        val lines = text.lineSequence().filter { it.isNotBlank() }.take(NDJSON_LINES_READ).toList()
        return lines.size >= 2 && lines.all { JsonDocument.parse(it).isValid }
    }

    private fun detectByLines(text: String): Format? {
        val lines = text.lineSequence()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
            .take(LINES_LOOKED_AT)
            .toList()
        val first = lines.first().trimStart()
        return when {
            first == "---" || first.startsWith("%YAML") -> Format.Yaml
            hasSameCount(lines, '\t') -> Format.Tsv
            else -> detectByContent(lines)
        }
    }

    /** The format of [lines] told by their keys, sections and delimiters, their comments left out. */
    private fun detectByContent(lines: List<String>): Format? {
        val content = lines.filterNot { isComment(it.trimStart()) }
        if (content.isEmpty()) return null
        val sections = content.count { SectionHeader.containsMatchIn(it.trim()) }
        val assignments = content.mapNotNull { KeyEqualsValue.find(it.trim())?.groupValues?.get(1)?.trim() }
        val keyColons = content.count { KeyColonValue.containsMatchIn(it) || ListItem.containsMatchIn(it) }

        return when {
            assignments.isNotEmpty() && keyColons <= assignments.size -> when {
                assignments.all { TomlValue.containsMatchIn(it) } -> Format.Toml
                sections > 0 -> Format.Ini
                else -> Format.Properties
            }

            sections > 0 -> Format.Ini

            hasSameCount(lines, ',') || hasSameCount(lines, ';') -> Format.Csv

            keyColons > 0 -> Format.Yaml

            else -> null
        }
    }

    private fun isComment(line: String): Boolean = line.startsWith("#") || line.startsWith(";") || line.startsWith("!")

    /** Whether every line has [delimiter] the same number of times, once at least: a table's rows. */
    private fun hasSameCount(lines: List<String>, delimiter: Char): Boolean {
        if (lines.size < 2) return false
        val count = lines.first().count { it == delimiter }
        return count > 0 && lines.all { line -> line.count { it == delimiter } == count }
    }
}
