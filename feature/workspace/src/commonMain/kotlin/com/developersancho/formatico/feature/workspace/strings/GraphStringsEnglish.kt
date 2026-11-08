package com.developersancho.formatico.feature.workspace.strings

/** [GraphStrings] in English. */
internal object GraphStringsEnglish : GraphStrings {
    override val graph = "Graph"

    override fun objectCard(count: Int, first: String, second: Int) =
        if (count == 1) "$first, an object of 1 key" else "$first, an object of $second keys"

    override fun arrayCard(count: Int, first: String, second: Int) =
        if (count == 1) "$first, a list of 1 item" else "$first, a list of $second items"

    override fun more(count: Int, first: Int) = "and $first more"

    override val showGraph = "Show the graph"
}
