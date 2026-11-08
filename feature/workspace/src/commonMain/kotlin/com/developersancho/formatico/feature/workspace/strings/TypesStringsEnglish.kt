package com.developersancho.formatico.feature.workspace.strings

/** [TypesStrings] in English. */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal object TypesStringsEnglish : TypesStrings {
    override val types = "Types"
    override val language = "In"

    override fun fromIn(first: String) = "From $first, in"

    override fun languageMenu(first: String) = "Write the types in $first. Choose another language"

    override val rootType = "Root type"
    override val rootTypeName = "The root type’s name"

    override fun copy(first: String) = "Copy the $first"

    override fun saveAs(first: String) = "Save as $first"

    override fun title(first: String) = "Types of $first"

    override fun written(count: Int, first: Int, second: String) =
        if (count == 1) "1 type from $second" else "$first types from $second"

    override fun sizes(count: Int, first: String, second: String, third: String, fourth: Int) =
        if (count == 1) "$first $second → $third: 1 type, $fourth lines" else "$first $second → $third: $count types, $fourth lines"

    override val label = "Types"
    override val copied = "Types copied"
    override val copyFailed = "The types couldn’t be copied"

    override fun invalid(count: Int, first: String, second: Int) =
        if (count == 1) {
            "The $first has 1 error. Repair it to write its types."
        } else {
            "The $first has $second errors. Repair them to write its types."
        }

    override val untypedTitle = "No types to write"

    override fun untyped(first: String) = "The $first holds no object: a single value, or a list of values, has no types to write."

    override val kotlinLibrary = "Kotlin data classes for kotlinx.serialization."
    override val typeScriptLibrary = "TypeScript interfaces."
    override val swiftLibrary = "Swift structs for Codable."
    override val goLibrary = "Go structs for encoding/json."
    override val swiftGuess = "String?, marked as a guess"

    override fun twoNames(first: String, second: String) = "$first and $second"

    override fun moreNames(first: String, second: Int) = "$first and $second more"

    override fun onlyNullOne(first: String, second: String) =
        "$first is null in every value, so its type cannot be told: it is written as $second."

    override fun onlyNullMany(first: String, second: String) =
        "$first are null in every value, so their types cannot be told: they are written as $second."

    override fun mixedOne(first: String, second: String) = "$first holds values of more than one kind: it is written as $second."

    override fun mixedMany(first: String, second: String) = "$first hold values of more than one kind: they are written as $second."

    override fun emptyOne(first: String, second: String) = "$first is empty in every value: its items are written as $second."

    override fun emptyMany(first: String, second: String) = "$first are empty in every value: their items are written as $second."

    override fun datesOne(first: String) = "$first stays a string."

    override fun datesMany(first: String) = "$first stay strings."

    override fun rootList(first: String) = "The document is a list: $first is the type of one of its items."

    override val groupTypes = "Types"

    override fun generate(first: String) = "Generate $first types"
}
