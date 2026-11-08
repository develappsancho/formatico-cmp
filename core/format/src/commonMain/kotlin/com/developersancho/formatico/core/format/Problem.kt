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

    /** A string, or an XML attribute's value, whose closing quote is not where it should be: on its line, in JSON. */
    UnterminatedString,

    /** A comment that is never closed: a block comment of JSON's, or an XML comment. */
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

    /** A YAML value with a colon and a space in it, which start a mapping where the value stands. */
    YamlColonInValue,

    /** A tab where YAML indents, which only spaces do. */
    YamlTab,

    /** A YAML line indented as none of the lines around it. */
    YamlIndentation,

    /** A YAML flow, `[` or `{`, never closed: the range runs from its bracket to where its closer belongs. */
    YamlUnclosedFlow,

    /** A YAML alias whose anchor is not given before it. */
    UndefinedAlias,

    /** A CSV field whose quote is never closed. */
    UnclosedQuote,

    /** Text after a CSV field's closing quote, before the delimiter. */
    TextAfterQuote,

    /** A quote inside a CSV field that is not quoted. */
    StrayQuote,

    /** A CSV record with another number of fields than the header. */
    FieldCount,

    /** An INI section's header with no `]` closing it. */
    IniUnclosedSection,

    /** An INI section given a second header: most readers take its keys into the first. */
    IniDuplicateSection,

    /** An XML element that is never closed: the range is its start tag. */
    XmlUnclosedElement,

    /** An XML end tag of another name than the element it closes: the range is the end tag's name. */
    XmlMismatchedEndTag,

    /** An XML end tag no element is open for. */
    XmlUnexpectedEndTag,

    /** An XML tag, or a DOCTYPE, with no `>` closing it. */
    XmlUnterminatedTag,

    /** An attribute given twice in one XML tag: the range is the later one's name. */
    XmlDuplicateAttribute,

    /** An XML attribute's value in no quotes. */
    XmlUnquotedAttribute,

    /** An XML attribute with no `=` and no value after its name. */
    XmlExpectedAttributeValue,

    /**
     * An `&` in XML that starts no reference, a reference to a character XML does not have, or an entity other than the
     * five XML has, `&amp;` `&lt;` `&gt;` `&quot;` `&apos;`, in a document with no DOCTYPE to declare it.
     */
    XmlInvalidReference,

    /** An XML CDATA section with no `]]>` closing it. */
    XmlUnterminatedCdata,

    /** An XML processing instruction with no `?>` closing it. */
    XmlUnterminatedInstruction,

    /** The XML declaration, `<?xml …?>`, anywhere but at the start of the text, or written in capitals. */
    XmlMisplacedDeclaration,

    /** An XML element after the document's root, which is the only element at the top level. */
    XmlSecondRoot,

    /** Text that is not whitespace before or after an XML document's root. */
    XmlTextOutsideRoot,

    /** An XML text with no element in it: comments only, for instance. */
    XmlNoRoot,

    /** Two hyphens inside an XML comment, which XML does not allow: `<!-- a -- b -->`, or a comment ending in `--->`. */
    XmlHyphensInComment,

    /** A TOML key with no `=` after it. */
    TomlExpectedEquals,

    /**
     * A TOML table defined again: a table, or a key made a table, given a header or keys once more, or a key given a value
     * where a table is already.
     */
    TomlTableRedefined,

    /** A TOML date or time that is none: `2026-02-30`, `25:00:00`, or one with no seconds, `07:32`. */
    TomlInvalidDateTime,

    /** A TOML inline table over more than one line, which TOML 1.0 keeps on one. */
    TomlInlineTableLineBreak
}
