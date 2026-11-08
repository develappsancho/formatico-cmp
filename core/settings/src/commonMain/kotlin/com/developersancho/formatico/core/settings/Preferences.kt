package com.developersancho.formatico.core.settings

import kotlinx.serialization.Serializable

/**
 * What the reader chose in Settings, as the Settings boards set it out.
 *
 * @property theme the light theme, the dark one, or the system's.
 * @property language the language Formatico speaks, or null for the system's.
 * @property indentation what Format writes a level in with.
 * @property wrapLines whether a line too long for the window goes on under itself.
 * @property sortKeys whether Format sorts the keys of objects as it writes them.
 * @property unknownFormat what is done with a text whose format cannot be told.
 * @property keepHistory whether the documents opened are kept in the history, on this device.
 * @property keepFor how long a document is kept in the history.
 */
@Serializable
data class Preferences(
    val theme: ThemeChoice = ThemeChoice.System,
    val language: AppLanguage? = null,
    val indentation: IndentChoice = IndentChoice.TwoSpaces,
    val wrapLines: Boolean = true,
    val sortKeys: Boolean = false,
    val unknownFormat: UnknownFormat = UnknownFormat.Ask,
    val keepHistory: Boolean = true,
    val keepFor: HistoryKeep = HistoryKeep.Days30
)

/** The theme Formatico is drawn in. */
enum class ThemeChoice {
    Light,
    Dark,

    /** Light or dark as the system is. */
    System
}

/** A language Formatico speaks, by its [tag], as BCP 47 writes it. */
enum class AppLanguage(val tag: String) {
    English("en"),
    Turkish("tr");

    companion object {
        /** The language a system whose language is [tag] is spoken to in: Turkish for Turkish, English for the rest. */
        fun forSystem(tag: String): AppLanguage =
            entries.firstOrNull { tag.substringBefore('-').equals(it.tag, ignoreCase = true) } ?: English
    }
}

/** What Format writes a level of a document in with. */
enum class IndentChoice {
    TwoSpaces,
    FourSpaces,
    Tabs
}

/** What is done with a text whose format cannot be told from what it holds. */
enum class UnknownFormat {
    /** The reader is asked which format to read it as. */
    Ask,

    /** It is read as JSON, and its errors shown. */
    Json,

    /** It is left as plain text. */
    Text
}

/** How long a document stays in the history: [days] days, or until the history is cleared where it is null. */
enum class HistoryKeep(val days: Int?) {
    Days7(WEEK),
    Days30(MONTH),
    UntilCleared(null)
}

/** The days of a week and of a month, as Settings counts them. */
private const val WEEK = 7
private const val MONTH = 30
