package com.developersancho.formatico.feature.settings.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.developersancho.formatico.core.designsystem.inLanguage
import com.developersancho.formatico.core.designsystem.inTheLanguage

/**
 * The words of Settings.
 *
 * In English, [SettingsStringsEnglish], and in Turkish, [SettingsStringsTurkish]. A sentence that holds values is a
 * function of them, in their order; one whose form follows a number takes it first, as `count`.
 */
internal interface SettingsStrings {
    val title: String
    val general: String
    val editor: String
    val formats: String
    val history: String
    val shortcuts: String
    val about: String
    val theme: String
    val themeBody: String
    val themeLight: String
    val themeDark: String
    val themeSystem: String
    val language: String
    val languageBody: String
    val indentation: String
    val indentationBody: String
    val indentTwo: String
    val indentFour: String
    val indentTabs: String
    val wrapLines: String
    val wrapLinesBody: String
    val sortKeys: String
    val sortKeysBody: String
    val unknown: String
    val unknownBody: String
    val unknownAsk: String
    val unknownJson: String
    val unknownText: String
    val keepHistory: String
    val keepHistoryShort: String

    /** "On this device only: {first} documents, {second}.", the form [count] chooses */
    fun historyKept(count: Int, first: Int, second: String): String

    /** "On this phone only: {first} documents, {second}.", the form [count] chooses */
    fun historyKeptPhone(count: Int, first: Int, second: String): String

    val historyEmpty: String
    val historyEmptyPhone: String
    val historyOff: String
    val keepFor: String
    val keepForBody: String
    val keep7: String
    val keep30: String
    val keepAlways: String
    val clear: String
    val clearBody: String
    val clearBodyPhone: String
    val clearShort: String
    val shortcutsBody: String

    /** "Formatico {first}" */
    fun version(first: String): String

    /** "Built with Pantrix UI {first}. Set in Geist and Geist Mono." */
    fun builtWith(first: String): String

    val licences: String
    val licencesBody: String
    val licencesBodyPhone: String

    /** "{first} · in the web app" */
    fun inTheWebApp(first: String): String
}

/** [SettingsStrings] in the theme's language. */
internal val settingsStrings: SettingsStrings
    @Composable
    @ReadOnlyComposable
    get() = inTheLanguage(SettingsStringsEnglish, SettingsStringsTurkish)

/** [SettingsStrings] in the language whose tag is [language], away from a screen. */
internal fun settingsStringsIn(language: String): SettingsStrings = inLanguage(language, SettingsStringsEnglish, SettingsStringsTurkish)
