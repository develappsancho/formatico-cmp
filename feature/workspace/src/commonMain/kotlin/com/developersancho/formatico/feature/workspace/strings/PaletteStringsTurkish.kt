package com.developersancho.formatico.feature.workspace.strings

/** [PaletteStrings] in Turkish. */
internal object PaletteStringsTurkish : PaletteStrings {
    override val groupDocument = "Belge"
    override val groupView = "Görünüm"
    override val groupConvert = "Dönüştürme"
    override val format = "Belgeyi biçimlendir"
    override val formatKeywords = "güzelleştir, girinti, pretty, beautify"
    override val minifyKeywords = "sıkıştır, tek satır, compact"
    override val sortKeywords = "sıra, alfabe, order"
    override val compare = "Başka bir belgeyle karşılaştır"
    override val compareKeywords = "fark, diff"
    override val tree = "Ağacı göster"
    override val table = "Tabloyu göster"
    override val queryJsonpath = "JSONPath ile sorgula"
    override val queryXpath = "XPath ile sorgula"
    override val queryKeywords = "jsonpath, xpath, süz, filtre"

    override fun convertTo(first: String) = "$first biçimine dönüştür"
}
