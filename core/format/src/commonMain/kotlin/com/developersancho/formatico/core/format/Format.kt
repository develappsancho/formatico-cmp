package com.developersancho.formatico.core.format

/**
 * A kind of structured text Formatico reads.
 *
 * @property extensions the file extensions it is known by, in lower case and without the dot.
 */
enum class Format(val extensions: List<String>) {
    /** JSON as RFC 8259 writes it. */
    Json(listOf("json")),

    /** JSON with comments and trailing commas, as editors' settings files write it. */
    Jsonc(listOf("jsonc")),

    /** JSON5: JSON with comments, single quotes, unquoted keys, hexadecimal numbers and more. */
    Json5(listOf("json5")),

    /** Newline-delimited JSON: one JSON value on each line. */
    Ndjson(listOf("ndjson", "jsonl")),

    /** XML. */
    Xml(listOf("xml", "svg", "xsd", "xsl", "plist")),

    /** YAML. */
    Yaml(listOf("yaml", "yml")),

    /** TOML. */
    Toml(listOf("toml")),

    /** Comma-separated values. */
    Csv(listOf("csv")),

    /** Tab-separated values. */
    Tsv(listOf("tsv", "tab")),

    /** Java's properties files. */
    Properties(listOf("properties")),

    /** INI files: sections of keys and values. */
    Ini(listOf("ini", "cfg", "conf"));

    /** The format's name as it is written in every language: `JSON`, `YAML`, `NDJSON`. */
    val displayName: String
        get() = when (this) {
            Json -> "JSON"
            Jsonc -> "JSONC"
            Json5 -> "JSON5"
            Ndjson -> "NDJSON"
            Xml -> "XML"
            Yaml -> "YAML"
            Toml -> "TOML"
            Csv -> "CSV"
            Tsv -> "TSV"
            Properties -> "Properties"
            Ini -> "INI"
        }

    companion object {
        /** The format a file's name says, by its extension, or null when the extension is not one of Formatico's. */
        fun fromFileName(fileName: String): Format? {
            val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
            if (extension.isEmpty()) return null
            return entries.firstOrNull { extension in it.extensions }
        }
    }
}
