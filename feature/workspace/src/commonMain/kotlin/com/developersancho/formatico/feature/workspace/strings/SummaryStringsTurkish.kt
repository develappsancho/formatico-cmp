package com.developersancho.formatico.feature.workspace.strings

/** [SummaryStrings] in Turkish. */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal object SummaryStringsTurkish : SummaryStrings {
    override val titleSize = "Boyut"
    override val titleValues = "Değerler"
    override val titleFound = "Değerlerde bulunanlar"
    override val titleNotes = "Notlar"
    override val copy = "Özeti metin olarak kopyala"
    override val label = "Özet"
    override val copied = "Özet kopyalandı"
    override val copyFailed = "Özet kopyalanamadı"
    override val text = "Metin"

    override fun textDetail(first: String) = "$first"

    override val minified = "Küçültülmüş"

    override fun minifiedShare(first: String) = "Metnin $first kadarı"

    override val notMinified = "Küçültülmüş biçimi yok"
    override val compressed = "Sıkıştırılmış"
    override val compressedMinified = "Küçültülmüş metin, gzip"
    override val compressedText = "Metin, gzip"
    override val measuring = "Ölçülüyor"
    override val values = "Değerler"

    override fun valuesIn(first: String, second: String) = "$first ve $second içinde"

    override val keys = "Anahtarlar"
    override val keysNone = "Hiçbir nesnede yinelenen yok"
    override val keysNoneShort = "Yinelenen yok"
    override val deepest = "En derin"
    override val levelsWord = "Düzey"
    override val longestArray = "En uzun dizi"
    override val noArray = "Dizi yok"
    override val longestText = "En uzun metin"
    override val noText = "Metin yok"
    override val none = "Yok"
    override val numbers = "Sayılar"
    override val numbersKept = "Hepsi yazıldığı gibi"
    override val kindText = "Metin"
    override val kindObject = "Nesne"
    override val kindNumber = "Sayı"
    override val kindArray = "Dizi"
    override val kindBoolean = "Mantıksal"
    override val kindNull = "Null"
    override val kindAlias = "Takma ad"
    override val and = " ve "

    override fun tableNote(first: String, second: String, third: String) =
        "$first: $second, hepsinde aynı $third var; bu yüzden tablo olarak okunuyor."

    override val showTable = "Tablo olarak göster"

    override fun objects(count: Int, first: Int) = "$first nesne"

    override fun arrays(count: Int, first: Int) = "$first dizi"

    override fun keysRepeated(count: Int, first: Int) = "Bir nesnede $first yinelenen"

    override fun keysRepeatedShort(count: Int, first: Int) = "$first yinelenen"

    override fun levels(count: Int, first: Int) = "$first düzey"

    override fun items(count: Int, first: Int) = "$first öğe"

    override fun characters(count: Int, first: Int) = "$first karakter"

    override fun kinds(count: Int, first: Int) = "Türüne göre $first değer"

    override fun dates(count: Int, first: Int) = "$first tarih"

    override fun pictures(count: Int, first: Int) = "$first resim"

    override fun links(count: Int, first: Int) = "$first bağlantı"

    override fun colours(count: Int, first: Int) = "$first renk"

    override fun more(count: Int, first: Int) = "$first tane daha"

    override fun keysCount(count: Int, first: Int) = "$first anahtar"

    override fun respelled(count: Int, first: String, second: String) =
        if (count == 1) {
            "$first burada yazıldığı gibi kalıyor. Onu sayı olarak okuyan bir araç $second yazar."
        } else {
            "$first burada yazıldığı gibi kalıyor. Onları sayı olarak okuyan bir araç $second yazar."
        }
}
