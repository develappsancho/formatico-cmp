package com.developersancho.formatico.feature.workspace.strings

/** [WorkspaceStrings] in Turkish. */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal object WorkspaceStringsTurkish : WorkspaceStrings {
    override val untitled = "Adsız"
    override val plainText = "Düz metin"

    override val format = "Biçimlendir"
    override val minify = "Küçült"
    override val sortKeys = "Anahtarları sırala"
    override val query = "Sorgu"
    override val convert = "Dönüştür"
    override val compare = "Karşılaştır"
    override val copyDocument = "Belgeyi kopyala"
    override val saveFile = "Dosya olarak kaydet"
    override val moreActions = "Diğer işlemler"
    override val closeDocument = "Belgeyi kapat"
    override val view = "Görünüm"
    override val viewText = "Metin"
    override val viewTree = "Ağaç"
    override val viewTable = "Tablo"
    override val find = "Anahtar ya da değer ara"
    override val findInTree = "Ağaçta ara"
    override val findInText = "Metinde ara"
    override val showSummaryItem = "Özeti göster"

    override fun statusButton(first: String) = "$first. Biçimi değiştir"

    override fun formatButton(first: String) = "Biçim: $first. Biçimi değiştir"

    override fun match(first: String) = "eşleşme $first"

    override val openTo = "Düzey"

    override fun openToLevel(first: Int) = "Ağacı $first. düzeye kadar aç"

    override val openAll = "Tümü"
    override val openAllLevels = "Ağacın tamamını aç"
    override val closeSearch = "Aramayı kapat"
    override val plainTitle = "Düz metnin ağacı yok"
    override val plainBody = "Hangi biçimde yazıldığını seçersen öyle okunur."
    override val emptyTitle = "Belge boş"
    override val emptyBody = "İçinde henüz okunacak bir şey yok."
    override val noValueTitle = "Hiçbir değer okunamadı"
    override val noValueBody = "Belgedeki sorunlar ağacın gösterilmesini engelliyor."

    override fun textOf(first: String) = "$first metni"

    override fun lineColumn(first: Int, second: Int) = "Satır $first, Sütun $second"

    override val statusReading = "Okunuyor…"

    override fun statusValid(first: String) = "Geçerli $first"

    override fun statusProblems(count: Int, first: Int) = "$first sorun"

    override fun lines(count: Int, first: Int) = "$first satır"

    override fun separated(first: String, second: String) = "$first · $second"

    override val encoding = "UTF-8"
    override val indentTabs = "Sekme"

    override fun indentSpaces(count: Int, first: Int) = "$first boşluk"

    override val formatted = "Biçimlendirildi"
    override val minified = "Küçültüldü"

    override fun minifiedComments(count: Int, first: Int) =
        if (count == 1) {
            "Küçültüldü, yorumu silindi"
        } else {
            "Küçültüldü, $first yorumu silindi"
        }

    override val keysSorted = "Anahtarlar sıralandı"
    override val undo = "Geri al"
    override val alreadyFormatted = "Zaten biçimlendirilmiş"
    override val alreadyMinified = "Zaten küçültülmüş"
    override val alreadySorted = "Anahtarları zaten sıralı"
    override val editImpossible = "Formatico yalnızca geçerli bir JSON, JSONC, JSON5 ya da YAML belgesini değiştirir"
    override val aliasBeforeAnchor = "Anahtarlar olduğu gibi bırakıldı: sıralamak bir takma adı çapasının önüne koyardı"
    override val copied = "Belge kopyalandı"
    override val copyFailed = "Belge kopyalanamadı"
    override val valueCopied = "Değer kopyalandı"
    override val valueCopyFailed = "Değer kopyalanamadı"
    override val saveFailed = "Dosya kaydedilemedi"
    override val documentLabel = "Belge"
    override val valueLabel = "Değer"
    override val showInText = "Metinde göster"
    override val copyValue = "Değeri kopyala"
    override val saveCsv = "CSV olarak kaydet…"
    override val tableOut = "Tabloyu kaydet ya da kopyala"
    override val copyCsv = "CSV olarak kopyala"
    override val copyMarkdown = "Markdown tablosu olarak kopyala"
    override val tableLabel = "Tablo"
    override val tableCopied = "Tablo kopyalandı"
    override val tableCopyFailed = "Tablo kopyalanamadı"
    override val copyPath = "Yolu kopyala"

    override fun copyPathOf(first: String) = "$first değerinin yolunu JSONPath, JSON Pointer ya da jq olarak kopyala"

    override fun copyPathAs(first: String) = "$first değerinin yolunu şöyle kopyala"

    override fun copyPathTitle(first: String) = "$first değerinin yolunu kopyala"

    override val pathLabel = "Yol"
    override val pathCopied = "Yol kopyalandı"
    override val pathCopyFailed = "Yol kopyalanamadı"
    override val kindDate = "Tarih"
    override val kindColour = "Renk"
    override val kindLink = "Bağlantı"
    override val kindPicture = "Resim"
    override val kindString = "Dizgi"
    override val kindNumber = "Sayı"
    override val kindBoolean = "Mantıksal"
    override val kindNull = "Null"
    override val kindObject = "Nesne"
    override val kindArray = "Dizi"
    override val kindAlias = "Takma ad"

    override fun colourOf(first: String) = "Renk $first"

    override val pictureAt = "Bu adresteki resim"
    override val openLink = "Bağlantıyı aç"

    override fun aliasDetail(first: String) = "&$first çapasını gösteriyor"

    override fun aliasMissing(first: String) = "Önünde &$first çapası yok"

    override val goToAnchor = "Çapaya git"

    override fun keys(count: Int, first: Int) = "$first anahtar"

    override fun items(count: Int, first: Int) = "$first öğe"

    override fun characters(count: Int, first: Int) = "$first karakter"

    override fun momentUtc(first: String, second: String) = "$first, $second UTC"

    override fun momentLocal(first: String) = "yerel saatle $first"

    override fun dayOfTheWeek(first: String, second: String) = "$first, $second"

    override val today = "bugün"
    override val yesterday = "dün"
    override val tomorrow = "yarın"

    override fun daysAgo(count: Int, first: Int) = "$first gün önce"

    override fun daysAhead(count: Int, first: Int) = "$first gün sonra"

    override fun rows(count: Int, first: Int) = "$first satır"

    override fun rowsOf(count: Int, first: Int, second: Int) = "$second satırdan $first"

    override fun tableColumns(count: Int, first: Int) = "$first sütun"

    override val filterRows = "Satırları filtrele"
    override val columns = "Sütunlar"

    override fun columnsAll(first: Int) = "Sütunlar: $first sütunun hepsi görünüyor. Sütunları seç"

    override fun columnsSome(first: Int, second: Int) = "Sütunlar: $second sütunun $first tanesi görünüyor. Sütunları seç"

    override val valueColumn = "değer"
    override val index = "Dizideki indisi"

    override fun missingKey(first: String) = "Bu nesnede $first anahtarı yok"

    override fun openValue(first: String) = "$first. Aç"

    override fun numbers(count: Int, first: String) = "$first sayı"

    override fun values(count: Int, first: String) = "$first değer"

    override val statMin = "en küçük"
    override val statMax = "en büyük"
    override val statAverage = "ortalama"
    override val statDifferent = "farklı"
    override val statEmpty = "boş"

    override fun sortedBy(first: String, second: String) = "$first sütununa göre sıralı, $second"

    override val newestFirst = "yeniden eskiye"
    override val oldestFirst = "eskiden yeniye"
    override val largestFirst = "büyükten küçüğe"
    override val smallestFirst = "küçükten büyüğe"
    override val aToZ = "A’dan Z’ye"
    override val zToA = "Z’den A’ya"
    override val noTableTitle = "Tablo olarak gösterilecek bir liste yok"
    override val noTableBody = "Tablo bir diziyi gösterir; bu belgede dizi yok."

    override fun noRows(first: String) = "“$first” geçen bir satır yok"

    override val repair = "Onar"
    override val repairAll = "Hepsini onar"
    override val readAsJson5 = "JSON5 olarak oku"

    override fun invalidTitle(first: String) = "Bu geçerli bir $first değil"

    override fun problemWhere(first: Int, second: Int) = "Satır $first, sütun $second"

    override val nextProblem = "Sonraki sorun"
    override val previousProblem = "Önceki sorun"

    override fun viewProblems(first: Int) = "Sorunlar ($first)"

    override val viewProblemsShort = "Sorunlar"

    override fun invalidBody(count: Int, first: Int) =
        if (count == 1) {
            "Formatico okuyabildiğini okudu: ağaç ve tablo onu gösteriyor. Onar, sorunu düzeltir."
        } else {
            "Formatico okuyabildiğini okudu: ağaç ve tablo onu gösteriyor. Onar, $first sorunun hepsini birden düzeltir."
        }

    override fun json5Allows(count: Int, first: Int, second: Int) = "JSON5 $second sorunun $first tanesine izin veriyor."

    override fun notValidCount(count: Int, first: Int, second: String) = "Geçerli bir $second değil: $first sorun"

    override fun problemOf(count: Int, first: Int, second: Int, third: Int, fourth: Int) =
        "Sorun $first / $second · satır $third, sütun $fourth"

    override val problemError = "Hata"
    override val problemWarning = "Uyarı"
    override val viewSummary = "Özet"

    override fun showSummary(first: String) = "$first. Belgenin özetini göster"

    override fun summaryShown(first: String) = "$first. Özet gösteriliyor"

    override val share = "Paylaş"
    override val dropToOpen = "Yeni bir sekmede açmak için bırakın"

    override fun dropSupporting(first: String) =
        "JSON, JSONC, JSON5, NDJSON, XML, YAML, TOML, CSV, TSV, properties ve INI, en çok $first"
}
