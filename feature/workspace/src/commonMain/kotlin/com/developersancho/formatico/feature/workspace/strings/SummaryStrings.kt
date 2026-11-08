package com.developersancho.formatico.feature.workspace.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.developersancho.formatico.core.designsystem.inLanguage
import com.developersancho.formatico.core.designsystem.inTheLanguage

/**
 * The words of the Summary.
 *
 * In English, [SummaryStringsEnglish], and in Turkish, [SummaryStringsTurkish]. A sentence that holds values is a
 * function of them, in their order; one whose form follows a number takes it first, as `count`.
 */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal interface SummaryStrings {
    val titleSize: String
    val titleValues: String
    val titleFound: String
    val titleNotes: String
    val copy: String
    val label: String
    val copied: String
    val copyFailed: String
    val text: String

    /** "{first}" */
    fun textDetail(first: String): String

    val minified: String

    /** "{first} of the text" */
    fun minifiedShare(first: String): String

    val notMinified: String
    val compressed: String
    val compressedMinified: String
    val compressedText: String
    val measuring: String
    val values: String

    /** "In {first} and {second}" */
    fun valuesIn(first: String, second: String): String

    val keys: String
    val keysNone: String
    val keysNoneShort: String
    val deepest: String
    val levelsWord: String
    val longestArray: String
    val noArray: String
    val longestText: String
    val noText: String
    val none: String
    val numbers: String
    val numbersKept: String
    val kindText: String
    val kindObject: String
    val kindNumber: String
    val kindArray: String
    val kindBoolean: String
    val kindNull: String
    val kindAlias: String
    val and: String

    /** "{first} holds {second} with the same {third}, so it reads as a table." */
    fun tableNote(first: String, second: String, third: String): String

    val showTable: String

    /** "{first} objects", the form [count] chooses */
    fun objects(count: Int, first: Int): String

    /** "{first} arrays", the form [count] chooses */
    fun arrays(count: Int, first: Int): String

    /** "{first} repeated in an object", the form [count] chooses */
    fun keysRepeated(count: Int, first: Int): String

    /** "{first} repeated", the form [count] chooses */
    fun keysRepeatedShort(count: Int, first: Int): String

    /** "{first} levels", the form [count] chooses */
    fun levels(count: Int, first: Int): String

    /** "{first} items", the form [count] chooses */
    fun items(count: Int, first: Int): String

    /** "{first} characters", the form [count] chooses */
    fun characters(count: Int, first: Int): String

    /** "The {first} values by kind", the form [count] chooses */
    fun kinds(count: Int, first: Int): String

    /** "{first} dates", the form [count] chooses */
    fun dates(count: Int, first: Int): String

    /** "{first} pictures", the form [count] chooses */
    fun pictures(count: Int, first: Int): String

    /** "{first} links", the form [count] chooses */
    fun links(count: Int, first: Int): String

    /** "{first} colours", the form [count] chooses */
    fun colours(count: Int, first: Int): String

    /** "{first} more", the form [count] chooses */
    fun more(count: Int, first: Int): String

    /** "{first} keys", the form [count] chooses */
    fun keysCount(count: Int, first: Int): String

    /**
     * "{first} keep their spelling here. A tool that reads them as numbers writes {second}.", the form [count]
     * chooses
     */
    fun respelled(count: Int, first: String, second: String): String
}

/** [SummaryStrings] in the theme's language. */
internal val summaryStrings: SummaryStrings
    @Composable
    @ReadOnlyComposable
    get() = inTheLanguage(SummaryStringsEnglish, SummaryStringsTurkish)

/** [SummaryStrings] in the language whose tag is [language], away from a screen. */
internal fun summaryStringsIn(language: String): SummaryStrings = inLanguage(language, SummaryStringsEnglish, SummaryStringsTurkish)
