package com.developersancho.formatico.feature.workspace.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.developersancho.formatico.core.designsystem.inLanguage
import com.developersancho.formatico.core.designsystem.inTheLanguage

/**
 * The words of an invalid document's problems and their repairs.
 *
 * In English, [ProblemStringsEnglish], and in Turkish, [ProblemStringsTurkish]. A sentence that holds values is a
 * function of them, in their order; one whose form follows a number takes it first, as `count`.
 */
internal interface ProblemStrings {
    /** "Take out the comment: {first} has none. Or read the document as JSONC." */
    fun commentNotAllowed(first: String): String

    /** "Add a comma after the value on line {first}." */
    fun expectedCommaOrEnd(first: Int): String

    /** "Put this key in double quotes: "{first}"." */
    fun unquotedPropertyName(first: String): String

    val trailingComma: String
    val unexpectedCharacter: String
    val unexpectedToken: String
    val unterminatedString: String
    val unterminatedComment: String

    /** "Write the backslash as two, or as an escape {first} knows." */
    fun invalidEscape(first: String): String

    val controlCharacterInString: String

    /** "Put the string in double quotes: {first} has no single ones." */
    fun singleQuotedString(first: String): String

    /** "Write the number as {first} spells it." */
    fun invalidNumber(first: String): String

    val expectedValue: String
    val expectedPropertyName: String
    val expectedColon: String
    val unclosed: String
    val trailingContent: String
    val tooDeep: String
    val duplicateKey: String
    val yamlSyntax: String
    val undefinedAlias: String
    val unclosedQuote: String
    val textAfterQuote: String
    val strayQuote: String
    val fieldCount: String
    val repairRemoveComment: String
    val repairAddComma: String
    val repairQuoteKey: String
    val repairRemoveComma: String
    val repairUseDoubleQuotes: String
    val repairClose: String
    val repairAddColon: String
    val repairRemove: String
    val repairEscape: String
    val repairFixNumber: String
    val repairDoubleTheQuote: String
    val yamlColonInValue: String
    val yamlTab: String
    val yamlIndentation: String
    val repairQuoteValue: String
    val repairUseSpaces: String
    val yamlUnclosedFlow: String
    val iniUnclosedSection: String
    val iniDuplicateSection: String
    val iniTrailingContent: String

    /** "Close <{first}>: its end tag, </{first}>, is missing." */
    fun xmlUnclosedElement(first: String): String

    val xmlMismatchedEndTag: String
    val xmlUnexpectedEndTag: String
    val xmlUnterminatedTag: String
    val xmlDuplicateAttribute: String
    val xmlUnquotedAttribute: String
    val xmlExpectedAttributeValue: String
    val xmlInvalidReference: String
    val xmlUnterminatedCdata: String
    val xmlUnterminatedInstruction: String
    val xmlMisplacedDeclaration: String
    val xmlSecondRoot: String
    val xmlTextOutsideRoot: String
    val xmlNoRoot: String
    val xmlHyphensInComment: String
    val xmlUnexpectedCharacter: String
    val xmlUnterminatedComment: String
    val xmlUnterminatedString: String
    val tomlExpectedEquals: String
    val tomlTableRedefined: String
    val tomlInvalidDateTime: String
    val tomlInlineTableLineBreak: String
    val tomlDuplicateKey: String
    val tomlTrailingContent: String
}

/** [ProblemStrings] in the theme's language. */
internal val problemStrings: ProblemStrings
    @Composable
    @ReadOnlyComposable
    get() = inTheLanguage(ProblemStringsEnglish, ProblemStringsTurkish)

/** [ProblemStrings] in the language whose tag is [language], away from a screen. */
internal fun problemStringsIn(language: String): ProblemStrings = inLanguage(language, ProblemStringsEnglish, ProblemStringsTurkish)
