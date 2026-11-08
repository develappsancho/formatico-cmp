package com.developersancho.formatico.core.format

import com.developersancho.formatico.core.format.text.TextPosition
import com.developersancho.formatico.core.format.text.TextRange

/**
 * Something wrong in a document, where it is.
 *
 * The problem carries no words: the app says [code] in the reader's language.
 *
 * @property code what is wrong.
 * @property severity whether the document is invalid because of it, or only questionable.
 * @property range the text it is about.
 * @property start where [range] starts, as a line and a column.
 * @property end where [range] ends.
 */
data class Problem(
    val code: ProblemCode,
    val severity: Severity,
    val range: TextRange,
    val start: TextPosition,
    val end: TextPosition
)

/** How serious a [Problem] is. */
enum class Severity {
    /** The document is not valid in its format. */
    Error,

    /** The document is valid, but likely not what was meant: a key given twice. */
    Warning
}

/** What a [Problem] is. */
enum class ProblemCode {
    /** A character no token of the format starts with. */
    UnexpectedCharacter,

    /** A token that cannot stand where it is. */
    UnexpectedToken,

    /** A string with no closing quote on its line. */
    UnterminatedString,

    /** A block comment with no `*` and `/` closing it. */
    UnterminatedComment,

    /** A backslash followed by what is not an escape. */
    InvalidEscape,

    /** A control character, a tab or a line break, written raw inside a string. */
    ControlCharacterInString,

    /** A string in single quotes, where the format has only double ones. */
    SingleQuotedString,

    /** A number the format does not spell that way: `01`, `1.`, `.5`, `+1`, `0x1F`. */
    InvalidNumber,

    /** A comment, where the format has none. */
    CommentNotAllowed,

    /** A comma after the last item, where the format does not allow one. */
    TrailingComma,

    /** A value was expected here. */
    ExpectedValue,

    /** A property's name was expected here. */
    ExpectedPropertyName,

    /** A key in no quotes, where the format asks for them. */
    UnquotedPropertyName,

    /** A colon between a property's name and its value was expected here. */
    ExpectedColon,

    /** A comma, or the end of the object or the array, was expected here. */
    ExpectedCommaOrEnd,

    /** An object or an array that is never closed. */
    Unclosed,

    /** Text after the document's value. */
    TrailingContent,

    /** Values nested deeper than Formatico reads. */
    TooDeep,

    /** A key given twice in one object: the later one wins in most readers. In YAML, an error. */
    DuplicateKey,

    /** YAML that cannot be read: what is wrong, the reader of YAML says in English, and it is not kept. */
    YamlSyntax,

    /** A YAML alias whose anchor is not given before it. */
    UndefinedAlias
}
