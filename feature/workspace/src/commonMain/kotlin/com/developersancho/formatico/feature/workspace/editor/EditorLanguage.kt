package com.developersancho.formatico.feature.workspace.editor

import androidx.compose.runtime.Composable
import com.developersancho.formatico.core.designsystem.FormaticoTheme
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.text.Indentation
import com.pantrixui.components.data.codeeditor.CodeEditorColors
import com.pantrixui.components.data.codeeditor.CodeHighlighter
import com.pantrixui.components.data.codeeditor.CodeSyntax

/** What the editor knows of a format: what colours it, and its brackets, quotes and comments. */
internal class EditorLanguage(val highlighter: CodeHighlighter<*>?, val syntax: CodeSyntax)

private val Json = EditorLanguage(JsonHighlighter, CodeSyntax(quotes = "\""))
private val JsonWithComments = EditorLanguage(
    JsonHighlighter,
    CodeSyntax(quotes = "\"", lineComment = "//", blockCommentStart = "/*", blockCommentEnd = "*/")
)
private val Json5 = EditorLanguage(
    JsonHighlighter,
    CodeSyntax(quotes = "\"'", lineComment = "//", blockCommentStart = "/*", blockCommentEnd = "*/")
)
private val Yaml = EditorLanguage(YamlHighlighter, CodeSyntax(quotes = "\"'", lineComment = "#"))
private val Toml = EditorLanguage(TomlHighlighter, CodeSyntax(quotes = "\"'", lineComment = "#"))
private val Ini =
    EditorLanguage(SettingsHighlighter(comments = ";#", separators = "="), CodeSyntax(brackets = "[]", quotes = "\"", lineComment = ";"))
private val Properties = EditorLanguage(PropertiesHighlighter, CodeSyntax(brackets = "", quotes = "", lineComment = "#"))
private val TableSyntax = CodeSyntax(brackets = "", quotes = "\"")
private val Csv = EditorLanguage(TableHighlighter(','), TableSyntax)
private val Tsv = EditorLanguage(TableHighlighter('\t'), TableSyntax)

/** A CSV and a TSV whose first row is a record, written as the other records are. */
private val CsvRecords = EditorLanguage(TableHighlighter(',', header = false), TableSyntax)
private val TsvRecords = EditorLanguage(TableHighlighter('\t', header = false), TableSyntax)
private val Xml = EditorLanguage(
    XmlHighlighter,
    CodeSyntax(brackets = "", quotes = "\"'", blockCommentStart = "<!--", blockCommentEnd = "-->")
)

/** Plain text: written in one colour, and no bracket or quote closed as it is typed. */
private val PlainText = EditorLanguage(null, CodeSyntax(brackets = "", quotes = ""))

/**
 * What the editor knows of [format], or of plain text where it is null; a CSV's or a TSV's first row in the key's colour
 * where it is the [firstRowHeader].
 */
internal fun editorLanguage(format: Format?, firstRowHeader: Boolean = true): EditorLanguage = when (format) {
    Format.Json, Format.Ndjson -> Json
    Format.Jsonc -> JsonWithComments
    Format.Json5 -> Json5
    Format.Yaml -> Yaml
    Format.Toml -> Toml
    Format.Ini -> Ini
    Format.Properties -> Properties
    Format.Csv -> if (firstRowHeader) Csv else CsvRecords
    Format.Tsv -> if (firstRowHeader) Tsv else TsvRecords
    Format.Xml -> Xml
    null -> PlainText
}

/** One step of indent as the document is indented: its own spaces or a tab, or two spaces where it shows none. */
internal fun editorIndent(indentation: Indentation?): String = when (indentation) {
    is Indentation.Spaces -> " ".repeat(indentation.count)
    Indentation.Tabs -> "\t"
    null -> "  "
}

/**
 * The editor in Formatico's syntax colours: keys, strings, numbers, `true`, `false` and `null`, signs, comments; and in
 * the code Types writes, types as keys are, annotations as numbers are, keywords as `true`, `false` and `null` are.
 */
@Composable
internal fun editorColors(): CodeEditorColors {
    val syntax = FormaticoTheme.syntax
    return CodeEditorColors(
        property = syntax.key,
        type = syntax.key,
        annotation = syntax.number,
        tag = syntax.key,
        string = syntax.string,
        number = syntax.number,
        keyword = syntax.literal,
        punctuation = syntax.punctuation,
        comment = syntax.comment,
        currentLine = syntax.currentLine
    )
}
