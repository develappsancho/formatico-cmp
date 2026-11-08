package com.developersancho.formatico.feature.workspace.strings

/** [GraphStrings] in Turkish. */
internal object GraphStringsTurkish : GraphStrings {
    override val graph = "Grafik"

    override fun objectCard(count: Int, first: String, second: Int) = "$first, $second anahtarlı bir nesne"

    override fun arrayCard(count: Int, first: String, second: Int) = "$first, $second öğeli bir dizi"

    override fun more(count: Int, first: Int) = "ve $first tane daha"

    override val showGraph = "Grafiği göster"
}
