package com.developersancho.formatico.feature.settings

import com.developersancho.formatico.core.settings.AppLanguage
import com.developersancho.formatico.core.settings.HistoryKeep
import com.developersancho.formatico.core.settings.IndentChoice
import com.developersancho.formatico.core.settings.Preferences
import com.developersancho.formatico.core.settings.ThemeChoice
import com.developersancho.formatico.core.settings.UnknownFormat
import kotlinx.coroutines.flow.StateFlow

/** What the history holds, as Settings says it: how many [documents], and how many [bytes] their texts have in UTF-8. */
data class HistorySummary(val documents: Int = 0, val bytes: Long = 0)

/** The history of the documents opened, as Settings sees it: what it holds, and clearing it. */
interface SettingsHistory {
    /** What the history holds now. */
    val summary: StateFlow<HistorySummary>

    /** Takes every document out of the history; the open tabs stay. */
    suspend fun clear()
}

/** Where Settings is: its sections, the open-source licences, or the text of one of them. */
sealed interface SettingsPage {
    /** The sections of Settings. */
    data object Settings : SettingsPage

    /** The libraries Formatico is built on, with their licences. */
    data object Licences : SettingsPage

    /** The licence [library] is used under, written out. */
    data class Licence(val library: OpenSourceLibrary) : SettingsPage
}

/**
 * What Settings draws.
 *
 * @property preferences what the reader chose.
 * @property history what the history holds.
 * @property page where Settings is.
 */
data class SettingsState(
    val preferences: Preferences = Preferences(),
    val history: HistorySummary = HistorySummary(),
    val page: SettingsPage = SettingsPage.Settings
)

/** What the reader did in Settings. */
sealed interface SettingsIntent {
    data class SetTheme(val theme: ThemeChoice) : SettingsIntent

    data class SetLanguage(val language: AppLanguage) : SettingsIntent

    data class SetIndentation(val indentation: IndentChoice) : SettingsIntent

    data class SetWrapLines(val wrapLines: Boolean) : SettingsIntent

    data class SetSortKeys(val sortKeys: Boolean) : SettingsIntent

    data class SetUnknownFormat(val unknownFormat: UnknownFormat) : SettingsIntent

    /** Keep a history, or keep none: turning it off forgets every document it holds. */
    data class SetKeepHistory(val keep: Boolean) : SettingsIntent

    data class SetKeepFor(val keepFor: HistoryKeep) : SettingsIntent

    data object ClearHistory : SettingsIntent

    /** Show the libraries Formatico is built on. */
    data object ShowLicences : SettingsIntent

    /** Show the licence [library] is used under. */
    data class ShowLicence(val library: OpenSourceLibrary) : SettingsIntent

    /** Go back a page; from the sections, leave Settings. */
    data object Back : SettingsIntent
}

/** What happens once in Settings. */
sealed interface SettingsEffect {
    /** Settings is left: back was asked for from its sections. */
    data object Close : SettingsEffect
}
