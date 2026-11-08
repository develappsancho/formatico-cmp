package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.Problem
import com.developersancho.formatico.core.format.ProblemCode
import com.developersancho.formatico.core.format.RepairKind
import com.developersancho.formatico.core.format.Repairs
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.problem_comment_not_allowed
import formatico.feature.workspace.generated.resources.problem_control_character_in_string
import formatico.feature.workspace.generated.resources.problem_duplicate_key
import formatico.feature.workspace.generated.resources.problem_expected_colon
import formatico.feature.workspace.generated.resources.problem_expected_comma_or_end
import formatico.feature.workspace.generated.resources.problem_expected_property_name
import formatico.feature.workspace.generated.resources.problem_expected_value
import formatico.feature.workspace.generated.resources.problem_field_count
import formatico.feature.workspace.generated.resources.problem_invalid_escape
import formatico.feature.workspace.generated.resources.problem_invalid_number
import formatico.feature.workspace.generated.resources.problem_single_quoted_string
import formatico.feature.workspace.generated.resources.problem_stray_quote
import formatico.feature.workspace.generated.resources.problem_text_after_quote
import formatico.feature.workspace.generated.resources.problem_too_deep
import formatico.feature.workspace.generated.resources.problem_trailing_comma
import formatico.feature.workspace.generated.resources.problem_trailing_content
import formatico.feature.workspace.generated.resources.problem_unclosed
import formatico.feature.workspace.generated.resources.problem_unclosed_quote
import formatico.feature.workspace.generated.resources.problem_undefined_alias
import formatico.feature.workspace.generated.resources.problem_unexpected_character
import formatico.feature.workspace.generated.resources.problem_unexpected_token
import formatico.feature.workspace.generated.resources.problem_unquoted_property_name
import formatico.feature.workspace.generated.resources.problem_unterminated_comment
import formatico.feature.workspace.generated.resources.problem_unterminated_string
import formatico.feature.workspace.generated.resources.problem_yaml_colon_in_value
import formatico.feature.workspace.generated.resources.problem_yaml_indentation
import formatico.feature.workspace.generated.resources.problem_yaml_syntax
import formatico.feature.workspace.generated.resources.problem_yaml_tab
import formatico.feature.workspace.generated.resources.problem_yaml_unclosed_flow
import formatico.feature.workspace.generated.resources.repair_add_colon
import formatico.feature.workspace.generated.resources.repair_add_comma
import formatico.feature.workspace.generated.resources.repair_close
import formatico.feature.workspace.generated.resources.repair_double_the_quote
import formatico.feature.workspace.generated.resources.repair_escape
import formatico.feature.workspace.generated.resources.repair_fix_number
import formatico.feature.workspace.generated.resources.repair_quote_key
import formatico.feature.workspace.generated.resources.repair_quote_value
import formatico.feature.workspace.generated.resources.repair_remove
import formatico.feature.workspace.generated.resources.repair_remove_comma
import formatico.feature.workspace.generated.resources.repair_remove_comment
import formatico.feature.workspace.generated.resources.repair_use_double_quotes
import formatico.feature.workspace.generated.resources.repair_use_spaces
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * What [problem] in [text], read as [format], says to the reader: what is wrong, said as what to do about it, as the
 * boards word it: "Add a comma after the value on line 9."
 */
@Composable
internal fun problemMessage(problem: Problem, text: String, format: Format): String = when (problem.code) {
    ProblemCode.CommentNotAllowed -> stringResource(Res.string.problem_comment_not_allowed, format.displayName)

    // The comma goes after the value before the problem, which is on its own line as often as not.
    ProblemCode.ExpectedCommaOrEnd -> stringResource(Res.string.problem_expected_comma_or_end, lineBefore(problem, text))

    ProblemCode.UnquotedPropertyName -> {
        stringResource(Res.string.problem_unquoted_property_name, text.substring(problem.range.start, problem.range.end))
    }

    ProblemCode.InvalidEscape -> stringResource(Res.string.problem_invalid_escape, format.displayName)

    ProblemCode.SingleQuotedString -> stringResource(Res.string.problem_single_quoted_string, format.displayName)

    ProblemCode.InvalidNumber -> stringResource(Res.string.problem_invalid_number, format.displayName)

    else -> stringResource(plainMessages.getValue(problem.code))
}

/** The problems whose words say nothing of the document. */
private val plainMessages: Map<ProblemCode, StringResource> = mapOf(
    ProblemCode.TrailingComma to Res.string.problem_trailing_comma,
    ProblemCode.UnexpectedCharacter to Res.string.problem_unexpected_character,
    ProblemCode.UnexpectedToken to Res.string.problem_unexpected_token,
    ProblemCode.UnterminatedString to Res.string.problem_unterminated_string,
    ProblemCode.UnterminatedComment to Res.string.problem_unterminated_comment,
    ProblemCode.ControlCharacterInString to Res.string.problem_control_character_in_string,
    ProblemCode.ExpectedValue to Res.string.problem_expected_value,
    ProblemCode.ExpectedPropertyName to Res.string.problem_expected_property_name,
    ProblemCode.ExpectedColon to Res.string.problem_expected_colon,
    ProblemCode.Unclosed to Res.string.problem_unclosed,
    ProblemCode.TrailingContent to Res.string.problem_trailing_content,
    ProblemCode.TooDeep to Res.string.problem_too_deep,
    ProblemCode.DuplicateKey to Res.string.problem_duplicate_key,
    ProblemCode.YamlSyntax to Res.string.problem_yaml_syntax,
    ProblemCode.UndefinedAlias to Res.string.problem_undefined_alias,
    ProblemCode.UnclosedQuote to Res.string.problem_unclosed_quote,
    ProblemCode.TextAfterQuote to Res.string.problem_text_after_quote,
    ProblemCode.StrayQuote to Res.string.problem_stray_quote,
    ProblemCode.FieldCount to Res.string.problem_field_count,
    ProblemCode.YamlColonInValue to Res.string.problem_yaml_colon_in_value,
    ProblemCode.YamlTab to Res.string.problem_yaml_tab,
    ProblemCode.YamlIndentation to Res.string.problem_yaml_indentation,
    ProblemCode.YamlUnclosedFlow to Res.string.problem_yaml_unclosed_flow
)

/** The line, from 1, the repair of [problem] writes on: where the comma goes, after the value before. */
private fun lineBefore(problem: Problem, text: String): Int {
    val at = Repairs.of(problem, text)?.edits?.firstOrNull()?.range?.start ?: return problem.start.line
    return text.substring(0, at).count { it == '\n' } + 1
}

/** What a repair's button says: "Add the comma". */
@Composable
internal fun repairLabel(kind: RepairKind): String = stringResource(
    when (kind) {
        RepairKind.RemoveComment -> Res.string.repair_remove_comment
        RepairKind.AddComma -> Res.string.repair_add_comma
        RepairKind.QuoteKey -> Res.string.repair_quote_key
        RepairKind.RemoveComma -> Res.string.repair_remove_comma
        RepairKind.UseDoubleQuotes -> Res.string.repair_use_double_quotes
        RepairKind.Close -> Res.string.repair_close
        RepairKind.AddColon -> Res.string.repair_add_colon
        RepairKind.Remove -> Res.string.repair_remove
        RepairKind.Escape -> Res.string.repair_escape
        RepairKind.FixNumber -> Res.string.repair_fix_number
        RepairKind.DoubleTheQuote -> Res.string.repair_double_the_quote
        RepairKind.QuoteValue -> Res.string.repair_quote_value
        RepairKind.UseSpaces -> Res.string.repair_use_spaces
    }
)
