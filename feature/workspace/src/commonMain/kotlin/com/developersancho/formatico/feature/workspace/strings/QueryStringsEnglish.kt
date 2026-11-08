package com.developersancho.formatico.feature.workspace.strings

/** [QueryStrings] in English. */
internal object QueryStringsEnglish : QueryStrings {
    override val recent = "Recent"
    override val askJsonpath = "Write a JSONPath, as \$.items[*].name"
    override val askXpath = "Write an XPath, as //item/title"
    override val jsonpathElsewhere = "JSONPath is for JSON documents."
    override val xpathElsewhere = "XPath is for XML documents."

    override fun statusAndNote(first: String, second: String) = "$first. $second"

    override val unread = "The document couldn’t be read, so there is nothing to query."
    override val computed = "The XPath comes to a value"
    override val noMatches = "No matches"

    override fun matches(count: Int, first: Int) = if (count == 1) "$first match" else "$first matches"

    override val value = "Value"

    override fun matchOf(first: Int, second: Int) = "Match $first of $second"

    override fun errorAt(first: String, second: Int) = "$first (character $second)"

    override val copyResults = "Copy the results"
    override val openResults = "Open as a document"
    override val chooseHint = "A match is marked in the text; choosing one moves the cursor to it."
    override val seeInText = "Choose one to see it in the text"
    override val resultsLabel = "Query results"
    override val resultsCopied = "Results copied"
    override val resultsCopyFailed = "The results couldn’t be copied"
    override val errorEmpty = "The query is empty"
    override val errorUnexpectedEnd = "The query ends too soon"
    override val errorUnexpectedCharacter = "This can’t stand here"
    override val errorMissingRoot = "A JSONPath starts with \$"
    override val errorUnclosedString = "This string is never closed"
    override val errorInvalidEscape = "This escape isn’t one JSONPath has"
    override val errorInvalidNumber = "This number isn’t written as JSONPath writes one"
    override val errorIntegerOutOfRange = "This number is too large for an index"
    override val errorNotSingular = "This can find more than one value, where one is compared"
    override val errorWrongType = "This can’t stand here: it is of another type"
    override val errorUnknownFunction = "There is no function of this name"
    override val errorWrongArgumentCount = "This function takes another number of arguments"
    override val errorUnknownAxis = "There is no axis of this name"
    override val errorUnsupported = "Formatico’s XPath has no variables or namespace axis"
    override val errorTooDeep = "The query is nested too deep"
}
