package com.developersancho.formatico.feature.workspace.strings

/** [TableStrings] in English. */
internal object TableStringsEnglish : TableStrings {
    override val typeText = "text"
    override val typeDate = "date"
    override val typeNumber = "number"
    override val typeBoolean = "boolean"
    override val typeObject = "object"
    override val typeArray = "array"
    override val typeAlias = "alias"
    override val typeNull = "null"
    override val firstRowHeader = "First row is the header"
    override val firstRowHeaderOff = "Off, the first row is a record, and the columns are named column 1, column 2…"

    override fun columnAt(place: Int) = "column $place"
}
