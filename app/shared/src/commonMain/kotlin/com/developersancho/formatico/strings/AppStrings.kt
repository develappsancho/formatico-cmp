package com.developersancho.formatico.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.developersancho.formatico.core.designsystem.inLanguage
import com.developersancho.formatico.core.designsystem.inTheLanguage

/**
 * The words of the app's bar, its tabs and its palette.
 *
 * In English, [AppStringsEnglish], and in Turkish, [AppStringsTurkish]. A sentence that holds values is a
 * function of them, in their order; one whose form follows a number takes it first, as `count`.
 */
internal interface AppStrings {
    val appName: String
    val newTab: String
    val searchOrRunACommand: String
    val useDarkTheme: String
    val useLightTheme: String
    val settings: String
    val untitled: String

    /** "{first}, one of {second} open documents. Switch documents" */
    fun switchDocuments(first: String, second: Int): String

    /** "New tab, beside {first} open documents. Switch documents", the form [count] chooses */
    fun switchDocumentsFromNewTab(count: Int, first: Int): String

    /** "{first} open documents", the form [count] chooses */
    fun openDocuments(count: Int, first: Int): String

    val palettePlaceholder: String
    val paletteEmpty: String
    val paletteNewDocument: String
    val paletteOpenFile: String
    val paletteOpenAddress: String
    val palettePaste: String
}

/** [AppStrings] in the theme's language. */
internal val appStrings: AppStrings
    @Composable
    @ReadOnlyComposable
    get() = inTheLanguage(AppStringsEnglish, AppStringsTurkish)

/** [AppStrings] in the language whose tag is [language], away from a screen. */
internal fun appStringsIn(language: String): AppStrings = inLanguage(language, AppStringsEnglish, AppStringsTurkish)
