package com.developersancho.formatico.feature.workspace.strings

/** [SummaryStrings] in English. */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal object SummaryStringsEnglish : SummaryStrings {
    override val titleSize = "Size"
    override val titleValues = "Values"
    override val titleFound = "Found in the values"
    override val titleNotes = "Notes"
    override val copy = "Copy the summary as text"
    override val label = "Summary"
    override val copied = "Summary copied"
    override val copyFailed = "The summary couldn’t be copied"
    override val text = "Text"

    override fun textDetail(first: String) = "$first"

    override val minified = "Minified"

    override fun minifiedShare(first: String) = "$first of the text"

    override val notMinified = "No minified form"
    override val compressed = "Compressed"
    override val compressedMinified = "The minified text, gzip"
    override val compressedText = "The text, gzip"
    override val measuring = "Being measured"
    override val values = "Values"

    override fun valuesIn(first: String, second: String) = "In $first and $second"

    override val keys = "Keys"
    override val keysNone = "None repeated in an object"
    override val keysNoneShort = "None repeated"
    override val deepest = "Deepest"
    override val levelsWord = "Levels"
    override val longestArray = "Longest array"
    override val noArray = "No arrays"
    override val longestText = "Longest text"
    override val noText = "No text"
    override val none = "None"
    override val numbers = "Numbers"
    override val numbersKept = "All kept as written"
    override val kindText = "Text"
    override val kindObject = "Objects"
    override val kindNumber = "Numbers"
    override val kindArray = "Arrays"
    override val kindBoolean = "Booleans"
    override val kindNull = "Null"
    override val kindAlias = "Aliases"
    override val and = " and "

    override fun tableNote(first: String, second: String, third: String) =
        "$first holds $second with the same $third, so it reads as a table."

    override val showTable = "Show as a table"

    override fun objects(count: Int, first: Int) = if (count == 1) "$first object" else "$first objects"

    override fun arrays(count: Int, first: Int) = if (count == 1) "$first array" else "$first arrays"

    override fun keysRepeated(count: Int, first: Int) = "$first repeated in an object"

    override fun keysRepeatedShort(count: Int, first: Int) = "$first repeated"

    override fun levels(count: Int, first: Int) = if (count == 1) "$first level" else "$first levels"

    override fun items(count: Int, first: Int) = if (count == 1) "$first item" else "$first items"

    override fun characters(count: Int, first: Int) = if (count == 1) "$first character" else "$first characters"

    override fun kinds(count: Int, first: Int) = if (count == 1) "The $first value by kind" else "The $first values by kind"

    override fun dates(count: Int, first: Int) = if (count == 1) "$first date" else "$first dates"

    override fun pictures(count: Int, first: Int) = if (count == 1) "$first picture" else "$first pictures"

    override fun links(count: Int, first: Int) = if (count == 1) "$first link" else "$first links"

    override fun colours(count: Int, first: Int) = if (count == 1) "$first colour" else "$first colours"

    override fun more(count: Int, first: Int) = "$first more"

    override fun keysCount(count: Int, first: Int) = if (count == 1) "$first key" else "$first keys"

    override fun respelled(count: Int, first: String, second: String) =
        if (count == 1) {
            "$first keeps its spelling here. A tool that reads it as a number writes $second."
        } else {
            "$first keep their spelling here. A tool that reads them as numbers writes $second."
        }
}
