package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.query.QueryErrorCode
import com.developersancho.formatico.feature.workspace.strings.QueryStrings

/** Why a query is none, as the query's field says it. */
internal fun QueryStrings.errorOf(code: QueryErrorCode): String = when (code) {
    QueryErrorCode.Empty -> errorEmpty
    QueryErrorCode.UnexpectedEnd -> errorUnexpectedEnd
    QueryErrorCode.UnexpectedCharacter -> errorUnexpectedCharacter
    QueryErrorCode.MissingRoot -> errorMissingRoot
    QueryErrorCode.UnclosedString -> errorUnclosedString
    QueryErrorCode.InvalidEscape -> errorInvalidEscape
    QueryErrorCode.InvalidNumber -> errorInvalidNumber
    QueryErrorCode.IntegerOutOfRange -> errorIntegerOutOfRange
    QueryErrorCode.NotSingular -> errorNotSingular
    QueryErrorCode.WrongType -> errorWrongType
    QueryErrorCode.UnknownFunction -> errorUnknownFunction
    QueryErrorCode.WrongArgumentCount -> errorWrongArgumentCount
    QueryErrorCode.UnknownAxis -> errorUnknownAxis
    QueryErrorCode.Unsupported -> errorUnsupported
    QueryErrorCode.TooDeep -> errorTooDeep
}
