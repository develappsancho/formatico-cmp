package com.developersancho.formatico.feature.workspace.strings

/** [ConvertStrings] in Turkish. */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal object ConvertStringsTurkish : ConvertStrings {
    override val to = "Hedef"

    override fun fromTo(first: String) = "$first biçiminden"

    override fun toMenu(first: String) = "$first biçimine dönüştür. Başka bir biçim seç"

    override val indentation = "Girinti"

    override fun copy(first: String) = "$first belgesini kopyala"

    override fun saveAs(first: String) = "$first olarak kaydet"

    override fun title(first: String) = "$first belgesini dönüştür"

    override val nulls = "Null bir değer"
    override val nullsChoice = "Null bir değerin neye dönüşeceği"
    override val nullsLeave = "Dışarıda bırak"
    override val nullsEmpty = "Boş bir dizgi yaz"

    override fun sizes(first: String, second: String, third: String, fourth: String) = "$first $second → $third $fourth"

    override val label = "Dönüştürülen belge"
    override val copied = "Dönüştürülen belge kopyalandı"
    override val copyFailed = "Dönüştürülen belge kopyalanamadı"

    override fun nullComment(first: String, second: String, third: String) = "$first = null ($second); $third biçiminde null yok"

    override fun source(count: Int, first: String, second: Int) = "$first · $second satır"

    override fun invalid(count: Int, first: String, second: Int) =
        if (count == 1) {
            "Bu $first belgesinde bir hata var. Belgeyi dönüştürmek için onu onar."
        } else {
            "Bu $first belgesinde $second hata var. Belgeyi dönüştürmek için onları onar."
        }

    override val nothingLost = "Hiçbir şey kaybolmuyor"

    override fun back(first: String) = "Olduğu gibi bu $first belgesine geri dönüşüyor."

    override fun backWith(first: String, second: String, third: String) =
        "$first; böylece $second belgesi olduğu gibi bu $third belgesine geri dönüşüyor."

    override fun noteQuotedDates(first: String) = "$first biçiminin tarih diye okuyacağı dizgiler tırnak içinde yazıldı"

    override fun noteQuotedValues(first: String) = "$first biçiminin başka bir değer diye okuyacağı dizgiler tırnak içinde yazıldı"

    override fun noteSpellingOne(first: String) = "$first yazılışını koruyor"

    override fun noteSpellingTwo(first: String, second: String) = "$first ve $second yazılışlarını koruyor"

    override fun noteSpellingMore(count: Int, first: String, second: Int) = "$first ve $second sayı daha yazılışlarını koruyor"

    override fun notesAnd(first: String, second: String) = "$first, $second"

    override fun losses(count: Int, first: Int) = "$first kayıp"

    override fun lossNullsTitle(first: String) = "$first biçiminde null yok"

    override fun lossNullsLeftOne(first: String, second: String) =
        "$first, $second belgesinde null. Dışarıda bırakıldı, yerini bir yorum gösteriyor: geri dönüştürmek onu getirmez."

    override fun lossNullsLeftMany(count: Int, first: String, second: Int, third: String) =
        "$first ve $second değer daha $third belgesinde null. Dışarıda bırakıldılar, yerlerini yorumlar gösteriyor: geri " +
            "dönüştürmek onları getirmez."

    override fun lossNullsEmptyOne(first: String, second: String) =
        "$first, $second belgesinde null. Boş bir dizgi olarak yazıldı: geri dönüştürmek bir dizgi verir."

    override fun lossNullsEmptyMany(count: Int, first: String, second: Int, third: String) =
        "$first ve $second değer daha $third belgesinde null. Boş dizgiler olarak yazıldılar: geri dönüştürmek dizgiler verir."

    override fun tagNullsLeft(count: Int, first: Int) = "$first değer dışarıda kaldı"

    override fun tagNullsEmpty(count: Int, first: Int) = "$first null boş yazıldı"

    override val lossCommentsTitle = "Yorumlar geride kalıyor"

    override fun lossComments(count: Int, first: String, second: Int, third: String) =
        if (count == 1) {
            "$first belgesinde bir yorum var; dönüştürme yorumları değil değerleri taşır: $third belgesinde o yok."
        } else {
            "$first belgesinde $second yorum var; dönüştürme yorumları değil değerleri taşır: $third belgesinde onlar yok."
        }

    override fun tagComments(count: Int, first: Int) = "$first yorum geride kaldı"

    override val lossAliasesTitle = "Takma adlar açılıyor"

    override fun lossAliasesOne(first: String) = "$first bir takma ad; gösterdiği değer olarak yazıldı: geri dönüştürmek bir kopya verir."

    override fun lossAliasesMany(count: Int, first: String, second: Int) =
        "$first ve $second takma ad daha gösterdikleri değerler olarak yazıldı: geri dönüştürmek kopyalar verir."

    override fun tagAliases(count: Int, first: Int) = "$first takma ad açıldı"

    override fun lossUntypedTitle(first: String) = "$first biçiminde yalnızca metin var"

    override fun lossUntypedOne(first: String) = "$first metin olarak yazıldı: geri dönüştürmek bir dizgi verir."

    override fun lossUntypedMany(count: Int, first: String, second: Int) =
        "$first ve $second değer daha metin olarak yazıldı: geri dönüştürmek dizgiler verir."

    override fun tagUntyped(count: Int, first: Int) = "$first değer metin oldu"

    override fun lossFlattenedTitle(first: String) = "$first biçiminde tek düzey var"

    override fun lossFlattenedOne(first: String) = "$first ayrı bir anahtar oldu: geri dönüştürmek onu tek düzeyde verir."

    override fun lossFlattenedMany(count: Int, first: String, second: Int) =
        "$first ve $second değer daha ayrı anahtarlar oldu: geri dönüştürmek onları tek düzeyde verir."

    override fun tagFlattened(count: Int, first: Int) = "$first değer düzleşti"

    override fun lossWrappedTitle(first: String) = "Belge $first anahtarının altına kondu"

    override fun lossWrapped(first: String, second: String, third: String) =
        "$second belgesinin değeri olduğu gibi bir $first belgesi olamaz: geri dönüştürmek onu $third altında verir."

    override val lossRowTitle = "Belge tek bir satır"
    override val lossLineTitle = "Belge tek bir satır"

    override fun lossInArray(first: String, second: String) =
        "$first biçimi bir liste yazar: geri dönüştürmek $second belgesinin değerini bir dizi içinde verir."

    override fun tagWrapped(first: String) = "$first anahtarının altına kondu"

    override val tagRow = "Tek satır"
    override val tagLine = "Tek satır"
    override val lossRenamedTitle = "Anahtarlar yeniden adlandırılıyor"

    override fun lossRenamedOne(first: String, second: String, third: String) = "$first bir $second adı değil: $third olarak yazıldı."

    override fun lossRenamedMany(count: Int, first: String, second: Int, third: String, fourth: String) =
        "$first ve $second anahtar daha $third adı değil: $first, $fourth olarak yazıldı."

    override fun tagRenamed(count: Int, first: Int) = "$first anahtar yeniden adlandırıldı"

    override val lossDuplicatesTitle = "İki kez verilen anahtarlar"

    override fun lossDuplicatesOne(first: String, second: String) =
        "$first iki kez verilmiş, $second biçimi bir anahtarı bir kez tutar: sonuncusu kaldı."

    override fun lossDuplicatesMany(count: Int, first: String, second: Int, third: String) =
        "$first ve $second anahtar daha iki kez verilmiş, $third biçimi bir anahtarı bir kez tutar: her birinin sonuncusu kaldı."

    override fun tagDuplicates(count: Int, first: Int) = "$first anahtar atıldı"

    override val lossRespelledTitle = "Sayıların yazılışı değişiyor"

    override fun lossRespelledOne(first: String, second: String, third: String) =
        "$first, $third biçiminin yazdığı gibi $second olarak yazıldı: geri dönüştürmek bu yazılışı korur."

    override fun lossRespelledMany(count: Int, first: String, second: Int, third: String, fourth: String) =
        "$first ve $second sayı daha $third biçiminin yazdığı gibi yazıldı, $first $fourth olarak: geri dönüştürmek bu yazılışı korur."

    override fun tagRespelled(count: Int, first: Int) = "$first sayının yazılışı değişti"

    override val lossUnwritableTitle = "Sayılar dizgi oluyor"

    override fun lossUnwritableOne(first: String, second: String) = "$first için $second biçiminde bir değer yok: dizgi olarak yazıldı."

    override fun lossUnwritableMany(count: Int, first: String, second: Int, third: String) =
        "$first ve $second sayı daha için $third biçiminde değer yok: dizgi olarak yazıldılar."

    override fun tagUnwritable(count: Int, first: Int) = "$first sayı dizgi oldu"

    override val lossDatesTitle = "Tarihler dizgi oluyor"

    override fun lossDatesOne(first: String, second: String, third: String) =
        "$first, $second belgesinde bir tarih, $third belgesinde bir dizgi: geri dönüştürmek bir dizgi verir."

    override fun lossDatesMany(count: Int, first: String, second: Int, third: String) =
        "$first ve $second tarih daha $third belgesinde dizgi: geri dönüştürmek dizgiler verir."

    override fun tagDates(count: Int, first: Int) = "$first tarih dizgi oldu"

    override val lossMovedTitle = "Anahtarlar yukarı çıkıyor"

    override fun lossMovedOne(first: String, second: String) =
        "$first biçimi bir düzeyin kendi değerlerini önce yazar: $second yukarı çıktı, geri dönüştürmek onu orada tutar."

    override fun lossMovedMany(count: Int, first: String, second: String, third: Int) =
        "$first biçimi bir düzeyin kendi değerlerini önce yazar: $second ve $third anahtar daha yukarı çıktı, geri dönüştürmek " +
            "onları orada tutar."

    override fun tagMoved(count: Int, first: Int) = "$first anahtar yer değiştirdi"

    override val lossEmptiedTitle = "Boş değerler kayboluyor"

    override fun lossEmptiedOne(first: String, second: String) =
        "$first bir şey tutmuyor, $second biçimi boş bir nesneyi ya da diziyi olduğu gibi yazamaz: geri dönüştürmek onu öyle vermez."

    override fun lossEmptiedMany(count: Int, first: String, second: Int, third: String) =
        "$first ve $second değer daha bir şey tutmuyor, $third biçimi boş bir nesneyi ya da diziyi olduğu gibi yazamaz: geri " +
            "dönüştürmek onları öyle vermez."

    override fun tagEmptied(count: Int, first: Int) = "$first boş değer kayboldu"

    override val lossOneLineTitle = "Değerler tek satıra iniyor"

    override fun lossOneLineOne(first: String, second: String) =
        "$first satır sonları içeriyor, $second biçimi bir değeri tek satıra yazar: satır sonları \\n olarak yazıldı."

    override fun lossOneLineMany(count: Int, first: String, second: Int, third: String) =
        "$first ve $second değer daha satır sonları içeriyor, $third biçimi bir değeri tek satıra yazar: satır sonları \\n olarak yazıldı."

    override fun tagOneLine(count: Int, first: Int) = "$first değer tek satıra indi"

    override val lossItemsTitle = "Öğeler item adını alıyor"

    override fun lossItemsOne(first: String) =
        "$first bir dizinin içinde bir dizi; XML biçiminde onun adı yok: elemanları item öğeleri olarak yazıldı."

    override fun lossItemsMany(count: Int, first: String, second: Int) =
        "$first ve $second değer daha dizilerin içinde diziler; XML biçiminde onların adı yok: elemanları item öğeleri olarak yazıldı."

    override fun tagItems(count: Int, first: Int) = "$first dizi adlandırıldı"
}
