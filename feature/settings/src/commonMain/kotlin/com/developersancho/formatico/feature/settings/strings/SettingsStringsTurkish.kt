package com.developersancho.formatico.feature.settings.strings

/** [SettingsStrings] in Turkish. */
internal object SettingsStringsTurkish : SettingsStrings {
    override val title = "Ayarlar"

    override fun backTo(first: String) = "Geri dön: $first"

    override val general = "Genel"
    override val editor = "Editör"
    override val formats = "Biçimler"
    override val history = "Geçmiş"
    override val shortcuts = "Klavye kısayolları"
    override val about = "Formatico hakkında"
    override val theme = "Tema"
    override val themeBody = "Açık, koyu ya da sistemdeki gibi."
    override val themeLight = "Açık"
    override val themeDark = "Koyu"
    override val themeSystem = "Sistemdeki gibi"
    override val language = "Dil"
    override val languageBody = "Formatico’nun konuştuğu dil. Belgelerine dokunulmaz."
    override val languageSystem = "Sistemdeki gibi"
    override val indentation = "Girinti"
    override val indentationBody = "Biçimlendir’in her düzeyi girintilemek için kullandığı karakter."
    override val indentTwo = "2 boşluk"
    override val indentFour = "4 boşluk"
    override val indentTabs = "Sekme"
    override val wrapLines = "Uzun satırları kaydır"
    override val wrapLinesBody = "Pencereye sığmayan satır alt satırda sürer."
    override val sortKeys = "Biçimlendirirken anahtarları sırala"
    override val sortKeysBody = "Kapalıyken Biçimlendir, anahtarları yazıldıkları sırada bırakır."
    override val unknown = "Biçim anlaşılamayınca"
    override val unknownBody = "Süslü ayracı, etiketi, anahtarı ya da sütunu olmayan düz metin."
    override val unknownAsk = "Bana sor"
    override val unknownJson = "JSON olarak oku"
    override val unknownText = "Metin olarak bırak"
    override val keepHistory = "Belgelerin geçmişini tut"
    override val keepHistoryShort = "Geçmişi tut"

    override fun historyKept(count: Int, first: Int, second: String) = "Yalnızca bu cihazda: $first belge, $second."

    override fun historyKeptPhone(count: Int, first: Int, second: String) = "Yalnızca bu telefonda: $first belge, $second."

    override val historyEmpty = "Yalnızca bu cihazda: henüz bir şey yok."
    override val historyEmptyPhone = "Yalnızca bu telefonda: henüz bir şey yok."
    override val historyOff = "Hiçbir şey tutulmuyor."
    override val keepFor = "Her belgeyi tutma süresi"
    override val keepForBody = "Daha eskiler geçmişten kendiliğinden çıkar."
    override val keep7 = "7 gün"
    override val keep30 = "30 gün"
    override val keepAlways = "Ben temizleyene kadar"
    override val clear = "Geçmişi temizle"
    override val clearBody = "Her belgeyi geçmişten çıkarır. Açık sekmeler kalır."
    override val clearBodyPhone = "Açık belgeler kalır."
    override val clearShort = "Temizle"
    override val shortcutsBody =
        "Her biri, komut paletinde aynı adı taşıyan komutu çalıştırır. Windows’ta ve Linux’ta ⌘ yerine Ctrl kullanılır."

    override fun version(first: String) = "Formatico $first"

    override fun builtWith(first: String) = "Pantrix UI $first ile yapıldı. Yazı tipleri: Geist ve Geist Mono."

    override val licences = "Açık kaynak lisansları"
    override val getTheApp = "Uygulamayı indir"
    override val getTheAppBody = "macOS, Windows ve Android için Formatico, GitHub'dan."
    override val licencesBody = "Formatico bunların üstüne kurulu; her biri kendi lisansıyla kullanılıyor. Okumak için birine bas."
    override val licencesBodyPhone = "Formatico bunların üstüne kurulu; her biri kendi lisansıyla kullanılıyor. Okumak için birine dokun."

    override fun inTheWebApp(first: String) = "$first · web uygulamasında"
}
