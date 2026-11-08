package com.developersancho.formatico.feature.workspace.editor

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.types.TypeLanguage
import com.pantrixui.components.data.codeblock.CodeTokenKind
import com.pantrixui.components.data.codeeditor.CodeHighlight
import com.pantrixui.components.data.codeeditor.CodeHighlighter
import com.pantrixui.components.data.codeeditor.CodeSyntax

/** What a line of code hands the next: whether it ends inside a block comment. */
internal enum class CodeLine { Code, InComment }

/**
 * The code Types writes, a line at a time, in one of its languages: the language's [keywords], the names of types (a
 * name in capitals, or one of its [typeWords]), annotations (`@Serializable`), strings in double quotes and, where
 * [backquoted] says so, Go's tags between backquotes; numbers, signs, and line and block comments. Where [namesFirst]
 * says so, as in Go, whose fields' names are in capitals too, the first word of a line that is no keyword is a name and
 * not a type. Its colours are the Types boards': keywords as `true`, `false` and `null` are, types as keys are,
 * annotations as numbers are.
 */
internal class TypesHighlighter(
    private val keywords: Set<String>,
    private val typeWords: Set<String> = emptySet(),
    private val backquoted: Boolean = false,
    private val namesFirst: Boolean = false
) : CodeHighlighter<CodeLine> {
    override val initialState = CodeLine.Code

    override fun highlight(line: String, state: CodeLine): CodeHighlight<CodeLine> {
        val scan = LineScanner(line)
        var inComment = state == CodeLine.InComment
        while (!scan.done) {
            if (inComment) {
                inComment = blockComment(scan)
            } else {
                scan.skipSpace()
                if (!scan.done) inComment = token(scan)
            }
        }
        return CodeHighlight(scan.tokens(), if (inComment) CodeLine.InComment else CodeLine.Code)
    }

    /** The run the scanner stands at: whether it opens a block comment that goes on past the line. */
    private fun token(scan: LineScanner): Boolean {
        val char = scan.peek() ?: return false
        when {
            scan.startsWith("//") -> scan.take(scan.line.length, CodeTokenKind.Comment)
            scan.startsWith("/*") -> return blockComment(scan)
            char == '"' -> scan.quoted()
            char == '`' && backquoted -> scan.quoted()
            char == '@' -> scan.take(wordEnd(scan, scan.at + 1), CodeTokenKind.Annotation)
            char.isDigit() -> scan.takeWhile(CodeTokenKind.Number) { it.isLetterOrDigit() || it == '.' || it == '_' }
            char.isLetter() || char == '_' || char == '`' -> word(scan)
            else -> scan.take(scan.at + 1, CodeTokenKind.Punctuation)
        }
        return false
    }

    /** A block comment from where the scanner is, to its `*` and `/` or the line's end: whether it goes on after the line. */
    private fun blockComment(scan: LineScanner): Boolean {
        val close = scan.line.indexOf(BLOCK_END, if (scan.startsWith("/*")) scan.at + BLOCK_END.length else scan.at)
        scan.take(if (close < 0) scan.line.length else close + BLOCK_END.length, CodeTokenKind.Comment)
        return close < 0
    }

    /** A word: a keyword, a type's name, or a name, which keeps the text's colour. A name in backquotes is a name. */
    private fun word(scan: LineScanner) {
        if (scan.peek() == '`') {
            scan.take(scan.quotedEnd(scan.at), CodeTokenKind.Plain)
            return
        }
        val end = wordEnd(scan, scan.at)
        val word = scan.line.substring(scan.at, end)
        val first = namesFirst && scan.line.substring(0, scan.at).isBlank()
        val kind = when {
            word in keywords -> CodeTokenKind.Keyword
            first -> CodeTokenKind.Plain
            word in typeWords || word.first().isUpperCase() -> CodeTokenKind.Type
            else -> CodeTokenKind.Plain
        }
        scan.take(end, kind)
    }

    private fun wordEnd(scan: LineScanner, from: Int): Int {
        var end = from
        while (end < scan.line.length && (scan.line[end].isLetterOrDigit() || scan.line[end] == '_')) end++
        return end
    }

    private companion object {
        const val BLOCK_END = "*/"
    }
}

private val KotlinTypes = TypesHighlighter(
    keywords = setOf(
        "import",
        "package",
        "data",
        "class",
        "val",
        "var",
        "fun",
        "object",
        "interface",
        "enum",
        "sealed",
        "typealias",
        "null",
        "true",
        "false",
        "private",
        "internal",
        "public",
        "override",
        "return"
    )
)

private val TypeScriptTypes = TypesHighlighter(
    keywords = setOf("export", "interface", "type", "import", "from", "readonly", "extends", "enum", "const", "null", "true", "false"),
    typeWords = setOf("string", "number", "boolean", "unknown", "any", "never", "void", "object", "bigint")
)

private val SwiftTypes = TypesHighlighter(
    keywords = setOf(
        "import",
        "struct",
        "class",
        "enum",
        "case",
        "let",
        "var",
        "func",
        "init",
        "protocol",
        "extension",
        "nil",
        "true",
        "false",
        "static",
        "private",
        "public",
        "internal",
        "throws",
        "throw",
        "try",
        "self",
        "return",
        "switch",
        "default",
        "if",
        "else",
        "guard",
        "do",
        "catch",
        "where",
        "in",
        "is",
        "as"
    )
)

private val GoTypes = TypesHighlighter(
    keywords = setOf("package", "import", "type", "struct", "interface", "func", "var", "const", "map", "nil", "true", "false", "return"),
    typeWords = setOf(
        "string",
        "bool",
        "int",
        "int8",
        "int16",
        "int32",
        "int64",
        "uint",
        "uint8",
        "uint16",
        "uint32",
        "uint64",
        "float32",
        "float64",
        "byte",
        "rune",
        "any",
        "error"
    ),
    backquoted = true,
    namesFirst = true
)

/**
 * What the editor knows of the code Types writes in [language]: what colours it, and its brackets, quotes and comments;
 * a JSON Schema is JSON.
 */
internal fun editorLanguage(language: TypeLanguage): EditorLanguage {
    val comments = CodeSyntax(quotes = "\"", lineComment = "//", blockCommentStart = "/*", blockCommentEnd = "*/")
    return when (language) {
        TypeLanguage.Kotlin -> EditorLanguage(KotlinTypes, comments)

        TypeLanguage.TypeScript -> EditorLanguage(TypeScriptTypes, comments)

        TypeLanguage.Swift -> EditorLanguage(SwiftTypes, comments)

        TypeLanguage.JsonSchema -> editorLanguage(Format.Json)

        TypeLanguage.Go -> EditorLanguage(
            GoTypes,
            CodeSyntax(quotes = "\"`", lineComment = "//", blockCommentStart = "/*", blockCommentEnd = "*/")
        )
    }
}
