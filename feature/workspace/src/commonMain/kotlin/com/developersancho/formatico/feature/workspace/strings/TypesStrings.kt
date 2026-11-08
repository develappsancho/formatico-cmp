package com.developersancho.formatico.feature.workspace.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.developersancho.formatico.core.designsystem.inTheLanguage

/**
 * The words of Types.
 *
 * In English, [TypesStringsEnglish], and in Turkish, [TypesStringsTurkish]. A sentence that holds values is a function
 * of them, in their order; one whose form follows a number takes it first, as `count`. Names of code (`JsonElement?`,
 * `unknown`, `any`) are no words: they are the same in every language.
 */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal interface TypesStrings {
    val types: String
    val language: String

    /** "From {first}, in" */
    fun fromIn(first: String): String

    /** "Write the types in {first}. Choose another language" */
    fun languageMenu(first: String): String

    val rootType: String
    val rootTypeName: String

    /** "Copy the {first}" */
    fun copy(first: String): String

    /** "Save as {first}" */
    fun saveAs(first: String): String

    /** "Types of {first}" */
    fun title(first: String): String

    /** "{first} types from {second}", the form [count] chooses */
    fun written(count: Int, first: Int, second: String): String

    /** "{first} {second} → {third}: {count} types, {fourth} lines", the form [count] chooses */
    fun sizes(count: Int, first: String, second: String, third: String, fourth: Int): String

    val label: String
    val copied: String
    val copyFailed: String

    /** "The {first} has {second} errors. Repair them to write its types.", the form [count] chooses */
    fun invalid(count: Int, first: String, second: Int): String

    val untypedTitle: String

    /** "The {first} holds no object: a single value, or a list of values, has no types to write." */
    fun untyped(first: String): String

    val kotlinLibrary: String
    val typeScriptLibrary: String
    val swiftLibrary: String
    val goLibrary: String
    val jsonSchemaLibrary: String
    val swiftGuess: String

    /** "{first} and {second}" */
    fun twoNames(first: String, second: String): String

    /** "{first} and {second} more" */
    fun moreNames(first: String, second: Int): String

    /** "{first} is null in every value, so its type cannot be told: it is written as {second}." */
    fun onlyNullOne(first: String, second: String): String

    /** "{first} are null in every value, so their types cannot be told: they are written as {second}." */
    fun onlyNullMany(first: String, second: String): String

    /** "{first} holds values of more than one kind: it is written as {second}." */
    fun mixedOne(first: String, second: String): String

    /** "{first} hold values of more than one kind: they are written as {second}." */
    fun mixedMany(first: String, second: String): String

    /** "{first} is empty in every value: its items are written as {second}." */
    fun emptyOne(first: String, second: String): String

    /** "{first} are empty in every value: their items are written as {second}." */
    fun emptyMany(first: String, second: String): String

    /** "{first} stays a string." */
    fun datesOne(first: String): String

    /** "{first} stay strings." */
    fun datesMany(first: String): String

    /** "The document is a list: {first} is the type of one of its items." */
    fun rootList(first: String): String

    val groupTypes: String

    /** "Generate {first} types" */
    fun generate(first: String): String

    val generateSchema: String
}

/** [TypesStrings] in the theme's language. */
internal val typesStrings: TypesStrings
    @Composable
    @ReadOnlyComposable
    get() = inTheLanguage(TypesStringsEnglish, TypesStringsTurkish)
