package com.developersancho.formatico.feature.workspace.strings

/** [PaletteStrings] in English. */
internal object PaletteStringsEnglish : PaletteStrings {
    override val groupDocument = "Document"
    override val groupView = "View"
    override val groupConvert = "Convert"
    override val format = "Format document"
    override val formatKeywords = "pretty, indent, beautify"
    override val minifyKeywords = "compact, one line"
    override val sortKeywords = "order, alphabet"
    override val compare = "Compare with another document"
    override val compareKeywords = "diff"
    override val tree = "Show the tree"
    override val table = "Show the table"
    override val queryJsonpath = "Query with JSONPath"
    override val queryXpath = "Query with XPath"
    override val queryKeywords = "jsonpath, xpath, filter"

    override fun convertTo(first: String) = "Convert to $first"
}
