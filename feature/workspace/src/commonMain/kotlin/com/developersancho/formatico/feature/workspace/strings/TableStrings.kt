package com.developersancho.formatico.feature.workspace.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.developersancho.formatico.core.designsystem.inLanguage
import com.developersancho.formatico.core.designsystem.inTheLanguage

/**
 * The words of the table.
 *
 * In English, [TableStringsEnglish], and in Turkish, [TableStringsTurkish]. A sentence that holds values is a
 * function of them, in their order; one whose form follows a number takes it first, as `count`.
 */
internal interface TableStrings {
    val typeText: String
    val typeDate: String
    val typeNumber: String
    val typeBoolean: String
    val typeObject: String
    val typeArray: String
    val typeAlias: String
    val typeNull: String

    /** The checkbox in a CSV's or a TSV's columns' choice that reads its first row as the header. */
    val firstRowHeader: String

    /** What reading the first row as a record does, under the checkbox. */
    val firstRowHeaderOff: String

    /** The name of the column at [place], counted from 1, in a CSV or a TSV whose first row is no header. */
    fun columnAt(place: Int): String
}

/** [TableStrings] in the theme's language. */
internal val tableStrings: TableStrings
    @Composable
    @ReadOnlyComposable
    get() = inTheLanguage(TableStringsEnglish, TableStringsTurkish)

/** [TableStrings] in the language whose tag is [language], away from a screen. */
internal fun tableStringsIn(language: String): TableStrings = inLanguage(language, TableStringsEnglish, TableStringsTurkish)
