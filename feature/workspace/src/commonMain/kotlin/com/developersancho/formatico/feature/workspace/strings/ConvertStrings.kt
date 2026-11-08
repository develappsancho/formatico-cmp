package com.developersancho.formatico.feature.workspace.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.developersancho.formatico.core.designsystem.inLanguage
import com.developersancho.formatico.core.designsystem.inTheLanguage

/**
 * The words of Convert.
 *
 * In English, [ConvertStringsEnglish], and in Turkish, [ConvertStringsTurkish]. A sentence that holds values is a
 * function of them, in their order; one whose form follows a number takes it first, as `count`.
 */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal interface ConvertStrings {
    val to: String

    /** "From {first} to" */
    fun fromTo(first: String): String

    /** "Convert to {first}. Choose another format" */
    fun toMenu(first: String): String

    val indentation: String

    /** "Copy the {first}" */
    fun copy(first: String): String

    /** "Save as {first}" */
    fun saveAs(first: String): String

    /** "Convert {first}" */
    fun title(first: String): String

    val nulls: String
    val nullsChoice: String
    val nullsLeave: String
    val nullsEmpty: String

    /** "{first} {second} → {third} {fourth}" */
    fun sizes(first: String, second: String, third: String, fourth: String): String

    val label: String
    val copied: String
    val copyFailed: String

    /** "{first} = null in the {second}: {third} has no null" */
    fun nullComment(first: String, second: String, third: String): String

    /** "{first} · {second} lines", the form [count] chooses */
    fun source(count: Int, first: String, second: Int): String

    /** "The {first} has {second} errors. Repair them to convert the document.", the form [count] chooses */
    fun invalid(count: Int, first: String, second: Int): String

    val nothingLost: String

    /** "It converts back to this {first} as it is." */
    fun back(first: String): String

    /** "{first}, so the {second} converts back to this {third} as it is." */
    fun backWith(first: String, second: String, third: String): String

    /** "Strings {first} would read as dates are written in quotes" */
    fun noteQuotedDates(first: String): String

    /** "Strings {first} would read as other values are written in quotes" */
    fun noteQuotedValues(first: String): String

    /** "{first} keeps its spelling" */
    fun noteSpellingOne(first: String): String

    /** "{first} and {second} keep their spelling" */
    fun noteSpellingTwo(first: String, second: String): String

    /** "{first} and {second} more numbers keep their spelling", the form [count] chooses */
    fun noteSpellingMore(count: Int, first: String, second: Int): String

    /** "{first}, and {second}" */
    fun notesAnd(first: String, second: String): String

    /** "{first} losses", the form [count] chooses */
    fun losses(count: Int, first: Int): String

    /** "{first} has no null" */
    fun lossNullsTitle(first: String): String

    /**
     * "{first} is null in the {second}. It is left out, and a comment marks where it was: converting back will not
     * bring it."
     */
    fun lossNullsLeftOne(first: String, second: String): String

    /**
     * "{first} and {second} more values are null in the {third}. They are left out, and comments mark where they
     * were: converting back will not bring them.", the form [count] chooses
     */
    fun lossNullsLeftMany(count: Int, first: String, second: Int, third: String): String

    /** "{first} is null in the {second}. It is written as an empty string: converting back gives a string." */
    fun lossNullsEmptyOne(first: String, second: String): String

    /**
     * "{first} and {second} more values are null in the {third}. They are written as empty strings: converting back
     * gives strings.", the form [count] chooses
     */
    fun lossNullsEmptyMany(count: Int, first: String, second: Int, third: String): String

    /** "{first} values left out", the form [count] chooses */
    fun tagNullsLeft(count: Int, first: Int): String

    /** "{first} nulls written empty", the form [count] chooses */
    fun tagNullsEmpty(count: Int, first: Int): String

    val lossCommentsTitle: String

    /**
     * "The {first} has {second} comments, and a conversion carries values, not comments: the {third} is without
     * them.", the form [count] chooses
     */
    fun lossComments(count: Int, first: String, second: Int, third: String): String

    /** "{first} comments left behind", the form [count] chooses */
    fun tagComments(count: Int, first: Int): String

    val lossAliasesTitle: String

    /** "{first} is an alias, and is written as the value it points at: converting back gives a copy." */
    fun lossAliasesOne(first: String): String

    /**
     * "{first} and {second} more aliases are written as the values they point at: converting back gives copies.",
     * the form [count] chooses
     */
    fun lossAliasesMany(count: Int, first: String, second: Int): String

    /** "{first} aliases written out", the form [count] chooses */
    fun tagAliases(count: Int, first: Int): String

    /** "{first} has only text" */
    fun lossUntypedTitle(first: String): String

    /** "{first} is written as text: converting back gives a string." */
    fun lossUntypedOne(first: String): String

    /**
     * "{first} and {second} more values are written as text: converting back gives strings.", the form [count]
     * chooses
     */
    fun lossUntypedMany(count: Int, first: String, second: Int): String

    /** "{first} values made text", the form [count] chooses */
    fun tagUntyped(count: Int, first: Int): String

    /** "{first} has one level" */
    fun lossFlattenedTitle(first: String): String

    /** "{first} is a name of its own: converting back gives it on one level." */
    fun lossFlattenedOne(first: String): String

    /**
     * "{first} and {second} more are names of their own: converting back gives them on one level.", the form [count]
     * chooses
     */
    fun lossFlattenedMany(count: Int, first: String, second: Int): String

    /** "{first} values flattened", the form [count] chooses */
    fun tagFlattened(count: Int, first: Int): String

    /** "The document is put under {first}" */
    fun lossWrappedTitle(first: String): String

    /** "The {second}’s value is no {first} document as it is: converting back gives it under {third}." */
    fun lossWrapped(first: String, second: String, third: String): String

    val lossRowTitle: String
    val lossLineTitle: String

    /** "{first} writes a list: converting back gives the {second}’s value in an array." */
    fun lossInArray(first: String, second: String): String

    /** "Put under {first}" */
    fun tagWrapped(first: String): String

    val tagRow: String
    val tagLine: String
    val lossRenamedTitle: String

    /** "{first} is no {second} name: it is written {third}." */
    fun lossRenamedOne(first: String, second: String, third: String): String

    /** "{first} and {second} more keys are no {third} names: {first} is written {fourth}.", the form [count] chooses */
    fun lossRenamedMany(count: Int, first: String, second: Int, third: String, fourth: String): String

    /** "{first} keys renamed", the form [count] chooses */
    fun tagRenamed(count: Int, first: Int): String

    val lossDuplicatesTitle: String

    /** "{first} is given twice, and {second} keeps a key once: the last is kept." */
    fun lossDuplicatesOne(first: String, second: String): String

    /**
     * "{first} and {second} more keys are given twice, and {third} keeps a key once: the last of each is kept.", the
     * form [count] chooses
     */
    fun lossDuplicatesMany(count: Int, first: String, second: Int, third: String): String

    /** "{first} keys dropped", the form [count] chooses */
    fun tagDuplicates(count: Int, first: Int): String

    val lossRespelledTitle: String

    /** "{first} is written {second}, as {third} writes it: converting back keeps that spelling." */
    fun lossRespelledOne(first: String, second: String, third: String): String

    /**
     * "{first} and {second} more numbers are written as {third} writes them, {first} as {fourth}: converting back
     * keeps that spelling.", the form [count] chooses
     */
    fun lossRespelledMany(count: Int, first: String, second: Int, third: String, fourth: String): String

    /** "{first} numbers respelled", the form [count] chooses */
    fun tagRespelled(count: Int, first: Int): String

    val lossUnwritableTitle: String

    /** "{first} has no value in {second}: it is written as a string." */
    fun lossUnwritableOne(first: String, second: String): String

    /**
     * "{first} and {second} more numbers have no value in {third}: they are written as strings.", the form [count]
     * chooses
     */
    fun lossUnwritableMany(count: Int, first: String, second: Int, third: String): String

    /** "{first} numbers made strings", the form [count] chooses */
    fun tagUnwritable(count: Int, first: Int): String

    val lossDatesTitle: String

    /** "{first} is a date in the {second} and a string in the {third}: converting back gives a string." */
    fun lossDatesOne(first: String, second: String, third: String): String

    /**
     * "{first} and {second} more dates are strings in the {third}: converting back gives strings.", the form [count]
     * chooses
     */
    fun lossDatesMany(count: Int, first: String, second: Int, third: String): String

    /** "{first} dates made strings", the form [count] chooses */
    fun tagDates(count: Int, first: Int): String

    val lossMovedTitle: String

    /** "{first} writes a level’s own values first: {second} moves up, and converting back keeps it there." */
    fun lossMovedOne(first: String, second: String): String

    /**
     * "{first} writes a level’s own values first: {second} and {third} more keys move up, and converting back keeps
     * them there.", the form [count] chooses
     */
    fun lossMovedMany(count: Int, first: String, second: String, third: Int): String

    /** "{first} keys moved", the form [count] chooses */
    fun tagMoved(count: Int, first: Int): String

    val lossEmptiedTitle: String

    /**
     * "{first} holds nothing, and {second} cannot write an empty object or array as it is: converting back does not
     * give it so."
     */
    fun lossEmptiedOne(first: String, second: String): String

    /**
     * "{first} and {second} more values hold nothing, and {third} cannot write an empty object or array as it is:
     * converting back does not give them so.", the form [count] chooses
     */
    fun lossEmptiedMany(count: Int, first: String, second: Int, third: String): String

    /** "{first} empty values lost", the form [count] chooses */
    fun tagEmptied(count: Int, first: Int): String

    val lossOneLineTitle: String

    /** "{first} has line breaks, and {second} writes a value on one line: they are written as \n." */
    fun lossOneLineOne(first: String, second: String): String

    /**
     * "{first} and {second} more values have line breaks, and {third} writes a value on one line: they are written
     * as \n.", the form [count] chooses
     */
    fun lossOneLineMany(count: Int, first: String, second: Int, third: String): String

    /** "{first} values put on one line", the form [count] chooses */
    fun tagOneLine(count: Int, first: Int): String

    val lossItemsTitle: String

    /** "{first} is an array in an array, which has no name in XML: its items are written as item elements." */
    fun lossItemsOne(first: String): String

    /**
     * "{first} and {second} more are arrays in arrays, which have no name in XML: their items are written as item
     * elements.", the form [count] chooses
     */
    fun lossItemsMany(count: Int, first: String, second: Int): String

    /** "{first} arrays named", the form [count] chooses */
    fun tagItems(count: Int, first: Int): String
}

/** [ConvertStrings] in the theme's language. */
internal val convertStrings: ConvertStrings
    @Composable
    @ReadOnlyComposable
    get() = inTheLanguage(ConvertStringsEnglish, ConvertStringsTurkish)

/** [ConvertStrings] in the language whose tag is [language], away from a screen. */
internal fun convertStringsIn(language: String): ConvertStrings = inLanguage(language, ConvertStringsEnglish, ConvertStringsTurkish)
