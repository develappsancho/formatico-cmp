package com.developersancho.formatico.feature.workspace.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.developersancho.formatico.core.designsystem.inLanguage
import com.developersancho.formatico.core.designsystem.inTheLanguage

/**
 * The words of Query.
 *
 * In English, [QueryStringsEnglish], and in Turkish, [QueryStringsTurkish]. A sentence that holds values is a
 * function of them, in their order; one whose form follows a number takes it first, as `count`.
 */
internal interface QueryStrings {
    val recent: String
    val languageName: String
    val askJsonpath: String
    val askXpath: String
    val jsonpathElsewhere: String
    val xpathElsewhere: String

    /** "{first}. {second}" */
    fun statusAndNote(first: String, second: String): String

    val unread: String
    val computed: String
    val noMatches: String

    /** "{first} matches", the form [count] chooses */
    fun matches(count: Int, first: Int): String

    val value: String

    /** "Match {first} of {second}" */
    fun matchOf(first: Int, second: Int): String

    /** "{first} (character {second})" */
    fun errorAt(first: String, second: Int): String

    val copyResults: String
    val openResults: String
    val chooseHint: String
    val seeInText: String
    val resultsLabel: String
    val resultsCopied: String
    val resultsCopyFailed: String
    val errorEmpty: String
    val errorUnexpectedEnd: String
    val errorUnexpectedCharacter: String
    val errorMissingRoot: String
    val errorUnclosedString: String
    val errorInvalidEscape: String
    val errorInvalidNumber: String
    val errorIntegerOutOfRange: String
    val errorNotSingular: String
    val errorWrongType: String
    val errorUnknownFunction: String
    val errorWrongArgumentCount: String
    val errorUnknownAxis: String
    val errorUnsupported: String
    val errorTooDeep: String
}

/** [QueryStrings] in the theme's language. */
internal val queryStrings: QueryStrings
    @Composable
    @ReadOnlyComposable
    get() = inTheLanguage(QueryStringsEnglish, QueryStringsTurkish)

/** [QueryStrings] in the language whose tag is [language], away from a screen. */
internal fun queryStringsIn(language: String): QueryStrings = inLanguage(language, QueryStringsEnglish, QueryStringsTurkish)
