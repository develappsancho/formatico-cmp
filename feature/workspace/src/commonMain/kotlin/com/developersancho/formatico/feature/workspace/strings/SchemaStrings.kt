package com.developersancho.formatico.feature.workspace.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.developersancho.formatico.core.designsystem.inTheLanguage
import com.pantrixui.foundation.l10n.PantrixStrings

/**
 * The words of Schema.
 *
 * In English, [SchemaStringsEnglish], and in Turkish, [SchemaStringsTurkish]. A sentence that holds values is a function
 * of them, in their order; one whose form follows a number takes it first, as `count`. A problem's sentence starts with
 * the name of the value, as the document writes it, and ends with what it is: neither is translated. JSON Schema's
 * keywords (`$schema`, `$ref`, `anyOf`) are no words: they are the same in every language.
 */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal interface SchemaStrings {
    /** Pantrix's words of the same language, which join the names a sentence lists. */
    val pantrix: PantrixStrings

    val schema: String
    val against: String

    /** "Validate {first} against" */
    fun validateAgainst(first: String): String

    /** "Validate {first}" */
    fun title(first: String): String

    /** "Validate against {first}. Choose another schema" */
    fun chooserLabel(first: String): String

    val chooseSchemaLabel: String
    val noSchema: String
    val chooseFile: String

    /** "Open {first}" */
    fun openAddress(first: String): String

    /** "{first} problems against {second}", the form [count] chooses */
    fun failedTitle(count: Int, first: Int, second: String): String

    /** "{first} is valid {second}, but not what the schema asks for." */
    fun failedBody(first: String, second: String): String

    /** "The first {first} of {second} are listed." */
    fun listedFirst(first: Int, second: Int): String

    /** "Valid against {first}" */
    fun validTitle(first: String): String

    /** "Every value of {first} is what the schema asks for (JSON Schema {second})." */
    fun validBody(first: String, second: String): String

    val noneTitle: String

    /** "One of the open JSON Schema documents, or a file. {first} names none in a $schema of its own." */
    fun noneBody(first: String): String

    /** "{first} names its schema in its $schema: {second}. Open it from the menu, or choose another." */
    fun namedBody(first: String, second: String): String

    /** "{first} cannot be used as a schema" */
    fun unusableTitle(first: String): String

    /** "Its $schema names {first}, which is no draft Formatico knows: …" */
    fun unknownDraft(first: String): String

    /** "Its $ref on line {first}, {second}, points at nothing. Correct it in its own tab, and {third} is validated again." */
    fun unresolvedAt(first: Int, second: String, third: String): String

    /** "Its $ref {first} points at nothing. Correct it in its own tab, and {second} is validated again." */
    fun unresolved(first: String, second: String): String

    /** "What it holds on line {first}, {second}, is not what JSON Schema {third} allows there. …, and {fourth} …" */
    fun notASchemaAt(first: Int, second: String, third: String, fourth: String): String

    /** "It is not what JSON Schema {first} allows. Correct it in its own tab, and {second} is validated again." */
    fun notASchema(first: String, second: String): String

    /** "The validator could not read it: {first}" */
    fun unreadable(first: String): String

    /** "No schema for {first}" */
    fun unsupportedTitle(first: String): String

    /** "A JSON Schema validates JSON, YAML and TOML: the values of a {first} document are all text." */
    fun unsupported(first: String): String

    /** "{first} has {second} errors. Repair them to validate it against a schema.", the form [count] chooses */
    fun invalid(count: Int, first: String, second: Int): String

    val theDocument: String
    val anObject: String
    val anArray: String
    val aString: String
    val aNumber: String
    val anInteger: String
    val trueOrFalse: String
    val nullValue: String

    /** "{first} must be {second}, and is {third}." */
    fun type(first: String, second: String, third: String): String

    /** "{first} has no {second}, which the schema requires." */
    fun requiredOne(first: String, second: String): String

    /** "{first} must have {second}, and has none of them." */
    fun requiredMany(first: String, second: String): String

    /** "{first} has {second}, so it must have {third} too." */
    fun dependentRequired(first: String, second: String, third: String): String

    /** "{first} must be one of {second}, and is {third}." */
    fun enumeration(first: String, second: String, third: String): String

    /** "{first} must be {second}, and is {third}." */
    fun constant(first: String, second: String, third: String): String

    /** "at least {first}" */
    fun atLeast(first: String): String

    /** "more than {first}" */
    fun moreThan(first: String): String

    /** "at most {first}" */
    fun atMost(first: String): String

    /** "less than {first}" */
    fun lessThan(first: String): String

    /** "{first} must be {second}, and is {third}." */
    fun bound(first: String, second: String, third: String): String

    /** "{first} must be a multiple of {second}, and is {third}." */
    fun multipleOf(first: String, second: String, third: String): String

    /** "{first} must be at least {second} characters long, and is {third}.", the form [count] chooses */
    fun minLength(count: Int, first: String, second: String, third: String): String

    /** "{first} must be at most {second} characters long, and is {third}.", the form [count] chooses */
    fun maxLength(count: Int, first: String, second: String, third: String): String

    /** "{first} must match {second}, and is {third}." */
    fun pattern(first: String, second: String, third: String): String

    /** "{first} must be written as {second}, and is {third}." */
    fun format(first: String, second: String, third: String): String

    /** "{first} must hold at least {second} items.", the form [count] chooses */
    fun minItems(count: Int, first: String, second: String): String

    /** "{first} must hold at most {second} items.", the form [count] chooses */
    fun maxItems(count: Int, first: String, second: String): String

    /** "The items of {first} must differ from each other." */
    fun uniqueItems(first: String): String

    /** "{first} must hold an item the schema describes." */
    fun contains(first: String): String

    /** "{first} must have at least {second} keys.", the form [count] chooses */
    fun minKeys(count: Int, first: String, second: String): String

    /** "{first} must have at most {second} keys.", the form [count] chooses */
    fun maxKeys(count: Int, first: String, second: String): String

    /** "{first} is not allowed here." */
    fun notAllowed(first: String): String

    /** "{first} must not be what the schema rules out." */
    fun not(first: String): String

    /** "{first} matches none of the {second} schemas its {third} allows." */
    fun noChoice(first: String, second: Int, third: String): String

    /** "{first} matches more than one of the schemas its oneOf allows, and must match one." */
    fun manyChoices(first: String): String

    /** "{first} does not meet the schema's {second}." */
    fun other(first: String, second: String): String

    /** "{first} schema problems", the form [count] chooses */
    fun statusProblems(count: Int, first: Int): String

    val statusValid: String
    val groupSchema: String
    val validate: String
}

/** [SchemaStrings] in the theme's language. */
internal val schemaStrings: SchemaStrings
    @Composable
    @ReadOnlyComposable
    get() = inTheLanguage(SchemaStringsEnglish, SchemaStringsTurkish)
