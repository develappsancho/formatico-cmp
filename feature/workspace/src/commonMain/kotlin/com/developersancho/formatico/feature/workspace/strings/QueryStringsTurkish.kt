package com.developersancho.formatico.feature.workspace.strings

/** [QueryStrings] in Turkish. */
internal object QueryStringsTurkish : QueryStrings {
    override val recent = "Son kullanılanlar"
    override val askJsonpath = "Bir JSONPath yaz, örneğin \$.items[*].name"
    override val askXpath = "Bir XPath yaz, örneğin //item/title"
    override val jsonpathElsewhere = "JSONPath, JSON belgeleri içindir."
    override val xpathElsewhere = "XPath, XML belgeleri içindir."

    override fun statusAndNote(first: String, second: String) = "$first. $second"

    override val unread = "Belge okunamadı; sorgulanacak bir şey yok."
    override val computed = "XPath tek bir değer döndürüyor"
    override val noMatches = "Eşleşme yok"

    override fun matches(count: Int, first: Int) = "$first eşleşme"

    override val value = "Değer"

    override fun matchOf(first: Int, second: Int) = "Eşleşme $first / $second"

    override fun errorAt(first: String, second: Int) = "$first ($second. karakter)"

    override val copyResults = "Sonuçları kopyala"
    override val openResults = "Belge olarak aç"

    // Shorter than the board's, as the English one (PANTRIX_FINDINGS, "CodeEditor, bir uygulamanın verdiği aralıkları
    // vurgulayamıyor").
    override val chooseHint = "Bir eşleşmeyi seçmek imleci ona götürür."
    override val seeInText = "Metinde görmek için birini seç"
    override val resultsLabel = "Sorgu sonuçları"
    override val resultsCopied = "Sonuçlar kopyalandı"
    override val resultsCopyFailed = "Sonuçlar kopyalanamadı"
    override val errorEmpty = "Sorgu boş"
    override val errorUnexpectedEnd = "Sorgu erken bitiyor"
    override val errorUnexpectedCharacter = "Bu burada olamaz"
    override val errorMissingRoot = "JSONPath \$ ile başlar"
    override val errorUnclosedString = "Bu dizgi hiç kapanmıyor"
    override val errorInvalidEscape = "Bu kaçış JSONPath’te yok"
    override val errorInvalidNumber = "Bu sayı JSONPath’in yazdığı gibi yazılmamış"
    override val errorIntegerOutOfRange = "Bu sayı bir indis için fazla büyük"
    override val errorNotSingular = "Bu birden fazla değer bulabilir; burada tek bir değer karşılaştırılır"
    override val errorWrongType = "Bu burada olamaz: türü uygun değil"
    override val errorUnknownFunction = "Bu adda bir işlev yok"
    override val errorWrongArgumentCount = "Bu işlev farklı sayıda argüman alır"
    override val errorUnknownAxis = "Bu adda bir eksen yok"
    override val errorUnsupported = "Formatico’nun XPath’inde değişken ya da ad alanı ekseni yok"
    override val errorTooDeep = "Sorgu fazla iç içe"
}
