package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.csv.CsvDocument
import com.developersancho.formatico.core.format.csv.formatted
import com.developersancho.formatico.core.format.ini.IniDocument
import com.developersancho.formatico.core.format.ini.formatted
import com.developersancho.formatico.core.format.ini.minified
import com.developersancho.formatico.core.format.json.JsonDialect
import com.developersancho.formatico.core.format.json.JsonDocument
import com.developersancho.formatico.core.format.json.formatted
import com.developersancho.formatico.core.format.json.minified
import com.developersancho.formatico.core.format.ndjson.NdjsonDocument
import com.developersancho.formatico.core.format.ndjson.formatted
import com.developersancho.formatico.core.format.ndjson.minified
import com.developersancho.formatico.core.format.properties.PropertiesDocument
import com.developersancho.formatico.core.format.properties.formatted
import com.developersancho.formatico.core.format.properties.minified
import com.developersancho.formatico.core.format.text.Indentation
import com.developersancho.formatico.core.format.text.TextLayout
import com.developersancho.formatico.core.format.toml.TomlDocument
import com.developersancho.formatico.core.format.toml.formatted
import com.developersancho.formatico.core.format.toml.minified
import com.developersancho.formatico.core.format.xml.XmlDocument
import com.developersancho.formatico.core.format.xml.formatted
import com.developersancho.formatico.core.format.xml.minified
import com.developersancho.formatico.core.format.yaml.YamlDocument
import com.developersancho.formatico.core.format.yaml.formatted
import com.developersancho.formatico.core.format.yaml.minified

/**
 * What Formatico can make of a document of this format: Format alone in CSV and TSV, whose columns have no keys and
 * which have no room to take out; Format and Minify in XML, whose elements keep the order they are written in; all of
 * it in the others.
 */
internal val Format.edits: Set<DocumentEdit>
    get() = when (this) {
        Format.Csv, Format.Tsv -> setOf(DocumentEdit.Format)
        Format.Xml -> setOf(DocumentEdit.Format, DocumentEdit.Minify)
        else -> DocumentEdit.entries.toSet()
    }

/** The JSON a format is read as, or null for one that is not JSON's. */
internal val Format.jsonDialect: JsonDialect?
    get() = when (this) {
        Format.Json -> JsonDialect.Json
        Format.Jsonc -> JsonDialect.Jsonc
        Format.Json5 -> JsonDialect.Json5
        else -> null
    }

/** What an edit makes of a document's text, before it is kept. */
internal sealed interface Edited {
    class Text(val text: String, val commentsRemoved: Int = 0) : Edited

    data object NotPossible : Edited

    data object AliasBeforeAnchor : Edited
}

/** What [edit] makes of [document]: nothing where it is plain text, or not valid. */
internal fun edited(document: OpenDocument, edit: DocumentEdit): Edited {
    val text = document.text
    val indentation = TextLayout.of(text).indentation ?: Indentation.Spaces(2)
    val format = document.format ?: return Edited.NotPossible
    if (edit !in format.edits) return Edited.NotPossible
    return when (format) {
        Format.Json, Format.Jsonc, Format.Json5 -> editedJson(text, requireNotNull(format.jsonDialect), edit, indentation)
        Format.Ndjson -> editedNdjson(text, edit)
        Format.Yaml -> editedYaml(text, edit, indentation)
        Format.Csv, Format.Tsv -> editedCsv(text, delimiter = if (format == Format.Tsv) '\t' else null)
        Format.Toml -> editedToml(text, edit, indentation)
        Format.Xml -> editedXml(text, edit, indentation)
        Format.Properties -> editedProperties(text, edit)
        Format.Ini -> editedIni(text, edit)
    }
}

private fun editedJson(text: String, dialect: JsonDialect, edit: DocumentEdit, indentation: Indentation): Edited {
    val json = JsonDocument.parse(text, dialect)
    if (!json.isValid) return Edited.NotPossible
    return when (edit) {
        DocumentEdit.Format -> Edited.Text(json.formatted(indentation))
        DocumentEdit.Minify -> Edited.Text(json.minified(), commentsRemoved = json.comments.size)
        DocumentEdit.SortKeys -> Edited.Text(json.formatted(indentation, sortKeys = true))
    }
}

/** What [edit] makes of a newline-delimited JSON [text]: its records stay a line each, whatever the edit. */
private fun editedNdjson(text: String, edit: DocumentEdit): Edited {
    val ndjson = NdjsonDocument.parse(text)
    if (!ndjson.isValid) return Edited.NotPossible
    return when (edit) {
        DocumentEdit.Format -> Edited.Text(ndjson.formatted())
        DocumentEdit.Minify -> Edited.Text(ndjson.minified())
        DocumentEdit.SortKeys -> Edited.Text(ndjson.formatted(sortKeys = true))
    }
}

/** What Format makes of a CSV or a TSV [text]: it quotes only what needs it. */
private fun editedCsv(text: String, delimiter: Char?): Edited {
    val csv = CsvDocument.parse(text, delimiter)
    return if (csv.isValid) Edited.Text(csv.formatted()) else Edited.NotPossible
}

/** What [edit] makes of a YAML [text]; its keys are not sorted where an alias would come before its anchor. */
private fun editedYaml(text: String, edit: DocumentEdit, indentation: Indentation): Edited {
    val yaml = YamlDocument.parse(text)
    if (!yaml.isValid) return Edited.NotPossible
    return when (edit) {
        DocumentEdit.Format -> {
            Edited.Text(yaml.formatted(indentation))
        }

        DocumentEdit.Minify -> {
            Edited.Text(yaml.minified(), commentsRemoved = yaml.comments.size)
        }

        DocumentEdit.SortKeys -> {
            val sorted = yaml.formatted(indentation, sortKeys = true)
            val again = YamlDocument.parse(sorted).problems
            if (again.any { it.code == ProblemCode.UndefinedAlias }) Edited.AliasBeforeAnchor else Edited.Text(sorted)
        }
    }
}

private fun editedToml(text: String, edit: DocumentEdit, indentation: Indentation): Edited {
    val toml = TomlDocument.parse(text)
    if (!toml.isValid) return Edited.NotPossible
    return when (edit) {
        DocumentEdit.Format -> Edited.Text(toml.formatted(indentation))
        DocumentEdit.Minify -> Edited.Text(toml.minified(), commentsRemoved = toml.comments.size)
        DocumentEdit.SortKeys -> Edited.Text(toml.formatted(indentation, sortKeys = true))
    }
}

/** What [edit] makes of an XML [text]: Format or Minify, as its elements are not sorted. */
private fun editedXml(text: String, edit: DocumentEdit, indentation: Indentation): Edited {
    val xml = XmlDocument.parse(text)
    if (!xml.isValid) return Edited.NotPossible
    return if (edit == DocumentEdit.Minify) {
        Edited.Text(xml.minified(), commentsRemoved = xml.comments.size)
    } else {
        Edited.Text(xml.formatted(indentation))
    }
}

private fun editedProperties(text: String, edit: DocumentEdit): Edited {
    val properties = PropertiesDocument.parse(text)
    if (!properties.isValid) return Edited.NotPossible
    return when (edit) {
        DocumentEdit.Format -> Edited.Text(properties.formatted())
        DocumentEdit.Minify -> Edited.Text(properties.minified(), commentsRemoved = properties.comments.size)
        DocumentEdit.SortKeys -> Edited.Text(properties.formatted(sortKeys = true))
    }
}

private fun editedIni(text: String, edit: DocumentEdit): Edited {
    val ini = IniDocument.parse(text)
    if (!ini.isValid) return Edited.NotPossible
    return when (edit) {
        DocumentEdit.Format -> Edited.Text(ini.formatted())
        DocumentEdit.Minify -> Edited.Text(ini.minified(), commentsRemoved = ini.comments.size)
        DocumentEdit.SortKeys -> Edited.Text(ini.formatted(sortKeys = true))
    }
}
