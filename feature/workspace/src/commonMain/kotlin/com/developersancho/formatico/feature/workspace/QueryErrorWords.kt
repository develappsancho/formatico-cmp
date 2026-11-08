package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.query.QueryErrorCode
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.query_error_empty
import formatico.feature.workspace.generated.resources.query_error_integer_out_of_range
import formatico.feature.workspace.generated.resources.query_error_invalid_escape
import formatico.feature.workspace.generated.resources.query_error_invalid_number
import formatico.feature.workspace.generated.resources.query_error_missing_root
import formatico.feature.workspace.generated.resources.query_error_not_singular
import formatico.feature.workspace.generated.resources.query_error_too_deep
import formatico.feature.workspace.generated.resources.query_error_unclosed_string
import formatico.feature.workspace.generated.resources.query_error_unexpected_character
import formatico.feature.workspace.generated.resources.query_error_unexpected_end
import formatico.feature.workspace.generated.resources.query_error_unknown_axis
import formatico.feature.workspace.generated.resources.query_error_unknown_function
import formatico.feature.workspace.generated.resources.query_error_unsupported
import formatico.feature.workspace.generated.resources.query_error_wrong_argument_count
import formatico.feature.workspace.generated.resources.query_error_wrong_type
import org.jetbrains.compose.resources.StringResource

/** Why a query is none, as the query's field says it. */
internal fun queryErrorWords(code: QueryErrorCode): StringResource = when (code) {
    QueryErrorCode.Empty -> Res.string.query_error_empty
    QueryErrorCode.UnexpectedEnd -> Res.string.query_error_unexpected_end
    QueryErrorCode.UnexpectedCharacter -> Res.string.query_error_unexpected_character
    QueryErrorCode.MissingRoot -> Res.string.query_error_missing_root
    QueryErrorCode.UnclosedString -> Res.string.query_error_unclosed_string
    QueryErrorCode.InvalidEscape -> Res.string.query_error_invalid_escape
    QueryErrorCode.InvalidNumber -> Res.string.query_error_invalid_number
    QueryErrorCode.IntegerOutOfRange -> Res.string.query_error_integer_out_of_range
    QueryErrorCode.NotSingular -> Res.string.query_error_not_singular
    QueryErrorCode.WrongType -> Res.string.query_error_wrong_type
    QueryErrorCode.UnknownFunction -> Res.string.query_error_unknown_function
    QueryErrorCode.WrongArgumentCount -> Res.string.query_error_wrong_argument_count
    QueryErrorCode.UnknownAxis -> Res.string.query_error_unknown_axis
    QueryErrorCode.Unsupported -> Res.string.query_error_unsupported
    QueryErrorCode.TooDeep -> Res.string.query_error_too_deep
}
