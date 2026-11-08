package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.Problem
import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.RepairKind
import com.developersancho.formatico.core.format.Repairs
import com.developersancho.formatico.feature.workspace.strings.ProblemStrings
import com.developersancho.formatico.feature.workspace.strings.problemStrings

/**
 * What [problem] in [text], read as [format], says to the reader: what is wrong, said as what to do about it, as the
 * boards word it: "Add a comma after the value on line 9."
 */
@Composable
internal fun problemMessage(problem: Problem, text: String, format: Format): String {
    ownMessages(format)[problem.code]?.let { return it(problemStrings) }
    return when (problem.code) {
        ProblemCode.XmlUnclosedElement -> problemStrings.xmlUnclosedElement(xmlName(problem, text))
        else -> jsonMessage(problem, text, format)
    }
}

/** What [problem] says in JSON's words, or in words that say nothing of the format. */
@Composable
private fun jsonMessage(problem: Problem, text: String, format: Format): String = when (problem.code) {
    ProblemCode.CommentNotAllowed -> problemStrings.commentNotAllowed(format.displayName)

    // The comma goes after the value before the problem, which is on its own line as often as not.
    ProblemCode.ExpectedCommaOrEnd -> problemStrings.expectedCommaOrEnd(lineBefore(problem, text, format))

    ProblemCode.UnquotedPropertyName -> {
        problemStrings.unquotedPropertyName(text.substring(problem.range.start, problem.range.end))
    }

    ProblemCode.InvalidEscape -> problemStrings.invalidEscape(format.displayName)

    ProblemCode.SingleQuotedString -> problemStrings.singleQuotedString(format.displayName)

    ProblemCode.InvalidNumber -> problemStrings.invalidNumber(format.displayName)

    else -> plainMessages.getValue(problem.code)(problemStrings)
}

/** The problems whose words say nothing of the document. */
private val plainMessages: Map<ProblemCode, (ProblemStrings) -> String> = mapOf(
    ProblemCode.TrailingComma to ProblemStrings::trailingComma,
    ProblemCode.UnexpectedCharacter to ProblemStrings::unexpectedCharacter,
    ProblemCode.UnexpectedToken to ProblemStrings::unexpectedToken,
    ProblemCode.UnterminatedString to ProblemStrings::unterminatedString,
    ProblemCode.UnterminatedComment to ProblemStrings::unterminatedComment,
    ProblemCode.ControlCharacterInString to ProblemStrings::controlCharacterInString,
    ProblemCode.ExpectedValue to ProblemStrings::expectedValue,
    ProblemCode.ExpectedPropertyName to ProblemStrings::expectedPropertyName,
    ProblemCode.ExpectedColon to ProblemStrings::expectedColon,
    ProblemCode.Unclosed to ProblemStrings::unclosed,
    ProblemCode.TrailingContent to ProblemStrings::trailingContent,
    ProblemCode.TooDeep to ProblemStrings::tooDeep,
    ProblemCode.DuplicateKey to ProblemStrings::duplicateKey,
    ProblemCode.YamlSyntax to ProblemStrings::yamlSyntax,
    ProblemCode.UndefinedAlias to ProblemStrings::undefinedAlias,
    ProblemCode.UnclosedQuote to ProblemStrings::unclosedQuote,
    ProblemCode.TextAfterQuote to ProblemStrings::textAfterQuote,
    ProblemCode.StrayQuote to ProblemStrings::strayQuote,
    ProblemCode.FieldCount to ProblemStrings::fieldCount,
    ProblemCode.YamlColonInValue to ProblemStrings::yamlColonInValue,
    ProblemCode.YamlTab to ProblemStrings::yamlTab,
    ProblemCode.YamlIndentation to ProblemStrings::yamlIndentation,
    ProblemCode.YamlUnclosedFlow to ProblemStrings::yamlUnclosedFlow,
    ProblemCode.IniUnclosedSection to ProblemStrings::iniUnclosedSection,
    ProblemCode.IniDuplicateSection to ProblemStrings::iniDuplicateSection,
    ProblemCode.XmlMismatchedEndTag to ProblemStrings::xmlMismatchedEndTag,
    ProblemCode.XmlUnexpectedEndTag to ProblemStrings::xmlUnexpectedEndTag,
    ProblemCode.XmlUnterminatedTag to ProblemStrings::xmlUnterminatedTag,
    ProblemCode.XmlDuplicateAttribute to ProblemStrings::xmlDuplicateAttribute,
    ProblemCode.XmlUnquotedAttribute to ProblemStrings::xmlUnquotedAttribute,
    ProblemCode.XmlExpectedAttributeValue to ProblemStrings::xmlExpectedAttributeValue,
    ProblemCode.XmlInvalidReference to ProblemStrings::xmlInvalidReference,
    ProblemCode.XmlUnterminatedCdata to ProblemStrings::xmlUnterminatedCdata,
    ProblemCode.XmlUnterminatedInstruction to ProblemStrings::xmlUnterminatedInstruction,
    ProblemCode.XmlMisplacedDeclaration to ProblemStrings::xmlMisplacedDeclaration,
    ProblemCode.XmlSecondRoot to ProblemStrings::xmlSecondRoot,
    ProblemCode.XmlTextOutsideRoot to ProblemStrings::xmlTextOutsideRoot,
    ProblemCode.XmlNoRoot to ProblemStrings::xmlNoRoot,
    ProblemCode.XmlHyphensInComment to ProblemStrings::xmlHyphensInComment,
    ProblemCode.TomlExpectedEquals to ProblemStrings::tomlExpectedEquals,
    ProblemCode.TomlTableRedefined to ProblemStrings::tomlTableRedefined,
    ProblemCode.TomlInvalidDateTime to ProblemStrings::tomlInvalidDateTime,
    ProblemCode.TomlInlineTableLineBreak to ProblemStrings::tomlInlineTableLineBreak
)

/**
 * The words a problem has in [format] alone, where a code it shares with JSON says something else there: a comment in
 * XML is closed with `-->`, not as JSON's is.
 */
private fun ownMessages(format: Format): Map<ProblemCode, (ProblemStrings) -> String> = when (format) {
    Format.Xml -> XmlMessages
    Format.Toml -> TomlMessages
    Format.Ini -> IniMessages
    else -> emptyMap()
}

private val TomlMessages: Map<ProblemCode, (ProblemStrings) -> String> = mapOf(
    ProblemCode.DuplicateKey to ProblemStrings::tomlDuplicateKey,
    ProblemCode.TrailingContent to ProblemStrings::tomlTrailingContent
)

private val XmlMessages: Map<ProblemCode, (ProblemStrings) -> String> = mapOf(
    ProblemCode.UnexpectedCharacter to ProblemStrings::xmlUnexpectedCharacter,
    ProblemCode.UnterminatedComment to ProblemStrings::xmlUnterminatedComment,
    ProblemCode.UnterminatedString to ProblemStrings::xmlUnterminatedString,
    ProblemCode.ExpectedValue to ProblemStrings::xmlNoRoot
)

private val IniMessages: Map<ProblemCode, (ProblemStrings) -> String> = mapOf(
    ProblemCode.TrailingContent to ProblemStrings::iniTrailingContent
)

/** The name of the element whose start tag [problem] is about: `item` for `<item id="1">`. */
private fun xmlName(problem: Problem, text: String): String =
    text.substring(problem.range.start + 1, problem.range.end).takeWhile { !it.isWhitespace() && it != '>' && it != '/' }

/** The line, from 1, the repair of [problem] writes on: where the comma goes, after the value before. */
private fun lineBefore(problem: Problem, text: String, format: Format): Int {
    val at = Repairs.of(problem, text, format)?.edits?.firstOrNull()?.range?.start ?: return problem.start.line
    return text.substring(0, at).count { it == '\n' } + 1
}

/** What a repair's button says: "Add the comma". */
@Composable
internal fun repairLabel(kind: RepairKind): String = when (kind) {
    RepairKind.RemoveComment -> problemStrings.repairRemoveComment
    RepairKind.AddComma -> problemStrings.repairAddComma
    RepairKind.QuoteKey -> problemStrings.repairQuoteKey
    RepairKind.RemoveComma -> problemStrings.repairRemoveComma
    RepairKind.UseDoubleQuotes -> problemStrings.repairUseDoubleQuotes
    RepairKind.Close -> problemStrings.repairClose
    RepairKind.AddColon -> problemStrings.repairAddColon
    RepairKind.Remove -> problemStrings.repairRemove
    RepairKind.Escape -> problemStrings.repairEscape
    RepairKind.FixNumber -> problemStrings.repairFixNumber
    RepairKind.DoubleTheQuote -> problemStrings.repairDoubleTheQuote
    RepairKind.QuoteValue -> problemStrings.repairQuoteValue
    RepairKind.UseSpaces -> problemStrings.repairUseSpaces
}
