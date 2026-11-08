package com.developersancho.formatico.feature.workspace.strings

/** [ConvertStrings] in English. */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal object ConvertStringsEnglish : ConvertStrings {
    override val to = "To"

    override fun fromTo(first: String) = "From $first to"

    override fun toMenu(first: String) = "Convert to $first. Choose another format"

    override val indentation = "Indentation"

    override fun copy(first: String) = "Copy the $first"

    override fun saveAs(first: String) = "Save as $first"

    override fun title(first: String) = "Convert $first"

    override val nulls = "A null value"
    override val nullsChoice = "What a null value becomes"
    override val nullsLeave = "Leave it out"
    override val nullsEmpty = "Write an empty string"

    override fun sizes(first: String, second: String, third: String, fourth: String) = "$first $second → $third $fourth"

    override val label = "Converted document"
    override val copied = "Converted document copied"
    override val copyFailed = "The converted document couldn’t be copied"

    override fun nullComment(first: String, second: String, third: String) = "$first = null in the $second: $third has no null"

    override fun source(count: Int, first: String, second: Int) = if (count == 1) "$first · $second line" else "$first · $second lines"

    override fun invalid(count: Int, first: String, second: Int) =
        if (count == 1) {
            "The $first has an error. Repair it to convert the document."
        } else {
            "The $first has $second errors. Repair them to convert the document."
        }

    override val nothingLost = "Nothing is lost"

    override fun back(first: String) = "It converts back to this $first as it is."

    override fun backWith(first: String, second: String, third: String) = "$first, so the $second converts back to this $third as it is."

    override fun noteQuotedDates(first: String) = "Strings $first would read as dates are written in quotes"

    override fun noteQuotedValues(first: String) = "Strings $first would read as other values are written in quotes"

    override fun noteSpellingOne(first: String) = "$first keeps its spelling"

    override fun noteSpellingTwo(first: String, second: String) = "$first and $second keep their spelling"

    override fun noteSpellingMore(count: Int, first: String, second: Int) =
        if (count == 1) {
            "$first and $second more number keep their spelling"
        } else {
            "$first and $second more numbers keep their spelling"
        }

    override fun notesAnd(first: String, second: String) = "$first, and $second"

    override fun losses(count: Int, first: Int) = if (count == 1) "$first loss" else "$first losses"

    override fun lossNullsTitle(first: String) = "$first has no null"

    override fun lossNullsLeftOne(first: String, second: String) =
        "$first is null in the $second. It is left out, and a comment marks where it was: converting back will not bring it."

    override fun lossNullsLeftMany(count: Int, first: String, second: Int, third: String) =
        if (count == 1) {
            "$first and $second more value are null in the $third. They are left out, and comments mark where they were: " +
                "converting back will not bring them."
        } else {
            "$first and $second more values are null in the $third. They are left out, and comments mark where they were: " +
                "converting back will not bring them."
        }

    override fun lossNullsEmptyOne(first: String, second: String) =
        "$first is null in the $second. It is written as an empty string: converting back gives a string."

    override fun lossNullsEmptyMany(count: Int, first: String, second: Int, third: String) =
        if (count == 1) {
            "$first and $second more value are null in the $third. They are written as empty strings: converting back gives strings."
        } else {
            "$first and $second more values are null in the $third. They are written as empty strings: converting back gives strings."
        }

    override fun tagNullsLeft(count: Int, first: Int) = if (count == 1) "$first value left out" else "$first values left out"

    override fun tagNullsEmpty(count: Int, first: Int) = if (count == 1) "$first null written empty" else "$first nulls written empty"

    override val lossCommentsTitle = "The comments are left behind"

    override fun lossComments(count: Int, first: String, second: Int, third: String) =
        if (count == 1) {
            "The $first has a comment, and a conversion carries values, not comments: the $third is without it."
        } else {
            "The $first has $second comments, and a conversion carries values, not comments: the $third is without them."
        }

    override fun tagComments(count: Int, first: Int) = if (count == 1) "$first comment left behind" else "$first comments left behind"

    override val lossAliasesTitle = "Aliases are written out"

    override fun lossAliasesOne(first: String) =
        "$first is an alias, and is written as the value it points at: converting back gives a copy."

    override fun lossAliasesMany(count: Int, first: String, second: Int) =
        if (count == 1) {
            "$first and $second more alias are written as the values they point at: converting back gives copies."
        } else {
            "$first and $second more aliases are written as the values they point at: converting back gives copies."
        }

    override fun tagAliases(count: Int, first: Int) = if (count == 1) "$first alias written out" else "$first aliases written out"

    override fun lossUntypedTitle(first: String) = "$first has only text"

    override fun lossUntypedOne(first: String) = "$first is written as text: converting back gives a string."

    override fun lossUntypedMany(count: Int, first: String, second: Int) =
        if (count == 1) {
            "$first and $second more value are written as text: converting back gives strings."
        } else {
            "$first and $second more values are written as text: converting back gives strings."
        }

    override fun tagUntyped(count: Int, first: Int) = if (count == 1) "$first value made text" else "$first values made text"

    override fun lossFlattenedTitle(first: String) = "$first has one level"

    override fun lossFlattenedOne(first: String) = "$first is a name of its own: converting back gives it on one level."

    override fun lossFlattenedMany(count: Int, first: String, second: Int) =
        "$first and $second more are names of their own: converting back gives them on one level."

    override fun tagFlattened(count: Int, first: Int) = if (count == 1) "$first value flattened" else "$first values flattened"

    override fun lossWrappedTitle(first: String) = "The document is put under $first"

    override fun lossWrapped(first: String, second: String, third: String) =
        "The $second’s value is no $first document as it is: converting back gives it under $third."

    override val lossRowTitle = "The document is one row"
    override val lossLineTitle = "The document is one line"

    override fun lossInArray(first: String, second: String) = "$first writes a list: converting back gives the $second’s value in an array."

    override fun tagWrapped(first: String) = "Put under $first"

    override val tagRow = "One row"
    override val tagLine = "One line"
    override val lossRenamedTitle = "Keys are renamed"

    override fun lossRenamedOne(first: String, second: String, third: String) = "$first is no $second name: it is written $third."

    override fun lossRenamedMany(count: Int, first: String, second: Int, third: String, fourth: String) =
        if (count == 1) {
            "$first and $second more key are no $third names: $first is written $fourth."
        } else {
            "$first and $second more keys are no $third names: $first is written $fourth."
        }

    override fun tagRenamed(count: Int, first: Int) = if (count == 1) "$first key renamed" else "$first keys renamed"

    override val lossDuplicatesTitle = "Keys given twice"

    override fun lossDuplicatesOne(first: String, second: String) = "$first is given twice, and $second keeps a key once: the last is kept."

    override fun lossDuplicatesMany(count: Int, first: String, second: Int, third: String) =
        if (count == 1) {
            "$first and $second more key are given twice, and $third keeps a key once: the last of each is kept."
        } else {
            "$first and $second more keys are given twice, and $third keeps a key once: the last of each is kept."
        }

    override fun tagDuplicates(count: Int, first: Int) = if (count == 1) "$first key dropped" else "$first keys dropped"

    override val lossRespelledTitle = "Numbers are respelled"

    override fun lossRespelledOne(first: String, second: String, third: String) =
        "$first is written $second, as $third writes it: converting back keeps that spelling."

    override fun lossRespelledMany(count: Int, first: String, second: Int, third: String, fourth: String) =
        if (count == 1) {
            "$first and $second more number are written as $third writes them, $first as $fourth: converting back keeps that spelling."
        } else {
            "$first and $second more numbers are written as $third writes them, $first as $fourth: converting back keeps that spelling."
        }

    override fun tagRespelled(count: Int, first: Int) = if (count == 1) "$first number respelled" else "$first numbers respelled"

    override val lossUnwritableTitle = "Numbers become strings"

    override fun lossUnwritableOne(first: String, second: String) = "$first has no value in $second: it is written as a string."

    override fun lossUnwritableMany(count: Int, first: String, second: Int, third: String) =
        if (count == 1) {
            "$first and $second more number have no value in $third: they are written as strings."
        } else {
            "$first and $second more numbers have no value in $third: they are written as strings."
        }

    override fun tagUnwritable(count: Int, first: Int) = if (count == 1) "$first number made a string" else "$first numbers made strings"

    override val lossDatesTitle = "Dates become strings"

    override fun lossDatesOne(first: String, second: String, third: String) =
        "$first is a date in the $second and a string in the $third: converting back gives a string."

    override fun lossDatesMany(count: Int, first: String, second: Int, third: String) =
        if (count == 1) {
            "$first and $second more date are strings in the $third: converting back gives strings."
        } else {
            "$first and $second more dates are strings in the $third: converting back gives strings."
        }

    override fun tagDates(count: Int, first: Int) = if (count == 1) "$first date made a string" else "$first dates made strings"

    override val lossMovedTitle = "Keys move up"

    override fun lossMovedOne(first: String, second: String) =
        "$first writes a level’s own values first: $second moves up, and converting back keeps it there."

    override fun lossMovedMany(count: Int, first: String, second: String, third: Int) =
        if (count == 1) {
            "$first writes a level’s own values first: $second and $third more key move up, and converting back keeps them there."
        } else {
            "$first writes a level’s own values first: $second and $third more keys move up, and converting back keeps them there."
        }

    override fun tagMoved(count: Int, first: Int) = if (count == 1) "$first key moved" else "$first keys moved"

    override val lossEmptiedTitle = "Empty values are lost"

    override fun lossEmptiedOne(first: String, second: String) =
        "$first holds nothing, and $second cannot write an empty object or array as it is: converting back does not give it so."

    override fun lossEmptiedMany(count: Int, first: String, second: Int, third: String) =
        if (count == 1) {
            "$first and $second more value hold nothing, and $third cannot write an empty object or array as it is: converting " +
                "back does not give them so."
        } else {
            "$first and $second more values hold nothing, and $third cannot write an empty object or array as it is: converting " +
                "back does not give them so."
        }

    override fun tagEmptied(count: Int, first: Int) = if (count == 1) "$first empty value lost" else "$first empty values lost"

    override val lossOneLineTitle = "Values are put on one line"

    override fun lossOneLineOne(first: String, second: String) =
        "$first has line breaks, and $second writes a value on one line: they are written as \\n."

    override fun lossOneLineMany(count: Int, first: String, second: Int, third: String) =
        if (count == 1) {
            "$first and $second more value have line breaks, and $third writes a value on one line: they are written as \\n."
        } else {
            "$first and $second more values have line breaks, and $third writes a value on one line: they are written as \\n."
        }

    override fun tagOneLine(count: Int, first: Int) = if (count == 1) "$first value put on one line" else "$first values put on one line"

    override val lossItemsTitle = "Items are named item"

    override fun lossItemsOne(first: String) =
        "$first is an array in an array, which has no name in XML: its items are written as item elements."

    override fun lossItemsMany(count: Int, first: String, second: Int) =
        "$first and $second more are arrays in arrays, which have no name in XML: their items are written as item elements."

    override fun tagItems(count: Int, first: Int) = if (count == 1) "$first array named" else "$first arrays named"
}
