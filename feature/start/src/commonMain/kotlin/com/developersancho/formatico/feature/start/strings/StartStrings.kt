package com.developersancho.formatico.feature.start.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.developersancho.formatico.core.designsystem.inLanguage
import com.developersancho.formatico.core.designsystem.inTheLanguage

/**
 * The words of Start.
 *
 * In English, [StartStringsEnglish], and in Turkish, [StartStringsTurkish]. A sentence that holds values is a
 * function of them, in their order; one whose form follows a number takes it first, as `count`.
 */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal interface StartStrings {
    val title: String
    val lead: String
    val leadShort: String
    val paste: String

    /** "or press {first} anywhere on this screen" */
    fun pasteHint(first: String): String

    val newEmptyDocument: String
    val newDocument: String
    val openFile: String
    val clipboardEmpty: String
    val linkBroken: String
    val clipboardUnreadable: String

    /** "JSON, JSONC, JSON5, NDJSON, XML, YAML, TOML, CSV, TSV, properties and INI, up to {first}" */
    fun fileDropSupporting(first: String): String

    /** "{first} is larger than {second}" */
    fun fileTooLarge(first: String, second: String): String

    /** "{first} is not a kind of text Formatico reads" */
    fun fileWrongType(first: String): String

    /** "{first} could not be read" */
    fun fileUnreadable(first: String): String

    val recent: String
    val clearHistory: String
    val recentEmptyTitle: String
    val recentEmptyText: String
    val untitled: String
    val plainText: String

    /** "{first} · {second} · {third}" */
    fun recentMeta(first: String, second: String, third: String): String

    val editedJustNow: String

    /** "edited {first} minutes ago", the form [count] chooses */
    fun editedMinutesAgo(count: Int, first: Int): String

    /** "edited today at {first}" */
    fun editedTodayAt(first: String): String

    val editedYesterday: String

    /** "edited on {first}" */
    fun editedOn(first: String): String

    val openedJustNow: String

    /** "opened {first} minutes ago", the form [count] chooses */
    fun openedMinutesAgo(count: Int, first: Int): String

    /** "opened today at {first}" */
    fun openedTodayAt(first: String): String

    val openedYesterday: String

    /** "opened on {first}" */
    fun openedOn(first: String): String

    val justNow: String

    /** "{first} minutes ago", the form [count] chooses */
    fun minutesAgo(count: Int, first: Int): String

    val today: String
    val yesterday: String
    val samples: String
    val samplesHint: String
    val allSamples: String
    val sampleOrder: String
    val sampleOrderDetail: String
    val sampleOrderDetailShort: String
    val sampleDeployment: String
    val sampleDeploymentShort: String
    val sampleDeploymentDetail: String
    val sampleDeploymentDetailShort: String
    val sampleManifest: String
    val sampleManifestDetail: String
    val sampleCountries: String
    val sampleCountriesDetail: String
    val privacy: String
    val privacyWeb: String
    val unknownTitle: String
    val unknownPasted: String

    /** "Formatico could not tell the format of {first}. Choose how to read it." */
    fun unknownFile(first: String): String

    val unknownReadAs: String
    val unknownRule: String
    val unknownLeave: String
    val unknownOpen: String
    val openAddress: String
    val addressMessage: String
    val addressLabel: String
    val addressPlaceholder: String
    val addressHelpWeb: String

    /** "Formatico downloads it itself. Up to {first}." */
    fun addressHelp(first: String): String

    val addressCancel: String
    val addressOpen: String
    val addressOpening: String
    val addressNotAllowed: String

    /** "The address answered {first}: there is no document there." */
    fun addressNotThere(first: String): String

    /** "The address answered {first}, and gave no document." */
    fun addressAnswered(first: String): String

    /** "The document is {first}, and Formatico opens up to {second} here." */
    fun addressTooLarge(first: String, second: String): String

    /** "The document is larger than Formatico opens here, {first}." */
    fun addressTooLargeUntold(first: String): String

    val addressNotText: String
    val addressUnreachable: String
    val addressServerUnreachable: String
    val sharedOpeningTitle: String
    val sharedOpeningMessage: String
    val sharedOpeningMessagePhone: String
    val sharedOpeningCancel: String
    val sharedLinkGone: String
    val sharedLinkUnreachable: String
    val sharedLinkTryAgain: String
}

/** [StartStrings] in the theme's language. */
internal val startStrings: StartStrings
    @Composable
    @ReadOnlyComposable
    get() = inTheLanguage(StartStringsEnglish, StartStringsTurkish)

/** [StartStrings] in the language whose tag is [language], away from a screen. */
internal fun startStringsIn(language: String): StartStrings = inLanguage(language, StartStringsEnglish, StartStringsTurkish)
