package com.developersancho.formatico.feature.workspace.strings

/** [TypesStrings] in Turkish. */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal object TypesStringsTurkish : TypesStrings {
    override val types = "Tipler"
    override val language = "Dil"

    override fun fromIn(first: String) = "$first belgesinin tipleri, dil"

    override fun languageMenu(first: String) = "Tipleri $first dilinde yaz. Başka bir dil seç"

    override val rootType = "Kök tip"
    override val rootTypeName = "Kök tipin adı"

    override fun copy(first: String) = "$first kodunu kopyala"

    override fun saveAs(first: String) = "$first olarak kaydet"

    override fun title(first: String) = "$first tipleri"

    override fun written(count: Int, first: Int, second: String) = "$second belgesinden $first tip"

    override fun sizes(count: Int, first: String, second: String, third: String, fourth: Int) =
        "$first $second → $third: $count tip, $fourth satır"

    override val label = "Tipler"
    override val copied = "Tipler kopyalandı"
    override val copyFailed = "Tipler kopyalanamadı"

    override fun invalid(count: Int, first: String, second: Int) =
        if (count == 1) {
            "$first belgesinde 1 hata var. Tiplerini yazmak için onu onar."
        } else {
            "$first belgesinde $second hata var. Tiplerini yazmak için onları onar."
        }

    override val untypedTitle = "Yazılacak tip yok"

    override fun untyped(first: String) = "$first belgesinde nesne yok: tek bir değerin ya da bir değer listesinin yazılacak tipi yok."

    override val kotlinLibrary = "kotlinx.serialization için Kotlin data class’ları."
    override val typeScriptLibrary = "TypeScript arayüzleri."
    override val swiftLibrary = "Codable için Swift struct’ları."
    override val goLibrary = "encoding/json için Go struct’ları."
    override val jsonSchemaLibrary = "2020-12 sürümünde bir JSON Schema: aynı biçimdeki belgeler ona göre doğrulanabilir."
    override val swiftGuess = "String?, tahmin diye işaretli"

    override fun twoNames(first: String, second: String) = "$first ve $second"

    override fun moreNames(first: String, second: Int) = "$first ve $second tane daha"

    override fun onlyNullOne(first: String, second: String) = "$first her değerde null, bu yüzden tipi bilinemiyor: $second olarak yazıldı."

    override fun onlyNullMany(first: String, second: String) =
        "$first her değerde null, bu yüzden tipleri bilinemiyor: $second olarak yazıldı."

    override fun mixedOne(first: String, second: String) = "$first birden çok türde değer taşıyor: $second olarak yazıldı."

    override fun mixedMany(first: String, second: String) = "$first birden çok türde değer taşıyor: $second olarak yazıldı."

    override fun emptyOne(first: String, second: String) = "$first her değerde boş: öğeleri $second olarak yazıldı."

    override fun emptyMany(first: String, second: String) = "$first her değerde boş: öğeleri $second olarak yazıldı."

    override fun datesOne(first: String) = "$first dizgi olarak kalıyor."

    override fun datesMany(first: String) = "$first dizgi olarak kalıyor."

    override fun rootList(first: String) = "Belge bir liste: $first, öğelerinden birinin tipi."

    override val groupTypes = "Tipler"

    override fun generate(first: String) = "$first tiplerini yaz"

    override val generateSchema = "Bir JSON Schema yaz"
}
