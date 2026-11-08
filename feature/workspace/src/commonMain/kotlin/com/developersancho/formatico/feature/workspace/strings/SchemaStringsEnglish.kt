package com.developersancho.formatico.feature.workspace.strings

import com.pantrixui.foundation.l10n.PantrixStrings

/** [SchemaStrings] in English. */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal object SchemaStringsEnglish : SchemaStrings {
    override val pantrix = PantrixStrings.English

    override val schema = "Schema"
    override val against = "Against"

    override fun validateAgainst(first: String) = "Validate $first against"

    override fun title(first: String) = "Validate $first"

    override fun chooserLabel(first: String) = "Validate against $first. Choose another schema"

    override val chooseSchemaLabel = "Validate against a schema. Choose one"
    override val noSchema = "No schema"
    override val chooseFile = "Choose a file"

    override fun openAddress(first: String) = "Open $first"

    override fun failedTitle(count: Int, first: Int, second: String) =
        if (count == 1) "1 problem against $second" else "$first problems against $second"

    override fun failedBody(first: String, second: String) = "$first is valid $second, but not what the schema asks for."

    override fun listedFirst(first: Int, second: Int) = "The first $first of $second are listed."

    override fun validTitle(first: String) = "Valid against $first"

    override fun validBody(first: String, second: String) = "Every value of $first is what the schema asks for (JSON Schema $second)."

    override val noneTitle = "Choose a schema"

    override fun noneBody(first: String) = "One of the open JSON Schema documents, or a file. $first names none in a \$schema of its own."

    override fun namedBody(first: String, second: String) =
        "$first names its schema in its \$schema: $second. Open it from the menu, or choose another."

    override fun unusableTitle(first: String) = "$first cannot be used as a schema"

    override fun unknownDraft(first: String) =
        "Its \$schema names $first, which is no draft Formatico knows: 2020-12, 2019-09, draft-07, draft-06 or draft-04."

    override fun unresolvedAt(first: Int, second: String, third: String) =
        "Its \$ref on line $first, $second, points at nothing. Correct it in its own tab, and $third is validated again."

    override fun unresolved(first: String, second: String) =
        "Its \$ref $first points at nothing. Correct it in its own tab, and $second is validated again."

    override fun notASchemaAt(first: Int, second: String, third: String, fourth: String) =
        "What it holds on line $first, $second, is not what JSON Schema $third allows there. Correct it in its own tab, and $fourth is " +
            "validated again."

    override fun notASchema(first: String, second: String) =
        "It is not what JSON Schema $first allows. Correct it in its own tab, and $second is validated again."

    override fun unreadable(first: String) = "The validator could not read it: $first"

    override fun unsupportedTitle(first: String) = "No schema for $first"

    override fun unsupported(first: String) = "A JSON Schema validates JSON, YAML and TOML: the values of a $first document are all text."

    override fun invalid(count: Int, first: String, second: Int) =
        if (count == 1) {
            "$first has 1 error. Repair it to validate the document against a schema."
        } else {
            "$first has $second errors. Repair them to validate the document against a schema."
        }

    override val theDocument = "The document"
    override val anObject = "an object"
    override val anArray = "an array"
    override val aString = "a string"
    override val aNumber = "a number"
    override val anInteger = "an integer"
    override val trueOrFalse = "true or false"
    override val nullValue = "null"

    override fun type(first: String, second: String, third: String) = "$first must be $second, and is $third."

    override fun requiredOne(first: String, second: String) = "$first has no $second, which the schema requires."

    override fun requiredMany(first: String, second: String) = "$first must have $second, and has none of them."

    override fun dependentRequired(first: String, second: String, third: String) = "$first has $second, so it must have $third too."

    override fun enumeration(first: String, second: String, third: String) = "$first must be one of $second, and is $third."

    override fun constant(first: String, second: String, third: String) = "$first must be $second, and is $third."

    override fun atLeast(first: String) = "at least $first"

    override fun moreThan(first: String) = "more than $first"

    override fun atMost(first: String) = "at most $first"

    override fun lessThan(first: String) = "less than $first"

    override fun bound(first: String, second: String, third: String) = "$first must be $second, and is $third."

    override fun multipleOf(first: String, second: String, third: String) = "$first must be a multiple of $second, and is $third."

    override fun minLength(count: Int, first: String, second: String, third: String) =
        "$first must be at least $second ${if (count == 1) "character" else "characters"} long, and is $third."

    override fun maxLength(count: Int, first: String, second: String, third: String) =
        "$first must be at most $second ${if (count == 1) "character" else "characters"} long, and is $third."

    override fun pattern(first: String, second: String, third: String) = "$first must match $second, and is $third."

    override fun format(first: String, second: String, third: String) = "$first must be written as $second, and is $third."

    override fun minItems(count: Int, first: String, second: String) =
        "$first must hold at least $second ${if (count == 1) "item" else "items"}."

    override fun maxItems(count: Int, first: String, second: String) =
        "$first must hold at most $second ${if (count == 1) "item" else "items"}."

    override fun uniqueItems(first: String) = "The items of $first must differ from each other."

    override fun contains(first: String) = "$first must hold an item the schema describes."

    override fun minKeys(count: Int, first: String, second: String) =
        "$first must have at least $second ${if (count == 1) "key" else "keys"}."

    override fun maxKeys(count: Int, first: String, second: String) =
        "$first must have at most $second ${if (count == 1) "key" else "keys"}."

    override fun notAllowed(first: String) = "$first is not allowed here."

    override fun not(first: String) = "$first must not be what the schema rules out."

    override fun noChoice(first: String, second: Int, third: String) = "$first matches none of the $second schemas its $third allows."

    override fun manyChoices(first: String) = "$first matches more than one of the schemas its oneOf allows, and must match one."

    override fun other(first: String, second: String) = "$first does not meet the schema’s $second."

    override fun statusProblems(count: Int, first: Int) = if (count == 1) "1 schema problem" else "$first schema problems"

    override val statusValid = "Valid against the schema"
    override val groupSchema = "Schema"
    override val validate = "Validate against a schema"
}
