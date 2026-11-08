package com.developersancho.formatico.feature.workspace.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.developersancho.formatico.core.designsystem.inLanguage
import com.developersancho.formatico.core.designsystem.inTheLanguage

/**
 * The words of Compare.
 *
 * In English, [CompareStringsEnglish], and in Turkish, [CompareStringsTurkish]. A sentence that holds values is a
 * function of them, in their order; one whose form follows a number takes it first, as `count`.
 */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal interface CompareStrings {
    val a: String
    val b: String

    /** "B: {first}. Choose another document" */
    fun bMenu(first: String): String

    val by: String
    val byMeaning: String
    val byText: String

    /** "{first} changed", the form [count] chooses */
    fun changed(count: Int, first: Int): String

    /** "{first} added", the form [count] chooses */
    fun added(count: Int, first: Int): String

    /** "{first} removed", the form [count] chooses */
    fun removed(count: Int, first: Int): String

    val stop: String

    /** "{first} differences", the form [count] chooses */
    fun differences(count: Int, first: Int): String

    val noDifferences: String
    val sameMeaning: String
    val sameText: String
    val changeChanged: String
    val changeAdded: String
    val changeRemoved: String

    /** "{first} → {second}" */
    fun what(first: String, second: String): String

    /** "Line {first}" */
    fun line(first: Int): String

    /** "Lines {first}–{second}" */
    fun lines(first: Int, second: Int): String

    val notCounted: String

    /** "The order of the keys: {first} and {second} changed places." */
    fun movedPair(first: String, second: String): String

    /** "The order of the keys: {first} and {second} more moved.", the form [count] chooses */
    fun moved(count: Int, first: String, second: Int): String

    /** "The spelling of the numbers: {first} and {second} are the same number." */
    fun respelledAlone(first: String, second: String): String

    /** "And {first} and {second}, which are the same number." */
    fun respelledAfter(first: String, second: String): String

    /** "And {first} and {second}, which are the same number, and {third} more.", the form [count] chooses */
    fun respelledMore(count: Int, first: String, second: String, third: Int): String

    /** "{first} {second}" */
    fun notCountedBoth(first: String, second: String): String

    /** "Difference {first} of {second}" */
    fun differenceOf(first: Int, second: Int): String

    val byMeaningNote: String
    val byTextNote: String

    /** "{first} · {second}" */
    fun sideShort(first: String, second: String): String

    val title: String
    val show: String

    /** "Show {first} in A and B" */
    fun showIn(first: String): String

    val inA: String
    val inB: String
    val notThere: String
    val aloneTitle: String
    val aloneBody: String

    /** "{first} has nothing to compare by meaning" */
    fun unreadTitle(first: String): String

    val unreadBody: String

    /** "{first} has {second} errors", the form [count] chooses */
    fun invalidTitle(count: Int, first: String, second: Int): String

    val invalidBody: String
}

/** [CompareStrings] in the theme's language. */
internal val compareStrings: CompareStrings
    @Composable
    @ReadOnlyComposable
    get() = inTheLanguage(CompareStringsEnglish, CompareStringsTurkish)

/** [CompareStrings] in the language whose tag is [language], away from a screen. */
internal fun compareStringsIn(language: String): CompareStrings = inLanguage(language, CompareStringsEnglish, CompareStringsTurkish)
