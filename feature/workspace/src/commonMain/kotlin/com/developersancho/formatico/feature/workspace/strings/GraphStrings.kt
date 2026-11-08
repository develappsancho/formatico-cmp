package com.developersancho.formatico.feature.workspace.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.developersancho.formatico.core.designsystem.inTheLanguage

/**
 * The words of Graph.
 *
 * In English, [GraphStringsEnglish], and in Turkish, [GraphStringsTurkish]. A sentence that holds values is a function
 * of them, in their order; one whose form follows a number takes it first, as `count`.
 */
internal interface GraphStrings {
    val graph: String

    /** "{first}, an object of {second} keys", the form [count] chooses: what a screen reader hears for a card. */
    fun objectCard(count: Int, first: String, second: Int): String

    /** "{first}, a list of {second} items", the form [count] chooses: what a screen reader hears for a card. */
    fun arrayCard(count: Int, first: String, second: Int): String

    /** "and {first} more", the form [count] chooses: the row at the foot of a card that does not show all its values. */
    fun more(count: Int, first: Int): String

    val showGraph: String
}

/** [GraphStrings] in the theme's language. */
internal val graphStrings: GraphStrings
    @Composable
    @ReadOnlyComposable
    get() = inTheLanguage(GraphStringsEnglish, GraphStringsTurkish)
