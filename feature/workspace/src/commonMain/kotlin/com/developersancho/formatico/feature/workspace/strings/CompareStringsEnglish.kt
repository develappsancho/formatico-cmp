package com.developersancho.formatico.feature.workspace.strings

/** [CompareStrings] in English. */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal object CompareStringsEnglish : CompareStrings {
    override val a = "A"
    override val b = "B"

    override fun bMenu(first: String) = "B: $first. Choose another document"

    override val by = "Compare by"
    override val byMeaning = "Meaning"
    override val byText = "Text"

    override fun changed(count: Int, first: Int) = "$first changed"

    override fun added(count: Int, first: Int) = "$first added"

    override fun removed(count: Int, first: Int) = "$first removed"

    override val stop = "Stop comparing"

    override fun differences(count: Int, first: Int) = if (count == 1) "$first difference" else "$first differences"

    override val noDifferences = "No differences"
    override val sameMeaning = "A and B mean the same."
    override val sameText = "A and B are the same text."
    override val changeChanged = "Changed"
    override val changeAdded = "Added"
    override val changeRemoved = "Removed"

    override fun what(first: String, second: String) = "$first → $second"

    override fun line(first: Int) = "Line $first"

    override fun lines(first: Int, second: Int) = "Lines $first–$second"

    override val notCounted = "Not counted as differences"

    override fun movedPair(first: String, second: String) = "The order of the keys: $first and $second changed places."

    override fun moved(count: Int, first: String, second: Int) =
        if (count == 1) {
            "The order of the keys: $first moved."
        } else {
            "The order of the keys: $first and $second more moved."
        }

    override fun respelledAlone(first: String, second: String) = "The spelling of the numbers: $first and $second are the same number."

    override fun respelledAfter(first: String, second: String) = "And $first and $second, which are the same number."

    override fun respelledMore(count: Int, first: String, second: String, third: Int) =
        "And $first and $second, which are the same number, and $third more."

    override fun notCountedBoth(first: String, second: String) = "$first $second"

    override fun differenceOf(first: Int, second: Int) = "Difference $first of $second"

    override val byMeaningNote = "Compared by meaning: key order and number spelling are not differences"
    override val byTextNote = "Compared by text: every character counts"

    override fun sideShort(first: String, second: String) = "$first · $second"

    override val title = "Compare"
    override val show = "Show"

    override fun showIn(first: String) = "Show $first in A and B"

    override val inA = "In A"
    override val inB = "In B"
    override val notThere = "not there"
    override val aloneTitle = "Nothing to compare with"
    override val aloneBody = "Open another document in a new tab, and compare this one with it."

    override fun unreadTitle(first: String) = "$first has nothing to compare by meaning"

    override val unreadBody = "It is plain text, or none of it could be read: compare it by text instead."

    override fun invalidTitle(count: Int, first: String, second: Int) =
        if (count == 1) {
            "$first has an error"
        } else {
            "$first has $second errors"
        }

    override val invalidBody = "Repair it to compare by meaning, or compare by text."
}
